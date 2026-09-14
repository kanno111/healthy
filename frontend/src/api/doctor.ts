import { ApiError } from './auth'

type ApiResponse<T> = { code: number; message: string; data: T }
export type Doctor = { id:number; name:string; gender:number; departmentId:number; departmentName:string; doctorCode:string; title:string|null; introduction:string|null; avatarUrl:string|null; sortOrder:number; status:number }
export type DoctorSavePayload = { name:string; gender:number; departmentId:number; doctorCode:string; title?:string; introduction?:string; avatarUrl?:string; sortOrder:number }
export type DoctorPage = { records:Doctor[]; total:number; page:number; pageSize:number; totalPages:number }
export type DoctorQuery = { page:number; pageSize:number; name?:string; departmentId?:number; status?:number }

async function request<T>(url:string, token:string, options?:RequestInit):Promise<T> {
  const response = await fetch(url,{...options,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json',...options?.headers}}).catch(()=>{throw new ApiError('无法连接到服务，请确认后端已启动')})
  const body = await response.json().catch(()=>null) as ApiResponse<T>|null
  if(!response.ok||!body||body.code!==0) throw new ApiError(body?.message||'请求失败，请稍后重试')
  return body.data
}
export const doctorApi={
  list:(token:string, query:DoctorQuery)=>{
    const params = new URLSearchParams({ page:String(query.page), pageSize:String(query.pageSize) })
    if(query.name) params.set('name',query.name)
    if(query.departmentId) params.set('departmentId',String(query.departmentId))
    if(query.status !== undefined) params.set('status',String(query.status))
    return request<DoctorPage>(`/api/admin/doctors?${params}`,token)
  },
  getById:(token:string,id:number)=>request<Doctor>(`/api/admin/doctors/${id}`,token),
  create:(token:string,payload:DoctorSavePayload)=>request<number>('/api/admin/doctors',token,{method:'POST',body:JSON.stringify(payload)}),
  update:(token:string,id:number,payload:DoctorSavePayload)=>request<null>(`/api/admin/doctors/${id}`,token,{method:'PUT',body:JSON.stringify(payload)}),
  updateStatus:(token:string,id:number,status:number)=>request<null>(`/api/admin/doctors/${id}/status?status=${status}`,token,{method:'PATCH'})
}
