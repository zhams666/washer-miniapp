<template>
  <el-drawer
    :model-value="visible"
    :title="t('orders.detail.title')"
    size="960px"
    @close="emit('close')"
  >
    <template v-if="detail">
      <div class="detail-stack">
        <div class="detail-grid">
          <div class="detail-card">
            <h3>{{ t('orders.detail.overview') }}</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('orders.detail.orderNo')">{{ detail.orderNo }}</el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.userId')">{{ detail.userId }}</el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.storeId')">{{ detail.storeId }}</el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.store')">
                {{ detail.storeName || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.deviceId')">
                {{ detail.deviceId ?? t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.deviceCode')">
                {{ detail.deviceCode || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.device')">
                {{ detail.deviceName || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.deviceStatus')">
                {{ formatDeviceStatus(detail.deviceStatus) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.status')">
                {{ formatOrderStatus(detail.orderStatus) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.payment')">
                {{ formatPaymentStatus(detail.paymentStatus) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.payMode')">
                {{ formatPayMode(detail.payMode) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.source')">
                {{ formatOrderSource(detail.orderSource) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.created')">
                {{ formatDateTime(detail.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.started')">
                {{ formatDateTime(detail.startTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.ended')">
                {{ formatDateTime(detail.endTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.settled')">
                {{ formatDateTime(detail.settleTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.remark')" :span="2">
                {{ detail.remark || t('common.noData') }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-card">
            <h3>{{ t('orders.detail.amounts') }}</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('orders.detail.estimated')">
                {{ formatAmount(detail.estimatedAmount) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.final')">
                {{ formatAmount(detail.finalAmount) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.paid')">
                {{ formatAmount(detail.paidAmount) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.discountAmount')">
                {{ formatAmount(detail.firstPeriodDiscountAmount) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.discountUsed')">
                {{ formatBooleanFlag(detail.isFirstPeriodDiscountUsed) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.discountDesc')">
                {{ detail.firstPeriodDiscountDesc || t('common.noData') }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.refund')">
                {{ formatAmount(detail.refundAmount) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.payDesc')" :span="2">
                {{ formatPaymentStatusDesc(detail.paymentStatusDesc) }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('orders.detail.abnormal')" :span="2">
                {{ detail.abnormalReason || t('common.noData') }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>

        <div class="detail-card">
          <h3>{{ t('orders.detail.paymentInfo') }}</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="t('orders.detail.cardUsageId')">
              {{ detail.cardUsageId ?? t('common.noData') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('orders.detail.cardDeductTimes')">
              {{ detail.cardDeductTimes ?? t('common.noData') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('orders.detail.payment')">
              {{ formatPaymentStatus(detail.paymentStatus) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('orders.detail.payMode')">
              {{ formatPayMode(detail.payMode) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-card">
          <h3>{{ t('orders.detail.timeline') }}</h3>

          <div v-if="detail.statusLogs.length > 0" class="timeline-list">
            <el-timeline>
              <el-timeline-item
                v-for="item in detail.statusLogs"
                :key="item.id"
                :timestamp="formatDateTime(item.createdAt)"
              >
                <div class="timeline-record">
                  <div class="timeline-record__title">
                    {{ formatOrderStatus(item.fromStatus || 'initial') }}
                    ->
                    {{ formatOrderStatus(item.toStatus) }}
                  </div>
                  <div class="timeline-record__meta">
                    {{ formatActionType(item.actionType) }}
                  </div>
                  <div class="timeline-record__meta">
                    {{ t('orders.detail.operatorType') }}:
                    {{ formatOperatorType(item.operatorType) }}
                    <span class="timeline-record__split">|</span>
                    {{ t('orders.detail.operatorId') }}:
                    {{ item.operatorId ?? t('common.noData') }}
                  </div>
                  <div class="timeline-record__remark">
                    {{ item.remark || t('common.noData') }}
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>

          <el-empty
            v-else
            :description="t('orders.detail.noStatusLogs')"
          />
        </div>

        <div class="detail-card">
          <h3>{{ t('orders.detail.paymentDetails') }}</h3>

          <el-table
            v-if="detail.paymentDetails.length > 0"
            :data="detail.paymentDetails"
            border
          >
            <el-table-column :label="t('orders.detail.sourceType')" min-width="120">
              <template #default="{ row }">
                {{ formatSourceType(row.sourceType) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('orders.detail.amountType')" min-width="120">
              <template #default="{ row }">
                {{ formatAmountType(row.amountType) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('orders.detail.amount')" min-width="100" align="right">
              <template #default="{ row }">
                {{ formatAmount(row.amount) }}
              </template>
            </el-table-column>
            <el-table-column prop="userCardId" :label="t('orders.detail.userCardId')" min-width="130" />
            <el-table-column prop="deductTimes" :label="t('orders.detail.times')" min-width="80" />
            <el-table-column prop="paymentSeq" :label="t('orders.detail.paymentSeq')" min-width="100" />
            <el-table-column :label="t('orders.detail.settleStage')" min-width="120">
              <template #default="{ row }">
                {{ formatSettleStage(row.settleStage) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('orders.detail.allocationStrategy')" min-width="120">
              <template #default="{ row }">
                {{ formatAllocationStrategy(row.allocationStrategy) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('orders.detail.refundedAmount')" min-width="120" align="right">
              <template #default="{ row }">
                {{ formatAmount(row.refundedAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="bizActionNo" :label="t('orders.detail.bizActionNo')" min-width="220" />
            <el-table-column :label="t('orders.detail.created')" min-width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>

          <el-empty
            v-else
            :description="t('orders.detail.noPaymentDetails')"
          />
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { t } from '@/i18n';
import type { AdminOrderDetail } from '@/types/order';
import {
  formatActionType,
  formatAllocationStrategy,
  formatAmount,
  formatAmountType,
  formatBooleanFlag,
  formatDateTime,
  formatDeviceStatus,
  formatOperatorType,
  formatOrderSource,
  formatOrderStatus,
  formatPayMode,
  formatPaymentStatus,
  formatPaymentStatusDesc,
  formatSettleStage,
  formatSourceType,
} from '@/utils/format';

defineProps<{
  visible: boolean;
  detail: AdminOrderDetail | null;
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

.timeline-list {
  padding-top: 4px;
}

.timeline-record {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.timeline-record__title {
  font-weight: 600;
  color: #182230;
}

.timeline-record__meta {
  color: #526071;
  line-height: 1.6;
}

.timeline-record__remark {
  color: #1d2939;
  line-height: 1.6;
}

.timeline-record__split {
  display: inline-block;
  margin: 0 8px;
  color: #98a2b3;
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
