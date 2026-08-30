<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">{{ t('settlementCenter.phase') }}</p>
        <h3>{{ t('settlementCenter.heroTitle') }}</h3>
        <span>{{ t('settlementCenter.heroDesc') }}</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>{{ t('settlementCenter.common.totalRecords') }}</span>
        </div>
        <div>
          <strong>{{ tableData.length }}</strong>
          <span>{{ t('settlementCenter.common.rowsOnPage') }}</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item :label="t('settlementCenter.filters.fromStore')">
          <el-select v-model="filters.fromStoreId" clearable style="width: 180px">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="String(store.id)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settlementCenter.filters.toStore')">
          <el-select v-model="filters.toStoreId" clearable style="width: 180px">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="String(store.id)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settlementCenter.filters.orderNo')">
          <el-input v-model="filters.orderNo" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item :label="t('settlementCenter.filters.bizDate')">
          <el-input v-model="filters.bizDate" clearable style="width: 140px" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="orderNo" :label="t('settlementCenter.table.orderNo')" min-width="180" />
        <el-table-column :label="t('settlementCenter.table.fromStore')" min-width="180">
          <template #default="{ row }">{{ resolveStoreLabel(row.fromStoreId) }}</template>
        </el-table-column>
        <el-table-column :label="t('settlementCenter.table.toStore')" min-width="180">
          <template #default="{ row }">{{ resolveStoreLabel(row.toStoreId) }}</template>
        </el-table-column>
        <el-table-column :label="t('settlementCenter.table.amount')" min-width="120" align="right">
          <template #default="{ row }">{{ formatAmount(row.principalAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('settlementCenter.table.bizDate')" min-width="130">
          <template #default="{ row }">{{ row.bizDate || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column :label="t('settlementCenter.table.detailStatus')" min-width="120">
          <template #default="{ row }">{{ row.detailStatus || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column :label="t('settlementCenter.common.actions')" fixed="right" width="120">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.orderId" @click="openOrderDetail(row.orderId)">
              {{ t('settlementCenter.common.viewOrder') }}
            </el-button>
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

    <OrderDetailDrawer :visible="detailVisible" :detail="detailData" @close="detailVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { fetchOrderDetail } from '@/api/order';
import { fetchSettlementDetailPage } from '@/api/payment-center';
import { fetchAdminStoreOptions } from '@/api/store';
import { t } from '@/i18n';
import type { AdminOrderDetail } from '@/types/order';
import type { AdminSettlementDetailItem, SettlementDetailQueryParams } from '@/types/payment-center';
import type { StoreOption } from '@/types/store';
import { formatAmount } from '@/utils/format';
import OrderDetailDrawer from '@/views/orders/OrderDetailDrawer.vue';

const loading = ref(false);
const tableData = ref<AdminSettlementDetailItem[]>([]);
const storeOptions = ref<StoreOption[]>([]);
const detailVisible = ref(false);
const detailData = ref<AdminOrderDetail | null>(null);

const filters = reactive({
  fromStoreId: '',
  toStoreId: '',
  orderNo: '',
  bizDate: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const parseOptionalNumber = (value: string) => {
  const parsed = Number(value);
  return value && !Number.isNaN(parsed) ? parsed : undefined;
};

const resolveStoreLabel = (storeId?: number | null) => {
  if (!storeId) {
    return t('common.noData');
  }
  const matched = storeOptions.value.find((store) => Number(store.id) === Number(storeId));
  return matched ? `${matched.storeName} (#${storeId})` : `#${storeId}`;
};

const loadStoreOptions = async () => {
  try {
    storeOptions.value = await fetchAdminStoreOptions();
  } catch (error) {
    ElMessage.error(t('settlementCenter.messages.loadStoresFailed'));
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const data = await fetchSettlementDetailPage({
      page: pagination.page,
      size: pagination.size,
      fromStoreId: parseOptionalNumber(filters.fromStoreId),
      toStoreId: parseOptionalNumber(filters.toStoreId),
      orderNo: filters.orderNo || undefined,
      bizDate: filters.bizDate || undefined,
    } as SettlementDetailQueryParams);
    tableData.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error(t('settlementCenter.messages.loadSettlementDetailsFailed'));
  } finally {
    loading.value = false;
  }
};

const openOrderDetail = async (id?: number | null) => {
  if (!id) {
    return;
  }
  try {
    detailData.value = await fetchOrderDetail(id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(t('orders.messages.loadOrderDetailFailed'));
  }
};

const handleSearch = () => {
  pagination.page = 1;
  void loadData();
};

const handleReset = () => {
  filters.fromStoreId = '';
  filters.toStoreId = '';
  filters.orderNo = '';
  filters.bizDate = '';
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
