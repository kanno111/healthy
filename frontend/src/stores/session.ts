import { reactive } from 'vue'

export type UserRole = 'PATIENT' | 'STAFF'
type Session = { loggedIn: boolean; role: UserRole | null; name: string }

const saved = localStorage.getItem('smart-appointment-session')
export const session = reactive<Session>(saved ? JSON.parse(saved) : { loggedIn: false, role: null, name: '' })

export function signIn(role: UserRole, name: string) {
  Object.assign(session, { loggedIn: true, role, name })
  localStorage.setItem('smart-appointment-session', JSON.stringify(session))
}
export function signOut() {
  Object.assign(session, { loggedIn: false, role: null, name: '' })
  localStorage.removeItem('smart-appointment-session')
}
