<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)

async function onLogin() {
  if (!username.value || !password.value) return
  loading.value = true
  try {
    await userStore.login(username.value, password.value)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">面经阁</h2>
      <p class="subtitle">面试题库复盘系统</p>
      <el-form label-position="top" @submit.prevent="onLogin">
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password placeholder="请输入密码" @keyup.enter="onLogin" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">登录</el-button>
      </el-form>
      <p class="hint">账号与初始密码见项目根目录 .env 配置，首次登录后请修改密码</p>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 360px;
}
.title {
  text-align: center;
  margin: 0;
}
.subtitle {
  text-align: center;
  color: #909399;
  margin: 8px 0 24px;
}
.login-btn {
  width: 100%;
}
.hint {
  margin-top: 12px;
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
}
</style>
