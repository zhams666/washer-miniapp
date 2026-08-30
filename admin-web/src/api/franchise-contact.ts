import http from './http';
import type { FranchiseContactPageResult } from '@/types/franchise-contact';

export const fetchFranchiseContactPage = (params: {
  page: number;
  size: number;
  keyword?: string;
}) => http.get<FranchiseContactPageResult>('/api/franchise-contacts/admin', { params });
