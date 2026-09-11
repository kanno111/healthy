import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }

export type Department = {
  id: number
  name: string
  description: string | null
  sortOrder: number
  status: number
  doctorCount: number
}

export type DepartmentSavePayload = {
  name: string
  description?: string
  sortOrder: number
}

async function request<T>(url: string, token: string, options?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(url, {
      ...options,
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', ...options?.headers }
    })
  } catch {
    throw new ApiError('无法连接到服务，请确认后端已启动')
  }
  const body = await response.json().catch(() => null) as ApiResponse<T> | null
  if (!response.ok || !body || body.code !== 0) throw new ApiError(body?.message || '请求失败，请稍后重试')
  return body.data
}

export const departmentApi = {
  list: (token: string) => request<Department[]>('/api/admin/departments', token),
  getById: (token: string, id: number) => request<Department>(`/api/admin/departments/${id}`, token),
  create: (token: string, payload: DepartmentSavePayload) => request<number>('/api/admin/departments', token, { method: 'POST', body: JSON.stringify(payload) }),
  update: (token: string, id: number, payload: DepartmentSavePayload) => request<null>(`/api/admin/departments/${id}`, token, { method: 'PUT', body: JSON.stringify(payload) }),
  updateStatus: (token: string, id: number, status: number) => request<null>(`/api/admin/departments/${id}/status?status=${status}`, token, { method: 'PATCH' })
}
