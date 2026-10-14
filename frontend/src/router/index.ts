import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { title: '面经阁' },
    },
  ],
})

// 简易登录守卫（M1-1 认证实现后完善）
router.beforeEach((to) => {
  const token = localStorage.getItem('mjg_token')
  if (to.path !== '/login' && !token) return '/login'
  if (to.path === '/login' && token) return '/'
  document.title = `${to.meta.title ?? ''} · 面经阁`
})

export default router
