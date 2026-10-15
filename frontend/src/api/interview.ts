import http from './request'
import type { PageResult } from './question'

export interface InterviewItem {
  id: number
  company: string
  position: string
  round: string | null
  interviewDate: string | null
  summary: string | null
  questionCount: number
  avgScore: number | null
  createdAt: string
}

export interface InterviewRecord {
  id: number
  interviewId: number
  questionId: number | null
  questionTitle: string | null
  questionSnapshot: string
  score: number | null
  answered: number
  answeredLabel: string
  deduction: string | null
  myAnswerSnapshot: string | null
}

export interface InterviewDetail {
  interview: InterviewItem
  questions: InterviewRecord[]
}

export interface InterviewQuery {
  page?: number
  size?: number
  keyword?: string
}

export interface InterviewSave {
  company: string
  position: string
  round?: string
  interviewDate?: string
  summary?: string
}

export interface InterviewRecordSave {
  questionId?: number | null
  questionSnapshot: string
  score?: number | null
  answered: number
  deduction?: string
  myAnswerSnapshot?: string
}

export const interviewApi = {
  page: (params: InterviewQuery) => http.get<PageResult<InterviewItem>>('/interviews', params),
  detail: (id: number) => http.get<InterviewDetail>(`/interviews/${id}`),
  create: (data: InterviewSave) => http.post<InterviewItem>('/interviews', data),
  update: (id: number, data: InterviewSave) => http.put<InterviewItem>(`/interviews/${id}`, data),
  remove: (id: number) => http.delete<void>(`/interviews/${id}`),
  addRecord: (interviewId: number, data: InterviewRecordSave) =>
    http.post<InterviewRecord>(`/interviews/${interviewId}/questions`, data),
  updateRecord: (interviewId: number, recordId: number, data: InterviewRecordSave) =>
    http.put<InterviewRecord>(`/interviews/${interviewId}/questions/${recordId}`, data),
  removeRecord: (interviewId: number, recordId: number) =>
    http.delete<void>(`/interviews/${interviewId}/questions/${recordId}`),
  saveAsPhrase: (interviewId: number, recordId: number) =>
    http.post<void>(`/interviews/${interviewId}/questions/${recordId}/save-as-phrase`),
}
