<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">{{ t('paymentCenter.phase') }}</p>
        <h3>{{ t('paymentCenter.walletTransactions.heroTitle') }}</h3>
        <span>{{ t('paymentCenter.walletTransactions.heroDesc') }}</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>{{ t('paymentCenter.common.totalRecords') }}</span>
        </div>
        <div>
          <strong>{{ tableData.length }}</strong>
          <span>{{ t('paymentCenter.common.rowsOnPage') }}</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item :label="t('paymentCenter.walletTransactions.filters.userId')">
          <el-input v-model="filters.userId" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('paymentCenter.walletTransactions.filters.storeId')">
          <el-select v-model="filters.storeId" clearable style="width: 180px">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="String(store.id)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('paymentCenter.walletTransactions.filters.bizType')">
          <el-select v-model="filters.bizType" clearable style="width: 160px">
            <el-option :label="t('walletBizType.recharge')" value="recharge" />
            <el-option :label="t('walletBizType.consume')" value="consume" />
            <el-option :label="t('walletBizType.refund')" value="refund" />
            <el-option :label="t('walletBizType.adjust')" value="adjust" />
            <el-option :label="t('walletBizType.clear_gift')" value="clear_gift" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('paymentCenter.walletTransactions.filters.relatedOrderNo')">
          <el-input v-model="filters.relatedOrderNo" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="transactionNo" :label="t('paymentCenter.walletTransactions.table.transactionNo')" min-width="180" />
        <el-table-column prop="userId" :label="t('paymentCenter.walletTransactions.table.userId')" min-width="90" />
        <el-table-column prop="storeName" :label="t('paymentCenter.walletTransactions.table.store')" min-width="140" />
        <el-table-column :label="t('paymentCenter.walletTransactions.table.bizType')" min-width="120">
          <template #default="{ row }">{{ formatWalletBizType(row.bizType) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.walletTransactions.table.amountType')" min-width="110">
          <template #default="{ row }">{{ formatAmountType(row.amountType) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.walletTransactions.table.changeType')" min-width="110">
          <template #default="{ row }">{{ formatWalletChangeType(row.changeType) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.walletTransactions.table.amount')" min-width="100" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.walletTransactions.table.balanceAfter')" min-width="120" align="right">
          <template #default="{ row }">{{ formatAmount(row.balanceAfter) }}</template>
        </el-table-column>
        <el-table-column prop="relatedOrderNo" :label="t('paymentCenter.walletTransactions.table.relatedOrderNo')" min-width="160">
          <template #default="{ row }">{{ row.relatedOrderNo || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('paymentCenter.walletTransactions.table.remark')" min-width="180">
          <template #default="{ row }">{{ row.remark || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.walletTransactions.table.createdAt')" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.common.actions')" fixed="right" width="120">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.relatedOrderId" @click="openOrderDetail(row.relatedOrderId)">
              {{ t('paymentCenter.common.viewOrder') }}
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
import { fetchWalletTransactionPage } from '@/api/payment-center';
import { fetchAdminStoreOptions } from '@/api/store';
import { t } from '@/i18n';
import type { AdminOrderDetail } from '@/types/order';
import type { AdminWalletTransactionCenterItem, WalletTransactionQueryParams } from '@/types/payment-center';
import type { StoreOption } from '@/types/store';
import { formatAmount, formatAmountType, formatDateTime, formatWalletBizType, formatWalletChangeType } from '@/utils/format';
import OrderDetailDrawer from '@/views/orders/OrderDetailDrawer.vue';

const loading = ref(false);
const tableData = ref<AdminWalletTransactionCenterItem[]>([]);
const storeOptions = ref<StoreOption[]>([]);
const detailVisible = ref(false);
const detailData = ref<AdminOrderDetail | null>(null);

const filters = reactive({
  userId: '',
  storeId: '',
  bizType: '',
  relatedOrderNo: '',
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

const loadStoreOptions = async () => {
  try {
    storeOptions.value = await fetchAdminStoreOptions();
  } catch (error) {
    ElMessage.error(t('paymentCenter.messages.loadStoresFailed'));
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const data = await fetchWalletTransactionPage({
      page: pagination.page,
      size: pagination.size,
      userId: parseOptionalNumber(filters.userId),
      storeId: parseOptionalNumber(filters.storeId),
      bizType: filters.bizType || undefined,
      relatedOrderNo: filters.relatedOrderNo || undefined,
    } as WalletTransactionQueryParams);
    tableData.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error(t('paymentCenter.messages.loadWalletTransactionsFailed'));
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
  filters.userId = '';
  filters.storeId = '';
  filters.bizType = '';
  filters.relatedOrderNo = '';
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
