import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import DoctorView from './views/DoctorView.vue'
import AppointmentConfirmView from './views/AppointmentConfirmView.vue'
import AppointmentsView from './views/AppointmentsView.vue'
import WaitlistsView from './views/WaitlistsView.vue'
import AdminView from './views/AdminView.vue'
import LoginView from './views/LoginView.vue'
import DoctorAppointmentsView from './views/DoctorAppointmentsView.vue'
import { session } from './stores/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/doctor/appointments', component: DoctorAppointmentsView, meta: { role: 'DOCTOR' } },
    { path: '/', component: HomeView, meta: { role: 'PATIENT' } },
    { path: '/doctor/:id', component: DoctorView, meta: { role: 'PATIENT' } },
    { path: '/appointment/confirm', component: AppointmentConfirmView, meta: { role: 'PATIENT' } },
    { path: '/appointments', component: AppointmentsView, meta: { role: 'PATIENT' } },
    { path: '/waitlists', component: WaitlistsView, meta: { role: 'PATIENT' } },
    { path: '/admin', component: AdminView, meta: { role: 'STAFF' } }
  ]
})

router.beforeEach((to) => {
  const roleHome = session.role === 'STAFF' ? '/admin' : session.role === 'DOCTOR' ? '/doctor/appointments' : '/'
  if (to.meta.public) return session.loggedIn ? roleHome : true
  if (!session.loggedIn) return '/login'
  return to.meta.role === session.role ? true : roleHome
})

export default router
