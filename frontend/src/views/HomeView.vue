<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { doctors } from '../api/mock'
const keyword = ref('')
const list = computed(() => doctors.filter(d => `${d.name}${d.department}${d.specialty}`.includes(keyword.value)))
</script>
<template>
  <section class="hero"><div><p class="eyebrow">让就医预约更从容</p><h1>找到合适的医生，<br>安排合适的时间。</h1><p>线上预约、候补与提醒服务，减少不必要的等待。</p><label class="search"><span>⌕</span><input v-model="keyword" placeholder="搜索科室、医生或擅长方向"><button>搜索</button></label></div><div class="hero-card"><b>今日服务</b><strong>126</strong><span>个可预约时段</span><small>号源实时更新，以最终提交结果为准</small></div></section>
  <section class="page-section"><div class="section-title"><div><p class="eyebrow">DEPARTMENTS</p><h2>按科室预约</h2></div><a>查看全部 →</a></div><div class="departments"><button v-for="item in ['内科','外科','儿科','妇产科','眼科','口腔科']" :key="item">{{ item }}<small>查看医生 →</small></button></div></section>
  <section class="page-section"><div class="section-title"><div><p class="eyebrow">DOCTORS</p><h2>推荐医生</h2></div></div><div class="doctor-grid"><article v-for="doctor in list" :key="doctor.id" class="doctor-card"><div class="avatar" :style="{ background: doctor.color }">{{ doctor.initials }}</div><div><h3>{{ doctor.name }} <small>{{ doctor.title }}</small></h3><p class="muted">{{ doctor.department }}</p><p>{{ doctor.specialty }}</p><RouterLink :to="`/doctor/${doctor.id}`" class="text-link">查看号源 →</RouterLink></div></article></div></section>
</template>
