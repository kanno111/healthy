<script setup lang="ts">
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import { session, signOut } from './stores/session'
import { logout as logoutRequest } from './api/auth'

const route = useRoute()
const router = useRouter()
const patientPage = computed(() => session.role === 'PATIENT' && route.path !== '/login')

function logout() {
  const token = session.token
  signOut()
  router.replace('/login')
  if (token) {
    void logoutRequest(token).catch(() => undefined)
  }
}
</script>

<template>
  <header v-if="patientPage" class="topbar">
    <RouterLink to="/" class="brand"><span class="brand-mark">+</span>智能医疗</RouterLink>
    <nav>
      <RouterLink to="/">预约挂号</RouterLink>
      <RouterLink to="/appointments">我的预约</RouterLink>
      <RouterLink to="/waitlists">我的候补</RouterLink>
    </nav>
    <button class="user user-button" @click="logout">{{ session.name }} · 退出</button>
  </header>
  <main><RouterView /></main>
</template>
