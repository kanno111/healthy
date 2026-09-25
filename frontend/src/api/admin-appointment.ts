import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }

export type AdminAppointment = {
  id: number
  appointmentNo: string
  patientName: string
  doctorName: string
  departmentName: string
  scheduleDate: string
  sessionName: string
  startTime: string
  endTime: string
  status: 'BOOKED' | 'CANCELLED' | 'COMPLETED'
  createdAt: string
}

export type AdminAppointmentPage = {
  records: AdminAppointment[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

async function request<T>(url: string, token: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: { Authorization: `Bearer ${token}`, ...options?.headers }
  }).catch(() => { throw new ApiError('无法连接到服务，请确认后端已启动') })
  const body = await response.json().catch(() => null) as ApiResponse<T> | null
  if (!response.ok || !body || body.code !== 0) throw new ApiError(body?.message || '请求失败，请稍后重试')
  return body.data
}

export const adminAppointmentApi = {
  page: (token: string, page: number, pageSize: number) => {
    const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) })
    return request<AdminAppointmentPage>(`/api/admin/appointments?${params}`, token)
  },
  complete: (token: string, appointmentId: number) => request<null>(`/api/admin/appointments/${appointmentId}/complete`, token, {
    method: 'PATCH'
  })
}
