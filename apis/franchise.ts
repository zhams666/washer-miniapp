import { REQUEST_URL } from '../config/url';
import { BaseEnum } from '../config/enums';
import type { IObject } from 'typings/interface.d';

export type FranchiseContactPayload = {
  contactName: string;
  contactPhone: string;
  source?: string;
  remark?: string;
};

export const submitFranchiseContact = (
  payload: FranchiseContactPayload
): Promise<IObject> => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: REQUEST_URL + '/api/franchise-contacts',
      data: {
        ...payload,
        wxAppId: BaseEnum.APP_ID,
      },
      header: {
        'content-type': 'application/json',
      },
      method: 'POST',
      success({ statusCode, data }) {
        const response = (data || {}) as Record<string, any>;
        if (statusCode === 200 && response.code === 0) {
          resolve((response.data || {}) as IObject);
          return;
        }
        reject(response);
      },
      fail(error) {
        reject(error);
      },
    });
  });
};
