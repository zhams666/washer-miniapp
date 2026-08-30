export interface FranchiseContactItem {
  id: number;
  contactName: string;
  contactPhone: string;
  source?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface FranchiseContactPageResult {
  records: FranchiseContactItem[];
  total: number;
  size: number;
  current: number;
}
