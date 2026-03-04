<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-cards">
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon blue">
              <node-index-outlined />
            </div>
            <div class="stat-info">
              <p class="stat-title">路由总数</p>
              <p class="stat-value">{{ statistics.routeCount || 0 }}</p>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon green">
              <check-circle-outlined />
            </div>
            <div class="stat-info">
              <p class="stat-title">启用路由</p>
              <p class="stat-value">{{ statistics.enabledRouteCount || 0 }}</p>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon orange">
              <safety-outlined />
            </div>
            <div class="stat-info">
              <p class="stat-title">限流规则</p>
              <p class="stat-value">{{ statistics.rateLimitCount || 0 }}</p>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon purple">
              <file-text-outlined />
            </div>
            <div class="stat-info">
              <p class="stat-title">今日请求</p>
              <p class="stat-value">{{ formatNumber(statistics.todayRequests || 0) }}</p>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="16" class="chart-row">
      <a-col :xs="24" :lg="16">
        <a-card title="请求趋势" class="chart-card">
          <div ref="trendChartRef" class="chart-container"></div>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="状态码分布" class="chart-card">
          <div ref="statusChartRef" class="chart-container"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 路由排行 -->
    <a-row :gutter="16" class="table-row">
      <a-col :xs="24">
        <a-card title="热门路由 TOP10" class="table-card">
          <a-table
            :columns="routeColumns"
            :data-source="routeRank"
            :pagination="false"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'errorRate'">
                <a-progress
                  :percent="record.errorRate * 100"
                  :stroke-color="record.errorRate > 0.05 ? '#ff4d4f' : '#52c41a'"
                  size="small"
                />
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import {
  NodeIndexOutlined,
  CheckCircleOutlined,
  SafetyOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { useRouteStore } from '@/store/modules/route'
import { useRateLimitStore } from '@/store/modules/rateLimit'
import { useLogStore } from '@/store/modules/log'

const routeStore = useRouteStore()
const rateLimitStore = useRateLimitStore()
const logStore = useLogStore()

// 统计数据
const statistics = ref({
  routeCount: 0,
  enabledRouteCount: 0,
  rateLimitCount: 0,
  todayRequests: 0
})

const routeRank = ref([])

// 图表引用
const trendChartRef = ref<HTMLElement>()
const statusChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let statusChart: echarts.ECharts | null = null

// 表格列
const routeColumns = [
  { title: '排名', dataIndex: 'index', key: 'index', width: 60, customRender: ({ index }: { index: number }) => index + 1 },
  { title: '路由ID', dataIndex: 'routeId', key: 'routeId' },
  { title: '请求次数', dataIndex: 'count', key: 'count', width: 120 },
  { title: '平均响应时间(ms)', dataIndex: 'avgResponseTime', key: 'avgResponseTime', width: 150 },
  { title: '错误率', dataIndex: 'errorRate', key: 'errorRate', width: 150 }
]

// 格式化数字
const formatNumber = (num: number) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 初始化趋势图
const initTrendChart = () => {
  if (!trendChartRef.value) return
  
  trendChart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '请求量',
        type: 'line',
        smooth: true,
        data: [120, 132, 101, 134, 90, 230, 210],
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
              { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
            ]
          }
        },
        itemStyle: {
          color: '#1890ff'
        }
      }
    ]
  }
  trendChart.setOption(option)
}

// 初始化状态码分布图
const initStatusChart = () => {
  if (!statusChartRef.value) return
  
  statusChart = echarts.init(statusChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '状态码',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: [
          { value: 1048, name: '200 OK', itemStyle: { color: '#52c41a' } },
          { value: 735, name: '404 Not Found', itemStyle: { color: '#faad14' } },
          { value: 580, name: '500 Error', itemStyle: { color: '#ff4d4f' } },
          { value: 484, name: '403 Forbidden', itemStyle: { color: '#1890ff' } }
        ]
      }
    ]
  }
  statusChart.setOption(option)
}

// 加载数据
const loadData = async () => {
  // 加载路由数据
  await routeStore.fetchAllRoutes()
  statistics.value.routeCount = routeStore.routeList.length
  statistics.value.enabledRouteCount = routeStore.routeList.filter(r => r.status === 1).length

  // 加载限流规则数据
  await rateLimitStore.fetchAllRateLimits()
  statistics.value.rateLimitCount = rateLimitStore.ruleList.length

  // 加载日志统计数据
  const endTime = new Date().toISOString()
  const startTime = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()
  
  try {
    const logStats = await logStore.fetchStatistics({ startTime, endTime })
    statistics.value.todayRequests = logStats.totalRequests

    const rankData = await logStore.fetchRouteRank({ startTime, endTime, top: 10 })
    routeRank.value = rankData
  } catch (error) {
    console.error('加载日志统计失败', error)
  }
}

onMounted(() => {
  loadData()
  initTrendChart()
  initStatusChart()
  
  window.addEventListener('resize', () => {
    trendChart?.resize()
    statusChart?.resize()
  })
})

onUnmounted(() => {
  trendChart?.dispose()
  statusChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  .stat-cards {
    margin-bottom: 16px;
  }

  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      color: #fff;

      &.blue {
        background: linear-gradient(135deg, #1890ff, #36cfc9);
      }

      &.green {
        background: linear-gradient(135deg, #52c41a, #95de64);
      }

      &.orange {
        background: linear-gradient(135deg, #fa8c16, #ffc53d);
      }

      &.purple {
        background: linear-gradient(135deg, #722ed1, #b37feb);
      }
    }

    .stat-info {
      flex: 1;

      .stat-title {
        color: #666;
        font-size: 14px;
        margin-bottom: 4px;
      }

      .stat-value {
        color: #333;
        font-size: 28px;
        font-weight: bold;
        margin: 0;
      }
    }
  }

  .chart-row {
    margin-bottom: 16px;
  }

  .chart-card {
    .chart-container {
      height: 300px;
    }
  }

  .table-card {
    .ant-table-wrapper {
      margin-top: 16px;
    }
  }
}
</style>
