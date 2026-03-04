<template>
  <a-layout class="main-layout">
    <!-- 侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      class="sider"
    >
      <div class="logo">
        <span v-if="!collapsed">Lite Gateway</span>
        <span v-else>LG</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        theme="dark"
        :items="menuItems"
        @click="handleMenuClick"
      />
    </a-layout-sider>

    <a-layout>
      <!-- 顶部导航 -->
      <a-layout-header class="header">
        <div class="header-left">
          <menu-unfold-outlined
            v-if="collapsed"
            class="trigger"
            @click="() => (collapsed = !collapsed)"
          />
          <menu-fold-outlined
            v-else
            class="trigger"
            @click="() => (collapsed = !collapsed)"
          />
          <breadcrumb />
        </div>
        <div class="header-right">
          <a-dropdown>
            <span class="user-info">
              <user-outlined />
              {{ userStore.username }}
              <down-outlined />
            </span>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="handleProfile">
                  <user-outlined />
                  个人中心
                </a-menu-item>
                <a-menu-item key="password" @click="handleChangePassword">
                  <lock-outlined />
                  修改密码
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout">
                  <logout-outlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 内容区域 -->
      <a-layout-content class="content">
        <div class="content-wrapper">
          <router-view />
        </div>
      </a-layout-content>

      <!-- 底部 -->
      <a-layout-footer class="footer">
        Lite Gateway Admin ©2024 Created by Lite Gateway Team
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, watch, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  UserOutlined,
  DownOutlined,
  LogoutOutlined,
  LockOutlined,
  DashboardOutlined,
  NodeIndexOutlined,
  SafetyOutlined,
  FileTextOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { useUserStore } from '@/store/modules/user'
import Breadcrumb from './components/Breadcrumb.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 侧边栏折叠状态
const collapsed = ref(false)

// 菜单选中状态
const selectedKeys = ref<string[]>([route.name as string])
const openKeys = ref<string[]>([])

// 监听路由变化
watch(
  () => route.name,
  (name) => {
    selectedKeys.value = [name as string]
  }
)

// 菜单配置
const menuItems = computed(() => [
  {
    key: 'Dashboard',
    icon: () => h(DashboardOutlined),
    label: '仪表盘'
  },
  {
    key: 'Route',
    icon: () => h(NodeIndexOutlined),
    label: '路由管理'
  },
  {
    key: 'RateLimit',
    icon: () => h(SafetyOutlined),
    label: '限流配置'
  },
  {
    key: 'Log',
    icon: () => h(FileTextOutlined),
    label: '日志监控'
  },
  {
    key: 'System',
    icon: () => h(SettingOutlined),
    label: '系统管理',
    children: [
      {
        key: 'User',
        label: '用户管理'
      }
    ]
  }
])

// 菜单点击
const handleMenuClick = ({ key }: { key: string }) => {
  router.push({ name: key })
}

// 个人中心
const handleProfile = () => {
  message.info('个人中心功能开发中')
}

// 修改密码
const handleChangePassword = () => {
  message.info('修改密码功能开发中')
}

// 退出登录
const handleLogout = () => {
  Modal.confirm({
    title: '确认退出',
    content: '确定要退出登录吗？',
    onOk: async () => {
      await userStore.logout()
      message.success('退出成功')
      router.push('/login')
    }
  })
}
</script>

<style scoped lang="scss">
.main-layout {
  min-height: 100vh;
}

.sider {
  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .trigger {
    font-size: 18px;
    cursor: pointer;
    transition: color 0.3s;

    &:hover {
      color: #1890ff;
    }
  }

  .header-right {
    .user-info {
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      border-radius: 4px;
      transition: background 0.3s;

      &:hover {
        background: rgba(0, 0, 0, 0.025);
      }
    }
  }
}

.content {
  margin: 24px;
  
  .content-wrapper {
    background: #fff;
    padding: 24px;
    min-height: calc(100vh - 64px - 24px - 24px - 70px);
    border-radius: 4px;
  }
}

.footer {
  text-align: center;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
</style>
