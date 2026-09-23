<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError, logout as logoutRequest } from '../api/auth'
import { adminDashboardApi, type AdminDashboardSummary } from '../api/admin-dashboard'
import { departmentApi, type Department } from '../api/department'
import { doctorApi, type Doctor } from '../api/doctor'
import { scheduleApi, type ScheduleBatchPayload, type ScheduleSessionPayload, type ScheduleSessionType, type ScheduleSlot } from '../api/schedule'
import { adminAppointmentApi, type AdminAppointment } from '../api/admin-appointment'
import { adminWaitlistApi, type AdminWaitlistRecord, type WaitlistSlotQueue, type WaitlistStatus } from '../api/admin-waitlist'
import { session, signOut } from '../stores/session'

const router = useRouter()
const active = ref('dashboard')
const dashboardSummary = ref<AdminDashboardSummary | null>(null)
const loadingDashboard = ref(false)
const dashboardError = ref('')
const dashboardUpdatedAt = ref('')
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
const adminAppointments = ref<AdminAppointment[]>([])
const loadingAppointments = ref(false)
const appointmentError = ref('')
const completingAppointmentId = ref<number | null>(null)
const waitlistRecords = ref<AdminWaitlistRecord[]>([])
const waitlistPage = ref(1)
const waitlistPageSize = ref(10)
const waitlistTotal = ref(0)
const waitlistTotalPages = ref(0)
const loadingWaitlists = ref(false)
const waitlistError = ref('')
const waitlistFilterDoctors = ref<Doctor[]>([])
const loadingWaitlistDoctors = ref(false)
const waitlistFilters = reactive({ status: '', scheduleDate: '', departmentId: 0, doctorId: 0, patientKeyword: '' })
const selectedWaitlistRecord = ref<AdminWaitlistRecord | null>(null)
const selectedWaitlistQueue = ref<WaitlistSlotQueue | null>(null)
const loadingWaitlistQueue = ref(false)
const waitlistQueueError = ref('')
const waitlistNow = ref(Date.now())
const refreshedExpiredOfferIds = new Set<number>()
let waitlistCountdownTimer: number | undefined
const waitlistStatusOptions: Array<{ value: '' | WaitlistStatus, label: string }> = [
  { value: '', label: '全部' },
  { value: 'WAITING', label: '排队中' },
  { value: 'OFFERED', label: '待确认' },
  { value: 'CONFIRMED', label: '候补成功' },
  { value: 'EXPIRED', label: '已过期' },
  { value: 'CANCELLED', label: '已取消' }
]
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
  { id: 'waitlist', label: '候补队列', icon: '⇄' }
]
const dashboardAvailabilityRate = computed(() => {
  const summary = dashboardSummary.value
  if (!summary || summary.totalCapacity === 0) return 0
  return Math.round(summary.remainingCapacity / summary.totalCapacity * 100)
})
const filteredDepartments = computed(() => {
  const keyword = departmentKeyword.value.trim()
  return keyword ? departments.value.filter((department) => department.name.includes(keyword)) : departments.value
})

async function loadDashboard() {
  if (!session.token) return
  loadingDashboard.value = true
  dashboardError.value = ''
  try {
    dashboardSummary.value = await adminDashboardApi.summary(session.token, formatDate(new Date()))
    dashboardUpdatedAt.value = new Date().toLocaleTimeString('zh-CN', {
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    })
  } catch (error) {
    dashboardError.value = error instanceof ApiError ? error.message : '运营概览加载失败'
  } finally {
    loadingDashboard.value = false
  }
}

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

async function loadAdminAppointments() {
  if (!session.token) return
  loadingAppointments.value = true
  appointmentError.value = ''
  try {
    adminAppointments.value = await adminAppointmentApi.list(session.token)
  } catch (error) {
    appointmentError.value = error instanceof ApiError ? error.message : '预约订单加载失败'
  } finally {
    loadingAppointments.value = false
  }
}

function canCompleteAppointment(item: AdminAppointment) {
  return item.status === 'BOOKED' && new Date(`${item.scheduleDate}T${item.startTime}`).getTime() <= Date.now()
}

function appointmentStatusText(status: AdminAppointment['status']) {
  return { BOOKED: '待就诊', CANCELLED: '已取消', COMPLETED: '已完成' }[status]
}

async function completeAppointment(item: AdminAppointment) {
  if (!session.token || completingAppointmentId.value !== null || !canCompleteAppointment(item)) return
  if (!window.confirm(`确认将预约单 ${item.appointmentNo} 标记为已完成？`)) return
  completingAppointmentId.value = item.id
  appointmentError.value = ''
  try {
    await adminAppointmentApi.complete(session.token, item.id)
    adminAppointments.value = adminAppointments.value.map((record) =>
      record.id === item.id ? { ...record, status: 'COMPLETED' } : record)
  } catch (error) {
    appointmentError.value = error instanceof ApiError ? error.message : '确认就诊失败'
    await loadAdminAppointments()
  } finally {
    completingAppointmentId.value = null
  }
}

const waitlistStatusText: Record<WaitlistStatus, string> = {
  WAITING: '排队中', OFFERED: '待确认', CONFIRMED: '候补成功', EXPIRED: '已过期', CANCELLED: '已取消'
}

function formatDateTime(value: string | null | undefined) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function maskPhone(phone: string | null | undefined) {
  if (!phone) return ''
  if (phone.length < 7) return phone
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

function waitlistKeyTime(record: AdminWaitlistRecord) {
  if (record.status === 'WAITING') return { label: '加入候补', value: record.createdAt }
  if (record.status === 'OFFERED') return { label: '确认截止', value: record.offerExpireTime }
  if (record.status === 'CONFIRMED') return { label: '确认时间', value: record.confirmedAt }
  if (record.status === 'CANCELLED') return { label: '取消时间', value: record.cancelledAt }
  return { label: '过期时间', value: record.offerExpireTime }
}

function isOverdueOffer(record: AdminWaitlistRecord) {
  return record.status === 'OFFERED'
    && !!record.offerExpireTime
    && new Date(record.offerExpireTime).getTime() <= waitlistNow.value
}

function waitlistCountdown(expireTime: string | null) {
  if (!expireTime) return '-'
  const seconds = Math.max(0, Math.ceil((new Date(expireTime).getTime() - waitlistNow.value) / 1000))
  const minutes = Math.floor(seconds / 60)
  return `${String(minutes).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
}

function waitlistFlow(record: AdminWaitlistRecord) {
  const flow: WaitlistStatus[] = ['WAITING']
  if (record.status !== 'WAITING' && record.status !== 'CANCELLED') flow.push('OFFERED')
  if (record.status === 'CANCELLED') flow.push('CANCELLED')
  if (record.status === 'CONFIRMED') flow.push('CONFIRMED')
  if (record.status === 'EXPIRED') flow.push('EXPIRED')
  return flow
}

async function loadWaitlistFilterDoctors() {
  if (!session.token) return
  loadingWaitlistDoctors.value = true
  try {
    const result = await doctorApi.list(session.token, {
      page: 1,
      pageSize: 100,
      departmentId: waitlistFilters.departmentId || undefined
    })
    waitlistFilterDoctors.value = result.records
  } catch (error) {
    waitlistError.value = error instanceof ApiError ? error.message : '医生筛选数据加载失败'
  } finally {
    loadingWaitlistDoctors.value = false
  }
}

async function loadAdminWaitlists(page = waitlistPage.value) {
  if (!session.token) return
  loadingWaitlists.value = true
  waitlistError.value = ''
  try {
    const result = await adminWaitlistApi.page(session.token, {
      page,
      pageSize: waitlistPageSize.value,
      status: waitlistFilters.status as WaitlistStatus || undefined,
      scheduleDate: waitlistFilters.scheduleDate || undefined,
      departmentId: waitlistFilters.departmentId || undefined,
      doctorId: waitlistFilters.doctorId || undefined,
      patientKeyword: waitlistFilters.patientKeyword.trim() || undefined
    })
    waitlistRecords.value = result.records
    waitlistPage.value = result.page
    waitlistTotal.value = result.total
    waitlistTotalPages.value = result.totalPages
  } catch (error) {
    waitlistError.value = error instanceof ApiError ? error.message : '候补队列加载失败'
  } finally {
    loadingWaitlists.value = false
  }
}

async function prepareWaitlistPage() {
  await loadDepartments()
  await loadWaitlistFilterDoctors()
  await loadAdminWaitlists(1)
}

function searchWaitlists() {
  void loadAdminWaitlists(1)
}

function selectWaitlistStatus(status: '' | WaitlistStatus) {
  if (waitlistFilters.status === status) return
  waitlistFilters.status = status
  void loadAdminWaitlists(1)
}

function resetWaitlistFilters() {
  waitlistFilters.status = ''
  waitlistFilters.scheduleDate = ''
  waitlistFilters.departmentId = 0
  waitlistFilters.doctorId = 0
  waitlistFilters.patientKeyword = ''
  void loadWaitlistFilterDoctors()
  void loadAdminWaitlists(1)
}

function changeWaitlistDepartment() {
  waitlistFilters.doctorId = 0
  void loadWaitlistFilterDoctors()
}

function changeWaitlistPage(page: number) {
  if (page < 1 || page > waitlistTotalPages.value || page === waitlistPage.value) return
  void loadAdminWaitlists(page)
}

function changeWaitlistPageSize() {
  void loadAdminWaitlists(1)
}

async function openWaitlistQueue(record: AdminWaitlistRecord) {
  if (!session.token) return
  selectedWaitlistRecord.value = null
  selectedWaitlistQueue.value = null
  waitlistQueueError.value = ''
  loadingWaitlistQueue.value = true
  try {
    selectedWaitlistQueue.value = await adminWaitlistApi.slotQueue(session.token, record.scheduleSlotId)
  } catch (error) {
    waitlistQueueError.value = error instanceof ApiError ? error.message : '班次候补队列加载失败'
  } finally {
    loadingWaitlistQueue.value = false
  }
}

function refreshWaitlistCountdown() {
  waitlistNow.value = Date.now()
  if (active.value !== 'waitlist') return
  const expiredIds = waitlistRecords.value
    .filter((record) => record.status === 'OFFERED' && record.offerExpireTime && new Date(record.offerExpireTime).getTime() <= waitlistNow.value)
    .map((record) => record.id)
    .filter((id) => !refreshedExpiredOfferIds.has(id))
  if (expiredIds.length) {
    expiredIds.forEach((id) => refreshedExpiredOfferIds.add(id))
    void loadAdminWaitlists()
  }
}

function logout() {
  const token = session.token
  signOut(); router.replace('/login')
  if (token) void logoutRequest(token).catch(() => undefined)
}

watch(active, (value) => {
  if (value === 'dashboard') void loadDashboard()
  if (value === 'resource') { void loadDepartments(); if (resourceTab.value === 'doctors') void loadDoctorPage() }
  if (value === 'schedule') void prepareSchedulePage()
  if (value === 'appointments') void loadAdminAppointments()
  if (value === 'waitlist') void prepareWaitlistPage()
})
watch(resourceTab, (value) => { if (value === 'doctors') void loadDoctorPage() })
onMounted(() => {
  if (active.value === 'dashboard') void loadDashboard()
  if (active.value === 'resource') void loadDepartments()
  waitlistCountdownTimer = window.setInterval(refreshWaitlistCountdown, 1000)
})
onBeforeUnmount(() => { if (waitlistCountdownTimer !== undefined) window.clearInterval(waitlistCountdownTimer) })
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar"><div class="admin-brand"><span class="brand-mark">+</span>智约医疗<small>运营中心</small></div><nav><button v-for="menu in menus" :key="menu.id" :class="{ active: active === menu.id }" @click="active = menu.id"><i>{{ menu.icon }}</i>{{ menu.label }}</button></nav><div class="admin-user"><div class="avatar">管</div><span><b>{{ session.name }}</b><small>运营管理员</small></span><button @click="logout">退出</button></div></aside>
    <main class="admin-main">
      <header>
        <div><p class="eyebrow">OPERATIONS CONSOLE</p><h1>{{ menus.find((item) => item.id === active)?.label }}</h1></div>
        <div v-if="active === 'dashboard'" class="dashboard-refresh">
          <span v-if="dashboardUpdatedAt">最后更新 {{ dashboardUpdatedAt }}</span>
          <button class="table-action" :disabled="loadingDashboard" @click="loadDashboard">{{ loadingDashboard ? '刷新中…' : '刷新数据' }}</button>
        </div>
        <button v-else-if="active === 'waitlist'" class="table-action waitlist-refresh" :disabled="loadingWaitlists" @click="() => loadAdminWaitlists()">{{ loadingWaitlists ? '刷新中…' : '刷新' }}</button>
      </header>
      <template v-if="active === 'dashboard'">
        <p v-if="dashboardError" class="resource-error dashboard-error">{{ dashboardError }}</p>
        <div v-if="loadingDashboard && !dashboardSummary" class="resource-empty dashboard-loading">正在加载真实运营数据…</div>
        <template v-else-if="dashboardSummary">
          <div class="metrics">
            <article><small>今日总号源</small><strong>{{ dashboardSummary.totalCapacity }}</strong><span>{{ dashboardSummary.scheduleSlotCount }} 个开放班次</span></article>
            <article><small>今日剩余号源</small><strong>{{ dashboardSummary.remainingCapacity }}</strong><span>可预约率 {{ dashboardAvailabilityRate }}%</span></article>
            <article><small>今日有效预约</small><strong>{{ dashboardSummary.activeAppointmentCount }}</strong><span>待就诊与已完成</span></article>
            <article><small>今日候补排队</small><strong>{{ dashboardSummary.waitingCount }}</strong><span :class="{ 'metric-warning': dashboardSummary.overdueOfferedCount > 0 }">超时待补偿 {{ dashboardSummary.overdueOfferedCount }}</span></article>
          </div>
          <section class="panel dashboard-panel">
            <div class="panel-head"><div><h2>常用管理</h2><p>概览数据来自当前数据库，点击进入对应业务模块。</p></div></div>
            <div class="dashboard-actions">
              <button @click="active = 'schedule'"><i>◴</i><span><b>排班与放号</b><small>维护出诊班次、状态和号源容量</small></span><em>进入 →</em></button>
              <button @click="active = 'appointments'"><i>▣</i><span><b>预约订单</b><small>查看预约状态与就诊完成情况</small></span><em>进入 →</em></button>
              <button @click="active = 'waitlist'"><i>⇄</i><span><b>候补队列</b><small>查看 WAITING、OFFERED 和递补状态</small></span><em>进入 →</em></button>
              <button @click="active = 'resource'"><i>♧</i><span><b>医生与科室</b><small>维护科室、医生及启停状态</small></span><em>进入 →</em></button>
            </div>
          </section>
        </template>
      </template>
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
      <section v-else-if="active === 'appointments'" class="resource-page">
        <div class="resource-intro"><div><h2>预约订单</h2><p>查看患者预约记录；仅已到就诊开始时间且仍待就诊的订单可标记为完成。</p></div><button class="table-action" :disabled="loadingAppointments" @click="loadAdminAppointments">刷新</button></div>
        <p v-if="appointmentError" class="resource-error">{{ appointmentError }}</p>
        <div v-if="loadingAppointments" class="resource-empty">正在加载预约订单…</div>
        <section v-else-if="adminAppointments.length" class="doctor-admin-table"><table><thead><tr><th>预约单号</th><th>患者</th><th>医生 / 科室</th><th>就诊时段</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in adminAppointments" :key="item.id"><td>{{ item.appointmentNo }}</td><td><b>{{ item.patientName }}</b></td><td>{{ item.doctorName }}<small class="appointment-department">{{ item.departmentName }}</small></td><td>{{ item.scheduleDate }} {{ item.startTime.slice(0, 5) }}-{{ item.endTime.slice(0, 5) }}<small class="appointment-department">{{ item.sessionName }}</small></td><td><span class="status" :class="item.status.toLowerCase()">{{ appointmentStatusText(item.status) }}</span></td><td><button v-if="canCompleteAppointment(item)" class="table-action" :disabled="completingAppointmentId !== null" @click="completeAppointment(item)">{{ completingAppointmentId === item.id ? '处理中…' : '确认完成' }}</button><span v-else class="appointment-action-hint">{{ item.status === 'BOOKED' ? '未到就诊时间' : '-' }}</span></td></tr></tbody></table></section>
        <div v-else class="resource-empty">暂无预约订单。</div>
      </section>
      <section v-else-if="active === 'waitlist'" class="resource-page">
        <p class="waitlist-page-description">只读查看候补状态、真实 FIFO 顺序与已保留的候补资格；不支持人工调整队列。</p>
        <nav class="waitlist-status-tabs" aria-label="候补状态筛选">
          <button v-for="option in waitlistStatusOptions" :key="option.value || 'ALL'" type="button" :class="{ active: waitlistFilters.status === option.value }" @click="selectWaitlistStatus(option.value)">{{ option.label }}</button>
        </nav>
        <form class="waitlist-filter-bar" @submit.prevent="searchWaitlists">
          <label class="waitlist-filter-field"><span>排班日期</span><input v-model="waitlistFilters.scheduleDate" type="date"></label>
          <label class="waitlist-filter-field"><span>科室</span><select v-model.number="waitlistFilters.departmentId" @change="changeWaitlistDepartment"><option :value="0">全部科室</option><option v-for="department in departments" :key="department.id" :value="department.id">{{ department.name }}</option></select></label>
          <label class="waitlist-filter-field"><span>医生</span><select v-model.number="waitlistFilters.doctorId" :disabled="loadingWaitlistDoctors"><option :value="0">{{ loadingWaitlistDoctors ? '加载医生中…' : '全部医生' }}</option><option v-for="doctor in waitlistFilterDoctors" :key="doctor.id" :value="doctor.id">{{ doctor.name }} · {{ doctor.departmentName }}</option></select></label>
          <label class="waitlist-filter-field waitlist-keyword"><span>患者</span><input v-model.trim="waitlistFilters.patientKeyword" type="search" placeholder="患者姓名 / 用户名 / 手机号"></label>
          <div class="waitlist-filter-actions"><button class="primary" type="submit">查询</button><button class="doctor-search-clear" type="button" @click="resetWaitlistFilters">重置</button></div>
        </form>
        <p v-if="waitlistError" class="resource-error">{{ waitlistError }}</p>
        <div v-if="loadingWaitlists" class="resource-empty">正在加载候补记录…</div>
        <section v-else-if="waitlistRecords.length" class="doctor-admin-table waitlist-admin-table"><table><thead><tr><th>候补单 / 患者</th><th>医生与排班</th><th>状态</th><th>关键时间</th><th>操作</th></tr></thead><tbody><tr v-for="record in waitlistRecords" :key="record.id" :class="{ 'waitlist-row-alert': isOverdueOffer(record) }"><td><div class="waitlist-patient"><b>#{{ record.id }} · {{ record.patientName }}</b><small>{{ record.username }}{{ record.phone ? ` · ${maskPhone(record.phone)}` : '' }}</small></div></td><td><div class="waitlist-schedule"><b>{{ record.doctorName }} · {{ record.departmentName }}</b><span>{{ record.scheduleDate }} {{ record.startTime.slice(0, 5) }}–{{ record.endTime.slice(0, 5) }} · {{ record.sessionName }}</span></div></td><td><span class="status" :class="record.status.toLowerCase()">{{ waitlistStatusText[record.status] }}</span><small v-if="record.status === 'OFFERED'" class="waitlist-countdown">{{ isOverdueOffer(record) ? '等待系统补偿' : `剩余 ${waitlistCountdown(record.offerExpireTime)}` }}</small></td><td><div class="waitlist-key-time"><small>{{ waitlistKeyTime(record).label }}</small><span>{{ formatDateTime(waitlistKeyTime(record).value) }}</span></div></td><td><button class="table-action waitlist-detail-button" @click="selectedWaitlistRecord = record">详情</button></td></tr></tbody></table></section>
        <div v-else class="resource-empty">没有符合筛选条件的候补记录。</div>
        <div v-if="waitlistTotal > 0" class="doctor-pagination waitlist-pagination"><label>每页<select v-model.number="waitlistPageSize" @change="changeWaitlistPageSize"><option :value="10">10 条</option><option :value="20">20 条</option><option :value="50">50 条</option></select></label><span>共 {{ waitlistTotal }} 条候补记录</span><div><button :disabled="waitlistPage === 1" @click="changeWaitlistPage(waitlistPage - 1)">上一页</button><b>第 {{ waitlistPage }} / {{ waitlistTotalPages }} 页</b><button :disabled="waitlistPage === waitlistTotalPages" @click="changeWaitlistPage(waitlistPage + 1)">下一页</button></div></div>

        <div v-if="selectedWaitlistQueue || loadingWaitlistQueue || waitlistQueueError" class="modal-mask" @click.self="selectedWaitlistQueue = null; waitlistQueueError = ''"><section class="department-modal waitlist-drawer"><div class="modal-head"><div><h2>班次候补队列</h2><p>排名由后端按 created_at、id 的真实 FIFO 规则返回。</p></div><button class="modal-close" @click="selectedWaitlistQueue = null; waitlistQueueError = ''">×</button></div><div v-if="loadingWaitlistQueue" class="resource-empty">正在加载班次候补队列…</div><p v-else-if="waitlistQueueError" class="resource-error">{{ waitlistQueueError }}</p><template v-else-if="selectedWaitlistQueue"><div class="detail-date"><b>{{ selectedWaitlistQueue.departmentName }} · {{ selectedWaitlistQueue.doctorName }}</b><span>{{ selectedWaitlistQueue.scheduleDate }} {{ selectedWaitlistQueue.startTime.slice(0, 5) }}-{{ selectedWaitlistQueue.endTime.slice(0, 5) }} · {{ selectedWaitlistQueue.sessionName }}</span></div><div class="detail-capacity"><article><small>当前总号源</small><strong>{{ selectedWaitlistQueue.totalCapacity }}</strong></article><article><small>当前剩余号源</small><strong>{{ selectedWaitlistQueue.remainingCapacity }}</strong></article><article><small>WAITING 人数</small><strong>{{ selectedWaitlistQueue.waitingCount }}</strong></article></div><section class="waitlist-offered"><h3>当前候补资格</h3><p v-if="!selectedWaitlistQueue.offeredCandidates.length" class="appointment-action-hint">当前没有待确认候补资格。</p><article v-for="candidate in selectedWaitlistQueue.offeredCandidates" :key="candidate.id"><b>{{ candidate.patientName }}</b><span>{{ candidate.username }}{{ candidate.phone ? ` · ${maskPhone(candidate.phone)}` : '' }}</span><small>截止：{{ formatDateTime(candidate.offerExpireTime) }}（剩余 {{ waitlistCountdown(candidate.offerExpireTime) }}）</small></article></section><section class="waitlist-fifo"><h3>WAITING FIFO 队列</h3><table><thead><tr><th>排名</th><th>患者</th><th>加入时间</th><th>状态</th></tr></thead><tbody><tr v-for="candidate in selectedWaitlistQueue.waitingCandidates" :key="candidate.id"><td>#{{ candidate.queuePosition }}</td><td><b>{{ candidate.patientName }}</b><small class="appointment-department">{{ candidate.username }}{{ candidate.phone ? ` · ${maskPhone(candidate.phone)}` : '' }}</small></td><td>{{ formatDateTime(candidate.createdAt) }}</td><td><span class="status waiting">排队中</span></td></tr></tbody></table><p v-if="!selectedWaitlistQueue.waitingCandidates.length" class="appointment-action-hint">当前没有 WAITING 候补用户。</p></section></template></section></div>

        <div v-if="selectedWaitlistRecord" class="modal-mask" @click.self="selectedWaitlistRecord = null"><section class="department-modal waitlist-detail"><div class="modal-head"><div><h2>候补记录 #{{ selectedWaitlistRecord.id }}</h2><p>{{ selectedWaitlistRecord.patientName }} · {{ selectedWaitlistRecord.doctorName }} · {{ selectedWaitlistRecord.departmentName }}</p></div><button class="modal-close" @click="selectedWaitlistRecord = null">×</button></div><div class="waitlist-flow"><template v-for="(step, index) in waitlistFlow(selectedWaitlistRecord)" :key="step"><i v-if="index">→</i><span :class="{ current: step === selectedWaitlistRecord.status }">{{ waitlistStatusText[step] }}</span></template></div><dl class="waitlist-detail-grid"><div><dt>加入候补时间</dt><dd>{{ formatDateTime(selectedWaitlistRecord.createdAt) }}</dd></div><div><dt>OFFERED 过期时间</dt><dd>{{ formatDateTime(selectedWaitlistRecord.offerExpireTime) }}</dd></div><div><dt>确认时间</dt><dd>{{ formatDateTime(selectedWaitlistRecord.confirmedAt) }}</dd></div><div><dt>取消时间</dt><dd>{{ formatDateTime(selectedWaitlistRecord.cancelledAt) }}</dd></div></dl><p class="detail-note">当前候补表没有独立状态历史表；确认/取消时间仅在该记录当前处于对应终态时，由 updated_at 展示。</p><div class="modal-actions"><button class="modal-cancel" @click="openWaitlistQueue(selectedWaitlistRecord)">查看班次队列</button><button class="primary" @click="selectedWaitlistRecord = null">关闭</button></div></section></div>
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
                <p>{{ slot.startTime.slice(0, 5) }}–{{ slot.endTime.slice(0, 5) }}</p>
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
          </div>
        </div>
        <button type="button" class="add-session" @click="addScheduleSession">+ 添加出诊班次</button>
        <p class="schedule-preview">预计生成 {{ estimatedSessionCount }} 个班次。</p>
        <p v-if="scheduleError" class="resource-error">{{ scheduleError }}</p>
        <div class="modal-actions"><button type="button" class="modal-cancel" @click="scheduleModalVisible = false">取消</button><button class="primary" :disabled="savingSchedule">{{ savingSchedule ? '生成中…' : '确认排班' }}</button></div>
      </form>
    </div>
    <div v-if="selectedScheduleSlot" class="modal-mask schedule-detail-mask" @click.self="selectedScheduleSlot = null">
      <section class="department-modal schedule-detail">
        <div class="modal-head"><div><h2>{{ selectedScheduleSlot.sessionName }}</h2><p>{{ selectedScheduleSlot.doctorName }} · {{ selectedScheduleSlot.departmentName }}</p></div><button class="modal-close" @click="selectedScheduleSlot = null">×</button></div>
        <div class="detail-date"><b>{{ selectedScheduleSlot.scheduleDate }}</b><span>{{ selectedScheduleSlot.startTime.slice(0, 5) }}–{{ selectedScheduleSlot.endTime.slice(0, 5) }}</span></div>
        <div class="detail-capacity"><article><small>总号源</small><strong>{{ selectedScheduleSlot.totalCapacity }}</strong></article><article><small>已预约</small><strong>{{ selectedScheduleSlot.bookedCapacity }}</strong></article><article><small>剩余</small><strong>{{ selectedScheduleSlot.remainingCapacity }}</strong></article></div>
        <div class="modal-actions"><button class="modal-cancel" @click="changeScheduleCapacity(selectedScheduleSlot)">调整号源</button><button class="primary" @click="toggleScheduleStatus(selectedScheduleSlot)">{{ selectedScheduleSlot.status === 'OPEN' ? '关闭班次' : '重新开放' }}</button></div>
      </section>
    </div>
  </div>
</template>
