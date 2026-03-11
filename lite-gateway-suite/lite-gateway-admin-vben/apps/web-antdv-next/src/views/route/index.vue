<template>
  <div class="route-management">
    <div class="route-header">
      <h1>{{ $t('page.route.title') }}</h1>
      <Button type="primary" @click="openAddModal">
        <template #icon>
          <Plus />
        </template>
        新增路由
      </Button>
      <Button @click="reloadConfig">
        <template #icon>
          <RefreshCw />
        </template>
        刷新配置
      </Button>
    </div>
    
    <Card class="route-search">
      <Form :model="searchForm" layout="inline">
        <FormItem label="路由名称">
          <Input v-model:value="searchForm.name" placeholder="请输入路由名称" />
        </FormItem>
        <FormItem label="路由状态">
          <Select 
            v-model:value="searchForm.status" 
            placeholder="请选择状态"
            :options="[
              { label: '全部', value: '' },
              { label: '启用', value: '1' },
              { label: '禁用', value: '0' }
            ]"
          />
        </FormItem>
        <FormItem label="服务URI">
          <Input v-model:value="searchForm.uri" placeholder="请输入服务URI" />
        </FormItem>
        <FormItem>
          <Button type="primary" @click="search">
            <template #icon>
              <Search />
            </template>
            搜索
          </Button>
          <Button @click="resetSearch">重置</Button>
        </FormItem>
      </Form>
    </Card>
    
    <Card class="route-table">
      <Table
        :columns="columns"
        :data-source="routes"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Switch
              :checked="record.status === '1'"
              @change="(checked) => updateStatus(record.id, checked ? '1' : '0')"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button size="small" type="primary" @click="openEditModal(record)">
                编辑
              </Button>
              <Button size="small" danger @click="deleteRoute(record.id)">
                删除
              </Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>
    
    <!-- 新增/编辑路由弹窗 -->
    <Modal
      v-model:open="modalOpen"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <Form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <FormItem label="路由名称" :required="true">
          <Input v-model:value="form.name" placeholder="请输入路由名称" />
        </FormItem>
        <FormItem label="服务URI" :required="true">
          <Input v-model:value="form.uri" placeholder="请输入服务URI" />
        </FormItem>
        <FormItem label="路由路径">
          <Input v-model:value="form.path" placeholder="请输入路由路径" />
        </FormItem>
        <FormItem label="是否去除前缀">
          <Select 
            v-model:value="form.stripPrefix"
            :options="[
              { label: '是', value: '1' },
              { label: '否', value: '0' }
            ]"
          />
        </FormItem>
        <FormItem label="状态">
          <Select 
            v-model:value="form.status"
            :options="[
              { label: '启用', value: '1' },
              { label: '禁用', value: '0' }
            ]"
          />
        </FormItem>
        <FormItem label="限流名称">
          <Input v-model:value="form.filterRateLimiterName" placeholder="请输入限流名称" />
        </FormItem>
        <FormItem label="令牌填充速率">
          <InputNumber v-model:value="form.replenishRate" placeholder="请输入令牌填充速率" />
        </FormItem>
        <FormItem label="令牌桶容量">
          <InputNumber v-model:value="form.burstCapacity" placeholder="请输入令牌桶容量" />
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { routeApi } from '@/api'
import type { RouteVO, RouteDTO, RouteQuery } from '@/types/api'
import { Plus, Search, RefreshCw } from '@vben/icons'
import { Button, Card, Form, FormItem, Input, InputNumber, Select, Table, Switch, Space, Modal, message } from 'antdv-next'

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
