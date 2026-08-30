<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">{{ t('settlementBill.phase') }}</p>
        <h3>{{ t('settlementBill.heroTitle') }}</h3>
        <span>{{ t('settlementBill.heroDesc') }}</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>{{ t('settlementBill.common.totalRecords') }}</span>
        </div>
        <div>
          <strong>{{ tableData.length }}</strong>
          <span>{{ t('settlementBill.common.rowsOnPage') }}</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item :label="t('settlementBill.filters.fromStore')">
          <el-select v-model="filters.fromStoreId" clearable style="width: 180px">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="String(store.id)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settlementBill.filters.toStore')">
          <el-select v-model="filters.toStoreId" clearable style="width: 180px">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="String(store.id)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settlementBill.filters.billNo')">
          <el-input v-model="filters.billNo" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item :label="t('settlementBill.filters.startDate')">
          <el-input v-model="filters.startDate" clearable style="width: 140px" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item :label="t('settlementBill.filters.endDate')">
          <el-input v-model="filters.endDate" clearable style="width: 140px" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="filter-bar filter-bar--compact">
      <el-form :inline="true" :model="generator" class="filter-form">
        <el-form-item :label="t('settlementBill.generator.startDate')">
          <el-input v-model="generator.startDate" clearable style="width: 140px" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item :label="t('settlementBill.generator.endDate')">
          <el-input v-model="generator.endDate" clearable style="width: 140px" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item :label="t('settlementBill.generator.periodType')">
          <el-select v-model="generator.settlementPeriodType" style="width: 140px">
            <el-option :label="t('settlementBill.generator.day')" value="day" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settlementBill.generator.remark')">
          <el-input v-model="generator.remark" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="generateLoading" @click="handleGenerate">
            {{ t('settlementBill.generator.generate') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="billNo" :label="t('settlementBill.table.billNo')" min-width="200" />
        <el-table-column :label="t('settlementBill.table.fromStore')" min-width="180">
          <template #default="{ row }">{{ resolveStoreLabel(row.fromStoreId) }}</template>
        </el-table-column>
        <el-table-column :label="t('settlementBill.table.toStore')" min-width="180">
          <template #default="{ row }">{{ resolveStoreLabel(row.toStoreId) }}</template>
        </el-table-column>
        <el-table-column :label="t('settlementBill.table.period')" min-width="200">
          <template #default="{ row }">
            <span>{{ row.startDate || t('common.noData') }}</span>
            <span v-if="row.endDate"> ~ {{ row.endDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalOrderCount" :label="t('settlementBill.table.orderCount')" min-width="120" />
        <el-table-column :label="t('settlementBill.table.totalAmount')" min-width="140" align="right">
          <template #default="{ row }">{{ formatAmount(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('settlementBill.table.netAmount')" min-width="140" align="right">
          <template #default="{ row }">{{ formatAmount(row.netAmount) }}</template>
        </el-table-column>
        <el-table-column prop="settlementStatus" :label="t('settlementBill.table.status')" min-width="120" />
        <el-table-column prop="createdAt" :label="t('settlementBill.table.createdAt')" min-width="180" />
        <el-table-column :label="t('settlementBill.common.actions')" fixed="right" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openBillDetails(row)">
              {{ t('settlementBill.common.viewDetails') }}
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

    <el-drawer v-model="billDetailVisible" size="70%" :title="drawerTitle">
      <el-table v-loading="detailLoading" :data="detailTableData" border>
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
          :current-page="detailPagination.page"
          :page-size="detailPagination.size"
          :page-sizes="[10, 20, 50]"
          :total="detailPagination.total"
          @current-change="handleDetailPageChange"
          @size-change="handleDetailSizeChange"
        />
      </div>
    </el-drawer>

    <OrderDetailDrawer :visible="detailVisible" :detail="detailData" @close="detailVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { fetchOrderDetail } from '@/api/order';
import { fetchSettlementBillPage, fetchSettlementDetailPage, generateSettlementBills } from '@/api/payment-center';
import { fetchAdminStoreOptions } from '@/api/store';
import { t } from '@/i18n';
import type { AdminOrderDetail } from '@/types/order';
import type {
  AdminSettlementBillItem,
  AdminSettlementDetailItem,
  SettlementBillGeneratePayload,
  SettlementBillQueryParams,
  SettlementDetailQueryParams,
} from '@/types/payment-center';
import type { StoreOption } from '@/types/store';
import { formatAmount } from '@/utils/format';
import OrderDetailDrawer from '@/views/orders/OrderDetailDrawer.vue';

const loading = ref(false);
const generateLoading = ref(false);
const detailLoading = ref(false);
const tableData = ref<AdminSettlementBillItem[]>([]);
const detailTableData = ref<AdminSettlementDetailItem[]>([]);
const storeOptions = ref<StoreOption[]>([]);
const detailVisible = ref(false);
const detailData = ref<AdminOrderDetail | null>(null);
const billDetailVisible = ref(false);
const activeBill = ref<AdminSettlementBillItem | null>(null);

const filters = reactive({
  fromStoreId: '',
  toStoreId: '',
  billNo: '',
  startDate: '',
  endDate: '',
});

const generator = reactive({
  startDate: '',
  endDate: '',
  settlementPeriodType: 'day',
  remark: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const detailPagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const drawerTitle = computed(() => {
  if (!activeBill.value?.billNo) {
    return t('settlementBill.drawer.title');
  }
  return `${t('settlementBill.drawer.title')} - ${activeBill.value.billNo}`;
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
    ElMessage.error(t('settlementBill.messages.loadStoresFailed'));
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const data = await fetchSettlementBillPage({
      page: pagination.page,
      size: pagination.size,
      fromStoreId: parseOptionalNumber(filters.fromStoreId),
      toStoreId: parseOptionalNumber(filters.toStoreId),
      billNo: filters.billNo || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
    } as SettlementBillQueryParams);
    tableData.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error(t('settlementBill.messages.loadBillsFailed'));
  } finally {
    loading.value = false;
  }
};

const loadBillDetails = async () => {
  if (!activeBill.value?.id) {
    return;
  }
  detailLoading.value = true;
  try {
    const data = await fetchSettlementDetailPage({
      page: detailPagination.page,
      size: detailPagination.size,
      billId: activeBill.value.id,
    } as SettlementDetailQueryParams);
    detailTableData.value = data.records || [];
    detailPagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error(t('settlementBill.messages.loadBillDetailsFailed'));
  } finally {
    detailLoading.value = false;
  }
};

const openBillDetails = async (bill: AdminSettlementBillItem) => {
  activeBill.value = bill;
  detailPagination.page = 1;
  billDetailVisible.value = true;
  await loadBillDetails();
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
  filters.billNo = '';
  filters.startDate = '';
  filters.endDate = '';
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

const handleDetailPageChange = (page: number) => {
  detailPagination.page = page;
  void loadBillDetails();
};

const handleDetailSizeChange = (size: number) => {
  detailPagination.size = size;
  detailPagination.page = 1;
  void loadBillDetails();
};

const handleGenerate = async () => {
  if (!generator.startDate || !generator.endDate) {
    ElMessage.error(t('settlementBill.messages.generateDateRequired'));
    return;
  }
  generateLoading.value = true;
  try {
    const payload: SettlementBillGeneratePayload = {
      settlementPeriodType: generator.settlementPeriodType,
      startDate: generator.startDate,
      endDate: generator.endDate,
      remark: generator.remark || undefined,
    };
    const result = await generateSettlementBills(payload);
    ElMessage.success(`${t('settlementBill.messages.generateSuccess')} ${result.generatedCount ?? 0} 张结算单`);
    await loadData();
  } catch (error) {
    ElMessage.error(t('settlementBill.messages.generateFailed'));
  } finally {
    generateLoading.value = false;
  }
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

.filter-bar--compact {
  padding-top: 8px;
}
</style>
