<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { questionApi } from '../api/question'
import { CATEGORIES, DIFFICULTIES } from '../constants/question'

const route = useRoute()
const router = useRouter()

const id = ref<number | null>(null)
const loading = ref(false)
const form = reactive({
  title: '',
  category: 'JAVA_BASIC',
  tags: '',
  difficulty: 2,
  stdAnswer: '',
  myAnswer: '',
})

const isEdit = route.params.id !== undefined

onMounted(async () => {
  if (!isEdit) return
  id.value = Number(route.params.id)
  loading.value = true
  try {
    const item = await questionApi.get(id.value)
    form.title = item.title
    form.category = item.category
    form.tags = item.tags ?? ''
    form.difficulty = item.difficulty
    form.stdAnswer = item.stdAnswer ?? ''
    form.myAnswer = item.myAnswer ?? ''
  } finally {
    loading.value = false
  }
})

async function onSave() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写题目')
    return
  }
  const payload = {
    title: form.title.trim(),
    category: form.category,
    tags: form.tags.trim() || undefined,
    difficulty: form.difficulty,
    stdAnswer: form.stdAnswer,
    myAnswer: form.myAnswer,
  }
  if (id.value) {
    await questionApi.update(id.value, payload)
    ElMessage.success('已保存')
  } else {
    await questionApi.create(payload)
    ElMessage.success('已创建')
  }
  router.push('/questions')
}
</script>

<template>
  <div class="edit-page" v-loading="loading">
    <el-card>
      <template #header>{{ isEdit ? '编辑题目' : '新建题目' }}</template>
      <el-form label-width="100px" class="form">
        <el-form-item label="题目" required>
          <el-input v-model="form.title" placeholder="输入面试题目" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" class="w-180">
            <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" required>
          <el-radio-group v-model="form.difficulty">
            <el-radio-button v-for="d in DIFFICULTIES" :key="d.value" :value="Number(d.value)">{{ d.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔，如：分库分表,基因法" />
        </el-form-item>
        <el-form-item label="标准答案">
          <el-input v-model="form.stdAnswer" type="textarea" :rows="10" placeholder="面试官认可的标准答案" />
        </el-form-item>
        <el-form-item label="个人话术">
          <el-input v-model="form.myAnswer" type="textarea" :rows="8" placeholder="你自己的回答版本" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSave">保存</el-button>
          <el-button @click="router.push('/questions')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.edit-page {
  padding: 16px 24px;
}
.form {
  max-width: 900px;
}
.w-180 {
  width: 180px;
}
</style>
