import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }

export type PatientAppointment = {
  id: number
  appointmentNo: string
  doctorId: number
  doctorName: string
  departmentId: number
  departmentName: string
  scheduleSlotId: number
  scheduleDate: string
  sessionName: string
  startTime: string
  endTime: string
  status: 'CONFIRMED'
  createdAt: string
}

async function request<T>(url: string, token: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: { Authorization: `Bearer ${token}`, ...init?.headers }
  }).catch(() => { throw new ApiError('无法连接到服务，请确认后端已启动') })
  const body = await response.json().catch(() => null) as ApiResponse<T> | null
  if (!response.ok || !body || body.code !== 0) throw new ApiError(body?.message || '请求失败，请稍后重试')
  return body.data
}

export const appointmentApi = {
  create: (token: string, scheduleSlotId: number) => request<PatientAppointment>('/api/user/appointments', token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ scheduleSlotId })
  }),
  listMine: (token: string) => request<PatientAppointment[]>('/api/user/appointments', token)
}
