import { t } from '@/i18n';

export const getOrderStatusOptions = () => [
  { label: t('orders.filters.allStatuses'), value: '' },
  { label: t('status.pending'), value: 'pending' },
  { label: t('status.running'), value: 'running' },
  { label: t('status.completed'), value: 'completed' },
];

export const getPaymentStatusOptions = () => [
  { label: t('orders.filters.allPayments'), value: '' },
  { label: t('status.unpaid'), value: 'unpaid' },
  { label: t('status.paid'), value: 'paid' },
];
