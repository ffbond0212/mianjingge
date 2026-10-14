import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi, type UserInfo } from '../api/auth'

/** 用户状态：token 持久化在 localStorage，用户信息存内存 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('mjg_token') ?? '')
  const userInfo = ref<UserInfo | null>(null)

  async function login(username: string, password: string) {
    const result = await authApi.login({ username, password })
    token.value = result.token
    localStorage.setItem('mjg_token', result.token)
    userInfo.value = { username: result.username, nickname: result.nickname }
  }

  async function fetchMe() {
    userInfo.value = await authApi.me()
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('mjg_token')
  }

  return { token, userInfo, login, fetchMe, logout }
})
