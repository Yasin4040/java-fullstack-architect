<template>
  <div class="dashboard">
    <a-card title="仪表盘" class="mb-4">
      <div class="dashboard-stats">
        <a-statistic
          title="路由数量"
          :value="routeCount"
          class="stat-card"
        >
          <template #suffix>
            <a-tag color="blue">个</a-tag>
          </template>
        </a-statistic>
        <a-statistic
          title="限流规则"
          :value="rateLimitCount"
          class="stat-card"
        >
          <template #suffix>
            <a-tag color="green">条</a-tag>
          </template>
        </a-statistic>
        <a-statistic
          title="今日请求"
          :value="todayRequests"
          class="stat-card"
        >
          <template #suffix>
            <a-tag color="orange">次</a-tag>
          </template>
        </a-statistic>
        <a-statistic
          title="系统状态"
          :value="systemStatus"
          class="stat-card"
        >
          <template #suffix>
            <a-tag :color="systemStatus === '正常' ? 'green' : 'red'">
              {{ systemStatus }}
            </a-tag>
          </template>
        </a-statistic>
      </div>
    </a-card>
    
    <a-card title="最近访问日志" class="mb-4">
      <a-table :columns="logColumns" :data-source="recentLogs" row-key="id" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import type { TableColumnsType } from 'antdv-next'
import { Tag } from 'antdv-next'

const routeCount = ref(12)
const rateLimitCount = ref(5)
const todayRequests = ref(1258)
const systemStatus = ref('正常')

interface LogItem {
  id: string
  path: string
  method: string
  status: number
  time: string
  ip: string
}

const recentLogs = ref<LogItem[]>([
  { id: '1', path: '/api/users', method: 'GET', status: 200, time: '2026-03-11 10:00:00', ip: '192.168.1.1' },
  { id: '2', path: '/api/products', method: 'POST', status: 201, time: '2026-03-11 09:45:00', ip: '192.168.1.2' },
  { id: '3', path: '/api/orders', method: 'GET', status: 200, time: '2026-03-11 09:30:00', ip: '192.168.1.3' },
  { id: '4', path: '/api/users/1', method: 'PUT', status: 200, time: '2026-03-11 09:15:00', ip: '192.168.1.4' },
  { id: '5', path: '/api/products/2', method: 'DELETE', status: 204, time: '2026-03-11 09:00:00', ip: '192.168.1.5' },
])

const logColumns: TableColumnsType<LogItem> = [
  { title: '路径', dataIndex: 'path', key: 'path' },
  { title: '方法', dataIndex: 'method', key: 'method' },
  { 
    title: '状态', 
    dataIndex: 'status', 
    key: 'status',
    render: (status: number) => {
      return h(Tag, {
        color: status >= 200 && status < 300 ? 'green' : 'red'
      }, status)
    }
  },
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: 'IP', dataIndex: 'ip', key: 'ip' },
]

onMounted(() => {
  // 这里可以添加数据获取逻辑
  console.log('Dashboard mounted')
})
</script>

<style scoped>
.dashboard {
  padding: 16px;
}

.dashboard-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 200px;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>