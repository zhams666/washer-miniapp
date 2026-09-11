<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">Voucher Center</p>
        <h3>兑换券核销</h3>
        <span>按门店、状态、券码或批次查看生成的兑换券和核销流水。</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>总记录</span>
        </div>
        <div>
          <strong>{{ redeemedCount }}</strong>
          <span>本页已核销</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="门店">
          <el-select v-model="filters.storeId" clearable style="width: 180px">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="String(store.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable style="width: 140px">
            <el-option label="未核销" value="unused" />
            <el-option label="已核销" value="redeemed" />
          </el-select>
        </el-form-item>
        <el-form-item label="券码/批次">
          <el-input v-model="filters.keyword" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="serialNo" label="序列号" min-width="180" />
        <el-table-column prop="batchNo" label="批次号" min-width="190" />
        <el-table-column prop="storeName" label="门店" min-width="150">
          <template #default="{ row }">{{ row.storeName || '无数据' }}</template>
        </el-table-column>
        <el-table-column label="金额" min-width="100" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'redeemed' ? 'success' : 'warning'">
              {{ formatVoucherStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核销用户" min-width="150">
          <template #default="{ row }">{{ formatRedeemedUser(row) }}</template>
        </el-table-column>
        <el-table-column prop="redeemTransactionNo" label="核销流水号" min-width="190">
          <template #default="{ row }">{{ row.redeemTransactionNo || '无数据' }}</template>
        </el-table-column>
        <el-table-column label="生成时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="核销时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.redeemedAt) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180">
          <template #default="{ row }">{{ row.remark || '无数据' }}</template>
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
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { fetchExchangeVoucherPage } from '@/api/payment-center';
import { fetchAdminStoreOptions } from '@/api/store';
import type { AdminExchangeVoucherItem, ExchangeVoucherQueryParams } from '@/types/payment-center';
import type { StoreOption } from '@/types/store';
import { formatAmount, formatDateTime } from '@/utils/format';

const loading = ref(false);
const tableData = ref<AdminExchangeVoucherItem[]>([]);
const storeOptions = ref<StoreOption[]>([]);

const filters = reactive({
  storeId: '',
  status: '',
  keyword: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const redeemedCount = computed(() => tableData.value.filter((item) => item.status === 'redeemed').length);

const parseOptionalNumber = (value: string) => {
  const parsed = Number(value);
  return value && !Number.isNaN(parsed) ? parsed : undefined;
};

const formatVoucherStatus = (value?: string) => {
  if (value === 'redeemed') {
    return '已核销';
  }
  if (value === 'unused') {
    return '未核销';
  }
  return value || '无数据';
};

const formatRedeemedUser = (row: AdminExchangeVoucherItem) => {
  return row.redeemedUserNickname || row.redeemedUserMobile || (row.redeemedUserId ? `用户${row.redeemedUserId}` : '未核销');
};

const loadStoreOptions = async () => {
  try {
    storeOptions.value = await fetchAdminStoreOptions();
  } catch (error) {
    ElMessage.error('门店列表加载失败');
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const data = await fetchExchangeVoucherPage({
      page: pagination.page,
      size: pagination.size,
      storeId: parseOptionalNumber(filters.storeId),
      status: filters.status || undefined,
      keyword: filters.keyword.trim() || undefined,
    } as ExchangeVoucherQueryParams);
    tableData.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error('兑换券列表加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.page = 1;
  void loadData();
};

const handleReset = () => {
  filters.storeId = '';
  filters.status = '';
  filters.keyword = '';
  pagination.page = 1;
  void loadData();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  void loadData();
};

const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.page = 1;
  void loadData();
};

onMounted(async () => {
  await loadStoreOptions();
  await loadData();
});
</script>

<style scoped lang="scss">
.page-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
</style>
