<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '../api/auth'
import { patientResourceApi, type PatientDoctor, type PatientScheduleSlot } from '../api/patient-resource'
import { waitlistApi } from '../api/waitlist'
import { session } from '../stores/session'

const route = useRoute()
const router = useRouter()
const doctorId = Number(route.params.id)
const doctor = ref<PatientDoctor | null>(null)
const slots = ref<PatientScheduleSlot[]>([])
const loading = ref(false)
const errorMessage = ref('')
const waitlistMessage = ref('')
const joiningWaitlistSlotId = ref<number | null>(null)

function addDays(source: Date, days: number) {
  const date = new Date(source)
  date.setDate(date.getDate() + days)
  return date
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const startDate = new Date()
startDate.setHours(0, 0, 0, 0)
const endDate = addDays(startDate, 13)
const dateTabs = Array.from({ length: 14 }, (_, index) => {
  const date = addDays(startDate, index)
  return {
    value: formatDate(date),
    dateText: `${date.getMonth() + 1}月${date.getDate()}日`,
    weekday: `周${'日一二三四五六'[date.getDay()]}`
  }
})
const selectedDate = ref(dateTabs[0].value)
const selectedSlots = computed(() => slots.value.filter((slot) => slot.scheduleDate === selectedDate.value))

async function loadDoctorResources() {
  if (!session.token || !Number.isInteger(doctorId)) return
  loading.value = true
  errorMessage.value = ''
  try {
    const [doctorData, slotData] = await Promise.all([
      patientResourceApi.getDoctor(session.token, doctorId),
      patientResourceApi.listScheduleSlots(session.token, doctorId, formatDate(startDate), formatDate(endDate))
    ])
    doctor.value = doctorData
    slots.value = slotData
    selectedDate.value = slotData.find((slot) => slot.remainingCapacity > 0)?.scheduleDate ?? slotData[0]?.scheduleDate ?? dateTabs[0].value
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '医生和号源加载失败'
  } finally { loading.value = false }
}

onMounted(loadDoctorResources)

function goToConfirm(slot: PatientScheduleSlot) {
  if (!doctor.value || slot.remainingCapacity < 1) return
  void router.push({
    path: '/appointment/confirm',
    query: { doctorId: String(doctor.value.id), slotId: String(slot.id) }
  })
}

async function joinWaitlist(slot: PatientScheduleSlot) {
  if (!session.token || slot.remainingCapacity > 0 || joiningWaitlistSlotId.value !== null) return
  if (!window.confirm(`该班次已满，确认加入 ${slot.sessionName} 候补吗？`)) return
  joiningWaitlistSlotId.value = slot.id
  errorMessage.value = ''
  waitlistMessage.value = ''
  try {
    const waitlist = await waitlistApi.join(session.token, slot.id)
    waitlistMessage.value = `已加入候补（记录 #${waitlist.id}），可在“我的候补”中查看。`
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '加入候补失败，请稍后重试'
  } finally {
    joiningWaitlistSlotId.value = null
  }
}
</script>

<template>
  <div class="page-section patient-doctor-page">
    <p class="crumb">预约挂号 / {{ doctor?.departmentName ?? '医生详情' }} / 查看号源</p>
    <p v-if="errorMessage" class="alert">{{ errorMessage }}</p>
    <p v-if="waitlistMessage" class="tip">{{ waitlistMessage }}</p>
    <div v-if="loading" class="patient-resource-loading">正在加载医生与号源…</div>
    <template v-else-if="doctor">
      <section class="doctor-hero">
        <div class="avatar large">{{ doctor.name.slice(0, 1) }}</div>
        <div><p class="eyebrow">{{ doctor.departmentName }}</p><h1>{{ doctor.name }} <span>{{ doctor.title || '医师' }}</span></h1><p>{{ doctor.introduction || '暂无医生简介' }}</p><small>以下号源来自医院排班，剩余数量以提交预约时为准。</small></div>
      </section>
      <section class="schedule patient-schedule">
        <div class="schedule-head"><div><p class="eyebrow">SCHEDULE</p><h2>未来两周号源</h2></div></div>
        <div class="date-tabs patient-date-tabs">
          <button v-for="date in dateTabs" :key="date.value" :class="{ active: selectedDate === date.value }" @click="selectedDate = date.value">{{ date.dateText }}<br><small>{{ date.weekday }}</small></button>
        </div>
        <div v-if="selectedSlots.length" class="slot-group patient-slot-group">
          <h3>{{ selectedDate }}</h3>
          <div v-for="slot in selectedSlots" :key="slot.id" class="slot">
            <div><b>{{ slot.sessionName }} · {{ slot.startTime.slice(0, 5) }}–{{ slot.endTime.slice(0, 5) }}</b><p>总号源 {{ slot.totalCapacity }}，当前剩余 {{ slot.remainingCapacity }}</p></div>
            <span :class="slot.remainingCapacity > 0 ? 'available' : 'full'">{{ slot.remainingCapacity > 0 ? `剩余 ${slot.remainingCapacity} 号` : '已约满' }}</span>
            <button v-if="slot.remainingCapacity > 0" @click="goToConfirm(slot)">预约</button>
            <button v-else class="secondary" :disabled="joiningWaitlistSlotId === slot.id" @click="joinWaitlist(slot)">{{ joiningWaitlistSlotId === slot.id ? '加入中…' : '加入候补' }}</button>
          </div>
        </div>
        <div v-else class="patient-slot-empty">当天暂无开放号源，请选择其他日期</div>
      </section>
    </template>
  </div>
</template>
