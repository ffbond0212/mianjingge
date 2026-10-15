<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { statsApi, type StatsOverview } from '../api/stats'

const router = useRouter()

// 图表配色（dataviz 参考调色板 light 模式：单系列蓝 + 中性墨水）
const SERIES_BLUE = '#2a78d6'
const INK = '#0b0b0b'
const MUTED = '#898781'
const GRIDLINE = '#e1e0d9'

const loading = ref(false)
const stats = ref<StatsOverview | null>(null)

const topRef = ref<HTMLElement>()
const radarRef = ref<HTMLElement>()
const trendRef = ref<HTMLElement>()
const charts: echarts.ECharts[] = []

async function load() {
  loading.value = true
  try {
    stats.value = await statsApi.overview()
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  disposeCharts()
  renderTop()
  renderRadar()
  renderTrend()
}

function disposeCharts() {
  charts.forEach((c) => c.dispose())
  charts.length = 0
}

/** 高频题 TOP10：横向条形图，条端直接标注被问次数 */
function renderTop() {
  const el = topRef.value
  if (!el || !stats.value) return
  const data = stats.value.topQuestions
  if (!data.length) return
  // 倒序让被问最多的排最上面
  const sorted = [...data].reverse()
  const chart = echarts.init(el)
  chart.setOption({
    grid: { left: 8, right: 40, top: 8, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'item',
      formatter: (p: { dataIndex: number }) => {
        const d = sorted[p.dataIndex]
        return `<b>${d.title}</b><br/>${d.categoryLabel ?? '未分类'} · 被问 ${d.askedCount} 次<br/>平均得分：${d.avgScore ?? '—'}`
      },
    },
    xAxis: { type: 'value', minInterval: 1, axisLabel: { color: MUTED }, splitLine: { lineStyle: { color: GRIDLINE } } },
    yAxis: {
      type: 'category',
      data: sorted.map((d) => (d.title.length > 18 ? `${d.title.slice(0, 18)}…` : d.title)),
      axisLabel: { color: INK, width: 190, overflow: 'truncate' },
      axisLine: { lineStyle: { color: GRIDLINE } },
    },
    series: [
      {
        type: 'bar',
        data: sorted.map((d) => d.askedCount),
        barWidth: 14,
        itemStyle: { color: SERIES_BLUE, borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', color: INK, fontWeight: 600 },
      },
    ],
  })
  charts.push(chart)
}

/** 分类均分雷达：十大类全轴，无数据类按 0 显示 */
function renderRadar() {
  const el = radarRef.value
  if (!el || !stats.value) return
  const data = stats.value.categoryRadar
  if (!data.length) return
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p: { dataIndex: number }) => {
        const d = data[p.dataIndex]
        return d.scoreCount
          ? `<b>${d.categoryLabel}</b><br/>平均得分 ${d.avgScore}（${d.scoreCount} 次打分 / ${d.questionCount} 题）`
          : `<b>${d.categoryLabel}</b><br/>暂无打分数据`
      },
    },
    radar: {
      indicator: data.map((d) => ({ name: d.categoryLabel, max: 10 })),
      radius: '62%',
      axisName: { color: INK, fontSize: 12 },
      splitLine: { lineStyle: { color: GRIDLINE } },
      splitArea: { show: false },
      axisLine: { lineStyle: { color: GRIDLINE } },
    },
    series: [
      {
        type: 'radar',
        data: [{ value: data.map((d) => d.avgScore ?? 0), name: '平均得分' }],
        symbolSize: 6,
        lineStyle: { color: SERIES_BLUE, width: 2 },
        itemStyle: { color: SERIES_BLUE },
        areaStyle: { color: 'rgba(42, 120, 214, 0.15)' },
      },
    ],
  })
  charts.push(chart)
}

/** 场次均分折线：按日期排序；题目数放 tooltip，不设双轴 */
function renderTrend() {
  const el = trendRef.value
  if (!el || !stats.value) return
  const data = stats.value.interviewTrend
  if (!data.length) return
  const chart = echarts.init(el)
  chart.setOption({
    grid: { left: 8, right: 16, top: 32, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params: { dataIndex: number }[]) => {
        const d = data[params[0].dataIndex]
        return `<b>${d.company} · ${d.position}</b>${d.round ? `<br/>${d.round}` : ''}${d.interviewDate ? `<br/>${d.interviewDate}` : ''}<br/>${d.questionCount} 题 · 平均分：${d.avgScore ?? '未打分'}`
      },
    },
    xAxis: {
      type: 'category',
      data: data.map((d) => `${d.interviewDate ?? ''}${d.round ? ` ${d.round}` : ''}`.trim() || `#${d.interviewId}`),
      axisLabel: { color: MUTED, rotate: data.length > 6 ? 30 : 0 },
      axisLine: { lineStyle: { color: GRIDLINE } },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 10,
      axisLabel: { color: MUTED },
      splitLine: { lineStyle: { color: GRIDLINE } },
    },
    series: [
      {
        type: 'line',
        data: data.map((d) => d.avgScore),
        symbolSize: 8,
        lineStyle: { color: SERIES_BLUE, width: 2 },
        itemStyle: { color: SERIES_BLUE, borderColor: '#fcfcfb', borderWidth: 2 },
        emphasis: { focus: 'series' },
      },
    ],
  })
  charts.push(chart)
}

function onResize() {
  charts.forEach((c) => c.resize())
}

onMounted(() => {
  window.addEventListener('resize', onResize)
  load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>

<template>
  <div class="stats-page">
    <div class="page-header">
      <h2>复盘统计</h2>
      <el-button :loading="loading" @click="load">刷新统计</el-button>
    </div>

    <div class="grid" v-loading="loading">
      <el-card class="card">
        <template #header><span>高频题 TOP10（被面试问到次数）</span></template>
        <div ref="topRef" class="chart tall" />
        <el-empty v-if="stats && !stats.topQuestions.length" description="还没有面试记录" :image-size="80" />
      </el-card>

      <el-card class="card">
        <template #header><span>分类均分雷达</span></template>
        <div ref="radarRef" class="chart tall" />
        <p class="note">按关联题库题的分类统计平均得分（0-10）；未打分或无数据的分类显示为 0</p>
      </el-card>

      <el-card class="card wide">
        <template #header><span>场次均分走势</span></template>
        <div ref="trendRef" class="chart" />
        <el-empty v-if="stats && !stats.interviewTrend.length" description="还没有面试场次" :image-size="80" />
      </el-card>
    </div>

    <el-button class="back" @click="router.push('/interviews')">返回面试记录</el-button>
  </div>
</template>

<style scoped>
.stats-page {
  padding: 16px 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 8px;
}
.card {
  min-width: 0;
}
.wide {
  grid-column: 1 / -1;
}
.chart {
  height: 320px;
}
.tall {
  height: 360px;
}
.note {
  color: #909399;
  font-size: 12px;
  margin: 0 0 4px;
}
.back {
  margin-top: 16px;
}
</style>
