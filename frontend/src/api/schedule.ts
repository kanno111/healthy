import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }

export type ScheduleSlot = {
  id: number
  doctorId: number
  doctorName: string
  departmentName: string
  scheduleDate: string
  sessionType: ScheduleSessionType
  sessionName: string
  startTime: string
  endTime: string
  averageConsultationMinutes: number
  totalCapacity: number
  bookedCapacity: number
  remainingCapacity: number
  status: 'OPEN' | 'CLOSED'
}

export type ScheduleSessionType = 'MORNING' | 'AFTERNOON' | 'OTHER'

export type ScheduleSessionPayload = {
  sessionType: ScheduleSessionType
  customSessionName?: string
  startTime: string
  endTime: string
  capacity: number
  averageConsultationMinutes: number
}

export type ScheduleBatchPayload = {
  doctorId: number
  startDate: string
  endDate: string
  weekdays: number[]
  sessions: ScheduleSessionPayload[]
}

type ScheduleBatchResult = { createdCount: number; slots: ScheduleSlot[] }

async function request<T>(url: string, token: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', ...options?.headers }
  }).catch(() => { throw new ApiError('无法连接到服务，请确认后端已启动') })
  const body = await response.json().catch(() => null) as ApiResponse<T> | null
  if (!response.ok || !body || body.code !== 0) throw new ApiError(body?.message || '请求失败，请稍后重试')
  return body.data
}

export const scheduleApi = {
  list: (token: string, startDate: string, endDate: string, doctorId?: number) => {
    const query = new URLSearchParams({ startDate, endDate })
    if (doctorId) query.set('doctorId', String(doctorId))
    return request<ScheduleSlot[]>(`/api/admin/schedule-slots?${query}`, token)
  },
  batchCreate: (token: string, payload: ScheduleBatchPayload) =>
    request<ScheduleBatchResult>('/api/admin/schedule-slots/batch', token, { method: 'POST', body: JSON.stringify(payload) }),
  updateStatus: (token: string, id: number, status: ScheduleSlot['status']) =>
    request<null>(`/api/admin/schedule-slots/${id}/status?status=${status}`, token, { method: 'PATCH' }),
  updateCapacity: (token: string, id: number, capacity: number) =>
    request<null>(`/api/admin/schedule-slots/${id}/capacity?capacity=${capacity}`, token, { method: 'PATCH' })
}
