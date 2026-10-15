<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { interviewApi, type InterviewSave } from '../api/interview'

const route = useRoute()
const router = useRouter()

const isEdit = !!route.params.id
const saving = ref(false)
const form = reactive<InterviewSave>({
  company: '',
  position: '',
  round: '',
  interviewDate: '',
  summary: '',
})

onMounted(async () => {
  if (isEdit) {
    const detail = await interviewApi.detail(Number(route.params.id))
    Object.assign(form, {
      company: detail.interview.company,
      position: detail.interview.position,
      round: detail.interview.round ?? '',
      interviewDate: detail.interview.interviewDate ?? '',
      summary: detail.interview.summary ?? '',
    })
  }
})

async function onSubmit() {
  saving.value = true
  try {
    if (isEdit) {
      await interviewApi.update(Number(route.params.id), form)
      ElMessage.success('已保存')
      router.push(`/interviews/${route.params.id}`)
    } else {
      const created = await interviewApi.create(form)
      ElMessage.success('场次已创建，去录入题目吧')
      router.push(`/interviews/${created.id}`)
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="interview-edit">
    <el-card>
      <template #header>
        <span>{{ isEdit ? '编辑面试' : '新建面试' }}</span>
      </template>
      <el-form label-width="90px" class="form">
        <el-form-item label="公司" required>
          <el-input v-model="form.company" placeholder="如：模拟面试（自评）" maxlength="100" />
        </el-form-item>
        <el-form-item label="岗位" required>
          <el-input v-model="form.position" placeholder="如：Java 后端（秋招 2027 届）" maxlength="100" />
        </el-form-item>
        <el-form-item label="轮次">
          <el-input v-model="form.round" placeholder="如：一面 / 二面 / HR面" maxlength="50" />
        </el-form-item>
        <el-form-item label="面试日期">
          <el-date-picker v-model="form.interviewDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="整体总结">
          <el-input v-model="form.summary" type="textarea" :rows="5" placeholder="这场面试的整体复盘：表现好的、暴露的问题、下次重点……" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.interview-edit {
  padding: 16px 24px;
}
.form {
  max-width: 560px;
}
</style>
