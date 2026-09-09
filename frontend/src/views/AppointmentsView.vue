<script setup lang="ts">
import { ref } from 'vue'
import { api } from '../api/mock'
import type { Appointment, AppointmentStatus } from '../types'
const items = ref<Appointment[]>([]); const filter = ref<'ALL' | AppointmentStatus>('ALL')
api.listAppointments().then(v => items.value = v)
const labels: Record<AppointmentStatus, string> = { PENDING_PAYMENT: '待支付', CONFIRMED: '已预约', WAITLISTED: '候补中', CANCELLED: '已取消', COMPLETED: '已完成', NO_SHOW: '已爽约' }
async function cancel(id: string) { await api.cancel(id); items.value = await api.listAppointments() }
</script>
<template><section class="page-section"><p class="eyebrow">MY APPOINTMENTS</p><h1>我的预约</h1><div class="filters"><button v-for="x in ['ALL','PENDING_PAYMENT','CONFIRMED','WAITLISTED','CANCELLED']" :key="x" :class="{ active: filter === x }" @click="filter = x as typeof filter">{{ x === 'ALL' ? '全部' : labels[x as AppointmentStatus] }}</button></div><div class="appointment-list"><article v-for="item in items.filter(x => filter === 'ALL' || x.status === filter)" :key="item.id" class="appointment-card"><div class="date-block"><b>{{ item.time.slice(5, 10) }}</b><small>{{ item.time.slice(11) }}</small></div><div><h3>{{ item.doctor }} <span>{{ item.department }}</span></h3><p>{{ item.id }} · 挂号费 ¥{{ item.fee }}</p><small v-if="item.expiresAt">请于今日 {{ item.expiresAt }} 前完成支付</small></div><span class="status" :class="item.status.toLowerCase()">{{ labels[item.status] }}</span><div class="actions"><button v-if="item.status === 'PENDING_PAYMENT'" class="primary">去支付</button><button v-if="['PENDING_PAYMENT','CONFIRMED','WAITLISTED'].includes(item.status)" @click="cancel(item.id)">取消{{ item.status === 'WAITLISTED' ? '候补' : '预约' }}</button></div></article></div></section></template>
