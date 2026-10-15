<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { applicationApi, type ApplicationItem, type ApplicationTimelineItem } from '../api/application'
import { interviewApi, type InterviewItem } from '../api/interview'

const router = useRouter()

/** 看板列：主流程 7 列 + 已挂终态列 */
const COLUMNS = [
  { status: 'APPLIED', label: '已投递' },
  { status: 'WRITTEN', label: '笔试' },
  { status: 'FIRST', label: '一面' },
  { status: 'SECOND', label: '二面' },
  { status: 'THIRD', label: '三面' },
  { status: 'HR', label: 'HR面' },
  { status: 'OFFER', label: 'Offer' },
  { status: 'REJECTED', label: '已挂/已拒' },
] as const

const loading = ref(false)
const apps = ref<ApplicationItem[]>([])

const grouped = computed(() => {
  const map = new Map<string, ApplicationItem[]>(COLUMNS.map((c) => [c.status, []]))
  for (const app of apps.value) {
    map.get(app.status)?.push(app)
  }
  return map
})

function nextLabel(app: ApplicationItem): string | null {
  const idx = COLUMNS.findIndex((c) => c.status === app.status)
  return idx >= 0 && idx < 6 ? COLUMNS[idx + 1].label : null
}

async function load() {
  loading.value = true
  try {
    apps.value = await applicationApi.list()
  } finally {
    loading.value = false
  }
}

// ---------- 新建/编辑 ----------
const saveVisible = ref(false)
const savingId = ref<number>()
const saveForm = reactive({
  company: '',
  position: '',
  channel: '',
  applyDate: '',
  city: '',
  salary: '',
  jdUrl: '',
  remark: '',
})

function openCreate() {
  savingId.value = undefined
  Object.assign(saveForm, { company: '', position: '', channel: '', applyDate: '', city: '', salary: '', jdUrl: '', remark: '' })
  saveVisible.value = true
}

function openEdit(app: ApplicationItem) {
  savingId.value = app.id
  Object.assign(saveForm, {
    company: app.company,
    position: app.position,
    channel: app.channel ?? '',
    applyDate: app.applyDate ?? '',
    city: app.city ?? '',
    salary: app.salary ?? '',
    jdUrl: app.jdUrl ?? '',
    remark: app.remark ?? '',
  })
  saveVisible.value = true
}

async function submitSave() {
  const body = {
    company: saveForm.company.trim(),
    position: saveForm.position.trim(),
    channel: saveForm.channel || undefined,
    applyDate: saveForm.applyDate || undefined,
    city: saveForm.city || undefined,
    salary: saveForm.salary || undefined,
    jdUrl: saveForm.jdUrl || undefined,
    remark: saveForm.remark || undefined,
  }
  if (savingId.value) {
    await applicationApi.update(savingId.value, body)
    ElMessage.success('已保存')
  } else {
    await applicationApi.create(body)
    ElMessage.success('已创建投递')
  }
  saveVisible.value = false
  load()
}

// ---------- 状态流转 ----------
const transitionVisible = ref(false)
const transitioning = ref<ApplicationItem | null>(null)
const transitionTo = ref('')
const transitionForm = ref<{ remark: string; interviewId?: number | null }>({ remark: '', interviewId: null })
const interviewOptions = ref<InterviewItem[]>([])
const interviewLoading = ref(false)

function openTransition(app: ApplicationItem, toStatus: string) {
  transitioning.value = app
  transitionTo.value = toStatus
  transitionForm.value = { remark: '', interviewId: app.interviewId }
  interviewOptions.value = []
  transitionVisible.value = true
}

async function searchInterviews(keyword: string) {
  interviewLoading.value = true
  try {
    const result = await interviewApi.page({ page: 1, size: 20, keyword: keyword || undefined })
    interviewOptions.value = result.records
  } finally {
    interviewLoading.value = false
  }
}

async function submitTransition() {
  if (!transitioning.value) return
  const vo = await applicationApi.transition(transitioning.value.id, {
    toStatus: transitionTo.value,
    remark: transitionForm.value.remark || undefined,
    interviewId: transitionForm.value.interviewId ?? null,
  })
  transitionVisible.value = false
  ElMessage.success(`已流转到「${vo.statusLabel}」`)
  load()
}

async function onDelete(app: ApplicationItem) {
  await ElMessageBox.confirm(`确定删除「${app.company} · ${app.position}」投递记录吗？时间线一并删除。`, '删除确认', {
    type: 'warning',
  })
  await applicationApi.remove(app.id)
  ElMessage.success('已删除')
  load()
}

// ---------- 详情时间线 ----------
const detailVisible = ref(false)
const detailApp = ref<ApplicationItem | null>(null)
const timeline = ref<ApplicationTimelineItem[]>([])

async function openDetail(app: ApplicationItem) {
  detailApp.value = app
  timeline.value = await applicationApi.timeline(app.id)
  detailVisible.value = true
}

onMounted(load)
</script>

<template>
  <div class="board-page" v-loading="loading">
    <div class="page-header">
      <h2>投递看板</h2>
      <el-button type="primary" @click="openCreate">新建投递</el-button>
    </div>

    <div class="board">
      <div v-for="col in COLUMNS" :key="col.status" class="column">
        <div class="column-header">
          <span>{{ col.label }}</span>
          <el-tag size="small" type="info" round>{{ grouped.get(col.status)?.length ?? 0 }}</el-tag>
        </div>
        <div class="column-body">
          <el-card v-for="app in grouped.get(col.status)" :key="app.id" class="card" shadow="hover">
            <div class="card-title">{{ app.company }}</div>
            <div class="card-pos">{{ app.position }}</div>
            <div class="card-meta">
              <span v-if="app.city">{{ app.city }}</span>
              <span v-if="app.applyDate">{{ app.applyDate }}</span>
            </div>
            <div class="card-actions">
              <el-button v-if="nextLabel(app)" link type="primary" size="small" @click="openTransition(app, COLUMNS[COLUMNS.findIndex((c) => c.status === app.status) + 1].status)">
                推进到{{ nextLabel(app) }}
              </el-button>
              <el-button v-if="app.status !== 'OFFER' && app.status !== 'REJECTED'" link type="danger" size="small" @click="openTransition(app, 'REJECTED')">
                已挂/拒
              </el-button>
              <el-button link size="small" @click="openDetail(app)">时间线</el-button>
              <el-button v-if="app.interviewId" link type="success" size="small" @click="router.push(`/interviews/${app.interviewId}`)">复盘</el-button>
            </div>
          </el-card>
          <el-empty v-if="!grouped.get(col.status)?.length" description="" :image-size="40" />
        </div>
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="saveVisible" :title="savingId ? '编辑投递' : '新建投递'" width="520">
      <el-form label-width="80">
        <el-form-item label="公司" required>
          <el-input v-model="saveForm.company" placeholder="必填" />
        </el-form-item>
        <el-form-item label="岗位" required>
          <el-input v-model="saveForm.position" placeholder="必填" />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="saveForm.channel" allow-create filterable clearable class="full">
            <el-option v-for="c in ['官网', '牛客网', 'Boss直聘', '猎聘', '内推']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="投递日期">
          <el-date-picker v-model="saveForm.applyDate" type="date" value-format="YYYY-MM-DD" class="full" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="saveForm.city" />
        </el-form-item>
        <el-form-item label="薪资">
          <el-input v-model="saveForm.salary" placeholder="如 20k*15" />
        </el-form-item>
        <el-form-item label="JD 链接">
          <el-input v-model="saveForm.jdUrl" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="saveForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 状态流转弹窗 -->
    <el-dialog v-model="transitionVisible" width="480">
      <template #header>
        <span>{{ transitioning?.company }} · {{ transitioning?.position }}</span>
      </template>
      <p class="transition-tip">
        从「{{ transitioning?.statusLabel }}」推进到
        <b>{{ COLUMNS.find((c) => c.status === transitionTo)?.label }}</b>
      </p>
      <el-form label-width="80">
        <el-form-item v-if="transitionTo !== 'REJECTED'" label="关联复盘">
          <el-select
            v-model="transitionForm.interviewId"
            filterable
            remote
            clearable
            :remote-method="searchInterviews"
            :loading="interviewLoading"
            placeholder="可选：关联已录的面试场次"
            class="full"
          >
            <el-option
              v-for="iv in interviewOptions"
              :key="iv.id"
              :label="`${iv.company} · ${iv.position}${iv.round ? ' · ' + iv.round : ''}${iv.interviewDate ? ' · ' + iv.interviewDate : ''}`"
              :value="iv.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transitionForm.remark" type="textarea" :rows="2" placeholder="如：笔试 8 题对 6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transitionVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransition">确认流转</el-button>
      </template>
    </el-dialog>

    <!-- 详情时间线弹窗 -->
    <el-dialog v-model="detailVisible" width="520">
      <template #header>
        <span>{{ detailApp?.company }} · {{ detailApp?.position }}</span>
      </template>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="状态">{{ detailApp?.statusLabel }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ detailApp?.channel ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="投递日期">{{ detailApp?.applyDate ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="城市">{{ detailApp?.city ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="薪资">{{ detailApp?.salary ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="JD">{{ detailApp?.jdUrl ?? '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-timeline class="timeline">
        <el-timeline-item
          v-for="t in timeline"
          :key="t.id"
          :timestamp="new Date(t.createdAt).toLocaleString()"
          :type="t.toStatus === 'REJECTED' ? 'danger' : t.toStatus === 'OFFER' ? 'success' : 'primary'"
        >
          {{ t.fromStatusLabel ? `${t.fromStatusLabel} → ` : '' }}{{ t.toStatusLabel }}
          <span v-if="t.remark" class="tl-remark">（{{ t.remark }}）</span>
        </el-timeline-item>
      </el-timeline>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailApp" @click="openEdit(detailApp)">编辑</el-button>
        <el-button v-if="detailApp?.interviewId" type="success" @click="router.push(`/interviews/${detailApp.interviewId}`)">去复盘</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.board-page {
  padding: 16px 24px;
  height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.board {
  flex: 1;
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-top: 8px;
}
.column {
  flex: 0 0 240px;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
  border-radius: 8px;
  padding: 8px;
}
.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 6px 10px;
  font-weight: 600;
}
.column-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}
.card {
  border-radius: 8px;
}
.card-title {
  font-weight: 600;
}
.card-pos {
  color: #606266;
  font-size: 13px;
  margin-top: 2px;
}
.card-meta {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
  display: flex;
  gap: 8px;
}
.card-actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
}
.full {
  width: 100%;
}
.transition-tip {
  margin: 0 0 12px;
}
.timeline {
  margin-top: 14px;
}
.tl-remark {
  color: #909399;
}
</style>
