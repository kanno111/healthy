<script setup lang="ts">
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import { session, signOut } from './stores/session'
const route = useRoute(); const router = useRouter()
const patientPage = computed(() => session.role === 'PATIENT' && route.path !== '/login')
function logout() { signOut(); router.push('/login') }
</script>
<template>
  <header v-if="patientPage" class="topbar"><RouterLink to="/" class="brand"><span class="brand-mark">+</span>智约医疗</RouterLink><nav><RouterLink to="/">预约挂号</RouterLink><RouterLink to="/appointments">我的预约</RouterLink></nav><button class="user user-button" @click="logout">{{ session.name }} · 退出</button></header>
  <main><RouterView /></main>
</template>
