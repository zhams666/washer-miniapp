<template>
  <div class="activity-page">
    <section class="activity-hero">
      <div>
        <p class="activity-hero__eyebrow">LIVE FEED</p>
        <h3>业务动态</h3>
        <span>集中查看钱包充值、钱包消费、次卡购买、后台发卡和次卡核销记录，首页只保留核心经营看板。</span>
      </div>
      <div class="activity-hero__stats">
        <article v-for="stat in activityStats" :key="stat.key">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </article>
      </div>
    </section>

    <section class="filter-bar">
      <el-form class="filter-form" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :md="10">
            <el-form-item label="日期范围">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                :clearable="false"
                :unlink-panels="true"
                style="width: 100%"
                @change="loadActivities"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="5">
            <el-form-item label="业务类型">
              <el-select v-model="selectedType" placeholder="全部类型" clearable>
                <el-option v-for="option in typeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="5">
            <el-form-item label="拉取数量">
              <el-select v-model="limit">
                <el-option label="最近 50 条" :value="50" />
                <el-option label="最近 100 条" :value="100" />
                <el-option label="最近 200 条" :value="200" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="4">
            <el-form-item label="操作">
              <el-button type="primary" :loading="loading" style="width: 100%" @click="loadActivities">刷新</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </section>

    <section class="table-card activity-table-card">
      <el-table :data="filteredActivities" v-loading="loading" stripe>
        <el-table-column label="业务类型" min-width="128">
          <template #default="{ row }">
            <el-tag :type="resolveTagType(row.type)" effect="light">
              {{ row.title || formatActivityType(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="storeName" label="门店" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.storeName || '未知门店' }}</template>
        </el-table-column>
        <el-table-column prop="referenceNo" label="关联单号/卡号" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.referenceNo || '--' }}</template>
        </el-table-column>
        <el-table-column label="金额/次数" min-width="150" align="right">
          <template #default="{ row }">
            <strong class="activity-value">{{ formatActivityValue(row) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="occurredAt" label="发生时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.occurredAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!filteredActivities.length && !loading" description="暂无业务动态" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { fetchDashboardActivities } from '@/api/dashboard';
import type { AdminDashboardActivityItem } from '@/types/dashboard';
import { formatDateTime } from '@/utils/format';

type ActivityStat = {
  key: string;
  label: string;
  value: string;
};

const typeOptions = [
  { label: '次卡核销', value: 'cardUsage' },
  { label: '钱包消费', value: 'walletConsume' },
  { label: '钱包充值', value: 'walletRecharge' },
  { label: '次卡购买/发卡', value: 'cardPurchase' },
];

const loading = ref(false);
const activities = ref<AdminDashboardActivityItem[]>([]);
const selectedType = ref('');
const limit = ref(100);
const today = formatLocalDate(new Date());
const dateRange = ref<[string, string]>([addDays(today, -6), today]);

const filteredActivities = computed(() => {
  if (!selectedType.value) {
    return activities.value;
  }
  return activities.value.filter((activity) => activity.type === selectedType.value);
});

const activityStats = computed<ActivityStat[]>(() => {
  const cardUsageTimes = activities.value
    .filter((activity) => activity.type === 'cardUsage')
    .reduce((sum, activity) => sum + Number(activity.times || 0), 0);
  const walletConsumeAmount = sumAmount('walletConsume');
  const walletRechargeAmount = sumAmount('walletRecharge');
  const cardPurchaseCount = activities.value.filter((activity) => activity.type === 'cardPurchase').length;

  return [
    { key: 'total', label: '动态总数', value: `${activities.value.length}` },
    { key: 'cardUsage', label: '核销次数', value: `${cardUsageTimes}` },
    { key: 'wallet', label: '钱包流水', value: `¥${formatMoney(walletConsumeAmount + walletRechargeAmount)}` },
    { key: 'cardPurchase', label: '购卡/发卡', value: `${cardPurchaseCount}` },
  ];
});

async function loadActivities() {
  loading.value = true;
  try {
    const [startDate, endDate] = normalizeDateRange(dateRange.value);
    activities.value = await fetchDashboardActivities({
      startDate,
      endDate,
      limit: limit.value,
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '业务动态加载失败');
  } finally {
    loading.value = false;
  }
}

function sumAmount(type: string) {
  return activities.value
    .filter((activity) => activity.type === type)
    .reduce((sum, activity) => sum + Number(activity.amount || 0), 0);
}

function formatLocalDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseDate(value: string) {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function addDays(value: string, days: number) {
  const date = parseDate(value);
  date.setDate(date.getDate() + days);
  return formatLocalDate(date);
}

function normalizeDateRange(range: [string, string]): [string, string] {
  const [startDate, endDate] = range;
  return startDate <= endDate ? [startDate, endDate] : [endDate, startDate];
}

function formatMoney(value?: number | null) {
  return Number(value || 0).toFixed(2);
}

function formatActivityType(type?: string) {
  const map: Record<string, string> = {
    cardUsage: '次卡核销',
    walletConsume: '钱包消费',
    walletRecharge: '钱包充值',
    cardPurchase: '次卡购买',
  };
  return type ? map[type] || type : '业务动态';
}

function formatActivityValue(activity: AdminDashboardActivityItem) {
  if (activity.type === 'cardUsage') {
    return `${Number(activity.times || 0)} 次`;
  }
  if (activity.type === 'cardPurchase') {
    const amount = Number(activity.amount || 0);
    const times = Number(activity.times || 0);
    return amount > 0 ? `¥${formatMoney(amount)} / ${times} 张` : `${times} 张`;
  }
  return `¥${formatMoney(activity.amount)}`;
}

function resolveTagType(type?: string) {
  const map: Record<string, 'success' | 'warning' | 'primary' | 'danger' | 'info'> = {
    cardUsage: 'success',
    walletConsume: 'warning',
    walletRecharge: 'primary',
    cardPurchase: 'danger',
  };
  return type ? map[type] || 'info' : 'info';
}

onMounted(() => {
  void loadActivities();
});
</script>

<style scoped lang="scss">
.activity-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.activity-hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 24px;
  overflow: hidden;
  padding: 24px;
  border: 1px solid rgba(17, 40, 61, 0.08);
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(17, 40, 61, 0.96), rgba(18, 104, 89, 0.92)),
    #11283d;
  color: #fff;
  box-shadow: 0 18px 52px rgba(31, 41, 55, 0.14);
}

.activity-hero__eyebrow {
  margin: 0 0 8px;
  color: #75f0cf;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.activity-hero h3 {
  margin: 0;
  font-size: 30px;
}

.activity-hero span {
  display: block;
  max-width: 560px;
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.74);
  line-height: 1.7;
}

.activity-hero__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(112px, 1fr));
  gap: 12px;
  min-width: min(560px, 100%);
}

.activity-hero__stats article {
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
}

.activity-hero__stats span {
  margin: 0 0 12px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
}

.activity-hero__stats strong {
  display: block;
  font-size: 26px;
}

.activity-table-card {
  min-height: 420px;
}

.activity-value {
  color: #1d2939;
}

@media (max-width: 1180px) {
  .activity-hero {
    flex-direction: column;
  }

  .activity-hero__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .activity-hero__stats {
    grid-template-columns: 1fr;
  }
}
</style>
