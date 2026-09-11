import http from './http';
import type {
  AdminRankingDurationItem,
  RankingDisplayAdjustmentCreatePayload,
  RankingDisplayAdjustmentItem,
  RankingDisplayAdjustmentPageResult,
} from '@/types/ranking-display-adjustment';

export const fetchRankingDisplayAdjustments = (params: { page: number; size: number; scope?: string; keyword?: string }) =>
  http.get<RankingDisplayAdjustmentPageResult>('/api/admin/ranking-display-adjustments', { params });

export const fetchRankingDurationRows = (params: { scope?: string; limit?: number }) =>
  http.get<AdminRankingDurationItem[]>('/api/admin/ranking-display-adjustments/rankings', { params });

export const createRankingDisplayAdjustment = (payload: RankingDisplayAdjustmentCreatePayload) =>
  http.post<RankingDisplayAdjustmentItem>('/api/admin/ranking-display-adjustments', payload);

export const setRankingDisplayAdjustment = (payload: RankingDisplayAdjustmentCreatePayload) =>
  http.post<RankingDisplayAdjustmentItem>('/api/admin/ranking-display-adjustments/set', payload);

export const deleteRankingDisplayAdjustment = (id: number) =>
  http.delete(`/api/admin/ranking-display-adjustments/${id}`);
