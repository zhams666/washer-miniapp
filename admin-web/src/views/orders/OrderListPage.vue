<template>
  <div class="order-page">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">{{ t('orders.phase') }}</p>
        <h3>{{ t('orders.heroTitle') }}</h3>
        <span>{{ t('orders.heroDesc') }}</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>{{ t('orders.totalOrders') }}</span>
        </div>
        <div>
          <strong>{{ tableData.length }}</strong>
          <span>{{ t('orders.rowsOnPage') }}</span>
        </div>
      </div>
    </div>

    <OrderFilterBar
      v-model="filters"
      :stores="storeOptions"
      @search="handleSearch"
      @reset="handleReset"
    />

    <div class="table-card">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="orderNo" :label="t('orders.table.orderNo')" min-width="220" />
        <el-table-column prop="storeName" :label="t('orders.table.store')" min-width="160" />
        <el-table-column prop="deviceName" :label="t('orders.table.device')" min-width="160">
          <template #default="{ row }">
            {{ row.deviceName || t('common.noData') }}
          </template>
        </el-table-column>
        <el-table-column prop="userId" :label="t('orders.table.userId')" min-width="90" />
        <el-table-column :label="t('orders.table.payMode')" min-width="120">
          <template #default="{ row }">
            {{ formatPayMode(row.payMode) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('orders.table.payment')" min-width="100">
          <template #default="{ row }">
            <OrderStatusTag :value="row.paymentStatus" mode="payment" />
          </template>
        </el-table-column>
        <el-table-column :label="t('orders.table.status')" min-width="100">
          <template #default="{ row }">
            <OrderStatusTag :value="row.orderStatus" />
          </template>
        </el-table-column>
        <el-table-column :label="t('orders.table.amount')" min-width="100" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.finalAmount) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('orders.table.created')" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('orders.table.remark')" min-width="180">
          <template #default="{ row }">
            {{ row.remark || t('common.noData') }}
          </template>
        </el-table-column>
        <el-table-column :label="t('orders.table.action')" fixed="right" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">
              {{ t('common.detail') }}
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

    <OrderDetailDrawer
      :visible="detailVisible"
      :detail="detailData"
      @close="detailVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { t } from '@/i18n';
import OrderFilterBar from '@/components/order/OrderFilterBar.vue';
import OrderStatusTag from '@/components/order/OrderStatusTag.vue';
import { fetchOrderDetail, fetchOrderPage, fetchStoreOptions } from '@/api/order';
import type { AdminOrderDetail, AdminOrderItem, StoreOption } from '@/types/order';
import { formatAmount, formatDateTime, formatPayMode } from '@/utils/format';
import OrderDetailDrawer from './OrderDetailDrawer.vue';

const loading = ref(false);
const tableData = ref<AdminOrderItem[]>([]);
const storeOptions = ref<StoreOption[]>([]);
const detailVisible = ref(false);
const detailData = ref<AdminOrderDetail | null>(null);

const filters = reactive({
  storeId: undefined as number | undefined,
  orderStatus: '',
  paymentStatus: '',
  keyword: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const loadStores = async () => {
  try {
    storeOptions.value = await fetchStoreOptions();
  } catch (error) {
    ElMessage.error(t('orders.messages.loadStoreOptionsFailed'));
  }
};

const loadOrders = async () => {
  loading.value = true;
  try {
    const data = await fetchOrderPage({
      page: pagination.page,
      size: pagination.size,
      storeId: filters.storeId,
      orderStatus: filters.orderStatus || undefined,
      paymentStatus: filters.paymentStatus || undefined,
      keyword: filters.keyword || undefined,
    });

    tableData.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error(t('orders.messages.loadOrdersFailed'));
  } finally {
    loading.value = false;
  }
};

const openDetail = async (id: number) => {
  try {
    detailData.value = await fetchOrderDetail(id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(t('orders.messages.loadOrderDetailFailed'));
  }
};

const handleSearch = () => {
  pagination.page = 1;
  void loadOrders();
};

const handleReset = () => {
  pagination.page = 1;
  void loadOrders();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  void loadOrders();
};

const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.page = 1;
  void loadOrders();
};

onMounted(async () => {
  await loadStores();
  await loadOrders();
});
</script>
