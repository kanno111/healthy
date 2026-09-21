<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError } from '../api/auth'
import { waitlistApi, type PatientAppointmentWaitlist } from '../api/waitlist'
import { session } from '../stores/session'

const items = ref<PatientAppointmentWaitlist[]>([])
const loading = ref(false)
const errorMessage = ref('')
const cancellingId = ref<number | null>(null)
const confirmingId = ref<number | null>(null)

const statusLabel: Record<PatientAppointmentWaitlist['status'], string> = {
  WAITING: '排队中',
  OFFERED: '已获得号源，等待确认',
  CONFIRMED: '候补成功',
  EXPIRED: '候补资格已过期',
  CANCELLED: '已取消'
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

async function loadWaitlists() {
  if (!session.token) return
  loading.value = true
  errorMessage.value = ''
  try {
    items.value = await waitlistApi.listMine(session.token)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '候补记录加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadWaitlists)

async function cancelWaitlist(item: PatientAppointmentWaitlist) {
  if (!session.token || item.status !== 'WAITING' || cancellingId.value !== null) return
  if (!window.confirm('确认取消该候补吗？')) return

  cancellingId.value = item.id
  errorMessage.value = ''
  try {
    await waitlistApi.cancel(session.token, item.id)
    await loadWaitlists()
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '取消候补失败'
  } finally {
    cancellingId.value = null
  }
}

async function confirmWaitlist(item: PatientAppointmentWaitlist) {
  if (!session.token || item.status !== 'OFFERED' || confirmingId.value !== null) return
  if (!window.confirm('确认使用候补号源并完成挂号吗？')) return

  confirmingId.value = item.id
  errorMessage.value = ''
  try {
    await waitlistApi.confirm(session.token, item.id)
    await loadWaitlists()
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '确认候补失败'
  } finally {
    confirmingId.value = null
  }
}
</script>

<template>
  <section class="page-section">
    <p class="eyebrow">MY WAITLISTS</p>
    <h1>我的候补</h1>
    <p class="tip">获得号源后请在过期前确认挂号；超时后系统会继续通知下一位候补患者。</p>
    <p v-if="errorMessage" class="alert">{{ errorMessage }}</p>
    <div v-if="loading" class="department-empty">正在加载候补记录…</div>
    <div v-else-if="items.length" class="appointment-list">
      <article v-for="item in items" :key="item.id" class="appointment-card">
        <div class="date-block"><b>{{ item.scheduleDate.slice(5) }}</b><small>{{ item.startTime.slice(0, 5) }}</small></div>
        <div>
          <h3>{{ item.doctorName }} <span>{{ item.departmentName }}</span></h3>
          <p>{{ item.sessionName }} · {{ item.startTime.slice(0, 5) }}–{{ item.endTime.slice(0, 5) }}</p>
          <small v-if="item.status === 'OFFERED'">请在 {{ formatTime(item.offerExpireTime) }} 前确认挂号</small>
          <small v-else>加入候补时间：{{ formatTime(item.createdAt) }}</small>
        </div>
        <div>
          <span class="status" :class="item.status.toLowerCase()">{{ statusLabel[item.status] }}</span>
          <button
            v-if="item.status === 'WAITING'"
            class="table-action"
            :disabled="cancellingId !== null"
            @click="cancelWaitlist(item)"
          >{{ cancellingId === item.id ? '取消中…' : '取消候补' }}</button>
          <button
            v-if="item.status === 'OFFERED'"
            class="primary"
            :disabled="confirmingId !== null"
            @click="confirmWaitlist(item)"
          >{{ confirmingId === item.id ? '确认中…' : '确认挂号' }}</button>
        </div>
      </article>
    </div>
    <div v-else class="department-empty">暂时没有候补记录</div>
  </section>
</template>
