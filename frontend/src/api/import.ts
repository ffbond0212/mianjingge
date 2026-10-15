import http from './request'

export interface ImportInterviewInfo {
  company: string
  position: string
  round?: string
  interviewDate?: string
}

export interface ImportItem {
  title: string
  /** 分类枚举名；解析映射失败为 null，预览必选 */
  category: string | null
  difficulty: number
  score: number | null
  answered: number
  deduction?: string
  myAnswerSnapshot?: string
  stdAnswer?: string
  /** 标题全等匹配到的题库已有题 id，null 表示新建 */
  matchedQuestionId: number | null
  /** 复用已有题时是否用导入的参考答案覆盖其标准答案 */
  overwriteStdAnswer: boolean
}

export interface ImportParseResult {
  interview: ImportInterviewInfo
  items: ImportItem[]
  total: number
  matchedCount: number
}

export const importApi = {
  parse: (content: string) => http.post<ImportParseResult>('/import/md/parse', { content }),
  confirm: (data: { interview: ImportInterviewInfo; items: ImportItem[] }) =>
    http.post<{ interviewId: number }>('/import/md/confirm', data),
}
