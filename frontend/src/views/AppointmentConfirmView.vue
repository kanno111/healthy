<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '../api/auth'
import { appointmentApi } from '../api/appointment'
import { patientResourceApi, type PatientDoctor, type PatientScheduleSlot } from '../api/patient-resource'
import { session } from '../stores/session'

const route = useRoute()
const router = useRouter()
const doctorId = Number(route.query.doctorId)
const slotId = Number(route.query.slotId)
const doctor = ref<PatientDoctor | null>(null)
const slot = ref<PatientScheduleSlot | null>(null)
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const alreadyBooked = ref(false)
const fee = 10
const requestId = crypto.randomUUID()

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const canSubmit = computed(() => doctor.value && slot.value && slot.value.remainingCapacity > 0 && !alreadyBooked.value && !submitting.value)

async function loadConfirmation() {
  if (!session.token || !Number.isInteger(doctorId) || !Number.isInteger(slotId)) {
    errorMessage.value = '预约信息无效，请返回医生页面重新选择时段'
    return
  }
  loading.value = true
  errorMessage.value = ''
  const startDate = new Date()
  startDate.setHours(0, 0, 0, 0)
  const endDate = new Date(startDate)
  endDate.setDate(endDate.getDate() + 13)
  try {
    const [doctorData, slots, appointments] = await Promise.all([
      patientResourceApi.getDoctor(session.token, doctorId),
      patientResourceApi.listScheduleSlots(session.token, doctorId, formatDate(startDate), formatDate(endDate)),
      appointmentApi.listMine(session.token)
    ])
    doctor.value = doctorData
    slot.value = slots.find((item) => item.id === slotId) ?? null
    alreadyBooked.value = appointments.some((item) => item.scheduleSlotId === slotId && item.status === 'BOOKED')
    if (!slot.value) errorMessage.value = '该号源已不可预约，请返回重新选择'
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '预约信息加载失败'
  } finally { loading.value = false }
}

async function submitAppointment() {
  if (!session.token || !slot.value || submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await appointmentApi.create(session.token, slot.value.id, requestId)
    await router.replace('/appointments')
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '预约创建失败，请稍后重试'
    submitting.value = false
  }
}

onMounted(loadConfirmation)
</script>

<template>
  <section class="page-section confirm-page">
    <p class="crumb">预约挂号 / 确认预约</p>
    <p class="eyebrow">CONFIRM APPOINTMENT</p>
    <h1>确认预约信息</h1>
    <p v-if="errorMessage" class="alert">{{ errorMessage }}</p>
    <div v-if="loading" class="department-empty">正在加载预约信息…</div>
    <section v-else-if="doctor && slot" class="confirm-card">
      <p v-if="alreadyBooked" class="alert">您已挂过该班次，请勿重复预约。</p>
      <div class="confirm-row"><span>就诊医生</span><b>{{ doctor.name }} · {{ doctor.title || '医师' }}</b></div>
      <div class="confirm-row"><span>就诊科室</span><b>{{ doctor.departmentName }}</b></div>
      <div class="confirm-row"><span>就诊日期</span><b>{{ slot.scheduleDate }}</b></div>
      <div class="confirm-row"><span>就诊时段</span><b>{{ slot.sessionName }} · {{ slot.startTime.slice(0, 5) }}–{{ slot.endTime.slice(0, 5) }}</b></div>
      <div class="confirm-row"><span>挂号费</span><b>¥{{ fee }}（模拟）</b></div>
      <div class="confirm-row"><span>就诊患者</span><b>{{ session.name }}（模拟患者本人）</b></div>
      <p class="confirm-tip">提交后将生成预约记录，并扣减该班次一个剩余号源。</p>
      <div class="confirm-actions"><button class="secondary" @click="router.back()">返回修改</button><button v-if="alreadyBooked" class="primary" @click="router.push('/appointments')">查看我的预约</button><button v-else class="primary" :disabled="!canSubmit" @click="submitAppointment">{{ submitting ? '提交中…' : '确认预约' }}</button></div>
    </section>
  </section>
</template>
