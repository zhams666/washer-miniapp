import http from './http';
import type {
  AdminCardUsageCenterItem,
  AdminCardUsagePageResult,
  AdminPaymentDetailItem,
  AdminPaymentDetailPageResult,
  AdminWalletTransactionCenterItem,
  AdminWalletTransactionPageResult,
  CardUsageQueryParams,
  PaymentDetailQueryParams,
  WalletTransactionQueryParams,
} from '@/types/payment-center';

export const fetchPaymentDetailPage = (params: PaymentDetailQueryParams) =>
  http.get<AdminPaymentDetailPageResult>('/api/admin/payment-details', { params });

export const fetchWalletTransactionPage = (params: WalletTransactionQueryParams) =>
  http.get<AdminWalletTransactionPageResult>('/api/admin/wallet-transactions', { params });

export const fetchCardUsagePage = (params: CardUsageQueryParams) =>
  http.get<AdminCardUsagePageResult>('/api/admin/card-usages', { params });
