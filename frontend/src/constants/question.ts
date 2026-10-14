/** 题目相关常量（与后端 enums/QuestionCategory、question.source、difficulty 对应） */

export interface Option {
  value: string
  label: string
}

export const CATEGORIES: Option[] = [
  { value: 'JAVA_BASIC', label: 'Java基础' },
  { value: 'CONCURRENCY', label: '并发' },
  { value: 'JVM', label: 'JVM' },
  { value: 'MYSQL', label: 'MySQL' },
  { value: 'REDIS', label: 'Redis' },
  { value: 'SPRING', label: 'Spring' },
  { value: 'NETWORK', label: '网络' },
  { value: 'OS', label: '操作系统' },
  { value: 'DISTRIBUTED', label: '分布式' },
  { value: 'BEHAVIOR', label: '行为面' },
]

export const DIFFICULTIES: Option[] = [
  { value: '1', label: '简单' },
  { value: '2', label: '中等' },
  { value: '3', label: '困难' },
]

export const SOURCES: Option[] = [
  { value: 'PRESET', label: '预置' },
  { value: 'IMPORT', label: '导入' },
  { value: 'MANUAL', label: '手动' },
]

export function categoryLabel(code: string): string {
  return CATEGORIES.find((c) => c.value === code)?.label ?? code
}

export function difficultyLabel(value: number): string {
  return DIFFICULTIES.find((d) => d.value === String(value))?.label ?? String(value)
}

export function sourceLabel(code: string): string {
  return SOURCES.find((s) => s.value === code)?.label ?? code
}
