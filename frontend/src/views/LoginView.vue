<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login as loginRequest, ApiError } from '../api/auth'
import { signIn, type UserRole } from '../stores/session'

const router = useRouter()
const role = ref<UserRole>('PATIENT')
const account = ref('patient_demo')
const password = ref('123456')
const error = ref('')
const loading = ref(false)

function switchRole(value: UserRole) {
  role.value = value
  account.value = value === 'PATIENT' ? 'patient_demo' : 'staff_demo'
  password.value = '123456'
  error.value = ''
}

async function login() {
  if (loading.value) return
  if (!account.value || !password.value) {
    error.value = '请输入账号和密码'
    return
  }

  loading.value = true
  error.value = ''
  try {
    const result = await loginRequest(account.value, password.value)
    signIn(result)
    router.replace(result.role === 'STAFF' ? '/admin' : '/')
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-intro">
      <div class="login-brand"><span class="brand-mark">+</span>智约医疗</div>
      <div class="intro-copy">
        <p class="eyebrow">SMART APPOINTMENT</p>
        <h1>让号源调度<br>更有秩序。</h1>
        <p>面向患者的便捷预约服务，和面向工作人员的可靠运营平台。</p>
      </div>
      <div class="intro-points">
        <span>✓ 号源实时调度</span>
        <span>✓ 候补自动递补</span>
        <span>✓ 预约全程可追溯</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-box">
        <p class="eyebrow">WELCOME BACK</p>
        <h2>登录智约医疗</h2>
        <p class="login-subtitle">请选择您的身份后继续</p>
        <div class="role-switch">
          <button :class="{ active: role === 'PATIENT' }" type="button" @click="switchRole('PATIENT')">
            <b>患者</b><small>预约、候补与就诊提醒</small>
          </button>
          <button :class="{ active: role === 'STAFF' }" type="button" @click="switchRole('STAFF')">
            <b>工作人员</b><small>排班、号源与运营管理</small>
          </button>
        </div>
        <form @submit.prevent="login">
          <label>账号<input v-model.trim="account" autocomplete="username" placeholder="请输入账号" :disabled="loading"></label>
          <label>密码<input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码" :disabled="loading"></label>
          <p v-if="error" class="form-error">{{ error }}</p>
          <button class="login-submit" :disabled="loading">
            {{ loading ? '登录中…' : `登录${role === 'PATIENT' ? '患者平台' : '运营平台'} →` }}
          </button>
        </form>
        <p class="demo-hint">演示账号会在本地数据库初始化后可用，密码均为 123456</p>
      </div>
    </section>
  </main>
</template>
