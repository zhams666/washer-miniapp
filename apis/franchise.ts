import { BaseEnum } from '../config/enums';
import type { IObject } from 'typings/interface.d';
import { apiRequest } from '../utils/container-request';

export type FranchiseContactPayload = {
  contactName: string;
  contactPhone: string;
  source?: string;
  remark?: string;
};

export const submitFranchiseContact = (
  payload: FranchiseContactPayload
): Promise<IObject> => {
  return apiRequest<IObject>('POST', '/api/franchise-contacts', {
    ...payload,
    wxAppId: BaseEnum.APP_ID,
  }).then((response) => response.code === 0 ? (response.data || {}) : Promise.reject(response));
};
