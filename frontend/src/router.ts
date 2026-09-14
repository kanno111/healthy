import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import DoctorView from './views/DoctorView.vue'
import AppointmentConfirmView from './views/AppointmentConfirmView.vue'
import AppointmentsView from './views/AppointmentsView.vue'
import AdminView from './views/AdminView.vue'
import LoginView from './views/LoginView.vue'
import { session } from './stores/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/', component: HomeView, meta: { role: 'PATIENT' } },
    { path: '/doctor/:id', component: DoctorView, meta: { role: 'PATIENT' } },
    { path: '/appointment/confirm', component: AppointmentConfirmView, meta: { role: 'PATIENT' } },
    { path: '/appointments', component: AppointmentsView, meta: { role: 'PATIENT' } },
    { path: '/admin', component: AdminView, meta: { role: 'STAFF' } }
  ]
})

router.beforeEach((to) => {
  if (to.meta.public) return session.loggedIn ? (session.role === 'STAFF' ? '/admin' : '/') : true
  if (!session.loggedIn) return '/login'
  return to.meta.role === session.role ? true : (session.role === 'STAFF' ? '/admin' : '/')
})

export default router
