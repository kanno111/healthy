export type SlotStatus = 'AVAILABLE' | 'FULL' | 'CLOSED'
export type AppointmentStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW'
export interface Slot { id: string; period: string; time: string; remaining: number; fee: number; status: SlotStatus }
export interface Doctor { id: string; name: string; title: string; department: string; specialty: string; initials: string; color: string; slots: Slot[] }
export interface Appointment { id: string; doctor: string; department: string; time: string; fee: number; status: AppointmentStatus; expiresAt?: string }
