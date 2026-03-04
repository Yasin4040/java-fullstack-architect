<template>
  <div class="log-page">
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card>
          <a-statistic
            title="总请求数"
            :value="logStore.statistics?.totalRequests || 0"
            :value-style="{ color: '#1890ff' }"
          >
            <template #prefix>
              <file-text-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card>
          <a-statistic
            title="成功请求"
            :value="logStore.statistics?.successCount || 0"
            :value-style="{ color: '#52c41a' }"
          >
            <template #prefix>
              <check-circle-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card>
          <a-statistic
            title="错误请求"
            :value="logStore.statistics?.errorCount || 0"
            :value-style="{ color: '#ff4d4f' }"
          >
            <template #prefix>
              <close-circle-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card>
          <a-statistic
            title="平均响应时间(ms)"
            :value="logStore.statistics?.avgResponseTime || 0"
            :precision="2"
            :value-style="{ color: '#faad14' }"
          >
            <template #prefix>
              <clock-circle-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 搜索栏 -->
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="queryForm">
        <a-form-item label="路由ID">
          <a-input
            v-model:value="queryForm.routeId"
            placeholder="请输入路由ID"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="请求路径">
          <a-input
            v-model:value="queryForm.path"
            placeholder="请输入请求路径"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="HTTP方法">
          <a-select
            v-model:value="queryForm.method"
            placeholder="请选择方法"
            allow-clear
            style="width: 120px"
          >
            <a-select-option value="GET">GET</a-select-option>
            <a-select-option value="POST">POST</a-select-option>
            <a-select-option value="PUT">PUT</a-select-option>
            <a-select-option value="DELETE">DELETE</a-select-option>
            <a-select-option value="PATCH">PATCH</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态码">
          <a-select
            v-model:value="queryForm.statusCode"
            placeholder="请选择状态码"
            allow-clear
            style="width: 120px"
          >
            <a-select-option :value="200">200 OK</a-select-option>
            <a-select-option :value="404">404 Not Found</a-select-option>
            <a-select-option :value="500">500 Error</a-select-option>
            <a-select-option :value="503">503 Service Unavailable</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="时间范围">
          <a-range-picker
            v-model:value="dateRange"
            :show-time="{ format: 'HH:mm' }"
            format="YYYY-MM-DD HH:mm"
            @change="handleDateChange"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleQuery">
            <search-outlined />
            查询
          </a-button>
          <a-button style="margin-left: 8px" @click="handleReset">
            <reload-outlined />
            重置
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 数据表格 -->
    <a-card class="table-card" :bordered="false">
      <template #title>
        <span>访问日志</span>
      </template>

      <a-table
        :columns="columns"
        :data-source="logStore.logList"
        :loading="logStore.loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'method'">
            <a-tag :color="getMethodColor(record.method)">{{ record.method }}</a-tag>
          </template>
          <template v-if="column.key === 'statusCode'">
            <a-tag :color="getStatusColor(record.statusCode)">{{ record.statusCode }}</a-tag>
          </template>
          <template v-if="column.key === 'duration'">
            <span :style="{ color: getDurationColor(record.duration) }">{{ record.duration }}ms</span>
          </template>
          <template v-if="column.key === 'path'">
            <a-typography-text ellipsis style="max-width: 200px">{{ record.path }}</a-typography-text>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleView(record)">
              <eye-outlined />
              详情
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="日志详情"
      :footer="null"
      width="700px"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="日志ID">{{ currentLog?.id }}</a-descriptions-item>
        <a-descriptions-item label="路由ID">{{ currentLog?.routeId }}</a-descriptions-item>
        <a-descriptions-item label="请求路径" :span="2">{{ currentLog?.path }}</a-descriptions-item>
        <a-descriptions-item label="HTTP方法">
          <a-tag :color="getMethodColor(currentLog?.method)">{{ currentLog?.method }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="状态码">
          <a-tag :color="getStatusColor(currentLog?.statusCode)">{{ currentLog?.statusCode }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="客户端IP">{{ currentLog?.clientIp }}</a-descriptions-item>
        <a-descriptions-item label="用户ID">{{ currentLog?.userId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="请求时间">{{ formatTime(currentLog?.requestTime) }}</a-descriptions-item>
        <a-descriptions-item label="响应时间">{{ formatTime(currentLog?.responseTime) }}</a-descriptions-item>
        <a-descriptions-item label="耗时">
          <span :style="{ color: getDurationColor(currentLog?.duration || 0) }">{{ currentLog?.duration }}ms</span>
        </a-descriptions-item>
        <a-descriptions-item label="请求大小">{{ formatBytes(currentLog?.requestSize || 0) }}</a-descriptions-item>
        <a-descriptions-item label="响应大小">{{ formatBytes(currentLog?.responseSize || 0) }}</a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2" v-if="currentLog?.errorMsg">
          <span style="color: #ff4d4f">{{ currentLog?.errorMsg }}</span>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  FileTextOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import { useLogStore } from '@/store/modules/log'
import type { GatewayLog, LogQuery } from '@/types/api'

const logStore = useLogStore()

// 日期范围
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

// 查询表单
const queryForm = reactive<LogQuery>({
  routeId: '',
  path: '',
  method: '',
  statusCode: '',
  startTime: '',
  endTime: '',
  pageNum: 1,
  pageSize: 10
})

// 表格列
const columns = [
  { title: '日志ID', dataIndex: 'id', key: 'id', width: 200, ellipsis: true },
  { title: '路由ID', dataIndex: 'routeId', key: 'routeId', width: 150 },
  { title: '请求路径', dataIndex: 'path', key: 'path', ellipsis: true },
  { title: '方法', dataIndex: 'method', key: 'method', width: 80 },
  { title: '状态码', dataIndex: 'statusCode', key: 'statusCode', width: 90 },
  { title: '客户端IP', dataIndex: 'clientIp', key: 'clientIp', width: 130 },
  { title: '耗时', dataIndex: 'duration', key: 'duration', width: 100 },
  { title: '请求时间', dataIndex: 'requestTime', key: 'requestTime', width: 180 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 详情弹窗
const detailVisible = ref(false)
const currentLog = ref<GatewayLog | null>(null)

// 获取方法颜色
const getMethodColor = (method?: string) => {
  const colors: Record<string, string> = {
    'GET': 'blue',
    'POST': 'green',
    'PUT': 'orange',
    'DELETE': 'red',
    'PATCH': 'purple'
  }
  return colors[method || ''] || 'default'
}

// 获取状态码颜色
const getStatusColor = (status?: number) => {
  if (!status) return 'default'
  if (status >= 200 && status < 300) return 'success'
  if (status >= 300 && status < 400) return 'warning'
  if (status >= 400 && status < 500) return 'error'
  return 'default'
}

// 获取耗时颜色
const getDurationColor = (duration: number) => {
  if (duration < 100) return '#52c41a'
  if (duration < 500) return '#faad14'
  return '#ff4d4f'
}

// 格式化时间
const formatTime = (timestamp?: number) => {
  if (!timestamp) return '-'
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

// 格式化字节
const formatBytes = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 日期变化
const handleDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates) {
    queryForm.startTime = dates[0].toISOString()
    queryForm.endTime = dates[1].toISOString()
  } else {
    queryForm.startTime = ''
    queryForm.endTime = ''
  }
}

// 查询
const handleQuery = () => {
  queryForm.pageNum = 1
  loadData()
  loadStatistics()
}

// 重置
const handleReset = () => {
  queryForm.routeId = ''
  queryForm.path = ''
  queryForm.method = ''
  queryForm.statusCode = ''
  queryForm.startTime = ''
  queryForm.endTime = ''
  dateRange.value = null
  queryForm.pageNum = 1
  loadData()
  loadStatistics()
}

// 加载数据
const loadData = async () => {
  await logStore.fetchLogPage(queryForm)
  pagination.total = logStore.total
  pagination.current = queryForm.pageNum || 1
  pagination.pageSize = queryForm.pageSize || 10
}

// 加载统计数据
const loadStatistics = async () => {
  const params: { startTime?: string; endTime?: string } = {}
  if (queryForm.startTime) params.startTime = queryForm.startTime
  if (queryForm.endTime) params.endTime = queryForm.endTime
  
  try {
    await logStore.fetchStatistics(params)
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

// 表格变化
const handleTableChange = (pag: any) => {
  queryForm.pageNum = pag.current
  queryForm.pageSize = pag.pageSize
  loadData()
}

// 查看详情
const handleView = (record: GatewayLog) => {
  currentLog.value = record
  detailVisible.value = true
}

onMounted(() => {
  // 默认查询最近24小时
  const end = dayjs()
  const start = dayjs().subtract(24, 'hour')
  dateRange.value = [start, end]
  queryForm.startTime = start.toISOString()
  queryForm.endTime = end.toISOString()
  
  loadData()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.log-page {
  .stat-row {
    margin-bottom: 16px;
  }

  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    .ant-tag {
      min-width: 50px;
      text-align: center;
    }
  }
}
</style>
