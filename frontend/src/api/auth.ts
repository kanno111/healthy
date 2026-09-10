import type { UserRole } from '../stores/session'

type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

export type LoginResult = {
  token: string
  userId: number
  name: string
  role: UserRole
}

export class ApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

export async function login(username: string, password: string): Promise<LoginResult> {
  let response: Response
  try {
    response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
  } catch {
    throw new ApiError('无法连接到服务，请稍后重试')
  }

  const body = await response.json().catch(() => null) as ApiResponse<LoginResult> | null
  if (!response.ok || !body || body.code !== 0) {
    throw new ApiError(body?.message || '登录失败，请稍后重试')
  }
  return body.data
}

export async function logout(token: string): Promise<void> {
  try {
    const response = await fetch('/api/auth/logout', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` }
    })
    const body = await response.json().catch(() => null) as ApiResponse<null> | null
    if (!response.ok || !body || body.code !== 0) {
      throw new ApiError(body?.message || '退出登录失败')
    }
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    throw new ApiError('无法连接到服务，已清除本地登录状态')
  }
}
