<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { doctors } from '../api/mock'
import { ApiError, logout as logoutRequest } from '../api/auth'
import { departmentApi, type Department } from '../api/department'
import { doctorApi, type Doctor } from '../api/doctor'
import { scheduleApi, type ScheduleBatchPayload, type ScheduleSessionPayload, type ScheduleSessionType, type ScheduleSlot } from '../api/schedule'
import { session, signOut } from '../stores/session'

const router = useRouter()
const active = ref('dashboard')
const resourceTab = ref<'departments' | 'doctors'>('departments')
const departments = ref<Department[]>([])
const departmentKeyword = ref('')
const loadingDepartments = ref(false)
const departmentError = ref('')
const departmentModalVisible = ref(false)
const editingDepartment = ref<Department | null>(null)
const savingDepartment = ref(false)
const departmentForm = reactive({ name: '', description: '', sortOrder: 0 })
const managedDoctors = ref<Doctor[]>([])
const doctorPageRecords = ref<Doctor[]>([])
const doctorPage = ref(1)
const doctorPageSize = ref(10)
const doctorTotal = ref(0)
const doctorTotalPages = ref(0)
const doctorKeyword = ref('')
const appliedDoctorKeyword = ref('')
const doctorError = ref('')
const loadingDoctors = ref(false)
const loadingDoctorPage = ref(false)
const doctorModalVisible = ref(false)
const editingDoctor = ref<Doctor | null>(null)
const savingDoctor = ref(false)
const doctorForm = reactive({ name: '', gender: 1, departmentId: 0, doctorCode: '', title: '', introduction: '', sortOrder: 0 })
const scheduleSlots = ref<ScheduleSlot[]>([])
const scheduleDepartmentId = ref(0)
const scheduleDoctorId = ref(0)
const loadingSchedule = ref(false)
const scheduleError = ref('')
const scheduleMessage = ref('')
const scheduleModalVisible = ref(false)
const savingSchedule = ref(false)
const selectedScheduleSlot = ref<ScheduleSlot | null>(null)
const weekdayOptions = [
  { value: 1, label: '一' }, { value: 2, label: '二' }, { value: 3, label: '三' }, { value: 4, label: '四' },
  { value: 5, label: '五' }, { value: 6, label: '六' }, { value: 7, label: '日' }
]

function addDays(source: Date, days: number) {
  const date = new Date(source)
  date.setDate(date.getDate() + days)
  return date
}

function startOfWeek(source: Date) {
  const date = new Date(source)
  date.setHours(0, 0, 0, 0)
  const offset = (date.getDay() + 6) % 7
  date.setDate(date.getDate() - offset)
  return date
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const currentMonday = startOfWeek(new Date())
const weekStart = ref([0, 6].includes(new Date().getDay()) ? addDays(currentMonday, 7) : currentMonday)
const weekDays = computed(() => weekdayOptions.map((weekday, index) => {
  const date = addDays(weekStart.value, index)
  return { ...weekday, date: formatDate(date), text: `${date.getMonth() + 1}月${date.getDate()}日` }
}))
const weekEnd = computed(() => addDays(weekStart.value, 6))
const weekTitle = computed(() => `${weekStart.value.getFullYear()}年 ${weekStart.value.getMonth() + 1}月${weekStart.value.getDate()}日 — ${weekEnd.value.getMonth() + 1}月${weekEnd.value.getDate()}日`)
const scheduleSummary = computed(() => scheduleSlots.value.reduce((summary, slot) => ({
  total: summary.total + slot.totalCapacity,
  booked: summary.booked + slot.bookedCapacity,
  remaining: summary.remaining + slot.remainingCapacity
}), { total: 0, booked: 0, remaining: 0 }))
const scheduleForm = reactive<ScheduleBatchPayload>({
  doctorId: 0,
  startDate: formatDate(weekStart.value),
  endDate: formatDate(weekEnd.value),
  weekdays: [1, 2, 3, 4, 5],
  sessions: []
})
const estimatedSessionCount = computed(() => {
  if (!scheduleForm.startDate || !scheduleForm.endDate) return 0
  const start = new Date(`${scheduleForm.startDate}T00:00:00`)
  const end = new Date(`${scheduleForm.endDate}T00:00:00`)
  let matchedDays = 0
  for (let date = start; date <= end; date = addDays(date, 1)) {
    const weekday = date.getDay() === 0 ? 7 : date.getDay()
    if (scheduleForm.weekdays.includes(weekday)) matchedDays++
  }
  return matchedDays * scheduleForm.sessions.length
})
const enabledDepartments = computed(() => departments.value.filter((department) => department.status === 1))
const menus = [
  { id: 'dashboard', label: '运营概览', icon: '◈' }, { id: 'resource', label: '医生与科室', icon: '♧' },
  { id: 'schedule', label: '排班与放号', icon: '◴' }, { id: 'appointments', label: '预约订单', icon: '▣' },
  { id: 'waitlist', label: '候补队列', icon: '⇄' }, { id: 'rules', label: '预约规则', icon: '⚙' }
]
const filteredDepartments = computed(() => {
  const keyword = departmentKeyword.value.trim()
  return keyword ? departments.value.filter((department) => department.name.includes(keyword)) : departments.value
})

async function loadDepartments() {
  if (!session.token) return
  loadingDepartments.value = true
  departmentError.value = ''
  try { departments.value = await departmentApi.list(session.token) }
  catch (error) { departmentError.value = error instanceof ApiError ? error.message : '科室数据加载失败' }
  finally { loadingDepartments.value = false }
}

async function loadScheduleDoctors() {
  if (!session.token || !scheduleDepartmentId.value) {
    managedDoctors.value = []
    loadingDoctors.value = false
    return
  }
  const departmentId = scheduleDepartmentId.value
  loadingDoctors.value = true
  doctorError.value = ''
  try {
    const result = await doctorApi.list(session.token, {
      page: 1,
      pageSize: 100,
      departmentId,
      status: 1
    })
    if (scheduleDepartmentId.value === departmentId) managedDoctors.value = result.records
  }
  catch (error) {
    if (scheduleDepartmentId.value === departmentId) {
      doctorError.value = error instanceof ApiError ? error.message : '医生数据加载失败'
    }
  }
  finally { if (scheduleDepartmentId.value === departmentId) loadingDoctors.value = false }
}

async function loadDoctorPage(page = doctorPage.value) {
  if (!session.token) return
  loadingDoctorPage.value = true
  doctorError.value = ''
  try {
    const result = await doctorApi.list(session.token, {
      page,
      pageSize: doctorPageSize.value,
      name: appliedDoctorKeyword.value || undefined
    })
    doctorPageRecords.value = result.records
    doctorPage.value = result.page
    doctorTotal.value = result.total
    doctorTotalPages.value = result.totalPages
  } catch (error) { doctorError.value = error instanceof ApiError ? error.message : '医生数据加载失败' }
  finally { loadingDoctorPage.value = false }
}

function changeDoctorPage(page: number) {
  if (page < 1 || page > doctorTotalPages.value || page === doctorPage.value) return
  void loadDoctorPage(page)
}

function searchDoctors() {
  appliedDoctorKeyword.value = doctorKeyword.value.trim()
  doctorPage.value = 1
  void loadDoctorPage(1)
}

function clearDoctorSearch() {
  doctorKeyword.value = ''
  appliedDoctorKeyword.value = ''
  doctorPage.value = 1
  void loadDoctorPage(1)
}

function changeDoctorPageSize() {
  doctorPage.value = 1
  void loadDoctorPage(1)
}

async function loadSchedule() {
  if (!session.token || !scheduleDoctorId.value) {
    scheduleSlots.value = []
    return
  }
  loadingSchedule.value = true
  scheduleError.value = ''
  try {
    scheduleSlots.value = await scheduleApi.list(
      session.token,
      formatDate(weekStart.value),
      formatDate(weekEnd.value),
      scheduleDoctorId.value
    )
  } catch (error) { scheduleError.value = error instanceof ApiError ? error.message : '排班数据加载失败' }
  finally { loadingSchedule.value = false }
}

async function prepareSchedulePage() {
  await loadDepartments()
  if (!scheduleDepartmentId.value) {
    managedDoctors.value = []
    scheduleDoctorId.value = 0
    scheduleSlots.value = []
    return
  }
  await loadScheduleDoctors()
  if (!managedDoctors.value.some((doctor) => doctor.id === scheduleDoctorId.value)) {
    scheduleDoctorId.value = 0
    scheduleSlots.value = []
    return
  }
  await loadSchedule()
}

async function changeScheduleDepartment() {
  scheduleDoctorId.value = 0
  scheduleSlots.value = []
  selectedScheduleSlot.value = null
  scheduleMessage.value = ''
  await loadScheduleDoctors()
}

function changeScheduleDoctor() {
  scheduleSlots.value = []
  selectedScheduleSlot.value = null
  void loadSchedule()
}

function slotsForDay(date: string) {
  return scheduleSlots.value.filter((slot) => slot.scheduleDate === date)
}

function capacityPercent(slot: ScheduleSlot) {
  return slot.totalCapacity ? Math.min(100, Math.round(slot.bookedCapacity / slot.totalCapacity * 100)) : 0
}

function changeWeek(offset: number) {
  weekStart.value = addDays(weekStart.value, offset * 7)
  void loadSchedule()
}

function newSession(sessionType: ScheduleSessionType = 'MORNING', startTime = '08:00', endTime = '12:00', capacity = 30, average = 8): ScheduleSessionPayload {
  return { sessionType, customSessionName: '', startTime, endTime, capacity, averageConsultationMinutes: average }
}

function openBatchSchedule() {
  scheduleForm.doctorId = scheduleDoctorId.value
  scheduleForm.startDate = formatDate(weekStart.value)
  scheduleForm.endDate = formatDate(weekEnd.value)
  scheduleForm.weekdays = [1, 2, 3, 4, 5]
  scheduleForm.sessions = [newSession(), newSession('AFTERNOON', '14:00', '17:00', 20, 9)]
  scheduleError.value = ''
  scheduleModalVisible.value = true
}

function addScheduleSession() {
  scheduleForm.sessions.push(newSession('OTHER', '18:00', '20:00', 10, 10))
}

function applySessionTypeDefaults(item: ScheduleSessionPayload) {
  if (item.sessionType === 'MORNING') { item.startTime = '08:00'; item.endTime = '12:00'; item.capacity = 30; item.averageConsultationMinutes = 8 }
  if (item.sessionType === 'AFTERNOON') { item.startTime = '14:00'; item.endTime = '17:00'; item.capacity = 20; item.averageConsultationMinutes = 9 }
  if (item.sessionType !== 'OTHER') item.customSessionName = ''
}

function removeScheduleSession(index: number) {
  if (scheduleForm.sessions.length > 1) scheduleForm.sessions.splice(index, 1)
}

async function saveBatchSchedule() {
  if (!session.token || savingSchedule.value) return
  savingSchedule.value = true
  scheduleError.value = ''
  try {
    const result = await scheduleApi.batchCreate(session.token, {
      ...scheduleForm,
      doctorId: Number(scheduleForm.doctorId),
      weekdays: [...scheduleForm.weekdays],
      sessions: scheduleForm.sessions.map((item) => ({
        ...item,
        customSessionName: item.sessionType === 'OTHER' ? item.customSessionName?.trim() : undefined,
        capacity: Number(item.capacity),
        averageConsultationMinutes: Number(item.averageConsultationMinutes)
      }))
    })
    scheduleDoctorId.value = scheduleForm.doctorId
    scheduleModalVisible.value = false
    scheduleMessage.value = `已成功生成 ${result.createdCount} 个出诊班次`
    await loadSchedule()
  } catch (error) { scheduleError.value = error instanceof ApiError ? error.message : '批量排班失败' }
  finally { savingSchedule.value = false }
}

async function toggleScheduleStatus(slot: ScheduleSlot) {
  if (!session.token) return
  const status = slot.status === 'OPEN' ? 'CLOSED' : 'OPEN'
  try {
    await scheduleApi.updateStatus(session.token, slot.id, status)
    scheduleSlots.value = scheduleSlots.value.map((item) => item.id === slot.id ? { ...item, status } : item)
    if (selectedScheduleSlot.value?.id === slot.id) selectedScheduleSlot.value = { ...selectedScheduleSlot.value, status }
  } catch (error) { scheduleError.value = error instanceof ApiError ? error.message : '班次状态更新失败' }
}

async function changeScheduleCapacity(slot: ScheduleSlot) {
  if (!session.token) return
  const input = window.prompt(`当前已预约 ${slot.bookedCapacity} 人，请输入新的总号源：`, String(slot.totalCapacity))
  if (input === null) return
  const capacity = Number(input)
  if (!Number.isInteger(capacity) || capacity < 1) { scheduleError.value = '总号源必须是大于 0 的整数'; return }
  try {
    await scheduleApi.updateCapacity(session.token, slot.id, capacity)
    await loadSchedule()
    selectedScheduleSlot.value = scheduleSlots.value.find((item) => item.id === slot.id) ?? null
  } catch (error) { scheduleError.value = error instanceof ApiError ? error.message : '号源调整失败' }
}

function openCreateDoctor() {
  editingDoctor.value = null
  doctorForm.name = ''; doctorForm.gender = 1; doctorForm.departmentId = enabledDepartments.value[0]?.id ?? 0
  doctorForm.doctorCode = ''; doctorForm.title = ''; doctorForm.introduction = ''; doctorForm.sortOrder = 0
  doctorError.value = ''
  doctorModalVisible.value = true
}

function openEditDoctor(doctor: Doctor) {
  editingDoctor.value = doctor
  doctorForm.name = doctor.name; doctorForm.gender = doctor.gender; doctorForm.departmentId = doctor.departmentId
  doctorForm.doctorCode = doctor.doctorCode; doctorForm.title = doctor.title ?? ''; doctorForm.introduction = doctor.introduction ?? ''; doctorForm.sortOrder = doctor.sortOrder
  doctorError.value = ''
  doctorModalVisible.value = true
}

async function saveDoctor() {
  if (!session.token || savingDoctor.value) return
  savingDoctor.value = true; doctorError.value = ''
  const payload = { ...doctorForm, title: doctorForm.title || undefined, introduction: doctorForm.introduction || undefined, sortOrder: Number(doctorForm.sortOrder) }
  try {
    if (editingDoctor.value) await doctorApi.update(session.token, editingDoctor.value.id, payload)
    else await doctorApi.create(session.token, payload)
    if (!editingDoctor.value) doctorPage.value = 1
    await loadDoctorPage(doctorPage.value)
    doctorModalVisible.value = false
  } catch (error) { doctorError.value = error instanceof ApiError ? error.message : '医生保存失败' }
  finally { savingDoctor.value = false }
}

async function toggleDoctorStatus(doctor: Doctor) {
  if (!session.token) return
  const status = doctor.status === 1 ? 0 : 1
  try {
    await doctorApi.updateStatus(session.token, doctor.id, status)
    doctorPageRecords.value = doctorPageRecords.value.map((item) => item.id === doctor.id ? { ...item, status } : item)
    managedDoctors.value = managedDoctors.value.map((item) => item.id === doctor.id ? { ...item, status } : item)
  }
  catch (error) { doctorError.value = error instanceof ApiError ? error.message : '医生状态更新失败' }
}

function openCreateDepartment() {
  editingDepartment.value = null
  departmentForm.name = ''; departmentForm.description = ''; departmentForm.sortOrder = 0
  departmentError.value = ''
  departmentModalVisible.value = true
}

function openEditDepartment(department: Department) {
  editingDepartment.value = department
  departmentForm.name = department.name
  departmentForm.description = department.description ?? ''
  departmentForm.sortOrder = department.sortOrder
  departmentError.value = ''
  departmentModalVisible.value = true
}

async function saveDepartment() {
  if (!session.token || savingDepartment.value) return
  savingDepartment.value = true
  departmentError.value = ''
  const payload = { name: departmentForm.name, description: departmentForm.description || undefined, sortOrder: Number(departmentForm.sortOrder) }
  try {
    const id = editingDepartment.value
      ? (await departmentApi.update(session.token, editingDepartment.value.id, payload), editingDepartment.value.id)
      : await departmentApi.create(session.token, payload)
    const savedDepartment = await departmentApi.getById(session.token, id)
    departments.value = [...departments.value.filter((department) => department.id !== id), savedDepartment]
      .sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
    departmentKeyword.value = ''
    departmentModalVisible.value = false
  } catch (error) { departmentError.value = error instanceof ApiError ? error.message : '保存失败，请稍后重试' }
  finally { savingDepartment.value = false }
}

async function toggleDepartmentStatus(department: Department) {
  if (!session.token) return
  departmentError.value = ''
  try {
    const status = department.status === 1 ? 0 : 1
    await departmentApi.updateStatus(session.token, department.id, status)
    departments.value = departments.value.map((item) => item.id === department.id ? { ...item, status } : item)
  } catch (error) { departmentError.value = error instanceof ApiError ? error.message : '状态更新失败' }
}

function logout() {
  const token = session.token
  signOut(); router.replace('/login')
  if (token) void logoutRequest(token).catch(() => undefined)
}

watch(active, (value) => {
  if (value === 'resource') { void loadDepartments(); if (resourceTab.value === 'doctors') void loadDoctorPage() }
  if (value === 'schedule') void prepareSchedulePage()
})
watch(resourceTab, (value) => { if (value === 'doctors') void loadDoctorPage() })
onMounted(() => { if (active.value === 'resource') void loadDepartments() })
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar"><div class="admin-brand"><span class="brand-mark">+</span>智约医疗<small>运营中心</small></div><nav><button v-for="menu in menus" :key="menu.id" :class="{ active: active === menu.id }" @click="active = menu.id"><i>{{ menu.icon }}</i>{{ menu.label }}</button></nav><div class="admin-user"><div class="avatar">管</div><span><b>{{ session.name }}</b><small>运营管理员</small></span><button @click="logout">退出</button></div></aside>
    <main class="admin-main">
      <header><div><p class="eyebrow">OPERATIONS CONSOLE</p><h1>{{ menus.find((item) => item.id === active)?.label }}</h1></div><p>本地演示数据</p></header>
      <template v-if="active === 'dashboard'"><div class="metrics"><article><small>今日开放号源</small><strong>240</strong><span>较昨日 +12%</span></article><article><small>今日预约</small><strong>186</strong><span>预约率 77.5%</span></article><article><small>候补排队中</small><strong>23</strong><span>等待自动递补</span></article><article><small>待处理异常</small><strong>8</strong><span>需执行规则校验</span></article></div><div class="admin-grid"><section class="panel"><div class="panel-head"><h2>医生与号源</h2><button class="primary" @click="active = 'resource'">进入管理</button></div><table><thead><tr><th>医生</th><th>科室</th><th>剩余号源</th><th>状态</th></tr></thead><tbody><tr v-for="doctor in doctors" :key="doctor.id"><td>{{ doctor.name }}</td><td>{{ doctor.department }}</td><td>{{ doctor.slots.reduce((total, slot) => total + slot.remaining, 0) }}</td><td><span class="tag">已启用</span></td></tr></tbody></table></section><section class="panel"><h2>候补队列</h2><ol class="queue"><li><b>王女士</b><span>心血管内科 · 林知远</span><em>等待中</em></li><li><b>李先生</b><span>消化内科 · 陈书宁</span><em>等待中</em></li></ol></section></div></template>
      <section v-else-if="active === 'resource'" class="resource-page">
        <div class="resource-toolbar"><div class="resource-tabs"><button :class="{ active: resourceTab === 'departments' }" @click="resourceTab = 'departments'">科室管理</button><button :class="{ active: resourceTab === 'doctors' }" @click="resourceTab = 'doctors'">医生管理</button></div><button class="primary resource-create" @click="resourceTab === 'departments' ? openCreateDepartment() : openCreateDoctor()">+ {{ resourceTab === 'departments' ? '新建科室' : '新增医生' }}</button></div>
        <template v-if="resourceTab === 'departments'"><div class="resource-intro"><div><h2>科室列表</h2><p>维护患者端可见的科室及其启用状态。</p></div><label class="resource-search">⌕ <input v-model.trim="departmentKeyword" placeholder="搜索科室名称"></label></div><p v-if="departmentError" class="resource-error">{{ departmentError }}</p><div v-if="loadingDepartments" class="resource-empty">正在加载科室数据…</div><div v-else-if="filteredDepartments.length" class="department-admin-grid"><article v-for="department in filteredDepartments" :key="department.id" class="department-admin-card"><div class="department-admin-icon">科</div><div><h3>{{ department.name }}</h3><p>已关联 {{ department.doctorCount }} 位医生</p></div><button class="status-chip status-button" @click="toggleDepartmentStatus(department)">{{ department.status === 1 ? '启用' : '停用' }}</button><button class="more-button" aria-label="编辑科室" title="编辑科室" @click="openEditDepartment(department)">⋯</button></article></div><div v-else class="resource-empty">还没有科室，点击右上角新建第一个科室。</div></template>
        <template v-else>
          <div class="resource-intro"><div><h2>医生列表</h2><p>维护医生资料、所属科室与出诊状态。</p></div><form class="doctor-search-form" @submit.prevent="searchDoctors"><label class="resource-search">⌕ <input v-model="doctorKeyword" type="search" placeholder="搜索医生姓名"></label><button class="table-action" type="submit">搜索</button><button v-if="doctorKeyword || appliedDoctorKeyword" class="doctor-search-clear" type="button" @click="clearDoctorSearch">清空</button></form></div>
          <p v-if="doctorError" class="resource-error">{{ doctorError }}</p>
          <div v-if="loadingDoctorPage" class="resource-empty">正在加载医生数据…</div>
          <section v-else-if="doctorPageRecords.length" class="doctor-admin-table"><table><thead><tr><th>医生</th><th>所属科室</th><th>职称</th><th>工号</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="doctor in doctorPageRecords" :key="doctor.id"><td><b>{{ doctor.name }}</b></td><td>{{ doctor.departmentName }}</td><td>{{ doctor.title || '-' }}</td><td>{{ doctor.doctorCode }}</td><td><button class="status-chip status-button" @click="toggleDoctorStatus(doctor)">{{ doctor.status === 1 ? '启用' : '停用' }}</button></td><td><button class="table-action" @click="openEditDoctor(doctor)">编辑</button></td></tr></tbody></table></section>
          <div v-if="doctorTotal > 0" class="doctor-pagination"><label>每页<select v-model.number="doctorPageSize" @change="changeDoctorPageSize"><option :value="10">10 条</option><option :value="20">20 条</option><option :value="50">50 条</option></select></label><span>共 {{ doctorTotal }} 位医生</span><div><button :disabled="doctorPage === 1" @click="changeDoctorPage(doctorPage - 1)">上一页</button><b>第 {{ doctorPage }} / {{ doctorTotalPages }} 页</b><button :disabled="doctorPage === doctorTotalPages" @click="changeDoctorPage(doctorPage + 1)">下一页</button></div></div>
          <div v-if="!loadingDoctorPage && !doctorPageRecords.length" class="resource-empty">没有找到符合条件的医生。</div>
        </template>
      </section>
      <section v-else-if="active === 'schedule'" class="schedule-page">
        <div class="schedule-toolbar">
          <select v-model.number="scheduleDepartmentId" class="schedule-doctor-select" @change="changeScheduleDepartment">
            <option :value="0">请选择科室</option>
            <option v-for="department in enabledDepartments" :key="department.id" :value="department.id">{{ department.name }}</option>
          </select>
          <select v-model.number="scheduleDoctorId" class="schedule-doctor-select" :disabled="!scheduleDepartmentId || loadingDoctors" @change="changeScheduleDoctor">
            <option :value="0">{{ loadingDoctors ? '正在加载医生…' : '请选择医生' }}</option>
            <option v-for="doctor in managedDoctors" :key="doctor.id" :value="doctor.id">{{ doctor.name }} · {{ doctor.departmentName }}</option>
          </select>
          <div class="schedule-controls">
            <button title="上一周" @click="changeWeek(-1)">‹</button>
            <span class="week-title">{{ weekTitle }}</span>
            <button title="下一周" @click="changeWeek(1)">›</button>
          </div>
          <button class="primary resource-create" :disabled="!scheduleDoctorId" @click="openBatchSchedule">+ 批量排班</button>
        </div>
        <div class="schedule-summary">
          <article><small>本周总号源</small><strong>{{ scheduleSummary.total }}</strong></article>
          <article><small>已预约</small><strong>{{ scheduleSummary.booked }}</strong></article>
          <article><small>剩余号源</small><strong>{{ scheduleSummary.remaining }}</strong></article>
        </div>
        <p v-if="scheduleMessage" class="schedule-message">{{ scheduleMessage }}</p>
        <p v-if="scheduleError" class="resource-error">{{ scheduleError }}</p>
        <div v-if="loadingSchedule" class="resource-empty">正在加载排班数据…</div>
        <div v-else-if="scheduleDoctorId" class="schedule-week">
          <section v-for="day in weekDays" :key="day.date" class="schedule-day" :class="{ today: day.date === formatDate(new Date()) }">
            <div class="schedule-day-head"><b>周{{ day.label }}</b><small>{{ day.text }}</small></div>
            <div class="schedule-day-body">
              <article v-for="slot in slotsForDay(day.date)" :key="slot.id" class="schedule-session-card" :class="{ closed: slot.status === 'CLOSED' }">
                <div class="session-card-head"><b>{{ slot.sessionName }}</b><span class="session-status" :class="{ closed: slot.status === 'CLOSED' }">{{ slot.status === 'OPEN' ? '开放中' : '已关闭' }}</span></div>
                <p>{{ slot.startTime.slice(0, 5) }}–{{ slot.endTime.slice(0, 5) }} · 约 {{ slot.averageConsultationMinutes }} 分钟/人</p>
                <p class="session-doctor">{{ slot.doctorName }} · {{ slot.departmentName }}</p>
                <div class="capacity-bar"><i :style="{ width: `${capacityPercent(slot)}%` }"></i></div>
                <div class="capacity-line"><span>已约 {{ slot.bookedCapacity }}/{{ slot.totalCapacity }}</span><strong>余 {{ slot.remainingCapacity }}</strong></div>
                <div class="session-actions"><button @click="changeScheduleCapacity(slot)">调号源</button><button @click="toggleScheduleStatus(slot)">{{ slot.status === 'OPEN' ? '关闭' : '开放' }}</button></div>
              </article>
              <p v-if="!slotsForDay(day.date).length" class="empty-day">暂无排班</p>
            </div>
          </section>
        </div>
        <div v-else class="resource-empty schedule-selection-empty">请先选择科室，再选择需要排班的医生。</div>
      </section>
      <section v-else class="panel module-placeholder"><div class="module-icon">{{ menus.find((item) => item.id === active)?.icon }}</div><h2>{{ menus.find((item) => item.id === active)?.label }}</h2><p>该模块将在预约核心链路完成后接入对应的管理接口与业务规则。</p></section>
    </main>
    <div v-if="departmentModalVisible" class="modal-mask" @click.self="departmentModalVisible = false"><form class="department-modal" @submit.prevent="saveDepartment"><div class="modal-head"><div><h2>{{ editingDepartment ? '编辑科室' : '新建科室' }}</h2><p>科室名称在系统内必须唯一。</p></div><button type="button" class="modal-close" @click="departmentModalVisible = false">×</button></div><label>科室名称<input v-model.trim="departmentForm.name" maxlength="50" required placeholder="例如：心血管内科"></label><label>科室简介<textarea v-model.trim="departmentForm.description" maxlength="500" placeholder="简要介绍科室服务范围"></textarea></label><label>排序值<input v-model.number="departmentForm.sortOrder" type="number" min="0" required></label><p v-if="departmentError" class="resource-error">{{ departmentError }}</p><div class="modal-actions"><button type="button" class="modal-cancel" @click="departmentModalVisible = false">取消</button><button class="primary" :disabled="savingDepartment">{{ savingDepartment ? '保存中…' : '保存' }}</button></div></form></div>
    <div v-if="doctorModalVisible" class="modal-mask" @click.self="doctorModalVisible = false"><form class="department-modal" @submit.prevent="saveDoctor"><div class="modal-head"><div><h2>{{ editingDoctor ? '编辑医生' : '新增医生' }}</h2><p>医生工号在系统内必须唯一。</p></div><button type="button" class="modal-close" @click="doctorModalVisible = false">×</button></div><label>医生姓名<input v-model.trim="doctorForm.name" maxlength="50" required></label><label>所属科室<select v-model.number="doctorForm.departmentId" required><option :value="0" disabled>请选择科室</option><option v-for="department in enabledDepartments" :key="department.id" :value="department.id">{{ department.name }}</option></select></label><label>性别<select v-model.number="doctorForm.gender"><option :value="1">男</option><option :value="2">女</option></select></label><label>医生工号<input v-model.trim="doctorForm.doctorCode" maxlength="32" required></label><label>职称<input v-model.trim="doctorForm.title" maxlength="50"></label><label>医生简介<textarea v-model.trim="doctorForm.introduction" maxlength="5000"></textarea></label><label>排序值<input v-model.number="doctorForm.sortOrder" type="number" min="0" required></label><p v-if="doctorError" class="resource-error">{{ doctorError }}</p><div class="modal-actions"><button type="button" class="modal-cancel" @click="doctorModalVisible = false">取消</button><button class="primary" :disabled="savingDoctor">{{ savingDoctor ? '保存中…' : '保存' }}</button></div></form></div>
    <div v-if="scheduleModalVisible" class="modal-mask" @click.self="scheduleModalVisible = false">
      <form class="department-modal schedule-modal" @submit.prevent="saveBatchSchedule">
        <div class="modal-head"><div><h2>批量排班</h2><p>按日期范围和星期，为医生生成上午、下午等出诊班次。</p></div><button type="button" class="modal-close" @click="scheduleModalVisible = false">×</button></div>
        <div class="schedule-form-grid">
          <label>出诊医生<select v-model.number="scheduleForm.doctorId" required><option :value="0" disabled>请选择医生</option><option v-for="doctor in managedDoctors.filter((item) => item.status === 1)" :key="doctor.id" :value="doctor.id">{{ doctor.name }} · {{ doctor.departmentName }}</option></select></label>
          <span></span>
          <label>开始日期<input v-model="scheduleForm.startDate" type="date" required></label>
          <label>结束日期<input v-model="scheduleForm.endDate" type="date" required></label>
        </div>
        <label>出诊星期</label>
        <div class="weekday-picker"><label v-for="weekday in weekdayOptions" :key="weekday.value"><input v-model="scheduleForm.weekdays" type="checkbox" :value="weekday.value"><span>周{{ weekday.label }}</span></label></div>
        <div v-for="(item, index) in scheduleForm.sessions" :key="index" class="session-editor">
          <div class="session-editor-head"><b>班次 {{ index + 1 }}</b><button type="button" @click="removeScheduleSession(index)">删除</button></div>
          <div class="session-editor-fields">
            <label>班次类型<select v-model="item.sessionType" @change="applySessionTypeDefaults(item)"><option value="MORNING">上午</option><option value="AFTERNOON">下午</option><option value="OTHER">其他</option></select></label>
            <label v-if="item.sessionType === 'OTHER'">自定义名称<input v-model.trim="item.customSessionName" maxlength="20" required placeholder="例如：夜间门诊"></label>
            <label>开始时间<input v-model="item.startTime" type="time" required></label>
            <label>结束时间<input v-model="item.endTime" type="time" required></label>
            <label>总号源<input v-model.number="item.capacity" type="number" min="1" required></label>
            <label>分钟/人<input v-model.number="item.averageConsultationMinutes" type="number" min="1" max="60" required></label>
          </div>
        </div>
        <button type="button" class="add-session" @click="addScheduleSession">+ 添加出诊班次</button>
        <p class="schedule-preview">预计生成 {{ estimatedSessionCount }} 个班次；患者预约后将根据排队号和平均接诊时长计算预计到诊时间。</p>
        <p v-if="scheduleError" class="resource-error">{{ scheduleError }}</p>
        <div class="modal-actions"><button type="button" class="modal-cancel" @click="scheduleModalVisible = false">取消</button><button class="primary" :disabled="savingSchedule">{{ savingSchedule ? '生成中…' : '确认排班' }}</button></div>
      </form>
    </div>
    <div v-if="selectedScheduleSlot" class="modal-mask schedule-detail-mask" @click.self="selectedScheduleSlot = null">
      <section class="department-modal schedule-detail">
        <div class="modal-head"><div><h2>{{ selectedScheduleSlot.sessionName }}</h2><p>{{ selectedScheduleSlot.doctorName }} · {{ selectedScheduleSlot.departmentName }}</p></div><button class="modal-close" @click="selectedScheduleSlot = null">×</button></div>
        <div class="detail-date"><b>{{ selectedScheduleSlot.scheduleDate }}</b><span>{{ selectedScheduleSlot.startTime.slice(0, 5) }}–{{ selectedScheduleSlot.endTime.slice(0, 5) }}</span></div>
        <div class="detail-capacity"><article><small>总号源</small><strong>{{ selectedScheduleSlot.totalCapacity }}</strong></article><article><small>已预约</small><strong>{{ selectedScheduleSlot.bookedCapacity }}</strong></article><article><small>剩余</small><strong>{{ selectedScheduleSlot.remainingCapacity }}</strong></article></div>
        <p class="detail-note">平均每位患者约 {{ selectedScheduleSlot.averageConsultationMinutes }} 分钟，预约成功后会据此计算预计到诊时间。</p>
        <div class="modal-actions"><button class="modal-cancel" @click="changeScheduleCapacity(selectedScheduleSlot)">调整号源</button><button class="primary" @click="toggleScheduleStatus(selectedScheduleSlot)">{{ selectedScheduleSlot.status === 'OPEN' ? '关闭班次' : '重新开放' }}</button></div>
      </section>
    </div>
  </div>
</template>
