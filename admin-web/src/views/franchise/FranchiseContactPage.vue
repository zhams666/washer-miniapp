<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">Franchise Leads</p>
        <h3>加盟联系</h3>
        <span>查看小程序首页提交的加盟姓名和电话号码。</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>总记录</span>
        </div>
        <div>
          <strong>{{ tableData.length }}</strong>
          <span>当前页</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="姓名或电话">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="输入姓名或电话号码"
            style="width: 260px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="contactName" label="姓名" min-width="160" />
        <el-table-column prop="contactPhone" label="电话" min-width="180" />
        <el-table-column prop="source" label="来源" min-width="120">
          <template #default="{ row }">
            {{ row.source || 'miniapp' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220">
          <template #default="{ row }">
            {{ row.remark || '无' }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="190">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :current-page="pagination.page"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { fetchFranchiseContactPage } from '@/api/franchise-contact';
import type { FranchiseContactItem } from '@/types/franchise-contact';

const loading = ref(false);
const tableData = ref<FranchiseContactItem[]>([]);
const filters = reactive({
  keyword: '',
});
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const loadData = async () => {
  loading.value = true;
  try {
    const result = await fetchFranchiseContactPage({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword.trim() || undefined,
    });
    tableData.value = result.records || [];
    pagination.total = Number(result.total || 0);
    pagination.page = Number(result.current || pagination.page);
    pagination.size = Number(result.size || pagination.size);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.page = 1;
  loadData();
};

const handleReset = () => {
  filters.keyword = '';
  pagination.page = 1;
  loadData();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  loadData();
};

const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.page = 1;
  loadData();
};

const formatDateTime = (value?: string) => {
  if (!value) {
    return '无';
  }
  return value.replace('T', ' ').slice(0, 19);
};

onMounted(loadData);
</script>
