<template>
  <div class="login-page">
    <!-- Left Panel -->
    <div class="login-left">
      <div class="login-brand">
        <div class="login-brand-icon">AI</div>
        <div class="login-brand-text">知识闯关</div>
      </div>
      <h2>学一遍不如闯一关</h2>
      <p>AI 驱动的智能学习平台，输入任意知识内容，自动生成闯关题目，让学习变得高效又有趣。</p>
      <div class="login-features">
        <div class="login-feature">
          <el-icon><Monitor /></el-icon>
          <span>AI 智能出题，覆盖全学科</span>
        </div>
        <div class="login-feature">
          <el-icon><Aim /></el-icon>
          <span>闯关模式，越学越上瘾</span>
        </div>
        <div class="login-feature">
          <el-icon><TrendCharts /></el-icon>
          <span>学习报告，可视化进步</span>
        </div>
      </div>
    </div>

    <!-- Right Panel -->
    <div class="login-right">
      <div class="login-title">{{ isRegister ? '创建账号' : '欢迎回来' }}</div>
      <div class="login-subtitle">
        {{ isRegister ? '注册新账号，开始闯关之旅' : '登录你的账号，继续闯关之旅' }}
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        label-width="80px"
        @submit.prevent="handleSubmit"
      >
        <el-form-item v-if="isRegister" label="昵称" prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称（选填）"
            size="large"
          />
        </el-form-item>

        <el-form-item label="邮箱地址" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item v-if="isRegister" label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item label="验证码" prop="captcha">
          <div class="captcha-row">
            <el-input
              v-model="form.captcha"
              placeholder="请输入验证码"
              size="large"
              class="captcha-input"
            />
            <div class="captcha-image" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else class="captcha-placeholder">加载中...</span>
            </div>
          </div>
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="btn-login"
          :loading="loading"
          @click="handleSubmit"
        >
          {{ isRegister ? '注册' : '登录' }}
        </el-button>
      </el-form>

      <div class="login-divider">或使用第三方账号登录</div>

      <div class="login-social">
        <el-button size="large" class="btn-social" @click="handleGithubLogin">
          <el-icon><Link /></el-icon>
          <span>使用 GitHub 登录</span>
        </el-button>
      </div>

      <div class="login-switch">
        {{ isRegister ? '已有账号？' : '没有账号？' }}
        <el-link type="primary" @click="isRegister = !isRegister; refreshCaptcha()">
          {{ isRegister ? '立即登录' : '立即注册' }}
        </el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Monitor, Aim, TrendCharts, Link } from '@element-plus/icons-vue'
import { register, login, getGithubAuthUrl, getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const isRegister = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
  captcha: ''
})
const captchaImage = ref('')

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    {
      required: true,
      validator: (rule: any, value: string, callback: Function) => {
        if (isRegister.value && !value) {
          callback(new Error('请再次输入密码'))
        } else if (isRegister.value && value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

async function refreshCaptcha() {
  try {
    const data = await getCaptcha()
    captchaImage.value = data.image
    form.captcha = ''
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

onMounted(() => {
  refreshCaptcha()
})

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    let data
    if (isRegister.value) {
      data = await register({
        email: form.email,
        password: form.password,
        nickname: form.nickname || undefined,
        captcha: form.captcha
      })
    } else {
      data = await login({
        email: form.email,
        password: form.password,
        captcha: form.captcha
      })
    }

    userStore.setUser(data)
    ElMessage.success(isRegister.value ? '注册成功' : '登录成功')
    const redirect = router.currentRoute.value.query.redirect as string
    router.push(redirect || '/')
  } catch (error) {
    // 错误已在拦截器中处理，刷新验证码
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

async function handleGithubLogin() {
  try {
    const data = await getGithubAuthUrl()
    window.location.href = data.url
  } catch (error: any) {
    ElMessage.error(error.message || '获取 GitHub 授权链接失败，请检查配置')
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, var(--primary), #8B7CF7);
  padding: 60px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 48px;
}

.login-brand-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
}

.login-brand-text {
  font-size: 24px;
  font-weight: 700;
}

.login-left h2 {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.4;
  margin-bottom: 16px;
}

.login-left p {
  font-size: 15px;
  opacity: 0.85;
  line-height: 1.7;
}

.login-features {
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-feature {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.9;
}

.login-feature .el-icon {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.login-right {
  flex: 1;
  padding: 60px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-placeholder {
  font-size: 12px;
  color: var(--text-light);
}

.btn-login {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 700;
  margin-top: 8px;
}

.login-divider {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 24px 0;
  font-size: 12px;
  color: var(--text-light);
}

.login-divider::before,
.login-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}

.login-social {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.btn-social {
  width: 100%;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.login-switch {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .login-left {
    padding: 40px 24px;
  }

  .login-right {
    padding: 40px 24px;
  }
}
</style>
