<template>
  <div class="rate-limit-management">
    <div class="rate-limit-header">
      <h1>{{ $t('page.rateLimit.title') }}</h1>
      <a-button type="primary" @click="openAddModal">
        <template #icon>
          <Plus />
        </template>
        新增限流规则
      </a-button>
    </div>
    
    <a-card class="rate-limit-search">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="规则名称">
          <a-input v-model:value="searchForm.ruleName" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="限流类型">
          <a-select v-model:value="searchForm.limitType" placeholder="请选择限流类型">
            <a-option value="">全部</a-option>
            <a-option value="1">IP限流</a-option>
            <a-option value="2">用户限流</a-option>
            <a-option value="3">全局限流</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态">
            <a-option value="">全部</a-option>
            <a-option value="1">启用</a-option>
            <a-option value="0">禁用</a-option>
          </a-select>
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
    
    <a-card class="rate-limit-table">
      <a-table
        :columns="columns"
        :data-source="rateLimits"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'limitType'">
            <span v-if="record.limitType === 1">IP限流</span>
            <span v-else-if="record.limitType === 2">用户限流</span>
            <span v-else-if="record.limitType === 3">全局限流</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              @change="(checked) => updateStatus(record.id, checked ? 1 : 0)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button size="small" type="primary" @click="openEditModal(record)">
                编辑
              </a-button>
              <a-button size="small" danger @click="deleteRateLimit(record.id)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    
    <!-- 新增/编辑限流规则弹窗 -->
    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="规则名称" :required="true">
          <a-input v-model:value="form.ruleName" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="限流类型" :required="true">
          <a-select v-model:value="form.limitType">
            <a-option value="1">IP限流</a-option>
            <a-option value="2">用户限流</a-option>
            <a-option value="3">全局限流</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="路由ID">
          <a-input v-model:value="form.routeId" placeholder="请输入路由ID（可选）" />
        </a-form-item>
        <a-form-item label="令牌填充速率" :required="true">
          <a-input-number v-model:value="form.replenishRate" placeholder="请输入令牌填充速率" />
        </a-form-item>
        <a-form-item label="令牌桶容量" :required="true">
          <a-input-number v-model:value="form.burstCapacity" placeholder="请输入令牌桶容量" />
        </a-form-item>
        <a-form-item label="请求令牌数">
          <a-input-number v-model:value="form.requestedTokens" placeholder="请输入请求令牌数" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="form.status">
            <a-option value="1">启用</a-option>
            <a-option value="0">禁用</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { rateLimitApi } from '@/api'
import type { RateLimitRule, RateLimitQuery } from '@/types/api'
import { Plus, Search } from '@vben/icons'
import { message, Modal } from 'antdv-next'

const loading = ref(false)
const rateLimits = ref<RateLimitRule[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  onChange: (page: number) => {
    pagination.current = page
    fetchRateLimits()
  },
  onShowSizeChange: (page: number, pageSize: number) => {
    pagination.current = page
    pagination.pageSize = pageSize
    fetchRateLimits()
  }
})

const searchForm = reactive<RateLimitQuery>({
  ruleName: '',
  limitType: '',
  status: ''
})

const modalOpen = ref(false)
const modalTitle = ref('新增限流规则')
const form = reactive<RateLimitRule>({
  ruleName: '',
  limitType: 1,
  replenishRate: 10,
  burstCapacity: 20,
  status: 1
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName' },
  { title: '限流类型', dataIndex: 'limitType', key: 'limitType' },
  { title: '路由ID', dataIndex: 'routeId', key: 'routeId' },
  { title: '令牌填充速率', dataIndex: 'replenishRate', key: 'replenishRate' },
  { title: '令牌桶容量', dataIndex: 'burstCapacity', key: 'burstCapacity' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { 
    title: '操作', 
    key: 'action',
    fixed: 'right',
    width: 150
  }
]

onMounted(() => {
  fetchRateLimits()
})

async function fetchRateLimits() {
  loading.value = true
  try {
    const response = await rateLimitApi.getRateLimitPage({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    rateLimits.value = response.list
    pagination.total = response.total
  } catch (error) {
    console.error('获取限流规则列表失败:', error)
    message.error('获取限流规则列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  fetchRateLimits()
}

function resetSearch() {
  searchForm.ruleName = ''
  searchForm.limitType = ''
  searchForm.status = ''
  searchForm.routeId = ''
  pagination.current = 1
  fetchRateLimits()
}

function openAddModal() {
  modalTitle.value = '新增限流规则'
  Object.assign(form, {
    ruleName: '',
    limitType: 1,
    routeId: undefined,
    replenishRate: 10,
    burstCapacity: 20,
    requestedTokens: undefined,
    status: 1
  })
  modalOpen.value = true
}

function openEditModal(rule: RateLimitRule) {
  modalTitle.value = '编辑限流规则'
  Object.assign(form, rule)
  modalOpen.value = true
}

async function handleOk() {
  loading.value = true
  try {
    if (form.id) {
      await rateLimitApi.updateRateLimit(form.id, form)
      message.success('更新限流规则成功')
    } else {
      await rateLimitApi.addRateLimit(form)
      message.success('新增限流规则成功')
    }
    modalOpen.value = false
    fetchRateLimits()
  } catch (error) {
    console.error('保存限流规则失败:', error)
    message.error('保存限流规则失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  modalOpen.value = false
}

async function updateStatus(id: number, status: number) {
  try {
    await rateLimitApi.updateRateLimitStatus(id, status)
    message.success('更新状态成功')
    fetchRateLimits()
  } catch (error) {
    console.error('更新状态失败:', error)
    message.error('更新状态失败')
  }
}

async function deleteRateLimit(id: number) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个限流规则吗？',
    onOk: async () => {
      try {
        await rateLimitApi.deleteRateLimit(id)
        message.success('删除限流规则成功')
        fetchRateLimits()
      } catch (error) {
        console.error('删除限流规则失败:', error)
        message.error('删除限流规则失败')
      }
    }
  })
}
</script>

<style scoped>
.rate-limit-management {
  padding: 24px;
}

.rate-limit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.rate-limit-header h1 {
  margin: 0;
}

.rate-limit-search {
  margin-bottom: 24px;
}

.rate-limit-table {
  margin-top: 24px;
}
</style>
