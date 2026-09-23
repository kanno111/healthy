import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }

async function request<T>(url: string, token: string): Promise<T> {
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` }
  }).catch(() => { throw new ApiError('无法连接到服务，请确认后端已启动') })
  const body = await response.json().catch(() => null) as ApiResponse<T> | null
  if (!response.ok || !body || body.code !== 0) throw new ApiError(body?.message || '请求失败，请稍后重试')
  return body.data
}

export type AdminDashboardSummary = {
  date: string
  scheduleSlotCount: number
  totalCapacity: number
  remainingCapacity: number
  activeAppointmentCount: number
  waitingCount: number
  overdueOfferedCount: number
}

export const adminDashboardApi = {
  summary: (token: string, date?: string) => {
    const query = date ? `?date=${encodeURIComponent(date)}` : ''
    return request<AdminDashboardSummary>(`/api/admin/dashboard/summary${query}`, token)
  }
}
