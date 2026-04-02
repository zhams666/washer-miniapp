import { t } from '@/i18n';

export const formatDateTime = (value?: string) => {
  if (!value) {
    return t('common.noData');
  }

  return value.replace('T', ' ').slice(0, 19);
};

export const formatAmount = (value?: number | null) => {
  if (value === undefined || value === null) {
    return t('common.noData');
  }

  return Number(value).toFixed(2);
};

export const formatOrderStatus = (value?: string) => {
  const map: Record<string, string> = {
    initial: t('status.initial'),
    pending: t('status.pending'),
    running: t('status.running'),
    completed: t('status.completed'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatPaymentStatus = (value?: string) => {
  const map: Record<string, string> = {
    paid: t('status.paid'),
    unpaid: t('status.unpaid'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatPayMode = (value?: string) => {
  const map: Record<string, string> = {
    wallet: t('payMode.wallet'),
    card: t('payMode.card'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatOrderSource = (value?: string) => {
  const map: Record<string, string> = {
    miniapp: t('source.miniapp'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatSourceType = (value?: string) => {
  const map: Record<string, string> = {
    wallet: t('source.wallet'),
    card: t('source.card'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatAmountType = (value?: string) => {
  const map: Record<string, string> = {
    principal: t('amountType.principal'),
    card: t('amountType.card'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatSettleStage = (value?: string) => {
  const map: Record<string, string> = {
    final: t('settleStage.final'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatActionType = (value?: string) => {
  const map: Record<string, string> = {
    create: t('actionType.create'),
    start: t('actionType.start'),
    finish: t('actionType.finish'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatPaymentStatusDesc = (value?: string) => {
  const map: Record<string, string> = {
    'wallet principal paid': t('paymentStatusDesc.walletPrincipalPaid'),
    'card paid': t('paymentStatusDesc.cardPaid'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatBooleanFlag = (value?: number | boolean | null) => {
  if (value === null || value === undefined) {
    return t('common.noData');
  }

  return Number(value) === 1 || value === true ? t('common.yes') : t('common.no');
};

export const formatOperatorType = (value?: string) => {
  const map: Record<string, string> = {
    user: t('operatorType.user'),
    admin: t('operatorType.admin'),
    system: t('operatorType.system'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatAllocationStrategy = (value?: string) => {
  const map: Record<string, string> = {
    manual: t('allocationStrategy.manual'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatDeviceStatus = (value?: string) => {
  const map: Record<string, string> = {
    online: t('deviceStatus.online'),
    offline: t('deviceStatus.offline'),
    running: t('deviceStatus.running'),
    idle: t('deviceStatus.idle'),
    paused: t('deviceStatus.paused'),
    fault: t('deviceStatus.fault'),
    disabled: t('deviceStatus.disabled'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatStoreStatus = (value?: number | null) => {
  if (value === null || value === undefined) {
    return t('common.noData');
  }

  return Number(value) === 1 ? t('storeStatus.enabled') : t('storeStatus.disabled');
};

export const formatUserStatus = (value?: number | null) => {
  if (value === null || value === undefined) {
    return t('common.noData');
  }

  return Number(value) === 1 ? t('userStatus.enabled') : t('userStatus.disabled');
};

export const formatRegisterSource = (value?: string) => {
  const map: Record<string, string> = {
    miniapp: t('registerSource.miniapp'),
    admin: t('registerSource.admin'),
    import: t('registerSource.import'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatWalletStatus = (value?: number | null) => {
  if (value === null || value === undefined) {
    return t('common.noData');
  }

  return Number(value) === 1 ? t('walletStatus.enabled') : t('walletStatus.disabled');
};

export const formatCardStatus = (value?: string) => {
  const map: Record<string, string> = {
    active: t('cardStatus.active'),
    used_up: t('cardStatus.used_up'),
    expired: t('cardStatus.expired'),
    cancelled: t('cardStatus.cancelled'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatWalletBizType = (value?: string) => {
  const map: Record<string, string> = {
    recharge: t('walletBizType.recharge'),
    consume: t('walletBizType.consume'),
    refund: t('walletBizType.refund'),
    adjust: t('walletBizType.adjust'),
    clear_gift: t('walletBizType.clear_gift'),
  };

  return value ? (map[value] || value) : t('common.noData');
};

export const formatWalletChangeType = (value?: string) => {
  const map: Record<string, string> = {
    in: t('walletChangeType.in'),
    out: t('walletChangeType.out'),
  };

  return value ? (map[value] || value) : t('common.noData');
};
