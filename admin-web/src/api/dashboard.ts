import http from './http';
import type { AdminDashboardActivityItem, AdminDashboardTodayOverview } from '@/types/dashboard';

export type DashboardQuery = {
  bizDate?: string;
  startDate?: string;
  endDate?: string;
  storeId?: number;
};

export type DashboardActivitiesQuery = {
  startDate?: string;
  endDate?: string;
  limit?: number;
  storeId?: number;
};

export const fetchTodayDashboard = (query?: string | DashboardQuery) => {
  const params =
    typeof query === 'string'
      ? { bizDate: query }
      : Object.fromEntries(
          Object.entries(query || {}).filter(([, value]) => value !== undefined && value !== null && value !== ''),
        );

  return http.get<AdminDashboardTodayOverview>('/api/admin/dashboard/today', {
    params: Object.keys(params).length ? params : undefined,
  });
};

export const fetchDashboardStatistics = (query?: DashboardQuery) =>
  http.get<AdminDashboardTodayOverview>('/api/admin/dashboard/today', {
    params: query,
  });

export const fetchDashboardActivities = (query?: DashboardActivitiesQuery) =>
  http.get<AdminDashboardActivityItem[]>('/api/admin/dashboard/activities', {
    params: query,
  });
