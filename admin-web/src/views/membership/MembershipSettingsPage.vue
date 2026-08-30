<template>
  <div class="page-stack membership-page">
    <div class="hero-panel membership-hero">
      <div>
        <p class="hero-panel__eyebrow">Membership Operations</p>
        <h3>会员权益与充值</h3>
        <span>集中维护会员日、月会员、年会员、支付订单和用户有效期。</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ activePlans.length }}</strong>
          <span>启用方案</span>
        </div>
        <div>
          <strong>{{ orderPage.total }}</strong>
          <span>会员订单</span>
        </div>
      </div>
    </div>

    <section class="settings-panel">
      <div class="section-heading">
        <div>
          <p class="section-kicker">Member Day</p>
          <h4>会员日设置</h4>
        </div>
        <el-button type="primary" :loading="savingSettings" @click="saveSettings">保存设置</el-button>
      </div>
      <el-form :model="settings" label-width="130px" class="settings-form">
        <el-form-item label="启用会员日">
          <el-switch v-model="settings.memberDayEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="会员日星期">
          <el-select v-model="settings.memberDayWeekday" style="width: 180px">
            <el-option v-for="item in weekdays" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时段">
          <el-time-picker v-model="settings.memberDayStartTime" value-format="HH:mm:ss" format="HH:mm" style="width: 180px" />
        </el-form-item>
        <el-form-item label="结束时段">
          <el-time-picker v-model="settings.memberDayEndTime" value-format="HH:mm:ss" format="HH:mm" style="width: 180px" />
        </el-form-item>
        <el-form-item label="优惠分钟数">
          <el-input-number v-model="settings.memberDayFirstMinutes" :min="1" :max="180" />
        </el-form-item>
        <el-form-item label="会员折扣">
          <el-input-number v-model="settings.memberDayDiscountRate" :min="0" :max="1" :step="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="福利说明" class="form-wide">
          <el-input v-model="settings.benefitText" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
    </section>

    <section class="table-card">
      <div class="section-heading compact">
        <div>
          <p class="section-kicker">Membership Plans</p>
          <h4>会员充值方案</h4>
        </div>
        <el-button type="primary" @click="openPlanCreate">新增方案</el-button>
      </div>
      <el-table v-loading="plansLoading" :data="plans" border>
        <el-table-column prop="planName" label="方案名称" min-width="130" />
        <el-table-column prop="planCode" label="编码" min-width="120" />
        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">{{ row.planType === 'yearly' ? '年会员' : '月会员' }}</template>
        </el-table-column>
        <el-table-column label="价格" min-width="110" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="durationMonths" label="有效月数" min-width="100" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openPlanEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.status !== 1" @click="disablePlan(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="table-card">
      <div class="section-heading compact">
        <div>
          <p class="section-kicker">Accounting Feed</p>
          <h4>会员充值记账</h4>
        </div>
        <el-button @click="loadOrders">刷新</el-button>
      </div>
      <el-table v-loading="ordersLoading" :data="orderPage.records" border>
        <el-table-column prop="orderNo" label="订单号" min-width="210" />
        <el-table-column prop="userId" label="用户编号" min-width="100" />
        <el-table-column label="方案" min-width="120">
          <template #default="{ row }">{{ planName(row.planId) }}</template>
        </el-table-column>
        <el-table-column label="实付金额" min-width="110" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column label="支付状态" min-width="100">
          <template #default="{ row }">{{ row.payStatus === 'paid' ? '已支付' : row.payStatus || '待支付' }}</template>
        </el-table-column>
        <el-table-column label="会员有效期至" min-width="180">
          <template #default="{ row }">{{ formatDate(row.memberExpireTime) }}</template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="180">
          <template #default="{ row }">{{ formatDate(row.payTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :current-page="orderPage.current"
          :page-size="orderPage.size"
          :page-sizes="[10, 20, 50]"
          :total="orderPage.total"
          @current-change="handleOrderPageChange"
          @size-change="handleOrderSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="planDialogVisible" :title="planDialogTitle" width="560px">
      <el-form :model="planForm" label-width="110px">
        <el-form-item label="方案编码"><el-input v-model="planForm.planCode" /></el-form-item>
        <el-form-item label="方案名称"><el-input v-model="planForm.planName" /></el-form-item>
        <el-form-item label="方案类型">
          <el-select v-model="planForm.planType" style="width: 180px">
            <el-option label="月会员" value="monthly" />
            <el-option label="年会员" value="yearly" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效月数"><el-input-number v-model="planForm.durationMonths" :min="1" /></el-form-item>
        <el-form-item label="充值金额"><el-input-number v-model="planForm.price" :min="0.01" :precision="2" :step="1" /></el-form-item>
        <el-form-item label="权益说明"><el-input v-model="planForm.benefitText" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="planForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPlan" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import {
  createMembershipPlan,
  disableMembershipPlan,
  fetchMembershipOrders,
  fetchMembershipPlans,
  fetchMembershipSettings,
  saveMembershipSettings,
  updateMembershipPlan,
} from '@/api/membership';
import type { MembershipOrderPage, MembershipPlan, MembershipSetting } from '@/types/membership';

const weekdays = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
];

const defaultSettings = (): MembershipSetting => ({
  memberDayEnabled: 1,
  memberDayWeekday: 3,
  memberDayStartTime: '00:00:00',
  memberDayEndTime: '23:59:59',
  memberDayFirstMinutes: 10,
  memberDayDiscountRate: 0.75,
  benefitText: '',
});

const defaultPlan = (): MembershipPlan => ({
  planCode: '',
  planName: '',
  planType: 'monthly',
  durationMonths: 1,
  price: 19.9,
  benefitText: '',
  status: 1,
  sortOrder: 0,
});

const settings = reactive<MembershipSetting>(defaultSettings());
const plans = ref<MembershipPlan[]>([]);
const savingSettings = ref(false);
const plansLoading = ref(false);
const ordersLoading = ref(false);
const savingPlan = ref(false);
const planDialogVisible = ref(false);
const editingPlanId = ref<number | null>(null);
const planForm = reactive<MembershipPlan>(defaultPlan());
const orderPage = reactive<MembershipOrderPage>({ records: [], total: 0, current: 1, size: 10 });

const activePlans = computed(() => plans.value.filter((plan) => plan.status === 1));
const planDialogTitle = computed(() => (editingPlanId.value ? '编辑会员方案' : '新增会员方案'));

const formatAmount = (value: unknown) => Number(value || 0).toFixed(2);
const formatDate = (value?: string) => (value ? value.replace('T', ' ').slice(0, 19) : '未支付');
const planName = (id?: number) => plans.value.find((plan) => plan.id === id)?.planName || `方案 ${id || '-'}`;

const loadSettings = async () => Object.assign(settings, defaultSettings(), await fetchMembershipSettings());
const loadPlans = async () => {
  plansLoading.value = true;
  try {
    plans.value = await fetchMembershipPlans();
  } finally {
    plansLoading.value = false;
  }
};
const loadOrders = async () => {
  ordersLoading.value = true;
  try {
    const result = await fetchMembershipOrders({ page: orderPage.current || 1, size: orderPage.size || 10 });
    Object.assign(orderPage, result, { records: result.records || [] });
  } finally {
    ordersLoading.value = false;
  }
};

const saveSettings = async () => {
  savingSettings.value = true;
  try {
    Object.assign(settings, await saveMembershipSettings(settings));
    ElMessage.success('会员日设置已保存');
  } finally {
    savingSettings.value = false;
  }
};

const openPlanCreate = () => {
  editingPlanId.value = null;
  Object.assign(planForm, defaultPlan());
  planDialogVisible.value = true;
};
const openPlanEdit = (plan: MembershipPlan) => {
  editingPlanId.value = plan.id || null;
  Object.assign(planForm, defaultPlan(), plan);
  planDialogVisible.value = true;
};
const savePlan = async () => {
  savingPlan.value = true;
  try {
    if (editingPlanId.value) {
      await updateMembershipPlan(editingPlanId.value, planForm);
    } else {
      await createMembershipPlan(planForm);
    }
    planDialogVisible.value = false;
    await loadPlans();
    ElMessage.success('会员方案已保存');
  } finally {
    savingPlan.value = false;
  }
};
const disablePlan = async (plan: MembershipPlan) => {
  if (!plan.id) return;
  await ElMessageBox.confirm(`确定下架 ${plan.planName || '该方案'} 吗？`, '确认操作', { type: 'warning' });
  await disableMembershipPlan(plan.id);
  await loadPlans();
  ElMessage.success('会员方案已下架');
};
const handleOrderPageChange = (page: number) => {
  orderPage.current = page;
  void loadOrders();
};
const handleOrderSizeChange = (size: number) => {
  orderPage.size = size;
  orderPage.current = 1;
  void loadOrders();
};

onMounted(async () => {
  await Promise.all([loadSettings(), loadPlans(), loadOrders()]);
});
</script>

<style scoped lang="scss">
.membership-page {
  --membership-blue: #0369a1;
  --membership-violet: #5b39b8;
}

.membership-hero {
  background: linear-gradient(135deg, #0f172a, #1d4ed8 62%, #5b39b8);
  color: #fff;
}

.membership-hero .hero-panel__eyebrow,
.membership-hero span {
  color: rgba(255, 255, 255, 0.78);
}

.settings-panel {
  padding: 24px;
  border: 1px solid var(--panel-border);
  border-radius: 24px;
  background: var(--panel-bg);
  box-shadow: 0 12px 40px rgba(31, 41, 55, 0.08);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
}

.section-heading.compact {
  margin: 0 0 18px;
}

.section-heading h4 {
  margin: 3px 0 0;
  font-size: 20px;
}

.section-kicker {
  margin: 0;
  color: var(--membership-blue);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.settings-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 28px;
}

.form-wide {
  grid-column: 1 / -1;
}

.table-card {
  overflow-x: auto;
}

@media (max-width: 760px) {
  .settings-form {
    grid-template-columns: 1fr;
  }

  .form-wide {
    grid-column: auto;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
