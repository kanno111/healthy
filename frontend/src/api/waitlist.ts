import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }

export type PatientAppointmentWaitlist = {
  id: number
  scheduleSlotId: number
  doctorName: string
  departmentName: string
  scheduleDate: string
  sessionName: string
  startTime: string
  endTime: string
  status: 'WAITING' | 'OFFERED' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED'
  offerExpireTime: string | null
  createdAt: string
  updatedAt: string
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

export const waitlistApi = {
  join: (token: string, scheduleSlotId: number) => request<PatientAppointmentWaitlist>('/api/user/waitlists', token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ scheduleSlotId })
  }),
  listMine: (token: string) => request<PatientAppointmentWaitlist[]>('/api/user/waitlists', token),
  cancel: (token: string, waitlistId: number) => request<null>(`/api/user/waitlists/${waitlistId}/cancel`, token, {
    method: 'PATCH'
  }),
  confirm: (token: string, waitlistId: number) => request<null>(`/api/user/waitlists/${waitlistId}/confirm`, token, {
    method: 'POST'
  })
}
