<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError, logout as logoutRequest } from '../api/auth'
import { doctorAppointmentApi, type DoctorAppointment, type DoctorAppointmentStatus } from '../api/doctor-appointment'
import { session, signOut } from '../stores/session'

const router = useRouter()
const appointments = ref<DoctorAppointment[]>([])
const status = ref<DoctorAppointmentStatus | ''>('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const completingId = ref<number | null>(null)
const errorMessage = ref('')
const today = (() => {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10)
})()
const pageSummary = computed(() => total.value ? `共 ${total.value} 条` : '暂无预约')

async function loadAppointments() {
  if (!session.token) return
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await doctorAppointmentApi.pageMine(session.token, {
      scheduleDate: today, status: status.value || undefined, page: page.value, pageSize
    })
    appointments.value = result.records
    total.value = result.total
    totalPages.value = result.totalPages
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '预约加载失败'
  } finally { loading.value = false }
}

async function changeStatus() { page.value = 1; await loadAppointments() }
async function changePage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value) return
  page.value = nextPage
  await loadAppointments()
}
async function completeAppointment(item: DoctorAppointment) {
  if (!session.token || item.status !== 'BOOKED' || completingId.value !== null) return
  if (!window.confirm(`确认完成预约 ${item.appointmentNo} 的就诊吗？`)) return
  completingId.value = item.id
  errorMessage.value = ''
  try {
    await doctorAppointmentApi.complete(session.token, item.id)
    await loadAppointments()
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '完成就诊失败'
  } finally { completingId.value = null }
}
function logout() {
  const token = session.token
  signOut()
  void router.replace('/login')
  if (token) void logoutRequest(token).catch(() => undefined)
}
onMounted(loadAppointments)
</script>

<template>
  <div class="doctor-shell">
    <header class="doctor-header">
      <div><span class="brand-mark">+</span><b>医生工作台</b></div>
      <div><span>{{ session.name }}</span><button type="button" @click="logout">退出</button></div>
    </header>
    <main class="doctor-content">
      <section class="doctor-title">
        <div><p class="eyebrow">TODAY'S APPOINTMENTS</p><h1>今日预约</h1><small>{{ today }}</small></div>
        <label>状态筛选<select v-model="status" :disabled="loading" @change="changeStatus"><option value="">全部状态</option><option value="BOOKED">待就诊</option><option value="COMPLETED">已完成</option><option value="CANCELLED">已取消</option></select></label>
      </section>
      <p v-if="errorMessage" class="doctor-alert">{{ errorMessage }}</p>
      <section class="doctor-table-card">
        <div v-if="loading" class="doctor-empty">正在加载预约…</div>
        <div v-else-if="!appointments.length" class="doctor-empty">今日暂无符合条件的预约</div>
        <div v-else class="doctor-table-wrap"><table>
          <thead><tr><th>预约号</th><th>患者</th><th>科室</th><th>就诊时间</th><th>状态</th><th>操作</th></tr></thead>
          <tbody><tr v-for="item in appointments" :key="item.id">
            <td class="appointment-number">{{ item.appointmentNo }}</td><td>{{ item.patientName }}</td><td>{{ item.departmentName }}</td>
            <td><b>{{ item.startTime.slice(0, 5) }}–{{ item.endTime.slice(0, 5) }}</b><small>{{ item.sessionName || '排班时段' }}</small></td>
            <td><span class="doctor-status" :class="item.status.toLowerCase()">{{ item.status }}</span></td>
            <td><button v-if="item.status === 'BOOKED'" class="complete-button" :disabled="completingId !== null" @click="completeAppointment(item)">{{ completingId === item.id ? '处理中…' : '完成就诊' }}</button><span v-else>—</span></td>
          </tr></tbody>
        </table></div>
        <footer class="doctor-pagination"><span>{{ pageSummary }}</span><div><button :disabled="loading || page <= 1" @click="changePage(page - 1)">上一页</button><b>第 {{ page }} / {{ Math.max(totalPages, 1) }} 页</b><button :disabled="loading || page >= totalPages" @click="changePage(page + 1)">下一页</button></div></footer>
      </section>
    </main>
  </div>
</template>

<style scoped>
.doctor-shell{min-height:100vh;background:#f4f7f6;color:#24383e}.doctor-header{height:68px;padding:0 max(4vw,28px);display:flex;align-items:center;justify-content:space-between;background:#173a41;color:#fff}.doctor-header>div{display:flex;align-items:center;gap:10px}.doctor-header>div:last-child{gap:18px;font-size:14px}.doctor-header button{background:transparent;color:#b9cecc}.doctor-content{width:min(1180px,calc(100% - 40px));margin:0 auto;padding:44px 0}.doctor-title{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:24px}.doctor-title h1{margin:4px 0;font-size:30px}.doctor-title small{color:#829298}.doctor-title label{font-size:13px;color:#60737a}.doctor-title select{display:block;margin-top:7px;min-width:150px;padding:9px 12px;border:1px solid #cfddda;border-radius:8px;background:#fff}.doctor-table-card{background:#fff;border:1px solid #e0e8e6;border-radius:13px;box-shadow:0 8px 25px rgba(26,61,66,.05);overflow:hidden}.doctor-table-wrap{overflow-x:auto}table{width:100%;border-collapse:collapse}th,td{padding:16px 18px;text-align:left;border-bottom:1px solid #edf1f0;font-size:14px}th{background:#f8faf9;color:#718188;font-size:12px}td small{display:block;margin-top:4px;color:#87969b}.appointment-number{font-family:monospace;color:#52676d}.doctor-status{display:inline-block;padding:5px 9px;border-radius:12px;font-size:11px;font-weight:700}.doctor-status.booked{background:#fff4d8;color:#977125}.doctor-status.completed{background:#e2f3ed;color:#267a68}.doctor-status.cancelled{background:#edf0f1;color:#718087}.complete-button{padding:8px 12px;border-radius:7px;background:#218d82;color:#fff;font-size:12px}.complete-button:disabled{opacity:.55}.doctor-pagination{display:flex;align-items:center;justify-content:space-between;padding:15px 18px;color:#78898e;font-size:13px}.doctor-pagination div{display:flex;align-items:center;gap:12px}.doctor-pagination button{padding:7px 11px;border:1px solid #cfddda;border-radius:7px;background:#fff;color:#237e73}.doctor-pagination button:disabled{opacity:.4}.doctor-alert{padding:11px 14px;border-radius:8px;background:#fff0ed;color:#aa4f42}.doctor-empty{padding:70px 20px;text-align:center;color:#819095}@media(max-width:700px){.doctor-title{align-items:flex-start;gap:18px;flex-direction:column}.doctor-title label,.doctor-title select{width:100%}.doctor-pagination{align-items:flex-start;gap:12px;flex-direction:column}.doctor-header{padding:0 20px}}
</style>
