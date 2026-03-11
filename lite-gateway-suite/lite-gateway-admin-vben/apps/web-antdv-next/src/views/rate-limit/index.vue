<script setup lang="ts">
import type { RateLimitQuery, RateLimitRule } from '@/types/api';

import { onMounted, reactive, ref } from 'vue';

import { Plus, Search } from '@vben/icons';

import { rateLimitApi } from '@/api';
import {
  Button,
  Card,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Space,
  Switch,
  Table,
} from 'antdv-next';

const loading = ref(false);
const rateLimits = ref<RateLimitRule[]>([]);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  onChange: (page: number) => {
    pagination.current = page;
    fetchRateLimits();
  },
  onShowSizeChange: (page: number, pageSize: number) => {
    pagination.current = page;
    pagination.pageSize = pageSize;
    fetchRateLimits();
  },
});

const searchForm = reactive<RateLimitQuery>({
  ruleName: '',
  limitType: '',
  status: '',
});

const modalOpen = ref(false);
const modalTitle = ref('新增限流规则');
const form = reactive<RateLimitRule>({
  ruleName: '',
  limitType: 1,
  replenishRate: 10,
  burstCapacity: 20,
  status: 1,
});

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
    width: 150,
  },
];

onMounted(() => {
  fetchRateLimits();
});

async function fetchRateLimits() {
  loading.value = true;
  try {
    const response = await rateLimitApi.getRateLimitPage({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    });
    rateLimits.value = response.list;
    pagination.total = response.total;
  } catch (error) {
    console.error('获取限流规则列表失败:', error);
    message.error('获取限流规则列表失败');
  } finally {
    loading.value = false;
  }
}

function search() {
  pagination.current = 1;
  fetchRateLimits();
}

function resetSearch() {
  searchForm.ruleName = '';
  searchForm.limitType = '';
  searchForm.status = '';
  searchForm.routeId = '';
  pagination.current = 1;
  fetchRateLimits();
}

function openAddModal() {
  modalTitle.value = '新增限流规则';
  Object.assign(form, {
    ruleName: '',
    limitType: 1,
    routeId: undefined,
    replenishRate: 10,
    burstCapacity: 20,
    requestedTokens: undefined,
    status: 1,
  });
  modalOpen.value = true;
}

function openEditModal(rule: RateLimitRule) {
  modalTitle.value = '编辑限流规则';
  Object.assign(form, rule);
  modalOpen.value = true;
}

async function handleOk() {
  loading.value = true;
  try {
    if (form.id) {
      await rateLimitApi.updateRateLimit(form.id, form);
      message.success('更新限流规则成功');
    } else {
      await rateLimitApi.addRateLimit(form);
      message.success('新增限流规则成功');
    }
    modalOpen.value = false;
    fetchRateLimits();
  } catch (error) {
    console.error('保存限流规则失败:', error);
    message.error('保存限流规则失败');
  } finally {
    loading.value = false;
  }
}

function handleCancel() {
  modalOpen.value = false;
}

async function updateStatus(id: number, status: number) {
  try {
    await rateLimitApi.updateRateLimitStatus(id, status);
    message.success('更新状态成功');
    fetchRateLimits();
  } catch (error) {
    console.error('更新状态失败:', error);
    message.error('更新状态失败');
  }
}

async function deleteRateLimit(id: number) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个限流规则吗？',
    onOk: async () => {
      try {
        await rateLimitApi.deleteRateLimit(id);
        message.success('删除限流规则成功');
        fetchRateLimits();
      } catch (error) {
        console.error('删除限流规则失败:', error);
        message.error('删除限流规则失败');
      }
    },
  });
}
</script>

<template>
  <div class="rate-limit-management">
    <div class="rate-limit-header">
      <h1>{{ $t('page.rateLimit.title') }}</h1>
      <Button type="primary" @click="openAddModal">
        <template #icon>
          <Plus />
        </template>
        新增限流规则
      </Button>
    </div>

    <Card class="rate-limit-search">
      <Form :model="searchForm" layout="inline">
        <FormItem label="规则名称">
          <Input
            v-model:value="searchForm.ruleName"
            placeholder="请输入规则名称"
          />
        </FormItem>
        <FormItem label="限流类型">
          <Select
            v-model:value="searchForm.limitType"
            placeholder="请选择限流类型"
            :options="[
              { label: '全部', value: '' },
              { label: 'IP限流', value: '1' },
              { label: '用户限流', value: '2' },
              { label: '全局限流', value: '3' },
            ]"
          />
        </FormItem>
        <FormItem label="状态">
          <Select
            v-model:value="searchForm.status"
            placeholder="请选择状态"
            :options="[
              { label: '全部', value: '' },
              { label: '启用', value: '1' },
              { label: '禁用', value: '0' },
            ]"
          />
        </FormItem>
        <FormItem>
          <Button type="primary" @click="search">
            <template #icon>
              <Search />
            </template>
            搜索
          </Button>
          <Button @click="resetSearch">重置 </Button>
        </FormItem>
      </Form>
    </Card>

    <Card class="rate-limit-table">
      <Table
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
            <Switch
              :checked="record.status === 1"
              @change="(checked) => updateStatus(record.id, checked ? 1 : 0)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button
                size="small"
                type="primary"
                @click="openEditModal(record)"
              >
                编辑
              </Button>
              <Button size="small" danger @click="deleteRateLimit(record.id)">
                删除
              </Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新增/编辑限流规则弹窗 -->
    <Modal
      v-model:open="modalOpen"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <Form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <FormItem label="规则名称" :required="true">
          <Input v-model:value="form.ruleName" placeholder="请输入规则名称" />
        </FormItem>
        <FormItem label="限流类型" :required="true">
          <Select
            v-model:value="form.limitType"
            :options="[
              { label: 'IP限流', value: '1' },
              { label: '用户限流', value: '2' },
              { label: '全局限流', value: '3' },
            ]"
          />
        </FormItem>
        <FormItem label="路由ID">
          <Input
            v-model:value="form.routeId"
            placeholder="请输入路由ID（可选）"
          />
        </FormItem>
        <FormItem label="令牌填充速率" :required="true">
          <InputNumber
            v-model:value="form.replenishRate"
            placeholder="请输入令牌填充速率"
          />
        </FormItem>
        <FormItem label="令牌桶容量" :required="true">
          <InputNumber
            v-model:value="form.burstCapacity"
            placeholder="请输入令牌桶容量"
          />
        </FormItem>
        <FormItem label="请求令牌数">
          <InputNumber
            v-model:value="form.requestedTokens"
            placeholder="请输入请求令牌数"
          />
        </FormItem>
        <FormItem label="状态">
          <Select
            v-model:value="form.status"
            :options="[
              { label: '启用', value: '1' },
              { label: '禁用', value: '0' },
            ]"
          />
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>

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
