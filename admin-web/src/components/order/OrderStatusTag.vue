<template>
  <el-tag :type="tagType" effect="light" round>
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { formatOrderStatus, formatPaymentStatus } from '@/utils/format';

const props = defineProps<{
  value?: string;
  mode?: 'order' | 'payment';
}>();

const label = computed(() => {
  if (props.mode === 'payment') {
    return formatPaymentStatus(props.value);
  }

  return formatOrderStatus(props.value);
});

const tagType = computed(() => {
  if (props.mode === 'payment') {
    if (props.value === 'paid') {
      return 'success';
    }
    if (props.value === 'unpaid') {
      return 'warning';
    }
    return 'info';
  }

  if (props.value === 'completed') {
    return 'success';
  }
  if (props.value === 'running') {
    return 'primary';
  }
  return 'info';
});
</script>
