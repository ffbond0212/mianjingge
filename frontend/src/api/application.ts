import http from './request'

export interface ApplicationItem {
  id: number
  company: string
  position: string
  channel: string | null
  applyDate: string | null
  city: string | null
  salary: string | null
  jdUrl: string | null
  status: string
  statusLabel: string
  interviewId: number | null
  remark: string | null
  createdAt: string
  updatedAt: string
}

export interface ApplicationSave {
  company: string
  position: string
  channel?: string
  applyDate?: string
  city?: string
  salary?: string
  jdUrl?: string
  remark?: string
}

export interface ApplicationTimelineItem {
  id: number
  fromStatus: string | null
  fromStatusLabel: string | null
  toStatus: string
  toStatusLabel: string
  remark: string | null
  createdAt: string
}

export const applicationApi = {
  list: () => http.get<ApplicationItem[]>('/applications'),
  create: (data: ApplicationSave) => http.post<ApplicationItem>('/applications', data),
  update: (id: number, data: ApplicationSave) => http.put<ApplicationItem>(`/applications/${id}`, data),
  remove: (id: number) => http.delete<void>(`/applications/${id}`),
  transition: (id: number, data: { toStatus: string; remark?: string; interviewId?: number | null }) =>
    http.post<ApplicationItem>(`/applications/${id}/transition`, data),
  timeline: (id: number) => http.get<ApplicationTimelineItem[]>(`/applications/${id}/timeline`),
}
