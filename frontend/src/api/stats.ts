import http from './request'

export interface TopQuestion {
  questionId: number
  title: string
  category: string | null
  categoryLabel: string | null
  askedCount: number
  avgScore: number | null
}

export interface CategoryScore {
  category: string
  categoryLabel: string
  avgScore: number | null
  scoreCount: number
  questionCount: number
}

export interface InterviewTrend {
  interviewId: number
  company: string
  position: string
  round: string | null
  interviewDate: string | null
  avgScore: number | null
  questionCount: number
}

export interface StatsOverview {
  topQuestions: TopQuestion[]
  categoryRadar: CategoryScore[]
  interviewTrend: InterviewTrend[]
}

export const statsApi = {
  overview: () => http.get<StatsOverview>('/stats/overview'),
}
