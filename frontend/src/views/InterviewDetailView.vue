<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { interviewApi, type InterviewDetail, type InterviewRecord, type InterviewRecordSave } from '../api/interview'
import { questionApi, type QuestionItem } from '../api/question'
import { CATEGORIES, DIFFICULTIES, categoryLabel } from '../constants/question'

const route = useRoute()
const router = useRouter()
const interviewId = Number(route.params.id)

const loading = ref(false)
const detail = ref<InterviewDetail | null>(null)

const ANSWERED_OPTIONS = [
  { value: 1, label: '答上' },
  { value: 2, label: '部分答上' },
  { value: 3, label: '未答上' },
]

function answeredTagType(answered: number) {
  return answered === 1 ? 'success' : answered === 2 ? 'warning' : 'danger'
}

async function load() {
  loading.value = true
  try {
    detail.value = await interviewApi.detail(interviewId)
  } finally {
    loading.value = false
  }
}

// ---------- 添加/编辑题目记录 ----------

const dialogVisible = ref(false)
const dialogSaving = ref(false)
/** 编辑的记录 id，null 表示新增 */
const editingId = ref<number | null>(null)
/** 题目来源方式：choose 从题库选 / create 现场新建；编辑时锁定为原关联 */
const linkMode = ref<'choose' | 'create'>('choose')
/** 已选中/已关联的题库题目 */
const pickedQuestion = ref<QuestionItem | null>(null)
/** 现场新建题目表单 */
const newQuestion = reactive({ title: '', category: '', difficulty: 2 })

const form = reactive<InterviewRecordSave>({
  questionId: null,
  questionSnapshot: '',
  score: undefined,
  answered: 3,
  deduction: '',
  myAnswerSnapshot: '',
})

const dialogTitle = computed(() => (editingId.value ? '编辑题目记录' : '添加题目记录'))

function openAdd() {
  editingId.value = null
  linkMode.value = 'choose'
  pickedQuestion.value = null
  newQuestion.title = ''
  newQuestion.category = ''
  newQuestion.difficulty = 2
  Object.assign(form, { questionId: null, questionSnapshot: '', score: undefined, answered: 3, deduction: '', myAnswerSnapshot: '' })
  dialogVisible.value = true
}

function openEdit(row: InterviewRecord) {
  editingId.value = row.id
  linkMode.value = row.questionId ? 'choose' : 'create'
  pickedQuestion.value = null
  Object.assign(form, {
    questionId: row.questionId,
    questionSnapshot: row.questionSnapshot,
    score: row.score ?? undefined,
    answered: row.answered,
    deduction: row.deduction ?? '',
    myAnswerSnapshot: row.myAnswerSnapshot ?? '',
  })
  dialogVisible.value = true
}

/** 远程搜索题库 */
const questionOptions = ref<QuestionItem[]>([])
const searching = ref(false)
async function searchQuestions(keyword: string) {
  searching.value = true
  try {
    const result = await questionApi.page({ keyword: keyword || undefined, size: 20 })
    questionOptions.value = result.records
  } finally {
    searching.value = false
  }
}

function onPickQuestion(id: number) {
  const item = questionOptions.value.find((q) => q.id === id)
  pickedQuestion.value = item ?? null
  form.questionId = id
  // 快照默认取题目标题，可再编辑
  form.questionSnapshot = item ? item.title : form.questionSnapshot
}

async function onSubmit() {
  if (linkMode.value === 'choose' && !form.questionId) {
    ElMessage.warning('请先从题库选择一道题目')
    return
  }
  if (linkMode.value === 'create') {
    if (!newQuestion.title.trim()) {
      ElMessage.warning('请填写题目')
      return
    }
    if (!newQuestion.category) {
      ElMessage.warning('请选择分类')
      return
    }
  }
  if (!form.questionSnapshot.trim()) {
    ElMessage.warning('请填写题目快照')
    return
  }
  dialogSaving.value = true
  try {
    let questionId: number | null = form.questionId ?? null
    if (!editingId.value && linkMode.value === 'create') {
      // 现场新建：先入库成为题库题目，再关联
      const created = await questionApi.create({
        title: newQuestion.title,
        category: newQuestion.category,
        difficulty: newQuestion.difficulty,
      })
      questionId = created.id
    }
    const payload: InterviewRecordSave = {
      questionId,
      questionSnapshot: form.questionSnapshot,
      score: form.score ?? null,
      answered: form.answered,
      deduction: form.deduction || undefined,
      myAnswerSnapshot: form.myAnswerSnapshot || undefined,
    }
    if (editingId.value) {
      await interviewApi.updateRecord(interviewId, editingId.value, payload)
      ElMessage.success('记录已更新')
    } else {
      await interviewApi.addRecord(interviewId, payload)
      ElMessage.success(linkMode.value === 'create' ? '已新建题目并添加记录' : '记录已添加')
    }
    dialogVisible.value = false
    load()
  } finally {
    dialogSaving.value = false
  }
}

async function onRemoveRecord(row: InterviewRecord) {
  await ElMessageBox.confirm(`确定删除这道题目记录「${row.questionSnapshot}」吗？`, '删除确认', { type: 'warning' })
  await interviewApi.removeRecord(interviewId, row.id)
  ElMessage.success('已删除')
  load()
}

async function onSaveAsPhrase(row: InterviewRecord) {
  await ElMessageBox.confirm('将现场回答快照保存为题库该题的「个人话术」？会覆盖原有话术。', '保存为话术', { type: 'info' })
  await interviewApi.saveAsPhrase(interviewId, row.id)
  ElMessage.success('已保存为个人话术')
}

onMounted(load)
</script>

<template>
  <div class="interview-detail">
    <template v-if="detail">
      <el-card class="header-card">
        <div class="header">
          <div>
            <h2 class="title">{{ detail.interview.company }} · {{ detail.interview.position }}</h2>
            <div class="meta">
              <el-tag v-if="detail.interview.round" type="primary">{{ detail.interview.round }}</el-tag>
              <span v-if="detail.interview.interviewDate">{{ detail.interview.interviewDate }}</span>
              <span>{{ detail.interview.questionCount }} 题</span>
              <span v-if="detail.interview.avgScore != null">平均分 {{ detail.interview.avgScore }}</span>
            </div>
            <p v-if="detail.interview.summary" class="summary">{{ detail.interview.summary }}</p>
          </div>
          <div class="actions">
            <el-button @click="router.push('/interviews')">返回列表</el-button>
            <el-button @click="router.push(`/interviews/${interviewId}/edit`)">编辑场次</el-button>
            <el-button type="primary" @click="openAdd">添加题目记录</el-button>
          </div>
        </div>
      </el-card>

      <el-card>
        <el-table :data="detail.questions" v-loading="loading" stripe>
          <el-table-column type="index" label="#" width="55" />
          <el-table-column label="题目" min-width="280">
            <template #default="{ row }">
              <el-link v-if="row.questionId" type="primary" @click="router.push(`/questions/${row.questionId}/edit`)">
                {{ row.questionTitle }}
              </el-link>
              <span v-else>{{ row.questionSnapshot }}</span>
              <div v-if="row.questionSnapshot && row.questionSnapshot !== row.questionTitle" class="snapshot">
                现场问法：{{ row.questionSnapshot }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="80">
            <template #default="{ row }">
              <b :class="row.score != null && row.score < 5 ? 'low' : ''">{{ row.score ?? '—' }}</b>
            </template>
          </el-table-column>
          <el-table-column label="是否答上" width="100">
            <template #default="{ row }">
              <el-tag :type="answeredTagType(row.answered)">{{ row.answeredLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deduction" label="扣分点" min-width="160" show-overflow-tooltip />
          <el-table-column prop="myAnswerSnapshot" label="现场回答" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-tooltip :disabled="!!row.questionId && !!row.myAnswerSnapshot" content="需关联题库题目且有现场回答">
                <span>
                  <el-button link type="success" :disabled="!row.questionId || !row.myAnswerSnapshot" @click="onSaveAsPhrase(row)">
                    保存为话术
                  </el-button>
                </span>
              </el-tooltip>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="onRemoveRecord(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!detail.questions.length" description="还没有题目记录，点击右上角「添加题目记录」开始录入" />
      </el-card>

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
        <el-form label-width="90px">
          <template v-if="!editingId">
            <el-form-item label="题目来源">
              <el-radio-group v-model="linkMode">
                <el-radio-button value="choose">从题库选择</el-radio-button>
                <el-radio-button value="create">现场新建</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </template>

          <template v-if="linkMode === 'choose'">
            <el-form-item v-if="!editingId" label="关联题目">
              <el-select
                v-model="form.questionId"
                filterable
                remote
                clearable
                :remote-method="searchQuestions"
                :loading="searching"
                placeholder="输入关键词搜索题库"
                class="question-select"
                @change="onPickQuestion"
              >
                <el-option
                  v-for="q in questionOptions"
                  :key="q.id"
                  :label="`${q.title}（${categoryLabel(q.category)}）`"
                  :value="q.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-else label="关联题目">
              <span>{{ form.questionSnapshot }}</span>
            </el-form-item>
          </template>

          <template v-else>
            <el-form-item label="题目">
              <el-input v-model="newQuestion.title" placeholder="现场问到的题目" maxlength="500" />
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="newQuestion.category" placeholder="选择分类" class="question-select">
                <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度">
              <el-select v-model="newQuestion.difficulty" class="question-select">
                <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="Number(d.value)" />
              </el-select>
            </el-form-item>
          </template>

          <el-form-item v-if="linkMode === 'create' || editingId" label="题目快照">
            <el-input v-model="form.questionSnapshot" type="textarea" :rows="2" placeholder="现场题目的原话（导入/记录用）" maxlength="500" />
          </el-form-item>

          <el-form-item label="是否答上" required>
            <el-select v-model="form.answered" class="small-select">
              <el-option v-for="a in ANSWERED_OPTIONS" :key="a.value" :label="a.label" :value="a.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="得分">
            <el-input-number v-model="form.score" :min="0" :max="10" placeholder="未答不填" />
            <span class="hint">0-10 分，未答上可留空</span>
          </el-form-item>
          <el-form-item label="扣分点">
            <el-input v-model="form.deduction" type="textarea" :rows="3" placeholder="面试官指出/自评的扣分点" />
          </el-form-item>
          <el-form-item label="现场回答">
            <el-input v-model="form.myAnswerSnapshot" type="textarea" :rows="4" placeholder="当时的回答（复盘后可润色，再保存为题库话术）" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="dialogSaving" @click="onSubmit">保存</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<style scoped>
.interview-detail {
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.title {
  margin: 0 0 8px;
}
.meta {
  display: flex;
  gap: 12px;
  align-items: center;
  color: #606266;
  font-size: 13px;
}
.summary {
  margin: 12px 0 0;
  color: #303133;
  white-space: pre-wrap;
}
.actions {
  flex-shrink: 0;
}
.snapshot {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.low {
  color: #f56c6c;
}
.question-select {
  width: 100%;
}
.small-select {
  width: 160px;
}
.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
</style>
