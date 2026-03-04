<template>
  <div class="user-page">
    <!-- 搜索栏 -->
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="queryForm">
        <a-form-item label="用户名">
          <a-input
            v-model:value="queryForm.username"
            placeholder="请输入用户名"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="真实姓名">
          <a-input
            v-model:value="queryForm.realName"
            placeholder="请输入真实姓名"
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
          <span>用户列表</span>
          <a-button type="primary" @click="handleAdd">
            <plus-outlined />
            新增用户
          </a-button>
        </div>
      </template>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="userList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'lastLoginTime'">
            {{ record.lastLoginTime || '-' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <edit-outlined />
                编辑
              </a-button>
              <a-button type="link" size="small" @click="handleResetPassword(record)">
                <key-outlined />
                重置密码
              </a-button>
              <a-popconfirm
                title="确定要删除这个用户吗？"
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
      width="600px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户名" name="username">
              <a-input
                v-model:value="formData.username"
                placeholder="请输入用户名"
                :disabled="isEdit"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="真实姓名" name="realName">
              <a-input
                v-model:value="formData.realName"
                placeholder="请输入真实姓名"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input
                v-model:value="formData.email"
                placeholder="请输入邮箱"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="电话" name="phone">
              <a-input
                v-model:value="formData.phone"
                placeholder="请输入电话"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="密码" name="password" v-if="!isEdit">
          <a-input-password
            v-model:value="formData.password"
            placeholder="请输入密码"
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

    <!-- 重置密码弹窗 -->
    <a-modal
      v-model:open="resetPasswordVisible"
      title="重置密码"
      :confirm-loading="resetPasswordLoading"
      @ok="handleResetPasswordOk"
      @cancel="handleResetPasswordCancel"
    >
      <a-form
        ref="resetPasswordFormRef"
        :model="resetPasswordForm"
        :rules="resetPasswordRules"
        layout="vertical"
      >
        <a-form-item label="新密码" name="newPassword">
          <a-input-password
            v-model:value="resetPasswordForm.newPassword"
            placeholder="请输入新密码"
          />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword">
          <a-input-password
            v-model:value="resetPasswordForm.confirmPassword"
            placeholder="请再次输入新密码"
          />
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
  DeleteOutlined,
  KeyOutlined
} from '@ant-design/icons-vue'
import { userApi } from '@/api/user'
import type { SysUser, UserQuery, PageResult } from '@/types/api'

// 用户列表
const userList = ref<SysUser[]>([])
const loading = ref(false)
const total = ref(0)

// 查询表单
const queryForm = reactive<UserQuery>({
  username: '',
  realName: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

// 表格列
const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
  { title: '真实姓名', dataIndex: 'realName', key: 'realName', width: 120 },
  { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
  { title: '电话', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '最后登录时间', dataIndex: 'lastLoginTime', key: 'lastLoginTime', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
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
const modalTitle = ref('新增用户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const formData = reactive<SysUser>({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  status: 1
})

// 表单校验规则
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: !isEdit.value, message: '请输入密码', trigger: 'blur' }]
}

// 重置密码相关
const resetPasswordVisible = ref(false)
const resetPasswordLoading = ref(false)
const resetPasswordFormRef = ref<FormInstance>()
const currentUserId = ref<number | null>(null)
const resetPasswordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const resetPasswordRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string) => {
        if (value !== resetPasswordForm.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

// 查询
const handleQuery = () => {
  queryForm.pageNum = 1
  loadData()
}

// 重置
const handleReset = () => {
  queryForm.username = ''
  queryForm.realName = ''
  queryForm.status = ''
  queryForm.pageNum = 1
  loadData()
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await userApi.getUserPage(queryForm)
    userList.value = res.list
    total.value = res.total
    pagination.total = res.total
    pagination.current = queryForm.pageNum || 1
    pagination.pageSize = queryForm.pageSize || 10
  } finally {
    loading.value = false
  }
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
  modalTitle.value = '新增用户'
  resetForm()
  modalVisible.value = true
}

// 编辑
const handleEdit = (record: SysUser) => {
  isEdit.value = true
  modalTitle.value = '编辑用户'
  Object.assign(formData, record)
  modalVisible.value = true
}

// 删除
const handleDelete = async (record: SysUser) => {
  try {
    await userApi.deleteUser(record.id!)
    message.success('删除成功')
    loadData()
  } catch (error) {
    message.error('删除失败')
  }
}

// 重置密码
const handleResetPassword = (record: SysUser) => {
  currentUserId.value = record.id!
  resetPasswordForm.newPassword = ''
  resetPasswordForm.confirmPassword = ''
  resetPasswordVisible.value = true
}

// 确认重置密码
const handleResetPasswordOk = async () => {
  try {
    await resetPasswordFormRef.value?.validate()
    resetPasswordLoading.value = true
    
    await userApi.resetPassword(currentUserId.value!, resetPasswordForm.newPassword)
    message.success('密码重置成功')
    resetPasswordVisible.value = false
  } catch (error) {
    console.error(error)
  } finally {
    resetPasswordLoading.value = false
  }
}

// 取消重置密码
const handleResetPasswordCancel = () => {
  resetPasswordVisible.value = false
  resetPasswordFormRef.value?.resetFields()
}

// 弹窗确认
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (isEdit.value) {
      await userApi.updateUser(formData.id!, { ...formData })
      message.success('更新成功')
    } else {
      await userApi.addUser({ ...formData })
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
  formData.username = ''
  formData.password = ''
  formData.realName = ''
  formData.email = ''
  formData.phone = ''
  formData.status = 1
  formRef.value?.resetFields()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.user-page {
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
}
</style>
