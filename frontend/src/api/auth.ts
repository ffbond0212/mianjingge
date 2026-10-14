import http from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  username: string
  nickname: string
}

export interface UserInfo {
  username: string
  nickname: string
}

export const authApi = {
  login: (data: LoginParams) => http.post<LoginResult>('/auth/login', data),
  me: () => http.get<UserInfo>('/auth/me'),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    http.post<void>('/auth/change-password', data),
}
