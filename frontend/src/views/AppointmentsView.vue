<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError } from '../api/auth'
import { appointmentApi, type PatientAppointment } from '../api/appointment'
import { session } from '../stores/session'

const items = ref<PatientAppointment[]>([])
const loading = ref(false)
const errorMessage = ref('')

async function loadAppointments() {
  if (!session.token) return
  loading.value = true
  errorMessage.value = ''
  try {
    items.value = await appointmentApi.listMine(session.token)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '预约记录加载失败'
  } finally { loading.value = false }
}

onMounted(loadAppointments)
</script>

<template>
  <section class="page-section">
    <p class="eyebrow">MY APPOINTMENTS</p>
    <h1>我的预约</h1>
    <p v-if="errorMessage" class="alert">{{ errorMessage }}</p>
    <div v-if="loading" class="department-empty">正在加载预约记录…</div>
    <div v-else-if="items.length" class="appointment-list">
      <article v-for="item in items" :key="item.id" class="appointment-card">
        <div class="date-block"><b>{{ item.scheduleDate.slice(5) }}</b><small>{{ item.startTime.slice(0, 5) }}</small></div>
        <div><h3>{{ item.doctorName }} <span>{{ item.departmentName }}</span></h3><p>{{ item.appointmentNo }} · {{ item.sessionName }}</p><small>{{ item.startTime.slice(0, 5) }}–{{ item.endTime.slice(0, 5) }} · 挂号费 ¥10（模拟）</small></div>
        <span class="status confirmed">已预约</span>
      </article>
    </div>
    <div v-else class="department-empty">暂时没有预约记录</div>
  </section>
</template>
