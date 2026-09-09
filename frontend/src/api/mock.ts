import type { Appointment, Doctor, Slot } from '../types'

export const doctors: Doctor[] = [
  { id: 'd1', name: '林知远', title: '主任医师', department: '心血管内科', specialty: '高血压、冠心病及心律失常', initials: '林', color: '#e67260', slots: [
    { id: 's1', period: '上午', time: '09:00 - 09:30', remaining: 3, fee: 30, status: 'AVAILABLE' },
    { id: 's2', period: '上午', time: '09:30 - 10:00', remaining: 0, fee: 30, status: 'FULL' },
    { id: 's3', period: '下午', time: '14:00 - 14:30', remaining: 1, fee: 30, status: 'AVAILABLE' }
  ] },
  { id: 'd2', name: '陈书宁', title: '副主任医师', department: '消化内科', specialty: '胃肠疾病、肝胆疾病诊疗', initials: '陈', color: '#80a8d9', slots: [] },
  { id: 'd3', name: '苏念安', title: '主治医师', department: '儿科', specialty: '儿童呼吸道与过敏性疾病', initials: '苏', color: '#9b8ed3', slots: [] }
]

let appointments: Appointment[] = [
  { id: 'A20260908001', doctor: '林知远', department: '心血管内科', time: '2026-09-10 09:00 - 09:30', fee: 30, status: 'CONFIRMED' },
  { id: 'A20260908002', doctor: '陈书宁', department: '消化内科', time: '2026-09-11 10:00 - 10:30', fee: 20, status: 'WAITLISTED' }
]
const delay = <T>(value: T) => new Promise<T>(resolve => setTimeout(() => resolve(value), 250))
export const api = {
  listDoctors: () => delay(doctors),
  getDoctor: (id: string) => delay(doctors.find(d => d.id === id) ?? doctors[0]),
  listAppointments: () => delay(appointments),
  reserve: async (doctor: Doctor, slot: Slot) => {
    if (slot.remaining < 1) throw new Error('该时段刚刚约满')
    slot.remaining--;
    if (!slot.remaining) slot.status = 'FULL'
    const item: Appointment = { id: `A${Date.now()}`, doctor: doctor.name, department: doctor.department, time: `2026-09-10 ${slot.time}`, fee: slot.fee, status: 'PENDING_PAYMENT', expiresAt: '15:00' }
    appointments = [item, ...appointments]
    return delay(item)
  },
  waitlist: async (doctor: Doctor, slot: Slot) => {
    const item: Appointment = { id: `W${Date.now()}`, doctor: doctor.name, department: doctor.department, time: `2026-09-10 ${slot.time}`, fee: slot.fee, status: 'WAITLISTED' }
    appointments = [item, ...appointments]
    return delay(item)
  },
  cancel: async (id: string) => { appointments = appointments.map(a => a.id === id ? { ...a, status: 'CANCELLED' } : a); return delay(true) }
}
