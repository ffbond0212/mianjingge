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
      <el-button type="primary" @click="router.push('/questions')">进入题库管理</el-button>
      <el-button type="primary" @click="router.push('/interviews')">进入面试记录</el-button>
      <el-button type="primary" @click="router.push('/stats')">复盘统计</el-button>
      <el-button type="success" @click="router.push('/review')">今日复习</el-button>
      <el-button @click="router.push('/import')">md 导入面试记录</el-button>
    </el-space>
    <el-space class="tags">
      <el-tag type="success">M0 骨架完成</el-tag>
      <el-tag type="success">M1-1 登录认证完成</el-tag>
      <el-tag type="success">M1-2 题库管理完成</el-tag>
      <el-tag type="success">M1-3 预置题库完成</el-tag>
      <el-tag type="success">M1-4 面试记录完成</el-tag>
      <el-tag type="success">M1-5 md 导入完成</el-tag>
      <el-tag type="success">M2-1 复盘统计完成</el-tag>
      <el-tag type="success">M3-1 复习计划完成</el-tag>
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
.tags {
  margin-top: 16px;
}
</style>
