import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles.css'
import './auth.css'
import './department.css'
import './admin-resource.css'
import './admin-pagination.css'
import './admin-department-modal.css'
import './admin-schedule.css'
import './admin-schedule-matrix.css'
import './patient-resource.css'

createApp(App).use(router).mount('#app')
