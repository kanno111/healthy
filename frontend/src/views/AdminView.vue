<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { doctors } from '../api/mock'
import { session, signOut } from '../stores/session'
import { logout as logoutRequest } from '../api/auth'
const router = useRouter(); const active = ref('dashboard')
const menus = [{ id: 'dashboard', label: '运营概览', icon: '▦' }, { id: 'schedule', label: '排班与放号', icon: '◫' }, { id: 'appointments', label: '预约订单', icon: '▤' }, { id: 'waitlist', label: '候补队列', icon: '↻' }, { id: 'resource', label: '医生与科室', icon: '♙' }, { id: 'rules', label: '爽约规则', icon: '⚙' }]
function logout() {
  const token = session.token
  signOut()
  router.replace('/login')
  if (token) {
    void logoutRequest(token).catch(() => undefined)
  }
}
</script>
<template><div class="admin-shell"><aside class="admin-sidebar"><div class="admin-brand"><span class="brand-mark">+</span>智约医疗<small>运营中心</small></div><nav><button v-for="menu in menus" :key="menu.id" :class="{ active: active === menu.id }" @click="active = menu.id"><i>{{ menu.icon }}</i>{{ menu.label }}</button></nav><div class="admin-user"><div class="avatar">王</div><span><b>{{ session.name }}</b><small>运营管理员</small></span><button @click="logout">退出</button></div></aside><main class="admin-main"><header><div><p class="eyebrow">OPERATIONS CONSOLE</p><h1>{{ menus.find(x => x.id === active)?.label }}</h1></div><p>2026年9月8日 · 星期二</p></header><template v-if="active === 'dashboard'"><div class="metrics"><article><small>今日开放号源</small><strong>240</strong><span>较昨日 +12%</span></article><article><small>今日预约</small><strong>186</strong><span>预约率 77.5%</span></article><article><small>候补排队中</small><strong>23</strong><span>等待自动递补</span></article><article><small>待处理爽约</small><strong>8</strong><span>需执行规则校验</span></article></div><div class="admin-grid"><section class="panel"><div class="panel-head"><h2>近期排班</h2><button class="primary" @click="active = 'schedule'">查看排班</button></div><table><thead><tr><th>医生</th><th>科室</th><th>号源</th><th>状态</th></tr></thead><tbody><tr v-for="d in doctors" :key="d.id"><td>{{ d.name }}</td><td>{{ d.department }}</td><td>{{ d.slots.reduce((n,s) => n + s.remaining, 0) }} / 20</td><td><span class="tag">已放号</span></td></tr></tbody></table></section><section class="panel"><h2>候补队列</h2><ol class="queue"><li><b>王女士</b><span>心血管内科 · 林知远</span><em>等待中</em></li><li><b>李先生</b><span>消化内科 · 陈书宁</span><em>等待中</em></li><li><b>赵女士</b><span>心血管内科 · 林知远</span><em>等待中</em></li></ol><small>取消预约后，由服务端按规则自动递补；管理端只展示结果与审计信息。</small></section></div></template><section v-else class="panel module-placeholder"><div class="module-icon">{{ menus.find(x => x.id === active)?.icon }}</div><h2>{{ menus.find(x => x.id === active)?.label }}</h2><p>该模块已从患者平台独立出来。下一步可在此接入对应的后端列表、筛选、新建和审计接口。</p><button class="primary">进入{{ menus.find(x => x.id === active)?.label }}管理</button></section></main></div></template>
