<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { signIn, type UserRole } from '../stores/session'
const router = useRouter()
const role = ref<UserRole>('PATIENT')
const account = ref('patient_demo')
const password = ref('123456')
const error = ref('')
function switchRole(value: UserRole) { role.value = value; account.value = value === 'PATIENT' ? 'patient_demo' : 'staff_demo'; password.value = '123456'; error.value = '' }
function login() { if (!account.value || !password.value) { error.value = '请输入账号和密码'; return }; signIn(role.value, role.value === 'PATIENT' ? '张小明' : '王管理员'); router.replace(role.value === 'PATIENT' ? '/' : '/admin') }
</script>
<template><main class="login-page"><section class="login-intro"><div class="login-brand"><span class="brand-mark">+</span>智约医疗</div><div class="intro-copy"><p class="eyebrow">SMART APPOINTMENT</p><h1>让号源调度<br>更有秩序。</h1><p>面向患者的便捷预约服务，和面向工作人员的可靠运营平台。</p></div><div class="intro-points"><span>✓ 号源实时调度</span><span>✓ 候补自动递补</span><span>✓ 预约全程可追溯</span></div></section><section class="login-panel"><div class="login-box"><p class="eyebrow">WELCOME BACK</p><h2>登录智约医疗</h2><p class="login-subtitle">请选择您的身份后继续</p><div class="role-switch"><button :class="{ active: role === 'PATIENT' }" @click="switchRole('PATIENT')"><b>患者</b><small>预约、候补与就诊提醒</small></button><button :class="{ active: role === 'STAFF' }" @click="switchRole('STAFF')"><b>工作人员</b><small>排班、号源与运营管理</small></button></div><form @submit.prevent="login"><label>账号<input v-model="account" autocomplete="username" placeholder="请输入账号"></label><label>密码<input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码"></label><p v-if="error" class="form-error">{{ error }}</p><button class="login-submit">登录{{ role === 'PATIENT' ? '患者平台' : '运营平台' }} →</button></form><p class="demo-hint">演示账号已自动填入，密码均为 123456</p></div></section></main></template>
