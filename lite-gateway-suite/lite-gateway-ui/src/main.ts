import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { errorConfigService } from '@/services/errorConfigService'

// 初始化应用
async function bootstrap() {
  // 1. 先加载错误码配置（在应用渲染前）
  await errorConfigService.init()

  // 2. 创建应用
  const app = createApp(App)

  // 3. 使用插件
  app.use(createPinia())
  app.use(router)
  app.use(Antd)

  // 4. 挂载应用
  app.mount('#app')
}

// 启动应用
bootstrap()
