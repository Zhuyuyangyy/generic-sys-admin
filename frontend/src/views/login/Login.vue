<template>
  <div class="login-page">
    <!-- Background decoration -->
    <div class="bg-orb bg-orb-1"></div>
    <div class="bg-orb bg-orb-2"></div>
    <div class="bg-orb bg-orb-3"></div>

    <!-- Login card -->
    <GradientCard class="login-card" variant="glass">
      <template #header>
        <div class="login-header">
          <div class="login-logo">
            <svg width="36" height="36" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="14" fill="url(#logoGrad)"/>
              <path d="M14 24 L24 14 L34 24 L24 34 Z" fill="white" opacity="0.9"/>
              <defs>
                <linearGradient id="logoGrad" x1="0" y1="0" x2="48" y2="48">
                  <stop offset="0%" stop-color="#667eea"/>
                  <stop offset="100%" stop-color="#764ba2"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <span class="login-title">通用管理系统</span>
          <span class="login-subtitle">Graduation Project · 毕业设计管理系统</span>
        </div>
      </template>

      <form class="login-form" @submit.prevent="handleLogin">
        <GradientInput
          v-model="form.username"
          label="用户名"
          placeholder="请输入用户名"
          :error="errors.username"
        />

        <GradientInput
          v-model="form.password"
          label="密码"
          type="password"
          placeholder="请输入密码"
          :error="errors.password"
        />

        <GradientToggle v-model="remember" label="记住登录状态" />

        <GradientButton
          type="submit"
          variant="primary"
          size="lg"
          :loading="loading"
          style="width: 100%; margin-top: 8px;"
        >
          登 录
        </GradientButton>
      </form>

      <template #footer>
        <div class="login-footer">
          <span class="footer-hint">默认账号：admin / admin123</span>
        </div>
      </template>
    </GradientCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { GradientCard, GradientInput, GradientButton, GradientToggle } from '@/components/ui'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '' })
const loading = ref(false)
const remember = ref(false)

const handleLogin = async () => {
  // Clear errors
  errors.username = ''
  errors.password = ''

  // Validate
  if (!form.username.trim()) {
    errors.username = '请输入用户名'
    return
  }
  if (!form.password.trim()) {
    errors.password = '请输入密码'
    return
  }

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功，欢迎回来！')
    router.push('/dashboard')
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  position: relative;
  overflow: hidden;
  font-family: 'Segoe UI', system-ui, sans-serif;
}

/* Background orbs */
.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
  pointer-events: none;
}

.bg-orb-1 {
  width: 500px;
  height: 500px;
  background: #667eea;
  top: -150px;
  left: -100px;
  animation: float 8s ease-in-out infinite;
}

.bg-orb-2 {
  width: 400px;
  height: 400px;
  background: #764ba2;
  bottom: -100px;
  right: -80px;
  animation: float 10s ease-in-out infinite reverse;
}

.bg-orb-3 {
  width: 300px;
  height: 300px;
  background: #06b6d4;
  top: 50%;
  left: 60%;
  transform: translate(-50%, -50%);
  animation: float 12s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(20px, -20px); }
  66% { transform: translate(-15px, 15px); }
}

/* Card */
.login-card {
  width: 420px;
  max-width: 95vw;
  position: relative;
  z-index: 10;
}

/* Header */
.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.login-logo {
  margin-bottom: 4px;
}

.login-title {
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.02em;
}

.login-subtitle {
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 0.04em;
}

/* Form */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* Footer */
.login-footer {
  display: flex;
  justify-content: center;
}

.footer-hint {
  font-size: 12px;
  color: #94a3b8;
}
</style>
