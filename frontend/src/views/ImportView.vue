<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { importApi, type ImportParseResult } from '../api/import'
import { CATEGORIES, DIFFICULTIES } from '../constants/question'

const router = useRouter()

const content = ref('')
const parsing = ref(false)
const confirming = ref(false)
const preview = ref<ImportParseResult | null>(null)

const ANSWERED_OPTIONS = [
  { value: 1, label: '答上' },
  { value: 2, label: '部分答上' },
  { value: 3, label: '未答上' },
]

/** 读取 .md 文件内容到输入框 */
function onFileChange(file: UploadFile) {
  const raw = file.raw
  if (!raw) return
  const reader = new FileReader()
  reader.onload = () => {
    content.value = String(reader.result ?? '')
    ElMessage.success(`已读取文件：${raw.name}`)
  }
  reader.readAsText(raw, 'utf-8')
}

async function onParse() {
  if (!content.value.trim()) {
    ElMessage.warning('请先粘贴 md 内容或选择文件')
    return
  }
  parsing.value = true
  try {
    preview.value = await importApi.parse(content.value)
    if (!preview.value.items.length) {
      ElMessage.warning('没有解析出题目，请检查格式（需包含「### 第 N 题」段落）')
    } else {
      ElMessage.success(`解析出 ${preview.value.total} 题（${preview.value.matchedCount} 题与题库重复，将复用）`)
    }
  } finally {
    parsing.value = false
  }
}

async function onConfirm() {
  const data = preview.value
  if (!data) return
  if (!data.interview.company.trim() || !data.interview.position.trim()) {
    ElMessage.warning('请填写公司和岗位')
    return
  }
  const missing = data.items.findIndex((i) => !i.category)
  if (missing >= 0) {
    ElMessage.warning(`第 ${missing + 1} 题的分类未识别，请手动选择`)
    return
  }
  confirming.value = true
  try {
    const result = await importApi.confirm({ interview: data.interview, items: data.items })
    ElMessage.success('导入成功，已生成面试场次')
    router.push(`/interviews/${result.interviewId}`)
  } finally {
    confirming.value = false
  }
}
</script>

<template>
  <div class="import-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>md 导入面试记录</span>
          <span class="hint">支持《面试问答记录》结构化格式：# 标题（轮次+日期）· 成绩总览表 · ### 第 N 题段落</span>
        </div>
      </template>
      <el-input v-model="content" type="textarea" :rows="9" placeholder="粘贴 md 全文，或选择 .md 文件" />
      <div class="toolbar">
        <el-upload :auto-upload="false" :show-file-list="false" accept=".md,.markdown,.txt" :on-change="onFileChange">
          <el-button>选择 md 文件</el-button>
        </el-upload>
        <el-button type="primary" :loading="parsing" @click="onParse">解析预览</el-button>
      </div>
    </el-card>

    <template v-if="preview">
      <el-card class="preview-card">
        <template #header>
          <div class="card-header">
            <span>预览与修正</span>
            <el-tag v-if="preview.matchedCount > 0" type="warning">复用已有题 {{ preview.matchedCount }} 道</el-tag>
          </div>
        </template>

        <el-form inline class="interview-form">
          <el-form-item label="公司" required>
            <el-input v-model="preview.interview.company" maxlength="100" class="w160" />
          </el-form-item>
          <el-form-item label="岗位" required>
            <el-input v-model="preview.interview.position" maxlength="100" class="w220" />
          </el-form-item>
          <el-form-item label="轮次">
            <el-input v-model="preview.interview.round" maxlength="50" class="w110" />
          </el-form-item>
          <el-form-item label="面试日期">
            <el-date-picker v-model="preview.interview.interviewDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </el-form>

        <el-table :data="preview.items" stripe>
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-body">
                <el-form label-width="90px">
                  <el-form-item label="难度">
                    <el-select v-model="row.difficulty" class="w110">
                      <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="Number(d.value)" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="参考答案">
                    <el-input v-model="row.stdAnswer" type="textarea" :rows="4" />
                  </el-form-item>
                  <el-form-item label="扣分点">
                    <el-input v-model="row.deduction" type="textarea" :rows="2" />
                  </el-form-item>
                  <el-form-item label="现场回答">
                    <el-input v-model="row.myAnswerSnapshot" type="textarea" :rows="2" />
                  </el-form-item>
                </el-form>
              </div>
            </template>
          </el-table-column>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="title" label="题目" min-width="260" show-overflow-tooltip />
          <el-table-column label="分类" width="130">
            <template #default="{ row }">
              <el-select v-model="row.category" placeholder="必选" size="small" class="w110">
                <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="80">
            <template #default="{ row }">{{ row.score ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="是否答上" width="115">
            <template #default="{ row }">
              <el-select v-model="row.answered" size="small" class="w100">
                <el-option v-for="a in ANSWERED_OPTIONS" :key="a.value" :label="a.label" :value="a.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="题库" width="130">
            <template #default="{ row }">
              <el-tooltip v-if="row.matchedQuestionId" content="标题与题库已有题完全一致，导入时复用该题">
                <el-tag type="warning">复用 #{{ row.matchedQuestionId }}</el-tag>
              </el-tooltip>
              <el-tag v-else type="info">新建</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="覆盖标准答案" width="120">
            <template #default="{ row }">
              <el-checkbox v-model="row.overwriteStdAnswer" :disabled="!row.matchedQuestionId || !row.stdAnswer">
                覆盖
              </el-checkbox>
            </template>
          </el-table-column>
        </el-table>

        <div class="confirm-bar">
          <el-button @click="router.push('/questions')">取消</el-button>
          <el-button type="primary" :loading="confirming" @click="onConfirm">
            确认导入 {{ preview.items.length }} 题
          </el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.import-page {
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.hint {
  color: #909399;
  font-size: 12px;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}
.preview-card {
  margin-top: 0;
}
.interview-form {
  margin-bottom: 8px;
}
.w100 {
  width: 100px;
}
.w110 {
  width: 110px;
}
.w160 {
  width: 160px;
}
.w220 {
  width: 220px;
}
.expand-body {
  padding: 8px 16px 8px 48px;
}
.confirm-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
</style>
