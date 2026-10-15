<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { interviewApi, type InterviewItem, type InterviewQuery } from '../api/interview'

const router = useRouter()

const query = reactive<InterviewQuery>({ page: 1, size: 10 })
const loading = ref(false)
const total = ref(0)
const records = ref<InterviewItem[]>([])

async function load() {
  loading.value = true
  try {
    const result = await interviewApi.page(query)
    records.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onReset() {
  query.keyword = undefined
  onSearch()
}

function onCreate() {
  router.push('/interviews/new')
}

function onDetail(row: InterviewItem) {
  router.push(`/interviews/${row.id}`)
}

function onEdit(row: InterviewItem) {
  router.push(`/interviews/${row.id}/edit`)
}

async function onDelete(row: InterviewItem) {
  await ElMessageBox.confirm(
    `确定删除「${row.company} · ${row.position}」这场面试吗？其中的题目记录将一并删除（题库题目不受影响）。`,
    '删除确认',
    { type: 'warning' },
  )
  await interviewApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div class="interview-page">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索公司 / 岗位 / 轮次" clearable class="keyword" @keyup.enter="onSearch" />
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="success" class="create-btn" @click="onCreate">新建面试</el-button>
    </div>

    <el-table :data="records" v-loading="loading" stripe @row-click="onDetail" class="clickable-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="company" label="公司" min-width="140" />
      <el-table-column prop="position" label="岗位" min-width="160" show-overflow-tooltip />
      <el-table-column prop="round" label="轮次" width="100">
        <template #default="{ row }">{{ row.round ?? '—' }}</template>
      </el-table-column>
      <el-table-column prop="interviewDate" label="面试日期" width="120">
        <template #default="{ row }">{{ row.interviewDate ?? '—' }}</template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题目数" width="80" />
      <el-table-column label="平均分" width="80">
        <template #default="{ row }">{{ row.avgScore ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="onDetail(row)">详情</el-button>
          <el-button link type="primary" @click.stop="onEdit(row)">编辑</el-button>
          <el-button link type="danger" @click.stop="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @current-change="load"
      @size-change="onSearch"
    />
  </div>
</template>

<style scoped>
.interview-page {
  padding: 16px 24px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.keyword {
  width: 320px;
}
.create-btn {
  margin-left: auto;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.clickable-table :deep(.el-table__row) {
  cursor: pointer;
}
</style>
