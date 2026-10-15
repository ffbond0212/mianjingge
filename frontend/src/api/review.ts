import http from './request'

export interface ReviewPlan {
  id: number
  questionId: number
  questionTitle: string
  category: string | null
  categoryLabel: string | null
  stage: number
  stageLabel: string
  intervalDays: number
  nextReviewDate: string
  reviewCount: number
  lastScore: number | null
  status: string
}

export const reviewApi = {
  today: () => http.get<ReviewPlan[]>('/review/today'),
  plans: () => http.get<ReviewPlan[]>('/review/plans'),
  enqueue: (questionId: number) => http.post<ReviewPlan>('/review/plans', { questionId }),
  checkin: (id: number, body: { answered: number; score?: number }) =>
    http.post<ReviewPlan>(`/review/plans/${id}/checkin`, body),
  remove: (id: number) => http.delete<void>(`/review/plans/${id}`),
  config: () => http.get<{ intervals: number[] }>('/review/config'),
  updateConfig: (intervals: number[]) => http.put<void>('/review/config', { intervals }),
}
