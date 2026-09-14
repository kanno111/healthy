<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError } from '../api/auth'
import { patientResourceApi, type PatientDepartment, type PatientDoctor } from '../api/patient-resource'
import { session } from '../stores/session'

const keyword = ref('')
const selectedDepartmentId = ref<number | null>(null)
const departments = ref<PatientDepartment[]>([])
const doctors = ref<PatientDoctor[]>([])
const doctorPage = ref(1)
const doctorPageSize = ref(10)
const doctorTotal = ref(0)
const doctorTotalPages = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const colors = ['#e67260', '#568f88', '#718fc2', '#9b83bd', '#d49355', '#4f9ba8']

const selectedDepartment = computed(() => departments.value.find((item) => item.id === selectedDepartmentId.value))
const visiblePageNumbers = computed(() => {
  let start = Math.max(1, doctorPage.value - 2)
  const end = Math.min(doctorTotalPages.value, start + 4)
  start = Math.max(1, end - 4)
  return Array.from({ length: Math.max(0, end - start + 1) }, (_, index) => start + index)
})
let keywordTimer: ReturnType<typeof setTimeout> | undefined
let doctorRequestId = 0

function doctorColor(doctor: PatientDoctor) { return colors[doctor.id % colors.length] }
function selectDepartment(id: number | null) { selectedDepartmentId.value = id }

async function loadResources() {
  if (!session.token) return
  const requestId = ++doctorRequestId
  loading.value = true
  errorMessage.value = ''
  try {
    const [departmentData, doctorPageData] = await Promise.all([
      patientResourceApi.listDepartments(session.token),
      patientResourceApi.listDoctors(session.token, { page: doctorPage.value, pageSize: doctorPageSize.value })
    ])
    departments.value = departmentData
    if (requestId === doctorRequestId) applyDoctorPage(doctorPageData)
  } catch (error) {
    if (requestId === doctorRequestId) {
      errorMessage.value = error instanceof ApiError ? error.message : '科室和医生加载失败'
    }
  } finally { if (requestId === doctorRequestId) loading.value = false }
}

async function loadDoctors() {
  if (!session.token) return
  const requestId = ++doctorRequestId
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await patientResourceApi.listDoctors(session.token, {
      departmentId: selectedDepartmentId.value ?? undefined,
      keyword: keyword.value.trim() || undefined,
      page: doctorPage.value,
      pageSize: doctorPageSize.value
    })
    if (requestId === doctorRequestId) applyDoctorPage(result)
  } catch (error) {
    if (requestId === doctorRequestId) {
      errorMessage.value = error instanceof ApiError ? error.message : '医生数据加载失败'
    }
  } finally {
    if (requestId === doctorRequestId) loading.value = false
  }
}

function applyDoctorPage(result: Awaited<ReturnType<typeof patientResourceApi.listDoctors>>) {
  doctors.value = result.records
  doctorPage.value = result.page
  doctorPageSize.value = result.pageSize
  doctorTotal.value = result.total
  doctorTotalPages.value = result.totalPages
}

function changeDoctorPage(page: number) {
  if (page < 1 || page > doctorTotalPages.value || page === doctorPage.value) return
  doctorPage.value = page
  void loadDoctors()
}

function resetPageAndLoadDoctors() {
  doctorPage.value = 1
  if (keywordTimer) {
    clearTimeout(keywordTimer)
    keywordTimer = undefined
  }
  void loadDoctors()
}

watch(selectedDepartmentId, resetPageAndLoadDoctors)
watch(doctorPageSize, resetPageAndLoadDoctors)
watch(keyword, () => {
  doctorPage.value = 1
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => void loadDoctors(), 300)
})

onMounted(loadResources)
onBeforeUnmount(() => { if (keywordTimer) clearTimeout(keywordTimer) })
</script>

<template>
  <section class="department-page">
    <header class="department-page__header">
      <div><p class="eyebrow">APPOINTMENT</p><h1>选择科室</h1><p>选择科室后，查看医生未来两周的开放号源。</p></div>
      <label class="department-search"><span aria-hidden="true">⌕</span><input v-model.trim="keyword" placeholder="搜索科室、医生、职称或擅长方向"></label>
    </header>
    <div class="department-layout">
      <aside class="department-sidebar" aria-label="科室列表">
        <button :class="{ active: selectedDepartmentId === null }" type="button" @click="selectDepartment(null)">全部科室</button>
        <button v-for="department in departments" :key="department.id" :class="{ active: selectedDepartmentId === department.id }" type="button" @click="selectDepartment(department.id)">{{ department.name }}</button>
      </aside>
      <main class="department-content">
        <div class="department-content__head"><div><h2>{{ selectedDepartment?.name ?? '全部科室' }}</h2><span>共 {{ doctorTotal }} 位医生</span></div></div>
        <p v-if="errorMessage" class="resource-error patient-resource-error">{{ errorMessage }}</p>
        <div v-if="loading" class="department-empty">正在加载科室和医生…</div>
        <div v-else-if="doctors.length" class="doctor-list">
          <article v-for="doctor in doctors" :key="doctor.id" class="doctor-list-item">
            <div class="doctor-list-avatar" :style="{ background: doctorColor(doctor) }">{{ doctor.name.slice(0, 1) }}</div>
            <div class="doctor-list-info">
              <div class="doctor-list-name"><h3>{{ doctor.name }}</h3><span>{{ doctor.title || '医师' }}</span></div>
              <p>{{ doctor.departmentName }}</p><small>{{ doctor.introduction || '暂无医生简介' }}</small>
              <b :class="{ unavailable: !doctor.hasAvailableSlots }">{{ doctor.hasAvailableSlots ? '未来两周有号' : '未来两周暂无号源' }}</b>
            </div>
            <RouterLink :to="`/doctor/${doctor.id}`" class="doctor-list-link">查看号源 →</RouterLink>
          </article>
        </div>
        <div v-else class="department-empty">没有找到匹配的医生或科室</div>
        <div v-if="!loading && doctorTotal > 0" class="patient-pagination">
          <label>每页<select v-model.number="doctorPageSize"><option :value="6">6 条</option><option :value="10">10 条</option><option :value="20">20 条</option></select></label>
          <div><button :disabled="doctorPage === 1" @click="changeDoctorPage(doctorPage - 1)">上一页</button><button v-for="page in visiblePageNumbers" :key="page" :class="{ active: page === doctorPage }" @click="changeDoctorPage(page)">{{ page }}</button><button :disabled="doctorPage === doctorTotalPages" @click="changeDoctorPage(doctorPage + 1)">下一页</button></div>
        </div>
      </main>
    </div>
  </section>
</template>
