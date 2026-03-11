<template>
  <div class="user-management">
    <div class="user-header">
      <h1>{{ $t('page.system.user.title') }}</h1>
      <a-button type="primary" @click="openAddModal">
        <template #icon>
          <Plus />
        </template>
        新增用户
      </a-button>
    </div>
    
    <a-card class="user-search">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="用户名">
          <a-input v-model:value="searchForm.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="真实姓名">
          <a-input v-model:value="searchForm.realName" placeholder="请输入真实姓名" />
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
    
    <a-card class="user-table">
      <a-table
        :columns="columns"
        :data-source="users"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
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
              <a-button size="small" danger @click="deleteUser(record.id)">
                删除
              </a-button>
              <a-button size="small" @click="openResetPasswordModal(record.id, record.username)">
                重置密码
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    
    <!-- 新增/编辑用户弹窗 -->
    <a-modal
      v-model:open="modalOpen"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="用户名" :required="true">
          <a-input v-model:value="form.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="真实姓名" :required="true">
          <a-input v-model:value="form.realName" placeholder="请输入真实姓名" />
        </a-form-item>
        <a-form-item label="密码" :required="!form.id">
          <a-input-password v-model:value="form.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model:value="form.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input v-model:value="form.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="form.status">
            <a-option value="1">启用</a-option>
            <a-option value="0">禁用</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    
    <!-- 重置密码弹窗 -->
    <a-modal
      v-model:open="resetPasswordModalOpen"
      title="重置密码"
      @ok="handleResetPassword"
      @cancel="handleCancelResetPassword"
    >
      <a-form :model="resetPasswordForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="用户名">
          <a-input v-model:value="resetPasswordForm.username" disabled />
        </a-form-item>
        <a-form-item label="新密码" :required="true">
          <a-input-password v-model:value="resetPasswordForm.password" placeholder="请输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { userApi } from '@/api'
import type { SysUser, UserQuery } from '@/types/api'
import { Plus, Search } from '@vben/icons'
import { message, Modal } from 'antdv-next'

const loading = ref(false)
const users = ref<SysUser[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  onChange: (page: number) => {
    pagination.current = page
    fetchUsers()
  },
  onShowSizeChange: (page: number, pageSize: number) => {
    pagination.current = page
    pagination.pageSize = pageSize
    fetchUsers()
  }
})

const searchForm = reactive<UserQuery>({
  username: '',
  realName: '',
  status: ''
})

const modalOpen = ref(false)
const modalTitle = ref('新增用户')
const form = reactive<SysUser>({
  username: '',
  realName: '',
  status: 1
})

const resetPasswordModalOpen = ref(false)
const resetPasswordForm = reactive({
  userId: undefined as number | undefined,
  username: '',
  password: ''
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '真实姓名', dataIndex: 'realName', key: 'realName' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '最后登录时间', dataIndex: 'lastLoginTime', key: 'lastLoginTime' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { 
    title: '操作', 
    key: 'action',
    fixed: 'right',
    width: 200
  }
]

onMounted(() => {
  fetchUsers()
})

async function fetchUsers() {
  loading.value = true
  try {
    const response = await userApi.getUserPage({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    users.value = response.list
    pagination.total = response.total
  } catch (error) {
    console.error('获取用户列表失败:', error)
    message.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  fetchUsers()
}

function resetSearch() {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.status = ''
  pagination.current = 1
  fetchUsers()
}

function openAddModal() {
  modalTitle.value = '新增用户'
  Object.assign(form, {
    username: '',
    realName: '',
    password: '',
    email: '',
    phone: '',
    status: 1
  })
  modalOpen.value = true
}

function openEditModal(user: SysUser) {
  modalTitle.value = '编辑用户'
  Object.assign(form, user)
  // 编辑时清空密码字段
  form.password = ''
  modalOpen.value = true
}

async function handleOk() {
  loading.value = true
  try {
    if (form.id) {
      await userApi.updateUser(form.id, form)
      message.success('更新用户成功')
    } else {
      await userApi.addUser(form)
      message.success('新增用户成功')
    }
    modalOpen.value = false
    fetchUsers()
  } catch (error) {
    console.error('保存用户失败:', error)
    message.error('保存用户失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  modalOpen.value = false
}

async function updateStatus(id: number, status: number) {
  try {
    await userApi.updateUserStatus(id, status)
    message.success('更新状态成功')
    fetchUsers()
  } catch (error) {
    console.error('更新状态失败:', error)
    message.error('更新状态失败')
  }
}

async function deleteUser(id: number) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个用户吗？',
    onOk: async () => {
      try {
        await userApi.deleteUser(id)
        message.success('删除用户成功')
        fetchUsers()
      } catch (error) {
        console.error('删除用户失败:', error)
        message.error('删除用户失败')
      }
    }
  })
}

function openResetPasswordModal(userId: number, username: string) {
  resetPasswordForm.userId = userId
  resetPasswordForm.username = username
  resetPasswordForm.password = ''
  resetPasswordModalOpen.value = true
}

async function handleResetPassword() {
  if (!resetPasswordForm.userId) return
  
  loading.value = true
  try {
    await userApi.resetPassword(resetPasswordForm.userId, resetPasswordForm.password)
    message.success('重置密码成功')
    resetPasswordModalOpen.value = false
  } catch (error) {
    console.error('重置密码失败:', error)
    message.error('重置密码失败')
  } finally {
    loading.value = false
  }
}

function handleCancelResetPassword() {
  resetPasswordModalOpen.value = false
}
</script>

<style scoped>
.user-management {
  padding: 24px;
}

.user-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.user-header h1 {
  margin: 0;
}

.user-search {
  margin-bottom: 24px;
}

.user-table {
  margin-top: 24px;
}
</style>
