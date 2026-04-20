<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">通用管理系统</div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%;" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 登录页视图
 * @description 企业级登录界面，集成表单校验、响应式数据绑定及路由跳转控制
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

/** 登录表单响应式数据绑定 */
const form = ref({ username: '', password: '' })
/** 登录按钮加载状态 */
const loading = ref(false)
/** 表单校验引用 */
const formRef = ref()

/** 表单校验规则 */
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

/**
 * 登录提交处理
 * @description 执行表单校验→调用 Pinia login action→存储 Token→路由跳转仪表盘
 */
const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.value.username, form.value.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 错误信息由响应拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 380px;
  .card-header {
    text-align: center;
    font-size: 20px;
    font-weight: bold;
    color: #333;
  }
}
</style>
