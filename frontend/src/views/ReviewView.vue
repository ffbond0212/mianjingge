<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewApi, type ReviewPlan } from '../api/review'
import { questionApi, type QuestionItem } from '../api/question'

const today = ref<ReviewPlan[]>([])
const plans = ref<ReviewPlan[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    ;[today.value, plans.value] = await Promise.all([reviewApi.today(), reviewApi.plans()])
  } finally {
    loading.value = false
  }
}

// ---------- 打卡 ----------
const checkinVisible = ref(false)
const checking = ref<ReviewPlan | null>(null)
const checkinForm = ref<{ answered: number | null; score: number | undefined }>({ answered: null, score: undefined })

function openCheckin(plan: ReviewPlan) {
  checking.value = plan
  checkinForm.value = { answered: null, score: undefined }
  checkinVisible.value = true
}

async function submitCheckin() {
  if (!checking.value || !checkinForm.value.answered) {
    ElMessage.warning('请选择是否答上')
    return
  }
  const vo = await reviewApi.checkin(checking.value.id, {
    answered: checkinForm.value.answered,
    score: checkinForm.value.score,
  })
  checkinVisible.value = false
  if (vo.status === 'COMPLETED') {
    ElMessage.success(`「${vo.questionTitle.slice(0, 20)}」已毕业归档，无需再复习`)
  } else {
    ElMessage.success(`已排下次复习：${vo.nextReviewDate}（${vo.stageLabel} · 间隔 ${vo.intervalDays} 天）`)
  }
  load()
}

// ---------- 手动加题 ----------
const addVisible = ref(false)
const addOptions = ref<QuestionItem[]>([])
const addLoading = ref(false)
const addQuestionId = ref<number>()

async function searchQuestions(keyword: string) {
  addLoading.value = true
  try {
    const result = await questionApi.page({ page: 1, size: 20, keyword: keyword || undefined })
    addOptions.value = result.records
  } finally {
    addLoading.value = false
  }
}

function openAdd() {
  addQuestionId.value = undefined
  addOptions.value = []
  addVisible.value = true
  searchQuestions('')
}

async function submitAdd() {
  if (!addQuestionId.value) {
    ElMessage.warning('请选择题目')
    return
  }
  const vo = await reviewApi.enqueue(addQuestionId.value)
  addVisible.value = false
  ElMessage.success(`「${vo.questionTitle.slice(0, 20)}」已入队，${vo.nextReviewDate} 首次复习`)
  load()
}

// ---------- 移出 ----------
async function onRemove(plan: ReviewPlan) {
  await ElMessageBox.confirm(`确定把「${plan.questionTitle}」移出复习队列吗？`, '移出确认', { type: 'warning' })
  await reviewApi.remove(plan.id)
  ElMessage.success('已移出')
  load()
}

// ---------- 复习设置 ----------
const configVisible = ref(false)
const intervalsText = ref('')
const configSaving = ref(false)

async function openConfig() {
  const cfg = await reviewApi.config()
  intervalsText.value = cfg.intervals.join(',')
  configVisible.value = true
}

async function submitConfig() {
  const parts = intervalsText.value.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
  const nums = parts.map(Number)
  if (nums.length < 2 || nums.length > 10) {
    ElMessage.warning('间隔需 2-10 档')
    return
  }
  for (const n of nums) {
    if (!Number.isInteger(n) || n < 1 || n > 365) {
      ElMessage.warning('每档间隔需为 1-365 的整数')
      return
    }
  }
  for (let i = 1; i < nums.length; i++) {
    if (nums[i] <= nums[i - 1]) {
      ElMessage.warning('间隔必须逐档递增')
      return
    }
  }
  configSaving.value = true
  try {
    await reviewApi.updateConfig(nums)
    configVisible.value = false
    ElMessage.success('间隔配置已更新（对新排期生效）')
  } finally {
    configSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="review-page" v-loading="loading">
    <div class="page-header">
      <h2>复习计划</h2>
      <el-space>
        <el-button type="primary" @click="openAdd">加入复习</el-button>
        <el-button @click="openConfig">复习设置</el-button>
      </el-space>
    </div>

    <!-- 今日复习 -->
    <el-card class="card">
      <template #header><span>今日复习（{{ today.length }} 道到期）</span></template>
      <div v-if="today.length" class="today-list">
        <div v-for="p in today" :key="p.id" class="today-item">
          <div class="today-info">
            <span class="title">{{ p.questionTitle }}</span>
            <el-tag v-if="p.categoryLabel" size="small" class="cat">{{ p.categoryLabel }}</el-tag>
            <el-tag size="small" type="info">{{ p.stageLabel }}</el-tag>
            <span v-if="p.lastScore != null" class="last">上次 {{ p.lastScore }} 分</span>
          </div>
          <el-button type="primary" size="small" @click="openCheckin(p)">打卡</el-button>
        </div>
      </div>
      <el-empty v-else description="今日无到期任务" :image-size="80" />
    </el-card>

    <!-- 全部计划 -->
    <el-card class="card">
      <template #header><span>全部复习计划（{{ plans.length }} 道进行中）</span></template>
      <el-table :data="plans" stripe>
        <el-table-column prop="questionTitle" label="题目" min-width="240" show-overflow-tooltip />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">{{ row.categoryLabel ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="stageLabel" label="轮次" width="90" />
        <el-table-column label="间隔" width="80">
          <template #default="{ row }">{{ row.intervalDays }} 天</template>
        </el-table-column>
        <el-table-column prop="nextReviewDate" label="下次复习" width="110" />
        <el-table-column prop="reviewCount" label="已复习" width="80" />
        <el-table-column label="上次得分" width="90">
          <template #default="{ row }">{{ row.lastScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCheckin(row)">打卡</el-button>
            <el-button link type="danger" @click="onRemove(row)">移出</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!plans.length" description="队列为空，去题库加题或面试低分题会自动入队" :image-size="80" />
    </el-card>

    <!-- 打卡弹窗 -->
    <el-dialog v-model="checkinVisible" :title="checking?.questionTitle ?? '打卡'" width="440">
      <el-form label-width="80">
        <el-form-item label="是否答上">
          <el-radio-group v-model="checkinForm.answered">
            <el-radio-button :value="1">答上</el-radio-button>
            <el-radio-button :value="2">部分答上</el-radio-button>
            <el-radio-button :value="3">未答上</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="得分">
          <el-input-number v-model="checkinForm.score" :min="0" :max="10" placeholder="可不填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkinVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCheckin">提交打卡</el-button>
      </template>
    </el-dialog>

    <!-- 手动加题弹窗 -->
    <el-dialog v-model="addVisible" title="加入复习队列" width="480">
      <el-select
        v-model="addQuestionId"
        filterable
        remote
        :remote-method="searchQuestions"
        :loading="addLoading"
        placeholder="搜索题目并选择"
        class="add-select"
      >
        <el-option v-for="q in addOptions" :key="q.id" :label="q.title" :value="q.id" />
      </el-select>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">入队</el-button>
      </template>
    </el-dialog>

    <!-- 复习设置弹窗 -->
    <el-dialog v-model="configVisible" title="遗忘曲线间隔设置" width="480">
      <p class="config-tip">逗号分隔的递增天数（2-10 档），如 1,3,7,15,30。对新排期生效，已在队列的任务按原排期。</p>
      <el-input v-model="intervalsText" placeholder="1,3,7,15,30" />
      <template #footer>
        <el-button @click="configVisible = false">取消</el-button>
        <el-button type="primary" :loading="configSaving" @click="submitConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.review-page {
  padding: 16px 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card {
  margin-top: 16px;
}
.today-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.today-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}
.today-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.title {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.last {
  color: #909399;
  font-size: 12px;
}
.add-select {
  width: 100%;
}
.config-tip {
  color: #909399;
  font-size: 12px;
  margin: 0 0 10px;
}
</style>
