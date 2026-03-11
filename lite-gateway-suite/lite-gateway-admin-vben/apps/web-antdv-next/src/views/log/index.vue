<template>
  <div class="log-management">
    <div class="log-header">
      <h1>{{ $t('page.log.title') }}</h1>
      <a-button danger @click="cleanLogs">
        <template #icon>
          <Trash2 />
        </template>
        清理日志
      </a-button>
    </div>
    
    <a-card class="log-search">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="路由ID">
          <a-input v-model:value="searchForm.routeId" placeholder="请输入路由ID" />
        </a-form-item>
        <a-form-item label="请求路径">
          <a-input v-model:value="searchForm.path" placeholder="请输入请求路径" />
        </a-form-item>
        <a-form-item label="请求方法">
          <a-input v-model:value="searchForm.method" placeholder="请输入请求方法" />
        </a-form-item>
        <a-form-item label="客户端IP">
          <a-input v-model:value="searchForm.clientIp" placeholder="请输入客户端IP" />
        </a-form-item>
        <a-form-item label="状态码">
          <a-input v-model:value="searchForm.statusCode" placeholder="请输入状态码" />
        </a-form-item>
        <a-form-item label="开始时间">
          <a-date-picker 
            v-model:value="startTime" 
            format="YYYY-MM-DD HH:mm:ss" 
            show-time 
          />
        </a-form-item>
        <a-form-item label="结束时间">
          <a-date-picker 
            v-model:value="endTime" 
            format="YYYY-MM-DD HH:mm:ss" 
            show-time 
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="search">
            <template #icon>
              <Search />
            </template>
            搜索
          </a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>
    
    <a-card class="log-table">
      <a-table
        :columns="columns"
        :data-source="logs"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'duration'">
            <span>{{ record.duration }}ms</span>
          </template>
          <template v-else-if="column.key === 'errorMsg'">
            <a-tooltip :title="record.errorMsg">
              <span class="error-msg">{{ record.errorMsg ? '查看错误' : '-' }}</span>
            </a-tooltip>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { logApi } from '@/api'
import type { LogQuery, GatewayLog } from '@/types/api'
import { Search, Trash2 } from '@vben/icons'
import { message, Modal } from 'antdv-next'
import dayjs from 'dayjs'

const loading = ref(false)
const logs = ref<GatewayLog[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  onChange: (page: number) => {
    pagination.current = page
    fetchLogs()
  },
  onShowSizeChange: (page: number, pageSize: number) => {
    pagination.current = page
    pagination.pageSize = pageSize
    fetchLogs()
  }
})

const searchForm = reactive<LogQuery>({
  routeId: '',
  path: '',
  method: '',
  clientIp: '',
  statusCode: ''
})

const startTime = ref<dayjs.Dayjs | null>(null)
const endTime = ref<dayjs.Dayjs | null>(null)

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '路由ID', dataIndex: 'routeId', key: 'routeId' },
  { title: '请求路径', dataIndex: 'path', key: 'path' },
  { title: '请求方法', dataIndex: 'method', key: 'method' },
  { title: '客户端IP', dataIndex: 'clientIp', key: 'clientIp' },
  { title: '状态码', dataIndex: 'statusCode', key: 'statusCode' },
  { title: '响应时间', dataIndex: 'duration', key: 'duration' },
  { title: '请求大小', dataIndex: 'requestSize', key: 'requestSize' },
  { title: '响应大小', dataIndex: 'responseSize', key: 'responseSize' },
  { title: '错误信息', dataIndex: 'errorMsg', key: 'errorMsg' },
  { title: '请求时间', dataIndex: 'requestTime', key: 'requestTime' }
]

onMounted(() => {
  fetchLogs()
})

async function fetchLogs() {
  loading.value = true
  try {
    const query: LogQuery = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    
    if (startTime.value) {
      query.startTime = startTime.value.format('YYYY-MM-DD HH:mm:ss')
    }
    if (endTime.value) {
      query.endTime = endTime.value.format('YYYY-MM-DD HH:mm:ss')
    }
    
    const response = await logApi.getLogPage(query)
    logs.value = response.list
    pagination.total = response.total
  } catch (error) {
    console.error('获取日志列表失败:', error)
    message.error('获取日志列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  fetchLogs()
}

function resetSearch() {
  searchForm.routeId = ''
  searchForm.path = ''
  searchForm.method = ''
  searchForm.clientIp = ''
  searchForm.statusCode = ''
  startTime.value = null
  endTime.value = null
  pagination.current = 1
  fetchLogs()
}

async function cleanLogs() {
  Modal.confirm({
    title: '确认清理',
    content: '确定要清理7天前的日志吗？',
    onOk: async () => {
      try {
        await logApi.cleanLogs()
        message.success('清理日志成功')
        fetchLogs()
      } catch (error) {
        console.error('清理日志失败:', error)
        message.error('清理日志失败')
      }
    }
  })
}
</script>

<style scoped>
.log-management {
  padding: 24px;
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.log-header h1 {
  margin: 0;
}

.log-search {
  margin-bottom: 24px;
}

.log-table {
  margin-top: 24px;
}

.error-msg {
  color: #ff4d4f;
  cursor: pointer;
}
</style>
