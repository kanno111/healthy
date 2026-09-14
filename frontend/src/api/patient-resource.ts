import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }

export type PatientDepartment = {
  id: number
  name: string
  description: string | null
}

export type PatientDoctor = {
  id: number
  name: string
  gender: number
  departmentId: number
  departmentName: string
  title: string | null
  introduction: string | null
  avatarUrl: string | null
  hasAvailableSlots: boolean
}

export type PatientDoctorPage = {
  records: PatientDoctor[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export type PatientDoctorQuery = {
  departmentId?: number
  keyword?: string
  page: number
  pageSize: number
}

export type PatientScheduleSlot = {
  id: number
  scheduleDate: string
  sessionType: 'MORNING' | 'AFTERNOON' | 'OTHER'
  sessionName: string
  startTime: string
  endTime: string
  totalCapacity: number
  remainingCapacity: number
}

async function request<T>(url: string, token: string): Promise<T> {
  const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    .catch(() => { throw new ApiError('无法连接到服务，请确认后端已启动') })
  const body = await response.json().catch(() => null) as ApiResponse<T> | null
  if (!response.ok || !body || body.code !== 0) throw new ApiError(body?.message || '请求失败，请稍后重试')
  return body.data
}

export const patientResourceApi = {
  listDepartments: (token: string) => request<PatientDepartment[]>('/api/user/departments', token),
  listDoctors: (token: string, options: PatientDoctorQuery) => {
    const query = new URLSearchParams({
      page: String(options.page),
      pageSize: String(options.pageSize)
    })
    if (options.departmentId) query.set('departmentId', String(options.departmentId))
    if (options.keyword?.trim()) query.set('keyword', options.keyword.trim())
    return request<PatientDoctorPage>(`/api/user/doctors?${query}`, token)
  },
  getDoctor: (token: string, id: number) => request<PatientDoctor>(`/api/user/doctors/${id}`, token),
  listScheduleSlots: (token: string, doctorId: number, startDate: string, endDate: string) => {
    const query = new URLSearchParams({ startDate, endDate })
    return request<PatientScheduleSlot[]>(`/api/user/doctors/${doctorId}/schedule-slots?${query}`, token)
  }
}
