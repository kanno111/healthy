import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }

export type DoctorAppointmentStatus = 'BOOKED' | 'COMPLETED' | 'CANCELLED'

export type DoctorAppointment = {
  id: number
  appointmentNo: string
  patientName: string
  departmentName: string
  scheduleSlotId: number
  scheduleDate: string
  sessionName: string | null
  startTime: string
  endTime: string
  status: DoctorAppointmentStatus
}

export type PageResult<T> = {
  records: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

async function request<T>(url: string, token: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: { Authorization: `Bearer ${token}`, ...options?.headers }
  }).catch(() => { throw new ApiError('无法连接到服务，请稍后重试') })
  const body = await response.json().catch(() => null) as ApiResponse<T> | null
  if (!response.ok || !body || body.code !== 0) {
    throw new ApiError(body?.message || '请求失败，请稍后重试')
  }
  return body.data
}

export const doctorAppointmentApi = {
  pageMine: (
    token: string,
    query: { scheduleDate?: string; status?: DoctorAppointmentStatus; page: number; pageSize: number }
  ) => {
    const params = new URLSearchParams({ page: String(query.page), pageSize: String(query.pageSize) })
    if (query.scheduleDate) params.set('scheduleDate', query.scheduleDate)
    if (query.status) params.set('status', query.status)
    return request<PageResult<DoctorAppointment>>(`/api/doctor/appointments?${params}`, token)
  },
  complete: (token: string, appointmentId: number) => request<null>(
    `/api/doctor/appointments/${appointmentId}/complete`, token, { method: 'PUT' }
  )
}
