import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }

export type WaitlistStatus = 'WAITING' | 'OFFERED' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED'

export type AdminWaitlistRecord = {
  id: number
  patientId: number
  patientName: string
  username: string
  phone: string | null
  scheduleSlotId: number
  departmentId: number
  departmentName: string
  doctorId: number
  doctorName: string
  scheduleDate: string
  sessionName: string
  startTime: string
  endTime: string
  status: WaitlistStatus
  createdAt: string
  offerExpireTime: string | null
  confirmedAt: string | null
  cancelledAt: string | null
}

export type AdminWaitlistPage = { records: AdminWaitlistRecord[]; total: number; page: number; pageSize: number; totalPages: number }
export type AdminWaitlistQuery = { page: number; pageSize: number; status?: WaitlistStatus; scheduleDate?: string; departmentId?: number; doctorId?: number; patientKeyword?: string }
export type WaitlistQueueItem = { id: number; patientId: number; patientName: string; username: string; phone: string | null; status: WaitlistStatus; queuePosition: number | null; createdAt: string; offerExpireTime: string | null }
export type WaitlistSlotQueue = { scheduleSlotId: number; departmentName: string; doctorName: string; scheduleDate: string; sessionName: string; startTime: string; endTime: string; totalCapacity: number; remainingCapacity: number; waitingCount: number; offeredCandidates: WaitlistQueueItem[]; waitingCandidates: WaitlistQueueItem[] }

async function request<T>(url: string, token: string): Promise<T> {
  const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` } }).catch(() => { throw new ApiError('无法连接到服务，请确认后端已启动') })
  const body = await response.json().catch(() => null) as ApiResponse<T> | null
  if (!response.ok || !body || body.code !== 0) throw new ApiError(body?.message || '请求失败，请稍后重试')
  return body.data
}

export const adminWaitlistApi = {
  page: (token: string, query: AdminWaitlistQuery) => {
    const params = new URLSearchParams({ page: String(query.page), pageSize: String(query.pageSize) })
    if (query.status) params.set('status', query.status)
    if (query.scheduleDate) params.set('scheduleDate', query.scheduleDate)
    if (query.departmentId) params.set('departmentId', String(query.departmentId))
    if (query.doctorId) params.set('doctorId', String(query.doctorId))
    if (query.patientKeyword) params.set('patientKeyword', query.patientKeyword)
    return request<AdminWaitlistPage>(`/api/admin/waitlists?${params}`, token)
  },
  slotQueue: (token: string, scheduleSlotId: number) => request<WaitlistSlotQueue>(`/api/admin/waitlists/slots/${scheduleSlotId}/queue`, token)
}
