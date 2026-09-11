export interface RankingDisplayAdjustmentItem {
  id: number;
  userId: number;
  scope: string;
  scopeName: string;
  userNo?: string;
  nickname?: string;
  mobile?: string;
  durationSeconds: number;
  durationMinutes: number;
  durationText: string;
  remark?: string;
  occurredAt?: string;
  createdAt?: string;
}

export interface RankingDisplayAdjustmentPageResult {
  records: RankingDisplayAdjustmentItem[];
  total: number;
  size: number;
  current: number;
}

export interface RankingDisplayAdjustmentCreatePayload {
  userId: number;
  scope?: string;
  durationMinutes: number;
  remark?: string;
}

export interface AdminRankingDurationItem {
  rank: number;
  userId: number;
  userNo?: string;
  nickname?: string;
  mobile?: string;
  avatarUrl?: string;
  realDurationSeconds: number;
  realDurationMinutes: number;
  realDurationText: string;
  durationSeconds: number;
  durationMinutes: number;
  durationText: string;
  displayAdjustmentSeconds: number;
  displayAdjustmentMinutes: number;
  displayAdjustmentText: string;
  orderCount: number;
  latestEndTime?: string;
}
