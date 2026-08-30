<template>
  <div class="dashboard-page" v-loading="loading">
    <section class="dashboard-header">
      <div class="dashboard-header__copy">
        <p>Operations Dashboard</p>
        <h2>数据统计</h2>
        <span>洗车、钱包、充值、次卡、团购核销和商家余额统一汇总，数据来自后台实时接口。</span>
      </div>

      <div class="dashboard-header__actions">
        <el-select
          v-model="selectedStoreId"
          size="large"
          class="dashboard-store-select"
          placeholder="全部门店"
          @change="handleStoreChange"
        >
          <el-option label="全部门店" value="" />
          <el-option
            v-for="store in storeOptions"
            :key="store.id"
            :label="store.storeName"
            :value="store.id"
          />
        </el-select>
        <el-radio-group v-model="rangePreset" size="large" @change="applyPreset">
          <el-radio-button label="today">今日</el-radio-button>
          <el-radio-button label="7d">近 7 天</el-radio-button>
          <el-radio-button label="30d">近 30 天</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
          :unlink-panels="true"
          @change="handleDateChange"
        />
        <el-button type="primary" size="large" :loading="loading" @click="loadDashboard">刷新</el-button>
      </div>
    </section>

    <section class="focus-grid">
      <article class="wash-focus">
        <div>
          <span>{{ isSingleDay ? '今日洗车' : '区间洗车' }}</span>
          <strong>{{ formatInteger(kpiCount('washCount')) }}<em>辆</em></strong>
          <small>{{ selectedPeriodLabel }}</small>
        </div>
        <div class="wash-focus__meta">
          <b>{{ formatGeneratedAt(overview?.generatedAt) }}</b>
          <span>实时接口更新时间</span>
        </div>
      </article>

      <article
        v-for="metric in topMetricCards"
        :key="metric.key"
        class="metric-card"
        :style="metricStyle(metric.tone)"
      >
        <div class="metric-card__title">
          <span>{{ metric.title }}</span>
          <i />
        </div>
        <strong>{{ metric.value }}<em>{{ metric.unit }}</em></strong>
        <small>{{ metric.description }}</small>
      </article>
    </section>

    <section class="range-board">
      <article v-for="metric in rangeMetricCards" :key="metric.key" class="range-cell">
        <span>{{ metric.title }}</span>
        <strong>{{ formatKpiValue(metric) }}<em>{{ metric.unit || defaultKpiUnit(metric) }}</em></strong>
        <small>{{ metric.description || '当前统计区间' }}</small>
      </article>
    </section>

    <section class="chart-grid">
      <article v-for="chart in trendCards" :key="chart.key" class="trend-panel" :style="chartStyle(chart)">
        <header class="panel-heading">
          <div>
            <p>{{ chart.eyebrow }}</p>
            <h3>{{ chart.title }}</h3>
          </div>
          <strong>{{ chart.totalText }}<em>{{ chart.unit }}</em></strong>
        </header>

        <div class="trend-plot">
          <span v-for="line in 4" :key="line" class="trend-plot__line" :style="{ bottom: `${line * 20}%` }" />
          <div v-for="(point, index) in chartPoints" :key="`${chart.key}-${point.date}`" class="trend-bar">
            <span :class="{ 'is-empty': trendValue(point, chart.key) <= 0 }">
              {{ formatChartValue(trendValue(point, chart.key), chart.money) }}
            </span>
            <i :style="{ height: trendBarHeight(chart, point) }" />
            <small>{{ shouldShowTrendLabel(index) ? point.label : '' }}</small>
          </div>
        </div>
      </article>
    </section>

    <section class="insight-grid">
      <article class="insight-panel">
        <header class="panel-heading">
          <div>
            <p>Store Rank</p>
            <h3>门店业务排行</h3>
          </div>
          <el-tag effect="plain">{{ selectedPeriodLabel }}</el-tag>
        </header>

        <div class="store-rank">
          <div v-for="store in storeMetrics" :key="store.storeId || store.storeName" class="store-rank__item">
            <div class="store-rank__main">
              <strong>{{ store.storeName || '未知门店' }}</strong>
              <span>消费 {{ formatMoney(store.walletConsumeAmount) }} / 充值 {{ formatMoney(store.walletRechargeAmount) }}</span>
            </div>
            <div class="store-rank__bar">
              <i :style="{ width: storeBarWidth(store) }" />
            </div>
            <div class="store-rank__side">
              <b>{{ Number(store.cardUsageTimes || 0) }}</b>
              <span>核销</span>
            </div>
          </div>
          <el-empty v-if="!storeMetrics.length && !loading" description="暂无门店统计" />
        </div>
      </article>

      <article class="insight-panel">
        <header class="panel-heading">
          <div>
            <p>Card Channel</p>
            <h3>次卡购买来源</h3>
          </div>
        </header>

        <div class="channel-list">
          <div v-for="channel in cardPurchaseChannels" :key="channel.channel || 'unknown'" class="channel-item">
            <div>
              <strong>{{ formatChannel(channel.channel) }}</strong>
              <span>{{ Number(channel.orderCount || 0) }} 单 / 发卡 {{ Number(channel.cardCount || 0) }} 张</span>
            </div>
            <b>¥{{ formatMoney(channel.payAmount) }}</b>
          </div>
          <el-empty v-if="!cardPurchaseChannels.length && !loading" description="暂无购卡数据" />
        </div>
      </article>
    </section>

    <section class="device-health-panel" :class="{ 'has-alert': deviceAbnormalCount > 0 }">
      <header class="panel-heading">
        <div>
          <p>Device Health</p>
          <h3>设备故障情况</h3>
        </div>
        <el-tag :type="deviceAbnormalCount > 0 ? 'danger' : 'success'" effect="light" round>
          {{ deviceAbnormalCount > 0 ? `${deviceAbnormalCount} 台异常` : '全部正常' }}
        </el-tag>
      </header>

      <div class="device-health-layout">
        <div class="device-health-summary">
          <article>
            <span>设备总数</span>
            <strong>{{ formatInteger(deviceCount('totalCount')) }}<em>台</em></strong>
            <small>当前门店筛选范围</small>
          </article>
          <article class="is-running">
            <span>运行/空闲</span>
            <strong>{{ formatInteger(deviceCount('runningCount') + deviceCount('idleCount')) }}<em>台</em></strong>
            <small>可继续服务设备</small>
          </article>
          <article class="is-danger">
            <span>故障设备</span>
            <strong>{{ formatInteger(deviceCount('faultCount')) }}<em>台</em></strong>
            <small>需要优先处理</small>
          </article>
          <article class="is-warning">
            <span>离线/停用</span>
            <strong>
              {{ formatInteger(deviceCount('offlineCount') + deviceCount('disabledCount') + deviceCount('pausedCount')) }}<em>台</em>
            </strong>
            <small>无法接单设备</small>
          </article>
        </div>

        <div class="device-alert-list">
          <div v-for="(device, index) in deviceAlerts" :key="device.deviceId || device.deviceCode || index" class="device-alert-item">
            <div class="device-alert-item__main">
              <strong>{{ device.deviceName || device.deviceCode || '未命名设备' }}</strong>
              <span>{{ device.storeName || '未知门店' }} · {{ device.deviceCode || '--' }}</span>
            </div>
            <el-tag :type="deviceStatusTagType(device.status)" effect="light" round>
              {{ formatDeviceStatusLabel(device.status) }}
            </el-tag>
            <span class="device-alert-item__time">{{ formatDeviceTime(device.lastHeartbeatTime || device.updatedAt) }}</span>
          </div>
          <el-empty v-if="!deviceAlerts.length && !loading" description="暂无故障或离线设备" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { fetchTodayDashboard } from '@/api/dashboard';
import { fetchAdminStoreOptions } from '@/api/store';
import type {
  AdminDashboardChannelMetric,
  AdminDashboardDailyTrendPoint,
  AdminDashboardDeviceAlertItem,
  AdminDashboardDeviceStatus,
  AdminDashboardKpiItem,
  AdminDashboardStoreMetric,
  AdminDashboardTodayOverview,
} from '@/types/dashboard';
import type { StoreOption } from '@/types/store';

type RangePreset = 'today' | '7d' | '30d' | 'custom';
type TrendValueKey = 'rechargeAmount' | 'consumeAmount' | 'cardUsageTimes' | 'groupVerifyCount';
type DeviceCountKey =
  | 'totalCount'
  | 'normalCount'
  | 'runningCount'
  | 'idleCount'
  | 'faultCount'
  | 'offlineCount'
  | 'disabledCount'
  | 'pausedCount'
  | 'abnormalCount';

type TrendCard = {
  key: TrendValueKey;
  title: string;
  eyebrow: string;
  unit: string;
  money: boolean;
  color: string;
  softColor: string;
  totalText: string;
};

type TopMetricCard = {
  key: string;
  title: string;
  value: string;
  unit: string;
  description: string;
  tone: string;
};

const moneyMetricKeys = new Set([
  'todayConsume',
  'todayRecharge',
  'rechargeAmount',
  'consumeAmount',
  'merchantTotalBalance',
  'merchantPrincipalBalance',
  'merchantGiftBalance',
]);
const rangeMetricOrder = [
  'washCount',
  'cardUsageTimes',
  'groupVerifyRange',
  'rechargeAmount',
  'consumeAmount',
  'walletOrderCount',
  'monthlyCardOrderCount',
  'cardOrderCount',
  'merchantTotalBalance',
  'merchantPrincipalBalance',
  'merchantGiftBalance',
  'merchantUsers',
];

const metricToneMap: Record<string, { color: string; bg: string }> = {
  blue: { color: '#2563eb', bg: '#eff6ff' },
  amber: { color: '#d97706', bg: '#fff7ed' },
  green: { color: '#059669', bg: '#ecfdf5' },
  violet: { color: '#7c3aed', bg: '#f5f3ff' },
};

const loading = ref(false);
const overview = ref<AdminDashboardTodayOverview | null>(null);
const trendOverview = ref<AdminDashboardTodayOverview | null>(null);
const rangePreset = ref<RangePreset>('today');
const today = formatLocalDate(new Date());
const dateRange = ref<[string, string]>([today, today]);
const storeOptions = ref<StoreOption[]>([]);
const selectedStoreId = ref<number | ''>('');
let refreshTimer: number | null = null;

const isSingleDay = computed(() => dateRange.value[0] === dateRange.value[1]);
const selectedPeriodLabel = computed(() => `${dateRange.value[0]} 至 ${dateRange.value[1]}`);
const topMetrics = computed(() => overview.value?.topMetrics || []);
const rangeMetrics = computed(() => overview.value?.rangeMetrics || []);
const chartPoints = computed(() => trendOverview.value?.dailyTrend?.length ? trendOverview.value.dailyTrend : buildEmptyDailyTrend());
const storeMetrics = computed(() => overview.value?.storeMetrics || []);
const cardPurchaseChannels = computed(() => overview.value?.cardPurchaseChannels || []);
const deviceStatus = computed<AdminDashboardDeviceStatus>(() => overview.value?.deviceStatus || { alerts: [] });
const deviceAlerts = computed<AdminDashboardDeviceAlertItem[]>(() => deviceStatus.value.alerts || []);
const deviceAbnormalCount = computed(() => deviceCount('abnormalCount'));

const topMetricCards = computed<TopMetricCard[]>(() => [
  buildTopMetricCard('todayConsume', isSingleDay.value ? '今日消费' : '区间消费', 'amount', 'blue'),
  buildTopMetricCard('todayRecharge', isSingleDay.value ? '今日充值' : '区间充值', 'amount', 'amber'),
  buildTopMetricCard('groupVerify', '团购核销(商)', 'count', 'green'),
  buildTopMetricCard('newUsers', isSingleDay.value ? '今日新增用户' : '区间新增用户', 'count', 'violet'),
]);

const rangeMetricCards = computed<AdminDashboardKpiItem[]>(() => (
  rangeMetricOrder.map((key) => normalizeKpi(key, rangeMetrics.value.find((item) => item.key === key)))
));

const trendCards = computed<TrendCard[]>(() => {
  const baseCards: Array<Omit<TrendCard, 'totalText'>> = [
    {
      key: 'rechargeAmount',
      title: '充值金额',
      eyebrow: 'Recharge Trend',
      unit: '元',
      money: true,
      color: '#2563eb',
      softColor: 'rgba(37, 99, 235, 0.16)',
    },
    {
      key: 'consumeAmount',
      title: '消费金额',
      eyebrow: 'Consume Trend',
      unit: '元',
      money: true,
      color: '#f97316',
      softColor: 'rgba(249, 115, 22, 0.16)',
    },
    {
      key: 'cardUsageTimes',
      title: '次卡使用',
      eyebrow: 'Card Usage',
      unit: '次',
      money: false,
      color: '#059669',
      softColor: 'rgba(5, 150, 105, 0.16)',
    },
    {
      key: 'groupVerifyCount',
      title: '团购核销',
      eyebrow: 'Group Verify',
      unit: '次',
      money: false,
      color: '#7c3aed',
      softColor: 'rgba(124, 58, 237, 0.16)',
    },
  ];

  return baseCards.map((card) => {
    const total = chartPoints.value.reduce((sum, point) => sum + trendValue(point, card.key), 0);
    return {
      ...card,
      totalText: card.money ? formatCompactAmount(total) : formatInteger(total),
    };
  });
});

const maxStoreScore = computed(() => {
  const values = storeMetrics.value.map((store) => resolveStoreScore(store));
  return Math.max(1, ...values);
});

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

function applyPreset() {
  const endDate = today;
  if (rangePreset.value === '7d') {
    dateRange.value = [addDays(endDate, -6), endDate];
  } else if (rangePreset.value === '30d') {
    dateRange.value = [addDays(endDate, -29), endDate];
  } else {
    dateRange.value = [endDate, endDate];
  }
  void loadDashboard();
}

function handleDateChange() {
  rangePreset.value = 'custom';
  void loadDashboard();
}

function handleStoreChange() {
  void loadDashboard();
}

async function loadDashboard(options?: { silent?: boolean }) {
  const silent = Boolean(options?.silent);
  if (silent && loading.value) {
    return;
  }
  if (!silent) {
    loading.value = true;
  }
  try {
    const [startDate, endDate] = normalizeDateRange(dateRange.value);
    const trendStartDate = addDays(endDate, -7);
    const storeId = selectedStoreId.value === '' ? undefined : Number(selectedStoreId.value);
    const [summary, trend] = await Promise.all([
      fetchTodayDashboard({ startDate, endDate, storeId }),
      fetchTodayDashboard({ startDate: trendStartDate, endDate, storeId }),
    ]);
    overview.value = summary;
    trendOverview.value = trend;
  } catch (error) {
    if (!silent) {
      ElMessage.error(error instanceof Error ? error.message : '首页数据加载失败');
    }
  } finally {
    if (!silent) {
      loading.value = false;
    }
  }
}

async function loadStoreOptions() {
  try {
    storeOptions.value = await fetchAdminStoreOptions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '门店列表加载失败');
  }
}

function normalizeDateRange(range: [string, string]): [string, string] {
  const [startDate, endDate] = range;
  return startDate <= endDate ? [startDate, endDate] : [endDate, startDate];
}

function normalizeKpi(key: string, item?: AdminDashboardKpiItem): AdminDashboardKpiItem {
  if (item) {
    return item;
  }
  const titles: Record<string, string> = {
    washCount: '洗车数量',
    cardUsageTimes: '次卡使用',
    groupVerifyRange: '团购核销(商)',
    rechargeAmount: '充值金额',
    consumeAmount: '消费金额',
    walletOrderCount: '余额订单数',
    monthlyCardOrderCount: '月卡订单数',
    cardOrderCount: '次卡订单数',
    merchantTotalBalance: '商家总余额',
    merchantPrincipalBalance: '本金余额',
    merchantGiftBalance: '赠送余额',
    merchantUsers: '用户总数',
  };
  return {
    key,
    title: titles[key] || key,
    amount: 0,
    count: 0,
    unit: defaultKpiUnit({ key }),
    description: '暂无数据',
  };
}

function findMetric(items: AdminDashboardKpiItem[], key: string) {
  return items.find((item) => item.key === key);
}

function buildTopMetricCard(key: string, title: string, valueType: 'amount' | 'count', tone: string): TopMetricCard {
  const metric = findMetric(topMetrics.value, key);
  const value = valueType === 'amount' ? formatCompactAmount(Number(metric?.amount || 0)) : formatInteger(Number(metric?.count || 0));
  return {
    key,
    title,
    value,
    unit: metric?.unit || (valueType === 'amount' ? '元' : ''),
    description: metric?.description || '暂无数据',
    tone,
  };
}

function kpiCount(key: string) {
  const metric = findMetric(rangeMetrics.value, key);
  return Number(metric?.count || 0);
}

function deviceCount(key: DeviceCountKey) {
  return Number(deviceStatus.value?.[key] || 0);
}

function formatDeviceStatusLabel(value?: string) {
  const status = String(value || '').trim().toLowerCase();
  const map: Record<string, string> = {
    online: '在线',
    offline: '离线',
    running: '运行中',
    idle: '空闲',
    paused: '暂停',
    fault: '故障',
    disabled: '停用',
  };
  return map[status] || (value || '未知');
}

function deviceStatusTagType(value?: string) {
  const status = String(value || '').trim().toLowerCase();
  if (status === 'fault' || status === 'disabled') {
    return 'danger';
  }
  if (status === 'offline' || status === 'paused') {
    return 'warning';
  }
  if (status === 'running') {
    return 'primary';
  }
  return 'info';
}

function formatDeviceTime(value?: string) {
  if (!value) {
    return '暂无心跳';
  }
  return `心跳 ${String(value).replace('T', ' ').slice(0, 19)}`;
}

function formatKpiValue(metric: AdminDashboardKpiItem) {
  if (moneyMetricKeys.has(metric.key)) {
    return formatCompactAmount(Number(metric.amount || 0));
  }
  return formatInteger(Number(metric.count || 0));
}

function defaultKpiUnit(metric: Pick<AdminDashboardKpiItem, 'key'>) {
  if (moneyMetricKeys.has(metric.key)) {
    return '元';
  }
  if (metric.key === 'washCount') {
    return '辆';
  }
  if (metric.key === 'cardUsageTimes') {
    return '次';
  }
  if (metric.key === 'merchantUsers') {
    return '人';
  }
  return metric.key.includes('Order') ? '单' : '次';
}

function buildEmptyDailyTrend(): AdminDashboardDailyTrendPoint[] {
  const endDate = normalizeDateRange(dateRange.value)[1];
  const startDate = addDays(endDate, -7);
  const points: AdminDashboardDailyTrendPoint[] = [];
  let cursor = startDate;
  while (cursor <= endDate) {
    points.push({
      date: cursor,
      label: cursor.slice(5),
      rechargeAmount: 0,
      consumeAmount: 0,
      washCount: 0,
      cardUsageTimes: 0,
      groupVerifyCount: 0,
    });
    cursor = addDays(cursor, 1);
  }
  return points;
}

function trendValue(point: AdminDashboardDailyTrendPoint, key: TrendValueKey) {
  return Number(point[key] || 0);
}

function trendMax(key: TrendValueKey) {
  return Math.max(1, ...chartPoints.value.map((point) => trendValue(point, key)));
}

function trendBarHeight(chart: TrendCard, point: AdminDashboardDailyTrendPoint) {
  const value = trendValue(point, chart.key);
  if (value <= 0) {
    return '5px';
  }
  return `${Math.max(18, Math.round((value / trendMax(chart.key)) * 140))}px`;
}

function chartStyle(chart: TrendCard) {
  return {
    '--chart-color': chart.color,
    '--chart-soft-color': chart.softColor,
  };
}

function metricStyle(tone: string) {
  const colors = metricToneMap[tone] || metricToneMap.blue;
  return {
    '--metric-color': colors.color,
    '--metric-bg': colors.bg,
  };
}

function shouldShowTrendLabel(index: number) {
  const total = chartPoints.value.length;
  if (total <= 10) {
    return true;
  }
  return index === 0 || index === total - 1 || index % Math.ceil(total / 7) === 0;
}

function formatMoney(value?: number | null) {
  return Number(value || 0).toFixed(2);
}

function formatCompactAmount(value?: number | null) {
  const amount = Number(value || 0);
  if (Math.abs(amount) >= 100000000) {
    return `${(amount / 100000000).toFixed(2)}亿`;
  }
  if (Math.abs(amount) >= 10000) {
    return `${(amount / 10000).toFixed(2)}万`;
  }
  return amount.toFixed(2);
}

function formatInteger(value?: number | null) {
  return String(Math.round(Number(value || 0)));
}

function formatChartValue(value: number, money: boolean) {
  return money ? formatCompactAmount(value) : formatInteger(value);
}

function formatGeneratedAt(value?: string) {
  if (!value) {
    return '--';
  }
  return String(value).replace('T', ' ').slice(0, 19);
}

function resolveStoreScore(store: AdminDashboardStoreMetric) {
  return (
    Number(store.walletConsumeAmount || 0) +
    Number(store.walletRechargeAmount || 0) +
    Number(store.cardPurchaseAmount || 0) +
    Number(store.cardUsageTimes || 0) * 10
  );
}

function storeBarWidth(store: AdminDashboardStoreMetric) {
  return `${Math.min(100, (resolveStoreScore(store) / maxStoreScore.value) * 100)}%`;
}

function formatChannel(channel?: string) {
  const map: Record<string, string> = {
    store: '本店购买',
    admin: '后台发放',
    miniapp: '小程序购买',
    douyin: '抖音券核销',
    meituan: '美团券核销',
    dazhong: '大众点评核销',
  };
  return channel ? map[channel] || channel : '未知来源';
}

onMounted(() => {
  void loadStoreOptions();
  void loadDashboard();
  refreshTimer = window.setInterval(() => {
    void loadDashboard({ silent: true });
  }, 30000);
});

onUnmounted(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer);
    refreshTimer = null;
  }
});
</script>

<style scoped lang="scss">
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 22px;
  padding: 22px 24px;
  border: 1px solid #e6eaf0;
  border-radius: 8px;
  background: #fff;
}

.dashboard-header__copy {
  min-width: 260px;
}

.dashboard-header__copy p,
.panel-heading p {
  margin: 0 0 7px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.dashboard-header__copy h2,
.panel-heading h3 {
  margin: 0;
  color: #111827;
}

.dashboard-header__copy span {
  display: block;
  margin-top: 8px;
  color: #667085;
  line-height: 1.6;
}

.dashboard-header__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.dashboard-store-select {
  width: 220px;
}

.focus-grid {
  display: grid;
  grid-template-columns: minmax(280px, 1.2fr) repeat(4, minmax(180px, 1fr));
  gap: 14px;
}

.wash-focus,
.metric-card,
.range-cell,
.trend-panel,
.insight-panel,
.device-health-panel {
  border: 1px solid #e6eaf0;
  border-radius: 8px;
  background: #fff;
}

.wash-focus {
  position: relative;
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 18px;
  overflow: hidden;
  min-height: 164px;
  padding: 22px;
  border-color: #ccefe8;
  background: linear-gradient(135deg, #f8fffd 0%, #ecfdf5 52%, #eff6ff 100%);
  box-shadow: 0 16px 34px rgba(15, 118, 110, 0.08);
}

.wash-focus::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 4px;
  background: linear-gradient(90deg, #14b8a6, #3b82f6);
  content: '';
}

.wash-focus > div {
  position: relative;
  z-index: 1;
}

.wash-focus span,
.wash-focus small {
  color: #475467;
}

.wash-focus strong {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 12px 0;
  color: #0f766e;
  font-size: 56px;
  line-height: 1;
}

.wash-focus em,
.metric-card em,
.range-cell em,
.panel-heading em {
  font-size: 14px;
  font-style: normal;
  font-weight: 700;
}

.wash-focus__meta {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-width: 148px;
  text-align: right;
}

.wash-focus__meta b {
  display: inline-flex;
  justify-content: flex-end;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
}

.metric-card {
  display: flex;
  min-height: 164px;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px;
}

.metric-card__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #667085;
}

.metric-card__title i {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--metric-bg);
  box-shadow: inset 0 0 0 1px rgba(17, 24, 39, 0.04);
}

.metric-card strong {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: var(--metric-color);
  font-size: 34px;
  line-height: 1;
}

.metric-card small,
.range-cell small {
  color: #667085;
  line-height: 1.45;
}

.range-board {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid #e6eaf0;
  border-radius: 8px;
  background: #e6eaf0;
}

.range-cell {
  min-height: 116px;
  padding: 18px;
  border: 0;
  border-radius: 0;
}

.range-cell span {
  display: block;
  color: #667085;
}

.range-cell strong {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 12px 0 8px;
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.device-health-panel {
  padding: 20px;
}

.device-health-panel.has-alert {
  border-color: #fed7aa;
  box-shadow: 0 16px 34px rgba(249, 115, 22, 0.08);
}

.device-health-layout {
  display: grid;
  grid-template-columns: minmax(360px, 0.86fr) minmax(420px, 1.14fr);
  gap: 18px;
}

.device-health-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.device-health-summary article {
  min-height: 112px;
  padding: 16px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfcfe;
}

.device-health-summary article.is-running {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.device-health-summary article.is-danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.device-health-summary article.is-warning {
  border-color: #fed7aa;
  background: #fff7ed;
}

.device-health-summary span,
.device-health-summary small {
  display: block;
  color: #667085;
}

.device-health-summary strong {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 12px 0 8px;
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.device-health-summary em {
  font-size: 14px;
  font-style: normal;
  font-weight: 700;
}

.device-alert-list {
  min-height: 236px;
  overflow: hidden;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfcfe;
}

.device-alert-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px 172px;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #edf2f7;
}

.device-alert-item:last-child {
  border-bottom: 0;
}

.device-alert-item__main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.device-alert-item__main strong {
  overflow: hidden;
  color: #111827;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-alert-item__main span,
.device-alert-item__time {
  color: #667085;
  font-size: 13px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.trend-panel,
.insight-panel {
  padding: 20px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-heading strong {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: var(--chart-color, #111827);
  font-size: 30px;
  line-height: 1;
}

.trend-plot {
  position: relative;
  display: grid;
  grid-template-columns: repeat(8, minmax(28px, 1fr));
  gap: 10px;
  min-height: 206px;
  align-items: end;
  padding-top: 28px;
}

.trend-plot__line {
  position: absolute;
  right: 0;
  left: 0;
  height: 1px;
  background: #ecf0f5;
}

.trend-bar {
  position: relative;
  z-index: 1;
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 7px;
}

.trend-bar span {
  min-height: 18px;
  color: #667085;
  font-size: 12px;
  white-space: nowrap;
}

.trend-bar span.is-empty {
  color: transparent;
}

.trend-bar i {
  display: block;
  width: min(44px, 72%);
  min-height: 5px;
  border-radius: 8px 8px 2px 2px;
  background: linear-gradient(180deg, var(--chart-color), var(--chart-soft-color));
}

.trend-bar small {
  min-height: 16px;
  color: #667085;
  font-size: 12px;
}

.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.72fr);
  gap: 14px;
}

.store-rank,
.channel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.store-rank__item {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(180px, 0.9fr) 72px;
  gap: 14px;
  align-items: center;
  padding: 14px;
  border: 1px solid #eef2f6;
  border-radius: 8px;
  background: #fbfcfe;
}

.store-rank__main,
.channel-item div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.store-rank__main strong,
.channel-item strong {
  overflow: hidden;
  color: #111827;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.store-rank__main span,
.store-rank__side span,
.channel-item span {
  color: #667085;
  font-size: 13px;
}

.store-rank__bar {
  height: 10px;
  overflow: hidden;
  border-radius: 8px;
  background: #eef2f6;
}

.store-rank__bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0f766e, #2563eb);
}

.store-rank__side {
  text-align: right;
}

.store-rank__side b {
  display: block;
  color: #111827;
  font-size: 22px;
  line-height: 1;
}

.channel-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid #eef2f6;
  border-radius: 8px;
  background: #fbfcfe;
}

.channel-item b {
  color: #0f766e;
  font-size: 18px;
}

@media (max-width: 1400px) {
  .focus-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .wash-focus {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1100px) {
  .dashboard-header {
    flex-direction: column;
  }

  .dashboard-header__actions {
    justify-content: flex-start;
  }

  .range-board,
  .chart-grid,
  .insight-grid,
  .device-health-layout {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .insight-grid,
  .device-health-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .focus-grid,
  .range-board,
  .chart-grid,
  .device-health-summary {
    grid-template-columns: 1fr;
  }

  .wash-focus,
  .store-rank__item,
  .channel-item,
  .device-alert-item {
    grid-template-columns: 1fr;
  }

  .wash-focus {
    flex-direction: column;
  }

  .wash-focus__meta,
  .store-rank__side {
    text-align: left;
  }
}
</style>
