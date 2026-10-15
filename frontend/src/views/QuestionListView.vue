<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { questionApi, type QuestionItem, type QuestionQuery } from '../api/question'
import { reviewApi } from '../api/review'
import { CATEGORIES, DIFFICULTIES, SOURCES, categoryLabel, difficultyLabel, sourceLabel } from '../constants/question'

const router = useRouter()

const query = reactive<QuestionQuery>({ page: 1, size: 10 })
const loading = ref(false)
const total = ref(0)
const records = ref<QuestionItem[]>([])

async function load() {
  loading.value = true
  try {
    const result = await questionApi.page(query)
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
  query.category = undefined
  query.source = undefined
  query.difficulty = undefined
  onSearch()
}

function onCreate() {
  router.push('/questions/new')
}

function onEdit(row: QuestionItem) {
  router.push(`/questions/${row.id}/edit`)
}

async function onDelete(row: QuestionItem) {
  await ElMessageBox.confirm(`确定删除题目「${row.title}」吗？删除后不可恢复。`, '删除确认', {
    type: 'warning',
  })
  await questionApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

/** 加入复习队列（低分/未答记录也会自动入队） */
async function onEnqueue(row: QuestionItem) {
  try {
    const vo = await reviewApi.enqueue(row.id)
    ElMessage.success(`已入队，${vo.nextReviewDate} 首次复习`)
  } catch {
    // 已在队列等错误已由拦截器提示
  }
}

onMounted(load)
</script>

<template>
  <div class="question-page">
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索标题 / 标准答案 / 个人话术" clearable class="keyword" @keyup.enter="onSearch" />
      <el-select v-model="query.category" placeholder="分类" clearable class="select">
        <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
      </el-select>
      <el-select v-model="query.difficulty" placeholder="难度" clearable class="select">
        <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="Number(d.value)" />
      </el-select>
      <el-select v-model="query.source" placeholder="来源" clearable class="select">
        <el-option v-for="s in SOURCES" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button @click="router.push('/import')">md 导入</el-button>
      <el-button type="success" class="create-btn" @click="onCreate">新建题目</el-button>
    </div>

    <el-table :data="records" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="题目" min-width="260" show-overflow-tooltip />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
      </el-table-column>
      <el-table-column label="难度" width="80">
        <template #default="{ row }">
          <el-tag :type="row.difficulty === 1 ? 'success' : row.difficulty === 2 ? 'warning' : 'danger'">
            {{ difficultyLabel(row.difficulty) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="80">
        <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
      </el-table-column>
      <el-table-column prop="reviewCount" label="复习" width="70" />
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ new Date(row.updatedAt).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="onEnqueue(row)">加入复习</el-button>
          <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
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
.question-page {
  padding: 16px 24px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.keyword {
  width: 320px;
}
.select {
  width: 130px;
}
.create-btn {
  margin-left: auto;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
