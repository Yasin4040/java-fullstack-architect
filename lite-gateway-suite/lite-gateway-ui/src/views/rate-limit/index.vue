<template>
  <div class="rate-limit-page">
    <!-- 搜索栏 -->
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="queryForm">
        <a-form-item label="规则名称">
          <a-input
            v-model:value="queryForm.ruleName"
            placeholder="请输入规则名称"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="关联路由">
          <a-input
            v-model:value="queryForm.routeId"
            placeholder="请输入路由ID"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="限流类型">
          <a-select
            v-model:value="queryForm.limitType"
            placeholder="请选择限流类型"
            allow-clear
            style="width: 150px"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option :value="1">IP限流</a-select-option>
            <a-select-option :value="2">用户限流</a-select-option>
            <a-select-option :value="3">全局限流</a-select-option>
          </a-select>
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
          <span>限流规则列表</span>
          <a-button type="primary" @click="handleAdd">
            <plus-outlined />
            新增规则
          </a-button>
        </div>
      </template>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="rateLimitStore.ruleList"
        :loading="rateLimitStore.loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'limitType'">
            <a-tag :color="getLimitTypeColor(record.limitType)">
              {{ getLimitTypeText(record.limitType) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'rateInfo'">
            <div class="rate-info">
              <div class="rate-item">
                <span class="rate-label">每秒令牌:</span>
                <span class="rate-value">{{ record.replenishRate }}</span>
              </div>
              <div class="rate-item">
                <span class="rate-label">桶容量:</span>
                <span class="rate-value">{{ record.burstCapacity }}</span>
              </div>
            </div>
          </template>
          <template v-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              :loading="statusLoading === record.id"
              @change="(checked) => handleStatusChange(record, checked)"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <edit-outlined />
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除这个限流规则吗？"
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
      width="700px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="规则名称" name="ruleName">
              <a-input
                v-model:value="formData.ruleName"
                placeholder="请输入规则名称"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="关联路由" name="routeId">
              <a-input
                v-model:value="formData.routeId"
                placeholder="请输入关联的路由ID"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="限流类型" name="limitType">
              <a-select
                v-model:value="formData.limitType"
                placeholder="请选择限流类型"
              >
                <a-select-option :value="1">IP限流</a-select-option>
                <a-select-option :value="2">用户限流</a-select-option>
                <a-select-option :value="3">全局限流</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="限流Key前缀" name="keyPrefix">
              <a-input
                v-model:value="formData.keyPrefix"
                placeholder="可选，自定义限流Key前缀"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="每秒补充令牌数" name="replenishRate">
              <a-input-number
                v-model:value="formData.replenishRate"
                :min="1"
                :max="10000"
                style="width: 100%"
                placeholder="每秒补充的令牌数量"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="令牌桶容量" name="burstCapacity">
              <a-input-number
                v-model:value="formData.burstCapacity"
                :min="1"
                :max="100000"
                style="width: 100%"
                placeholder="令牌桶的最大容量"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="每次请求消耗令牌" name="requestedTokens">
          <a-input-number
            v-model:value="formData.requestedTokens"
            :min="1"
            :max="100"
            style="width: 100%"
            placeholder="每次请求消耗的令牌数量"
          />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { useRateLimitStore } from '@/store/modules/rateLimit'
import type { RateLimitRule, RateLimitQuery } from '@/types/api'

const rateLimitStore = useRateLimitStore()

// 查询表单
const queryForm = reactive<RateLimitQuery>({
  ruleName: '',
  routeId: '',
  limitType: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

// 表格列
const columns = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 150 },
  { title: '关联路由', dataIndex: 'routeId', key: 'routeId', width: 150 },
  { title: '限流类型', dataIndex: 'limitType', key: 'limitType', width: 120 },
  { title: '限流配置', key: 'rateInfo', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
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
const modalTitle = ref('新增限流规则')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const statusLoading = ref<number | null>(null)

// 表单数据
const formData = reactive<RateLimitRule>({
  ruleName: '',
  routeId: '',
  limitType: 1,
  keyPrefix: '',
  replenishRate: 10,
  burstCapacity: 20,
  requestedTokens: 1,
  status: 1
})

// 表单校验规则
const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  limitType: [{ required: true, message: '请选择限流类型', trigger: 'change' }],
  replenishRate: [{ required: true, message: '请输入每秒补充令牌数', trigger: 'blur' }],
  burstCapacity: [{ required: true, message: '请输入令牌桶容量', trigger: 'blur' }]
}

// 获取限流类型颜色
const getLimitTypeColor = (type: number) => {
  const colors: Record<number, string> = {
    1: 'blue',
    2: 'green',
    3: 'orange'
  }
  return colors[type] || 'default'
}

// 获取限流类型文本
const getLimitTypeText = (type: number) => {
  const texts: Record<number, string> = {
    1: 'IP限流',
    2: '用户限流',
    3: '全局限流'
  }
  return texts[type] || '未知'
}

// 查询
const handleQuery = () => {
  queryForm.pageNum = 1
  loadData()
}

// 重置
const handleReset = () => {
  queryForm.ruleName = ''
  queryForm.routeId = ''
  queryForm.limitType = ''
  queryForm.status = ''
  queryForm.pageNum = 1
  loadData()
}

// 加载数据
const loadData = async () => {
  await rateLimitStore.fetchRateLimitPage(queryForm)
  pagination.total = rateLimitStore.total
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
  modalTitle.value = '新增限流规则'
  resetForm()
  modalVisible.value = true
}

// 编辑
const handleEdit = (record: RateLimitRule) => {
  isEdit.value = true
  modalTitle.value = '编辑限流规则'
  Object.assign(formData, record)
  modalVisible.value = true
}

// 删除
const handleDelete = async (record: RateLimitRule) => {
  try {
    await rateLimitStore.deleteRateLimit(record.id!)
    message.success('删除成功')
    loadData()
  } catch (error) {
    message.error('删除失败')
  }
}

// 状态变更
const handleStatusChange = async (record: RateLimitRule, checked: boolean) => {
  statusLoading.value = record.id!
  try {
    const newStatus = checked ? 1 : 0
    await rateLimitStore.updateRateLimitStatus(record.id!, newStatus)
    message.success('状态更新成功')
    loadData()
  } catch (error) {
    message.error('状态更新失败')
  } finally {
    statusLoading.value = null
  }
}

// 弹窗确认
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (isEdit.value) {
      await rateLimitStore.updateRateLimit(formData.id!, { ...formData })
      message.success('更新成功')
    } else {
      await rateLimitStore.addRateLimit({ ...formData })
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
  formData.ruleName = ''
  formData.routeId = ''
  formData.limitType = 1
  formData.keyPrefix = ''
  formData.replenishRate = 10
  formData.burstCapacity = 20
  formData.requestedTokens = 1
  formData.status = 1
  formRef.value?.resetFields()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.rate-limit-page {
  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  .rate-info {
    .rate-item {
      display: flex;
      justify-content: space-between;
      margin-bottom: 4px;

      &:last-child {
        margin-bottom: 0;
      }

      .rate-label {
        color: #666;
        font-size: 12px;
      }

      .rate-value {
        font-weight: 500;
        color: #1890ff;
      }
    }
  }
}
</style>
