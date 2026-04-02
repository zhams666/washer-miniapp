<template>
  <div class="filter-bar">
    <el-form :inline="true" :model="localModel" class="filter-form">
      <el-form-item :label="t('orders.filters.store')">
        <el-select
          v-model="localModel.storeId"
          :placeholder="t('orders.filters.allStores')"
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="store in stores"
            :key="store.id"
            :label="store.storeName"
            :value="store.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('orders.filters.orderStatus')">
        <el-select
          v-model="localModel.orderStatus"
          :placeholder="t('orders.filters.allStatuses')"
          clearable
          style="width: 160px"
        >
          <el-option
            v-for="item in orderStatusOptions"
            :key="item.value || 'all-order-status'"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('orders.filters.paymentStatus')">
        <el-select
          v-model="localModel.paymentStatus"
          :placeholder="t('orders.filters.allPayments')"
          clearable
          style="width: 170px"
        >
          <el-option
            v-for="item in paymentStatusOptions"
            :key="item.value || 'all-payment-status'"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('orders.filters.keyword')">
        <el-input
          v-model="localModel.keyword"
          :placeholder="t('orders.filters.keywordPlaceholder')"
          clearable
          style="width: 220px"
          @keyup.enter="emitSearch"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="emitSearch">{{ t('common.search') }}</el-button>
        <el-button @click="emitReset">{{ t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue';
import { t } from '@/i18n';
import { getOrderStatusOptions, getPaymentStatusOptions } from '@/utils/constants';
import type { StoreOption } from '@/types/order';

type FilterModel = {
  storeId?: number;
  orderStatus?: string;
  paymentStatus?: string;
  keyword?: string;
};

const props = defineProps<{
  modelValue: FilterModel;
  stores: StoreOption[];
}>();

const emit = defineEmits<{
  'update:modelValue': [value: FilterModel];
  search: [];
  reset: [];
}>();

const orderStatusOptions = getOrderStatusOptions();
const paymentStatusOptions = getPaymentStatusOptions();

const localModel = reactive<FilterModel>({
  storeId: props.modelValue.storeId,
  orderStatus: props.modelValue.orderStatus,
  paymentStatus: props.modelValue.paymentStatus,
  keyword: props.modelValue.keyword,
});

watch(
  () => props.modelValue,
  (value) => {
    localModel.storeId = value.storeId;
    localModel.orderStatus = value.orderStatus;
    localModel.paymentStatus = value.paymentStatus;
    localModel.keyword = value.keyword;
  },
  { deep: true },
);

watch(
  localModel,
  (value) => {
    emit('update:modelValue', { ...value });
  },
  { deep: true },
);

const emitSearch = () => emit('search');

const emitReset = () => {
  localModel.storeId = undefined;
  localModel.orderStatus = '';
  localModel.paymentStatus = '';
  localModel.keyword = '';
  emit('update:modelValue', { ...localModel });
  emit('reset');
};
</script>
