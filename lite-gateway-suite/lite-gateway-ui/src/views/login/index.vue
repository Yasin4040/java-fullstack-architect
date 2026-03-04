<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h1>Lite Gateway</h1>
        <p>轻量级网关管理后台</p>
      </div>
      
      <a-form
        :model="formState"
        :rules="rules"
        @finish="handleLogin"
        class="login-form"
      >
        <a-form-item name="username">
          <a-input
            v-model:value="formState.username"
            size="large"
            placeholder="用户名"
          >
            <template #prefix>
              <user-outlined />
            </template>
          </a-input>
        </a-form-item>
        
        <a-form-item name="password">
          <a-input-password
            v-model:value="formState.password"
            size="large"
            placeholder="密码"
            @pressEnter="handleLogin"
          >
            <template #prefix>
              <lock-outlined />
            </template>
          </a-input-password>
        </a-form-item>

        <!-- 验证码（可选） -->
        <a-form-item name="captcha" v-if="showCaptcha">
          <a-input
            v-model:value="formState.captcha"
            size="large"
            placeholder="验证码"
          >
            <template #prefix>
              <safety-outlined />
            </template>
            <template #suffix>
              <img 
                v-if="captchaImage" 
                :src="captchaImage" 
                class="captcha-img" 
                @click="refreshCaptcha"
                alt="验证码"
              />
            </template>
          </a-input>
        </a-form-item>
        
        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            :loading="loading"
            block
          >
            登录
          </a-button>
        </a-form-item>
      </a-form>
      
      <div class="login-footer">
        <p>默认账号: admin / 123456</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { userApi } from '@/api/user'
import type { LoginParams } from '@/types/api'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const showCaptcha = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

const formState = reactive<LoginParams>({
  username: '',
  password: '',
  captcha: '',
  captchaKey: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: false, message: '请输入验证码', trigger: 'blur' }]
}

// 获取认证配置 - 静默模式，不显示错误提示
const fetchAuthConfig = async () => {
  try {
    const config = await userApi.getAuthConfig()
    console.log('[Login] Auth config:', config)
  } catch (error: any) {
    // 静默处理错误，只在控制台输出，不显示错误提示
    console.warn('[Login] Failed to get auth config:', error?.message || '网络错误')
  }
}

// 刷新验证码
const refreshCaptcha = async () => {
  // 这里可以实现验证码刷新逻辑
  console.log('[Login] Refresh captcha')
}

const handleLogin = async () => {
  loading.value = true
  try {
    // 如果有验证码，添加验证码Key
    if (showCaptcha.value && captchaKey.value) {
      formState.captchaKey = captchaKey.value
    }
    
    await userStore.login(formState)
    message.success('登录成功')
    router.push('/')
  } catch (error: any) {
    // 处理登录失败
    const errorMsg = error?.message || '登录失败，请检查用户名和密码'
    message.error(errorMsg)
    
    // 如果是验证码错误，显示验证码输入框
    if (error?.code === 'A0240') {
      showCaptcha.value = true
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAuthConfig()
})
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-container {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;

  h1 {
    font-size: 28px;
    color: #1890ff;
    margin-bottom: 8px;
  }

  p {
    color: #666;
    font-size: 14px;
  }
}

.login-form {
  .ant-input-affix-wrapper {
    border-radius: 4px;
  }

  .ant-btn {
    border-radius: 4px;
    height: 44px;
    font-size: 16px;
  }
}

.captcha-img {
  height: 32px;
  cursor: pointer;
  border-radius: 4px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;

  p {
    color: #999;
    font-size: 12px;
  }
}
</style>
