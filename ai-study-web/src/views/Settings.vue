<template>
  <div class="settings-page">
    <div class="page-nav">
      <div class="page-nav-back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </div>
      <div class="page-nav-title">账号设置</div>
      <div class="page-nav-action"></div>
    </div>

    <div class="settings-body">
      <!-- Avatar -->
      <div class="settings-section">
        <div class="settings-section-title">头像</div>
        <div class="settings-avatar-row">
          <div class="settings-avatar">
            {{ userStore.nickname?.charAt(0) || '?' }}
          </div>
          <div class="settings-avatar-info">
            <div class="settings-avatar-text">当前头像取昵称首字母，暂不支持自定义上传</div>
          </div>
        </div>
      </div>

      <!-- Nickname -->
      <div class="settings-section">
        <div class="settings-section-title">昵称</div>
        <div class="settings-form-row">
          <el-input
            v-model="nickname"
            placeholder="请输入昵称"
            size="large"
            maxlength="20"
            show-word-limit
          />
          <el-button
            type="primary"
            size="large"
            :loading="savingNickname"
            @click="handleSaveNickname"
          >
            保存
          </el-button>
        </div>
      </div>

      <!-- Email -->
      <div class="settings-section">
        <div class="settings-section-title">邮箱</div>
        <div class="settings-email">{{ userStore.userInfo?.email || '未绑定' }}</div>
      </div>

      <!-- Password -->
      <div class="settings-section">
        <div class="settings-section-title">修改密码</div>
        <el-form
          ref="pwdFormRef"
          :model="pwdForm"
          :rules="pwdRules"
          label-width="80px"
          class="settings-pwd-form"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input
              v-model="pwdForm.oldPassword"
              type="password"
              placeholder="请输入旧密码"
              size="large"
              show-password
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="pwdForm.newPassword"
              type="password"
              placeholder="请输入新密码（不少于6位）"
              size="large"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="pwdForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              size="large"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="savingPassword"
              @click="handleChangePassword"
            >
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo } from '@/api/auth'
import request from '@/api/request'

const router = useRouter()
const userStore = useUserStore()

const nickname = ref('')
const savingNickname = ref(false)
const savingPassword = ref(false)

const pwdFormRef = ref<FormInstance>()
const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    {
      required: true,
      validator: (_rule: any, value: string, callback: Function) => {
        if (!value) {
          callback(new Error('请再次输入新密码'))
        } else if (value !== pwdForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  nickname.value = userStore.nickname || ''
})

async function handleSaveNickname() {
  if (!nickname.value.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  savingNickname.value = true
  try {
    await updateUserInfo({ nickname: nickname.value.trim() })
    // Update local store
    if (userStore.userInfo) {
      userStore.userInfo.nickname = nickname.value.trim()
      localStorage.setItem('user', JSON.stringify(userStore.userInfo))
    }
    ElMessage.success('昵称已更新')
  } catch (error) {
    ElMessage.error('更新昵称失败')
  } finally {
    savingNickname.value = false
  }
}

async function handleChangePassword() {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }

  savingPassword.value = true
  try {
    await request.post('/v1/user/change-password', {
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clearUser()
    router.push('/login')
  } catch (error) {
    // error handled by interceptor
  } finally {
    savingPassword.value = false
  }
}
</script>

<style scoped>
.settings-page {
  max-width: 960px;
  margin: 0 auto;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.page-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 40px;
  background: white;
  border-bottom: 1px solid var(--border);
}

.page-nav-back {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}

.page-nav-back:hover {
  color: var(--primary);
}

.page-nav-title {
  font-size: 16px;
  font-weight: 700;
}

.page-nav-action {
  min-width: 40px;
}

.settings-body {
  padding: 24px 40px 40px;
}

.settings-section {
  margin-bottom: 32px;
}

.settings-section-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 12px;
}

.settings-avatar-row {
  display: flex;
  align-items: center;
  gap: 20px;
}

.settings-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), #8B7CF7);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 800;
  flex-shrink: 0;
}

.settings-avatar-text {
  font-size: 13px;
  color: var(--text-light);
}

.settings-form-row {
  display: flex;
  gap: 12px;
  max-width: 400px;
}

.settings-form-row .el-input {
  flex: 1;
}

.settings-email {
  font-size: 14px;
  color: var(--text-secondary);
  padding: 8px 0;
}

.settings-pwd-form {
  max-width: 400px;
}

.settings-pwd-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .page-nav,
  .settings-body {
    padding-left: 20px;
    padding-right: 20px;
  }

  .settings-form-row {
    flex-direction: column;
  }

  .settings-pwd-form {
    max-width: 100%;
  }
}
</style>
