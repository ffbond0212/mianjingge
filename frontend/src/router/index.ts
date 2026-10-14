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
    {
      path: '/questions',
      name: 'question-list',
      component: () => import('../views/QuestionListView.vue'),
      meta: { title: '题库管理' },
    },
    {
      path: '/questions/new',
      name: 'question-create',
      component: () => import('../views/QuestionEditView.vue'),
      meta: { title: '新建题目' },
    },
    {
      path: '/questions/:id/edit',
      name: 'question-edit',
      component: () => import('../views/QuestionEditView.vue'),
      meta: { title: '编辑题目' },
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
