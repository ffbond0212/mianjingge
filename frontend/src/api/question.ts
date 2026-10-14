import http from './request'

export interface QuestionItem {
  id: number
  title: string
  category: string
  categoryLabel: string
  tags: string | null
  difficulty: number
  stdAnswer: string | null
  myAnswer: string | null
  source: string
  reviewCount: number
  createdAt: string
  updatedAt: string
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export interface QuestionQuery {
  page?: number
  size?: number
  keyword?: string
  category?: string
  source?: string
  difficulty?: number
}

export interface QuestionSave {
  title: string
  category: string
  tags?: string
  difficulty: number
  stdAnswer?: string
  myAnswer?: string
}

export const questionApi = {
  page: (params: QuestionQuery) => http.get<PageResult<QuestionItem>>('/questions', params),
  get: (id: number) => http.get<QuestionItem>(`/questions/${id}`),
  create: (data: QuestionSave) => http.post<QuestionItem>('/questions', data),
  update: (id: number, data: QuestionSave) => http.put<QuestionItem>(`/questions/${id}`, data),
  remove: (id: number) => http.delete<void>(`/questions/${id}`),
}
