import http from './http';
import type {
  AdminCardUsageCenterItem,
  AdminCardUsagePageResult,
  AdminPaymentDetailItem,
  AdminPaymentDetailPageResult,
  AdminSettlementDetailPageResult,
  AdminSettlementBillGenerateResult,
  AdminSettlementBillPageResult,
  SettlementDetailQueryParams,
  SettlementBillGeneratePayload,
  SettlementBillQueryParams,
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

export const fetchSettlementDetailPage = (params: SettlementDetailQueryParams) =>
  http.get<AdminSettlementDetailPageResult>('/api/admin/settlement-details', { params });

export const fetchSettlementBillPage = (params: SettlementBillQueryParams) =>
  http.get<AdminSettlementBillPageResult>('/api/admin/settlement-bills', { params });

export const generateSettlementBills = (payload: SettlementBillGeneratePayload) =>
  http.post<AdminSettlementBillGenerateResult>('/api/admin/settlement-bills/generate', payload);
