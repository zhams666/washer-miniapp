<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">{{ t('paymentCenter.phase') }}</p>
        <h3>{{ t('paymentCenter.paymentDetails.heroTitle') }}</h3>
        <span>{{ t('paymentCenter.paymentDetails.heroDesc') }}</span>
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
        <el-form-item :label="t('paymentCenter.paymentDetails.filters.orderNo')">
          <el-input v-model="filters.orderNo" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item :label="t('paymentCenter.paymentDetails.filters.userId')">
          <el-input v-model="filters.userId" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('paymentCenter.paymentDetails.filters.storeId')">
          <el-select v-model="filters.storeId" clearable style="width: 180px">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="String(store.id)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('paymentCenter.paymentDetails.filters.payMode')">
          <el-select v-model="filters.payMode" clearable style="width: 140px">
            <el-option :label="t('payMode.wallet')" value="wallet" />
            <el-option :label="t('payMode.card')" value="card" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('paymentCenter.paymentDetails.filters.paymentStatus')">
          <el-select v-model="filters.paymentStatus" clearable style="width: 140px">
            <el-option :label="t('status.paid')" value="paid" />
            <el-option :label="t('status.unpaid')" value="unpaid" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="orderNo" :label="t('paymentCenter.paymentDetails.table.orderNo')" min-width="180" />
        <el-table-column prop="userId" :label="t('paymentCenter.paymentDetails.table.userId')" min-width="90" />
        <el-table-column prop="storeName" :label="t('paymentCenter.paymentDetails.table.store')" min-width="140">
          <template #default="{ row }">{{ row.storeName || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.paymentDetails.table.payMode')" min-width="110">
          <template #default="{ row }">{{ formatPayMode(row.payMode) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.paymentDetails.table.paymentStatus')" min-width="100">
          <template #default="{ row }">{{ formatPaymentStatus(row.paymentStatus) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.paymentDetails.table.amountType')" min-width="110">
          <template #default="{ row }">{{ formatAmountType(row.amountType) }}</template>
        </el-table-column>
        <el-table-column prop="cardNo" :label="t('paymentCenter.paymentDetails.table.cardNo')" min-width="180">
          <template #default="{ row }">{{ row.cardNo || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.paymentDetails.table.amount')" min-width="100" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.paymentDetails.table.refundedAmount')" min-width="120" align="right">
          <template #default="{ row }">{{ formatAmount(row.refundedAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.paymentDetails.table.createdAt')" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('paymentCenter.common.actions')" fixed="right" width="120">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.orderId" @click="openOrderDetail(row.orderId)">
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
import { fetchPaymentDetailPage } from '@/api/payment-center';
import { fetchAdminStoreOptions } from '@/api/store';
import { t } from '@/i18n';
import type { AdminOrderDetail } from '@/types/order';
import type { AdminPaymentDetailItem, PaymentDetailQueryParams } from '@/types/payment-center';
import type { StoreOption } from '@/types/store';
import { formatAmount, formatAmountType, formatDateTime, formatPayMode, formatPaymentStatus } from '@/utils/format';
import OrderDetailDrawer from '@/views/orders/OrderDetailDrawer.vue';

const loading = ref(false);
const tableData = ref<AdminPaymentDetailItem[]>([]);
const storeOptions = ref<StoreOption[]>([]);
const detailVisible = ref(false);
const detailData = ref<AdminOrderDetail | null>(null);

const filters = reactive({
  orderNo: '',
  userId: '',
  storeId: '',
  payMode: '',
  paymentStatus: '',
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
    const data = await fetchPaymentDetailPage({
      page: pagination.page,
      size: pagination.size,
      orderNo: filters.orderNo || undefined,
      userId: parseOptionalNumber(filters.userId),
      storeId: parseOptionalNumber(filters.storeId),
      payMode: filters.payMode || undefined,
      paymentStatus: filters.paymentStatus || undefined,
    } as PaymentDetailQueryParams);
    tableData.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error(t('paymentCenter.messages.loadPaymentDetailsFailed'));
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
  filters.orderNo = '';
  filters.userId = '';
  filters.storeId = '';
  filters.payMode = '';
  filters.paymentStatus = '';
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
