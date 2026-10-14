<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

onMounted(() => {
  if (!userStore.userInfo) userStore.fetchMe()
})

function onLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="dashboard">
    <div class="header">
      <h1>面经阁</h1>
      <el-space>
        <span class="nickname">{{ userStore.userInfo?.nickname ?? userStore.userInfo?.username }}</span>
        <el-button @click="onLogout">退出登录</el-button>
      </el-space>
    </div>
    <p>面试题库复盘系统</p>
    <el-space>
      <el-tag type="success">M0 骨架完成</el-tag>
      <el-tag type="success">M1-1 登录认证完成</el-tag>
      <el-tag type="info">M1-2 题库管理开发中</el-tag>
    </el-space>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 24px 48px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nickname {
  color: #606266;
}
</style>
