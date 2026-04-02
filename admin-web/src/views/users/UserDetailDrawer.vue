<template>
  <el-drawer :model-value="visible" :title="t('users.detail.title')" size="1080px" @close="emit('close')">
    <template v-if="detail">
      <div class="detail-stack">
        <div class="detail-grid">
          <div class="detail-card">
            <h3>{{ t('users.detail.basicInfo') }}</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('users.table.nickname')">
                {{ detail.nickname || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.table.userNo')">
                {{ detail.userNo || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.table.realName')">
                {{ detail.realName || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.table.mobile')">
                {{ detail.mobile || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.table.userStatus')">
                {{ formatUserStatus(detail.userStatus) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.table.registerSource')">
                {{ formatRegisterSource(detail.registerSource) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.table.member')">
                {{ formatBooleanFlag(detail.isMember) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.table.memberLevel')">
                {{ detail.memberLevel || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.detail.memberSinceTime')">
                {{ formatDateTime(detail.memberSinceTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.table.lastConsumeTime')">
                {{ formatDateTime(detail.lastConsumeTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.detail.lastLoginTime')">
                {{ formatDateTime(detail.lastLoginTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.table.createdAt')">
                {{ formatDateTime(detail.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.detail.remark')" :span="2">
                {{ detail.remark || t('common.noData') }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-card">
            <h3>{{ t('users.detail.todayDiscount') }}</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('users.detail.discountUsed')">
                {{ formatBooleanFlag(detail.todayFirstPeriodDiscountUsed) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.detail.discountDate')">
                {{ detail.todayFirstPeriodDiscountDate || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.detail.discountStore')">
                {{ detail.todayFirstPeriodDiscountStoreName || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.detail.discountOrderNo')">
                {{ detail.todayFirstPeriodDiscountOrderNo || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('users.detail.discountAmount')" :span="2">
                {{ formatAmount(detail.todayFirstPeriodDiscountAmount) }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>

        <div class="detail-card">
          <h3>{{ t('users.detail.walletAssets') }}</h3>
          <el-table v-if="detail.wallets.length > 0" :data="detail.wallets" border>
            <el-table-column prop="storeName" :label="t('users.wallet.store')" min-width="150">
              <template #default="{ row }">
                {{ row.storeName || t('common.noData') }}
              </template>
            </el-table-column>
            <el-table-column :label="t('users.wallet.principalBalance')" min-width="130" align="right">
              <template #default="{ row }">{{ formatAmount(row.principalBalance) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.wallet.availablePrincipalBalance')" min-width="150" align="right">
              <template #default="{ row }">{{ formatAmount(row.availablePrincipalBalance) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.wallet.giftBalance')" min-width="130" align="right">
              <template #default="{ row }">{{ formatAmount(row.giftBalance) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.wallet.availableGiftBalance')" min-width="150" align="right">
              <template #default="{ row }">{{ formatAmount(row.availableGiftBalance) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.wallet.status')" min-width="100">
              <template #default="{ row }">{{ formatWalletStatus(row.status) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.wallet.updatedAt')" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else :description="t('users.detail.noWallets')" />
        </div>

        <div class="detail-card">
          <h3>{{ t('users.detail.cards') }}</h3>
          <el-table v-if="detail.cards.length > 0" :data="detail.cards" border>
            <el-table-column prop="storeName" :label="t('users.cards.store')" min-width="150" />
            <el-table-column prop="cardNo" :label="t('users.cards.cardNo')" min-width="180" />
            <el-table-column prop="cardType" :label="t('users.cards.cardType')" min-width="120" />
            <el-table-column prop="sourceChannel" :label="t('users.cards.sourceChannel')" min-width="120" />
            <el-table-column prop="remainingTimes" :label="t('users.cards.remainingTimes')" min-width="100" />
            <el-table-column prop="usedTimes" :label="t('users.cards.usedTimes')" min-width="100" />
            <el-table-column prop="totalTimes" :label="t('users.cards.totalTimes')" min-width="100" />
            <el-table-column :label="t('users.cards.status')" min-width="120">
              <template #default="{ row }">{{ formatCardStatus(row.status) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.cards.expireTime')" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else :description="t('users.detail.noCards')" />
        </div>

        <div class="detail-card">
          <h3>{{ t('users.detail.recentOrders') }}</h3>
          <el-table v-if="detail.recentOrders.length > 0" :data="detail.recentOrders" border>
            <el-table-column prop="orderNo" :label="t('users.orders.orderNo')" min-width="180" />
            <el-table-column prop="storeName" :label="t('users.orders.store')" min-width="140" />
            <el-table-column prop="deviceName" :label="t('users.orders.device')" min-width="140" />
            <el-table-column :label="t('users.orders.payMode')" min-width="120">
              <template #default="{ row }">{{ formatPayMode(row.payMode) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.orders.paymentStatus')" min-width="100">
              <template #default="{ row }">{{ formatPaymentStatus(row.paymentStatus) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.orders.orderStatus')" min-width="100">
              <template #default="{ row }">{{ formatOrderStatus(row.orderStatus) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.orders.finalAmount')" min-width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.finalAmount) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.orders.createdAt')" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else :description="t('users.detail.noOrders')" />
        </div>

        <div class="detail-card">
          <h3>{{ t('users.detail.recentWalletTransactions') }}</h3>
          <el-table v-if="detail.recentWalletTransactions.length > 0" :data="detail.recentWalletTransactions" border>
            <el-table-column prop="transactionNo" :label="t('users.walletTransactions.transactionNo')" min-width="180" />
            <el-table-column prop="storeName" :label="t('users.walletTransactions.store')" min-width="140" />
            <el-table-column :label="t('users.walletTransactions.bizType')" min-width="120">
              <template #default="{ row }">{{ formatWalletBizType(row.bizType) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.walletTransactions.amountType')" min-width="120">
              <template #default="{ row }">{{ formatAmountType(row.amountType) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.walletTransactions.changeType')" min-width="120">
              <template #default="{ row }">{{ formatWalletChangeType(row.changeType) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.walletTransactions.amount')" min-width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.walletTransactions.balanceAfter')" min-width="120" align="right">
              <template #default="{ row }">{{ formatAmount(row.balanceAfter) }}</template>
            </el-table-column>
            <el-table-column prop="relatedOrderNo" :label="t('users.walletTransactions.relatedOrderNo')" min-width="160" />
            <el-table-column :label="t('users.walletTransactions.createdAt')" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else :description="t('users.detail.noWalletTransactions')" />
        </div>

        <div class="detail-card">
          <h3>{{ t('users.detail.recentCardUsages') }}</h3>
          <el-table v-if="detail.recentCardUsages.length > 0" :data="detail.recentCardUsages" border>
            <el-table-column prop="usageNo" :label="t('users.cardUsages.usageNo')" min-width="180" />
            <el-table-column prop="storeName" :label="t('users.cardUsages.store')" min-width="140" />
            <el-table-column prop="orderNo" :label="t('users.cardUsages.orderNo')" min-width="180" />
            <el-table-column prop="usedTimes" :label="t('users.cardUsages.usedTimes')" min-width="100" />
            <el-table-column :label="t('users.cardUsages.usageTime')" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.usageTime) }}</template>
            </el-table-column>
            <el-table-column :label="t('users.cardUsages.createdAt')" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else :description="t('users.detail.noCardUsages')" />
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { t } from '@/i18n';
import type { AdminUserOverview } from '@/types/user';
import {
  formatAmount,
  formatAmountType,
  formatBooleanFlag,
  formatCardStatus,
  formatDateTime,
  formatOrderStatus,
  formatPayMode,
  formatPaymentStatus,
  formatRegisterSource,
  formatUserStatus,
  formatWalletBizType,
  formatWalletChangeType,
  formatWalletStatus,
} from '@/utils/format';

defineProps<{
  visible: boolean;
  detail: AdminUserOverview | null;
}>();

const emit = defineEmits<{
  close: [];
}>();
</script>

<style scoped lang="scss">
.detail-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-card h3 {
  margin: 0;
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
