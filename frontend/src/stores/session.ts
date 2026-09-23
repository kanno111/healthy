import { reactive } from 'vue'

export type UserRole = 'PATIENT' | 'STAFF' | 'DOCTOR'
type Session = { loggedIn: boolean; token: string; userId: number | null; role: UserRole | null; name: string }

const saved = localStorage.getItem('smart-appointment-session')
const createEmptySession = (): Session => ({ loggedIn: false, token: '', userId: null, role: null, name: '' })
let initialSession = createEmptySession()
if (saved) {
  try {
    const parsed = JSON.parse(saved) as Partial<Session>
    if (parsed.loggedIn && parsed.token && parsed.userId && parsed.role && parsed.name) {
      initialSession = parsed as Session
    }
  } catch {
    localStorage.removeItem('smart-appointment-session')
  }
}
export const session = reactive<Session>(initialSession)

export function signIn(data: { token: string; userId: number; role: UserRole; name: string }) {
  Object.assign(session, { loggedIn: true, ...data })
  localStorage.setItem('smart-appointment-session', JSON.stringify(session))
}
export function signOut() {
  Object.assign(session, createEmptySession())
  localStorage.removeItem('smart-appointment-session')
}
