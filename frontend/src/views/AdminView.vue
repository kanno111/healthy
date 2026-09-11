<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { doctors } from '../api/mock'
import { ApiError, logout as logoutRequest } from '../api/auth'
import { departmentApi, type Department } from '../api/department'
import { doctorApi, type Doctor } from '../api/doctor'
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
const doctorError = ref('')
const loadingDoctors = ref(false)
const doctorModalVisible = ref(false)
const editingDoctor = ref<Doctor | null>(null)
const savingDoctor = ref(false)
const doctorForm = reactive({ name: '', gender: 1, departmentId: 0, doctorCode: '', title: '', introduction: '', sortOrder: 0 })
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

async function loadDoctors() {
  if (!session.token) return
  loadingDoctors.value = true
  doctorError.value = ''
  try { managedDoctors.value = await doctorApi.list(session.token) }
  catch (error) { doctorError.value = error instanceof ApiError ? error.message : '医生数据加载失败' }
  finally { loadingDoctors.value = false }
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
    const id = editingDoctor.value ? (await doctorApi.update(session.token, editingDoctor.value.id, payload), editingDoctor.value.id) : await doctorApi.create(session.token, payload)
    const saved = await doctorApi.getById(session.token, id)
    managedDoctors.value = [...managedDoctors.value.filter((doctor) => doctor.id !== id), saved].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
    doctorModalVisible.value = false
  } catch (error) { doctorError.value = error instanceof ApiError ? error.message : '医生保存失败' }
  finally { savingDoctor.value = false }
}

async function toggleDoctorStatus(doctor: Doctor) {
  if (!session.token) return
  const status = doctor.status === 1 ? 0 : 1
  try { await doctorApi.updateStatus(session.token, doctor.id, status); managedDoctors.value = managedDoctors.value.map((item) => item.id === doctor.id ? { ...item, status } : item) }
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

watch(active, (value) => { if (value === 'resource') { void loadDepartments(); void loadDoctors() } })
watch(resourceTab, (value) => { if (value === 'doctors') void loadDoctors() })
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
          <div class="resource-intro"><div><h2>医生列表</h2><p>维护医生资料、所属科室与出诊状态。</p></div></div>
          <p v-if="doctorError" class="resource-error">{{ doctorError }}</p>
          <div v-if="loadingDoctors" class="resource-empty">正在加载医生数据…</div>
          <section v-else-if="managedDoctors.length" class="doctor-admin-table"><table><thead><tr><th>医生</th><th>所属科室</th><th>职称</th><th>工号</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="doctor in managedDoctors" :key="doctor.id"><td><b>{{ doctor.name }}</b></td><td>{{ doctor.departmentName }}</td><td>{{ doctor.title || '-' }}</td><td>{{ doctor.doctorCode }}</td><td><button class="status-chip status-button" @click="toggleDoctorStatus(doctor)">{{ doctor.status === 1 ? '启用' : '停用' }}</button></td><td><button class="table-action" @click="openEditDoctor(doctor)">编辑</button></td></tr></tbody></table></section>
          <div v-else class="resource-empty">还没有医生，请先创建启用的科室，再新增医生。</div>
        </template>
      </section>
      <section v-else class="panel module-placeholder"><div class="module-icon">{{ menus.find((item) => item.id === active)?.icon }}</div><h2>{{ menus.find((item) => item.id === active)?.label }}</h2><p>该模块将在预约核心链路完成后接入对应的管理接口与业务规则。</p></section>
    </main>
    <div v-if="departmentModalVisible" class="modal-mask" @click.self="departmentModalVisible = false"><form class="department-modal" @submit.prevent="saveDepartment"><div class="modal-head"><div><h2>{{ editingDepartment ? '编辑科室' : '新建科室' }}</h2><p>科室名称在系统内必须唯一。</p></div><button type="button" class="modal-close" @click="departmentModalVisible = false">×</button></div><label>科室名称<input v-model.trim="departmentForm.name" maxlength="50" required placeholder="例如：心血管内科"></label><label>科室简介<textarea v-model.trim="departmentForm.description" maxlength="500" placeholder="简要介绍科室服务范围"></textarea></label><label>排序值<input v-model.number="departmentForm.sortOrder" type="number" min="0" required></label><p v-if="departmentError" class="resource-error">{{ departmentError }}</p><div class="modal-actions"><button type="button" class="modal-cancel" @click="departmentModalVisible = false">取消</button><button class="primary" :disabled="savingDepartment">{{ savingDepartment ? '保存中…' : '保存' }}</button></div></form></div>
    <div v-if="doctorModalVisible" class="modal-mask" @click.self="doctorModalVisible = false"><form class="department-modal" @submit.prevent="saveDoctor"><div class="modal-head"><div><h2>{{ editingDoctor ? '编辑医生' : '新增医生' }}</h2><p>医生工号在系统内必须唯一。</p></div><button type="button" class="modal-close" @click="doctorModalVisible = false">×</button></div><label>医生姓名<input v-model.trim="doctorForm.name" maxlength="50" required></label><label>所属科室<select v-model.number="doctorForm.departmentId" required><option :value="0" disabled>请选择科室</option><option v-for="department in enabledDepartments" :key="department.id" :value="department.id">{{ department.name }}</option></select></label><label>性别<select v-model.number="doctorForm.gender"><option :value="1">男</option><option :value="2">女</option></select></label><label>医生工号<input v-model.trim="doctorForm.doctorCode" maxlength="32" required></label><label>职称<input v-model.trim="doctorForm.title" maxlength="50"></label><label>医生简介<textarea v-model.trim="doctorForm.introduction" maxlength="5000"></textarea></label><label>排序值<input v-model.number="doctorForm.sortOrder" type="number" min="0" required></label><p v-if="doctorError" class="resource-error">{{ doctorError }}</p><div class="modal-actions"><button type="button" class="modal-cancel" @click="doctorModalVisible = false">取消</button><button class="primary" :disabled="savingDoctor">{{ savingDoctor ? '保存中…' : '保存' }}</button></div></form></div>
  </div>
</template>
