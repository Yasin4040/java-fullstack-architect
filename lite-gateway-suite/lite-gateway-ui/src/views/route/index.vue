<template>
  <div class="route-page">
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
        <a-form-item label="路由名称">
          <a-input
            v-model:value="queryForm.routeName"
            placeholder="请输入路由名称"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="queryForm.status"
            placeholder="请选择状态"
            allow-clear
            style="width: 120px"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
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

    <!-- 操作栏 -->
    <a-card class="table-card" :bordered="false">
      <template #title>
        <div class="table-header">
          <span>路由列表</span>
          <div class="table-actions">
            <a-button type="primary" @click="handleAdd">
              <plus-outlined />
              新增路由
            </a-button>
            <a-button style="margin-left: 8px" @click="handleReload">
              <reload-outlined />
              刷新配置
            </a-button>
          </div>
        </div>
      </template>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="routeStore.routeList"
        :loading="routeStore.loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              :loading="statusLoading === record.id"
              @change="(checked) => handleStatusChange(record, checked)"
            />
          </template>
          <template v-if="column.key === 'uri'">
            <a-typography-text copyable>{{ record.uri }}</a-typography-text>
          </template>
          <template v-if="column.key === 'orderNum'">
            <a-tag color="blue">{{ record.orderNum }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <edit-outlined />
                编辑
              </a-button>
              <a-button type="link" size="small" @click="handleView(record)">
                <eye-outlined />
                详情
              </a-button>
              <a-popconfirm
                title="确定要删除这个路由吗？"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" danger size="small">
                  <delete-outlined />
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      width="800px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="路由ID" name="routeId">
              <a-input
                v-model:value="formData.routeId"
                placeholder="请输入路由ID，如：user-service"
                :disabled="isEdit"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="路由名称" name="routeName">
              <a-input
                v-model:value="formData.routeName"
                placeholder="请输入路由名称"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="目标URI" name="uri">
          <a-input
            v-model:value="formData.uri"
            placeholder="请输入目标URI，如：lb://user-service 或 http://localhost:8080"
          />
        </a-form-item>
        <a-form-item label="路径断言" name="path">
          <a-input
            v-model:value="formData.path"
            placeholder="请输入路径，如：/api/user/**"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="优先级" name="orderNum">
              <a-input-number
                v-model:value="formData.orderNum"
                :min="0"
                :max="9999"
                style="width: 100%"
                placeholder="数字越小优先级越高"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="formData.status">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">禁用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea
            v-model:value="formData.description"
            :rows="3"
            placeholder="请输入路由描述"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="路由详情"
      :footer="null"
      width="700px"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="路由ID">{{ currentRoute?.routeId }}</a-descriptions-item>
        <a-descriptions-item label="路由名称">{{ currentRoute?.routeName }}</a-descriptions-item>
        <a-descriptions-item label="目标URI" :span="2">{{ currentRoute?.uri }}</a-descriptions-item>
        <a-descriptions-item label="路径断言" :span="2">{{ currentRoute?.path || '-' }}</a-descriptions-item>
        <a-descriptions-item label="优先级">{{ currentRoute?.orderNum }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="currentRoute?.status === 1 ? 'success' : 'error'">
            {{ currentRoute?.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentRoute?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ currentRoute?.updateTime }}</a-descriptions-item>
        <a-descriptions-item label="描述" :span="2">{{ currentRoute?.description || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import { useRouteStore } from '@/store/modules/route'
import type { Route, RouteQuery } from '@/types/api'

const routeStore = useRouteStore()

// 查询表单
const queryForm = reactive<RouteQuery>({
  routeId: '',
  routeName: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

// 表格列
const columns = [
  { title: '路由ID', dataIndex: 'routeId', key: 'routeId', width: 150 },
  { title: '路由名称', dataIndex: 'routeName', key: 'routeName', width: 150 },
  { title: '目标URI', dataIndex: 'uri', key: 'uri', ellipsis: true },
  { title: '路径', dataIndex: 'path', key: 'path', width: 180 },
  { title: '优先级', dataIndex: 'orderNum', key: 'orderNum', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
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

// 弹窗相关
const modalVisible = ref(false)
const modalLoading = ref(false)
const modalTitle = ref('新增路由')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const currentRoute = ref<Route | null>(null)
const detailVisible = ref(false)
const statusLoading = ref<number | null>(null)

// 表单数据
const formData = reactive<Route>({
  routeId: '',
  routeName: '',
  uri: '',
  path: '',
  orderNum: 0,
  status: 1,
  description: ''
})

// 表单校验规则
const formRules = {
  routeId: [{ required: true, message: '请输入路由ID', trigger: 'blur' }],
  routeName: [{ required: true, message: '请输入路由名称', trigger: 'blur' }],
  uri: [{ required: true, message: '请输入目标URI', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入优先级', trigger: 'blur' }]
}

// 查询
const handleQuery = () => {
  queryForm.pageNum = 1
  loadData()
}

// 重置
const handleReset = () => {
  queryForm.routeId = ''
  queryForm.routeName = ''
  queryForm.status = ''
  queryForm.pageNum = 1
  loadData()
}

// 加载数据
const loadData = async () => {
  await routeStore.fetchRoutePage(queryForm)
  pagination.total = routeStore.total
  pagination.current = queryForm.pageNum || 1
  pagination.pageSize = queryForm.pageSize || 10
}

// 表格变化
const handleTableChange = (pag: any) => {
  queryForm.pageNum = pag.current
  queryForm.pageSize = pag.pageSize
  loadData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  modalTitle.value = '新增路由'
  resetForm()
  modalVisible.value = true
}

// 编辑
const handleEdit = (record: Route) => {
  isEdit.value = true
  modalTitle.value = '编辑路由'
  Object.assign(formData, record)
  modalVisible.value = true
}

// 查看详情
const handleView = (record: Route) => {
  currentRoute.value = record
  detailVisible.value = true
}

// 删除
const handleDelete = async (record: Route) => {
  try {
    await routeStore.deleteRoute(record.id!)
    message.success('删除成功')
    loadData()
  } catch (error) {
    message.error('删除失败')
  }
}

// 状态变更
const handleStatusChange = async (record: Route, checked: boolean) => {
  statusLoading.value = record.id!
  try {
    const newStatus = checked ? 1 : 0
    await routeStore.updateRouteStatus(record.id!, newStatus)
    message.success('状态更新成功')
    loadData()
  } catch (error) {
    message.error('状态更新失败')
  } finally {
    statusLoading.value = null
  }
}

// 刷新配置
const handleReload = async () => {
  try {
    await routeStore.reloadConfig()
    message.success('配置刷新成功')
  } catch (error) {
    message.error('配置刷新失败')
  }
}

// 弹窗确认
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (isEdit.value) {
      await routeStore.updateRoute(formData.id!, { ...formData })
      message.success('更新成功')
    } else {
      await routeStore.addRoute({ ...formData })
      message.success('添加成功')
    }

    modalVisible.value = false
    loadData()
  } catch (error) {
    console.error(error)
  } finally {
    modalLoading.value = false
  }
}

// 弹窗取消
const handleModalCancel = () => {
  modalVisible.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  formData.id = undefined
  formData.routeId = ''
  formData.routeName = ''
  formData.uri = ''
  formData.path = ''
  formData.orderNum = 0
  formData.status = 1
  formData.description = ''
  formRef.value?.resetFields()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.route-page {
  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .table-actions {
      display: flex;
      gap: 8px;
    }
  }
}
</style>
