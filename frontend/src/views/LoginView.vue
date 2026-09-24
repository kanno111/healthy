<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login as loginRequest, register as registerRequest, ApiError } from '../api/auth'
import { signIn, type UserRole } from '../stores/session'

type AuthMode = 'LOGIN' | 'REGISTER'
const router = useRouter()
const mode = ref<AuthMode>('LOGIN')
const loginRole = ref<UserRole>('PATIENT')
const account = ref('')
const password = ref('')
const error = ref('')
const notice = ref('')
const loading = ref(false)
const registerForm = reactive({ username: '', password: '', name: '', phone: '', gender: 1 as 1 | 2 })

function switchMode(value: AuthMode) { mode.value = value; error.value = ''; notice.value = '' }
function switchLoginRole(value: UserRole) {
  loginRole.value = value
  account.value = ''
  password.value = ''
  error.value = ''
}
async function login() {
  if (!account.value || !password.value) { error.value = '请输入账号和密码'; return }
  const result = await loginRequest(account.value, password.value)
  signIn(result)
  await router.replace(result.role === 'STAFF' ? '/admin' : result.role === 'DOCTOR' ? '/doctor/appointments' : '/')
}
async function register() {
  await registerRequest(registerForm)
  account.value = registerForm.username
  password.value = ''
  loginRole.value = 'PATIENT'
  mode.value = 'LOGIN'
  notice.value = '注册成功，请使用新账号登录'
}
async function submit() {
  if (loading.value) return
  loading.value = true; error.value = ''; notice.value = ''
  try { if (mode.value === 'LOGIN') await login(); else await register() }
  catch (e) { error.value = e instanceof ApiError ? e.message : '操作失败，请稍后重试' }
  finally { loading.value = false }
}
</script>

<template>
  <main class="login-page">
    <section class="login-intro">
      <div class="login-brand"><span class="brand-mark">+</span>智约医疗</div>
      <div class="intro-copy"><p class="eyebrow">SMART APPOINTMENT</p><h1>让号源调度<br>更有秩序。</h1><p>面向患者的便捷预约服务，和面向管理人员的可靠运营平台。</p></div>
      <div class="intro-points"><span>✓ 号源实时调度</span><span>✓ 候补自动递补</span><span>✓ 预约全程可追溯</span></div>
    </section>
    <section class="login-panel"><div class="login-box">
      <p class="eyebrow">WELCOME</p>
      <h2>{{ mode === 'LOGIN' ? '登录智约医疗' : '注册患者账号' }}</h2>
      <div class="auth-mode">
        <button type="button" :class="{ active: mode === 'LOGIN' }" @click="switchMode('LOGIN')">登录</button>
        <button type="button" :class="{ active: mode === 'REGISTER' }" @click="switchMode('REGISTER')">患者注册</button>
      </div>
      <form @submit.prevent="submit">
        <template v-if="mode === 'LOGIN'">
          <div class="role-switch">
            <button :class="{ active: loginRole === 'PATIENT' }" type="button" @click="switchLoginRole('PATIENT')"><b>患者</b><small>预约与查看就诊记录</small></button>
            <button :class="{ active: loginRole === 'DOCTOR' }" type="button" @click="switchLoginRole('DOCTOR')"><b>医生</b><small>查看预约与完成就诊</small></button>
            <button :class="{ active: loginRole === 'STAFF' }" type="button" @click="switchLoginRole('STAFF')"><b>管理员</b><small>维护医生、排班与号源</small></button>
          </div>
          <label>账号<input v-model.trim="account" autocomplete="username" placeholder="请输入账号" :disabled="loading"></label>
          <label>密码<input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码" :disabled="loading"></label>
        </template>
        <template v-else>
          <label>姓名<input v-model.trim="registerForm.name" autocomplete="name" placeholder="请输入真实姓名" :disabled="loading"></label>
          <label>账号<input v-model.trim="registerForm.username" autocomplete="username" placeholder="4 到 50 个字符" :disabled="loading"></label>
          <label>手机号<input v-model.trim="registerForm.phone" autocomplete="tel" placeholder="请输入 11 位手机号" :disabled="loading"></label>
          <label>性别<select v-model.number="registerForm.gender" :disabled="loading"><option :value="1">男</option><option :value="2">女</option></select></label>
          <label>密码<input v-model="registerForm.password" type="password" autocomplete="new-password" placeholder="至少 6 个字符" :disabled="loading"></label>
        </template>
        <p v-if="error" class="form-error">{{ error }}</p><p v-if="notice" class="form-notice">{{ notice }}</p>
        <button class="login-submit" :disabled="loading">{{ loading ? '提交中…' : mode === 'LOGIN' ? '登录 →' : '注册患者账号 →' }}</button>
      </form>
    </div></section>
  </main>
</template>

<style scoped>
.role-switch { grid-template-columns: repeat(3, minmax(0, 1fr)); }
@media (max-width: 560px) { .role-switch { grid-template-columns: 1fr; } }
</style>
