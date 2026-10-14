import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/** 后端统一响应结构 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截：自动携带 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('mjg_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：统一解包 Result；401 清 token 跳登录
request.interceptors.response.use(
  (response) => {
    const result = response.data as Result
    if (result.code !== 0) {
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(new Error(result.message))
    }
    return result.data as never
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('mjg_token')
      router.push('/login')
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  },
)

/** 类型安全的请求封装：直接返回 Result.data */
export const http = {
  get<T = unknown>(url: string, params?: object): Promise<T> {
    return request.get(url, { params }) as Promise<T>
  },
  post<T = unknown>(url: string, data?: object): Promise<T> {
    return request.post(url, data) as Promise<T>
  },
  put<T = unknown>(url: string, data?: object): Promise<T> {
    return request.put(url, data) as Promise<T>
  },
  delete<T = unknown>(url: string): Promise<T> {
    return request.delete(url) as Promise<T>
  },
}

export default http
