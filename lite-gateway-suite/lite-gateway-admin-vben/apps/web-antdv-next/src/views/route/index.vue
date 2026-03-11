<template>
  <div class="route-management">
    <div class="route-header">
      <h1>{{ $t('page.route.title') }}</h1>
      <a-button type="primary" @click="openAddModal">
        <template #icon>
          <Plus />
        </template>
        新增路由
      </a-button>
      <a-button @click="reloadConfig">
        <template #icon>
          <RefreshCw />
        </template>
        刷新配置
      </a-button>
    </div>
    
    <a-card class="route-search">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="路由名称">
          <a-input v-model:value="searchForm.name" placeholder="请输入路由名称" />
        </a-form-item>
        <a-form-item label="路由状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态">
            <a-option value="">全部</a-option>
            <a-option value="1">启用</a-option>
            <a-option value="0">禁用</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="服务URI">
          <a-input v-model:value="searchForm.uri" placeholder="请输入服务URI" />
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
    
    <a-card class="route-table">
      <a-table
        :columns="columns"
        :data-source="routes"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-switch
              :checked="record.status === '1'"
              @change="(checked) => updateStatus(record.id, checked ? '1' : '0')"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button size="small" type="primary" @click="openEditModal(record)">
                编辑
              </a-button>
              <a-button size="small" danger @click="deleteRoute(record.id)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    
    <!-- 新增/编辑路由弹窗 -->
    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="路由名称" :required="true">
          <a-input v-model:value="form.name" placeholder="请输入路由名称" />
        </a-form-item>
        <a-form-item label="服务URI" :required="true">
          <a-input v-model:value="form.uri" placeholder="请输入服务URI" />
        </a-form-item>
        <a-form-item label="路由路径">
          <a-input v-model:value="form.path" placeholder="请输入路由路径" />
        </a-form-item>
        <a-form-item label="是否去除前缀">
          <a-select v-model:value="form.stripPrefix">
            <a-option value="1">是</a-option>
            <a-option value="0">否</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="form.status">
            <a-option value="1">启用</a-option>
            <a-option value="0">禁用</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="限流名称">
          <a-input v-model:value="form.filterRateLimiterName" placeholder="请输入限流名称" />
        </a-form-item>
        <a-form-item label="令牌填充速率">
          <a-input-number v-model:value="form.replenishRate" placeholder="请输入令牌填充速率" />
        </a-form-item>
        <a-form-item label="令牌桶容量">
          <a-input-number v-model:value="form.burstCapacity" placeholder="请输入令牌桶容量" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { routeApi } from '@/api'
import type { RouteVO, RouteDTO, RouteQuery } from '@/types/api'
import { Plus, Search, RefreshCw } from '@vben/icons'
import { message, Modal } from 'antdv-next'

const loading = ref(false)
const routes = ref<RouteVO[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  onChange: (page: number) => {
    pagination.current = page
    fetchRoutes()
  },
  onShowSizeChange: (page: number, pageSize: number) => {
    pagination.current = page
    pagination.pageSize = pageSize
    fetchRoutes()
  }
})

const searchForm = reactive<RouteQuery>({
  name: '',
  status: '',
  uri: ''
})

const modalOpen = ref(false)
const modalTitle = ref('新增路由')
const form = reactive<RouteDTO>({
  name: '',
  uri: '',
  path: '',
  stripPrefix: 1,
  status: '1'
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '路由名称', dataIndex: 'name', key: 'name' },
  { title: '服务URI', dataIndex: 'uri', key: 'uri' },
  { title: '路由路径', dataIndex: 'path', key: 'path' },
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
  fetchRoutes()
})

async function fetchRoutes() {
  loading.value = true
  try {
    const response = await routeApi.getRoutePage({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    routes.value = response.list
    pagination.total = response.total
  } catch (error) {
    console.error('获取路由列表失败:', error)
    message.error('获取路由列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  fetchRoutes()
}

function resetSearch() {
  searchForm.name = ''
  searchForm.status = ''
  searchForm.uri = ''
  searchForm.path = ''
  pagination.current = 1
  fetchRoutes()
}

function openAddModal() {
  modalTitle.value = '新增路由'
  Object.assign(form, {
    name: '',
    uri: '',
    path: '',
    stripPrefix: 1,
    status: '1',
    filterRateLimiterName: '',
    replenishRate: undefined,
    burstCapacity: undefined
  })
  modalOpen.value = true
}

function openEditModal(route: RouteVO) {
  modalTitle.value = '编辑路由'
  Object.assign(form, route)
  modalOpen.value = true
}

async function handleOk() {
  loading.value = true
  try {
    if (form.id) {
      await routeApi.updateRoute(form.id, form)
      message.success('更新路由成功')
    } else {
      await routeApi.addRoute(form)
      message.success('新增路由成功')
    }
    modalOpen.value = false
    fetchRoutes()
  } catch (error) {
    console.error('保存路由失败:', error)
    message.error('保存路由失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  modalOpen.value = false
}

async function updateStatus(id: number | string, status: string) {
  try {
    await routeApi.updateRouteStatus(id, status)
    message.success('更新状态成功')
    fetchRoutes()
  } catch (error) {
    console.error('更新状态失败:', error)
    message.error('更新状态失败')
  }
}

async function deleteRoute(id: number | string) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个路由吗？',
    onOk: async () => {
      try {
        await routeApi.deleteRoute(id)
        message.success('删除路由成功')
        fetchRoutes()
      } catch (error) {
        console.error('删除路由失败:', error)
        message.error('删除路由失败')
      }
    }
  })
}

async function reloadConfig() {
  try {
    await routeApi.reloadConfig()
    message.success('刷新配置成功')
  } catch (error) {
    console.error('刷新配置失败:', error)
    message.error('刷新配置失败')
  }
}
</script>

<style scoped>
.route-management {
  padding: 24px;
}

.route-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.route-header h1 {
  margin: 0;
}

.route-search {
  margin-bottom: 24px;
}

.route-table {
  margin-top: 24px;
}
</style>
