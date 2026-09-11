<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { doctors } from '../api/mock'

const keyword = ref('')
const selectedDepartment = ref('全部科室')
const departments = computed(() => ['全部科室', ...new Set(doctors.map((doctor) => doctor.department))])
const doctorList = computed(() => doctors.filter((doctor) => {
  const matchedDepartment = selectedDepartment.value === '全部科室' || doctor.department === selectedDepartment.value
  const matchedKeyword = `${doctor.name}${doctor.department}${doctor.specialty}`.includes(keyword.value.trim())
  return matchedDepartment && matchedKeyword
}))

function selectDepartment(department: string) { selectedDepartment.value = department }
</script>

<template>
  <section class="department-page">
    <header class="department-page__header">
      <div><p class="eyebrow">APPOINTMENT</p><h1>选择科室</h1><p>选择科室后，查看可预约医生与号源。</p></div>
      <label class="department-search"><span aria-hidden="true">⌕</span><input v-model.trim="keyword" placeholder="搜索科室、医生、擅长方向"></label>
    </header>
    <div class="department-layout">
      <aside class="department-sidebar" aria-label="科室列表">
        <button v-for="department in departments" :key="department" :class="{ active: selectedDepartment === department }" type="button" @click="selectDepartment(department)">{{ department }}</button>
      </aside>
      <main class="department-content">
        <div class="department-content__head"><div><h2>{{ selectedDepartment }}</h2><span>共 {{ doctorList.length }} 位医生</span></div></div>
        <div v-if="doctorList.length" class="doctor-list">
          <article v-for="doctor in doctorList" :key="doctor.id" class="doctor-list-item">
            <div class="doctor-list-avatar" :style="{ background: doctor.color }">{{ doctor.initials }}</div>
            <div class="doctor-list-info">
              <div class="doctor-list-name"><h3>{{ doctor.name }}</h3><span>{{ doctor.title }}</span></div>
              <p>{{ doctor.department }}</p><small>{{ doctor.specialty }}</small>
              <b :class="{ unavailable: !doctor.slots.some((slot) => slot.remaining > 0) }">{{ doctor.slots.some((slot) => slot.remaining > 0) ? '有号' : '暂无号源' }}</b>
            </div>
            <RouterLink :to="`/doctor/${doctor.id}`" class="doctor-list-link">查看号源 →</RouterLink>
          </article>
        </div>
        <div v-else class="department-empty">没有找到匹配的医生或科室</div>
      </main>
    </div>
  </section>
</template>
