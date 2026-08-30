<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">{{ t('users.phase') }}</p>
        <h3>{{ t('users.heroTitle') }}</h3>
        <span>{{ t('users.heroDesc') }}</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>{{ t('users.totalUsers') }}</span>
        </div>
        <div>
          <strong>{{ tableData.length }}</strong>
          <span>{{ t('users.rowsOnPage') }}</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item :label="t('users.filters.keyword')">
          <el-input
            v-model="filters.keyword"
            :placeholder="t('users.filters.keywordPlaceholder')"
            clearable
            style="width: 260px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="toolbar-row">
        <el-button type="primary" @click="openCreate">
          {{ t('users.actions.create') }}
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="userNo" :label="t('users.table.userNo')" min-width="160">
          <template #default="{ row }">{{ row.userNo || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column prop="nickname" :label="t('users.table.nickname')" min-width="140">
          <template #default="{ row }">{{ row.nickname || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column prop="realName" :label="t('users.table.realName')" min-width="120">
          <template #default="{ row }">{{ row.realName || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column prop="mobile" :label="t('users.table.mobile')" min-width="140">
          <template #default="{ row }">{{ row.mobile || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column :label="t('users.table.userStatus')" min-width="100">
          <template #default="{ row }">{{ formatUserStatus(row.userStatus) }}</template>
        </el-table-column>
        <el-table-column :label="t('users.table.member')" min-width="90">
          <template #default="{ row }">{{ formatBooleanFlag(row.isMember) }}</template>
        </el-table-column>
        <el-table-column prop="memberLevel" :label="t('users.table.memberLevel')" min-width="110">
          <template #default="{ row }">{{ row.memberLevel || t('common.noData') }}</template>
        </el-table-column>
        <el-table-column prop="points" :label="t('users.table.points')" min-width="90" align="right">
          <template #default="{ row }">{{ row.points ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="会员剩余" min-width="150">
          <template #default="{ row }">{{ formatMemberExpiry(row) }}</template>
        </el-table-column>
        <el-table-column :label="t('users.table.registerSource')" min-width="120">
          <template #default="{ row }">{{ formatRegisterSource(row.registerSource) }}</template>
        </el-table-column>
        <el-table-column :label="t('users.table.lastConsumeTime')" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastConsumeTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('users.table.createdAt')" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('users.table.actions')" fixed="right" width="360">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">
              {{ t('common.view') }}
            </el-button>
            <el-button link type="primary" @click="openEdit(row.id)">
              {{ t('common.edit') }}
            </el-button>
            <el-button link type="primary" @click="openCardManager(row)">
              {{ t('users.actions.cards') }}
            </el-button>
            <el-button link type="primary" @click="openRecharge(row)">
              {{ t('users.actions.recharge') }}
            </el-button>
            <el-button link type="danger" @click="openRefund(row)">
              {{ t('users.actions.refund') }}
            </el-button>
            <el-button link type="warning" @click="openFine(row)">
              {{ t('users.actions.fine') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :current-page="pagination.page"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <UserDetailDrawer :visible="detailVisible" :detail="detailData" @close="detailVisible = false" />
    <UserCardManagerDrawer
      :visible="cardManagerVisible"
      :user="cardManagerUser"
      :store-options="storeOptions"
      @close="cardManagerVisible = false"
      @changed="handleCardsChanged"
    />

    <el-dialog v-model="formVisible" :title="formTitle" width="640px" @closed="handleFormClosed">
      <el-form ref="formRef" :model="userForm" label-width="110px">
        <div class="form-grid">
          <el-form-item :label="t('users.table.nickname')">
            <el-input v-model="userForm.nickname" />
          </el-form-item>
          <el-form-item :label="t('users.table.realName')">
            <el-input v-model="userForm.realName" />
          </el-form-item>
          <el-form-item :label="t('users.table.mobile')">
            <el-input v-model="userForm.mobile" />
          </el-form-item>
          <el-form-item :label="t('users.table.userStatus')">
            <el-switch
              v-model="userForm.userStatus"
              :active-value="1"
              :inactive-value="0"
              :active-text="t('userStatus.enabled')"
              :inactive-text="t('userStatus.disabled')"
            />
          </el-form-item>
          <el-form-item :label="t('users.form.remark')" class="form-grid__wide">
            <el-input v-model="userForm.remark" type="textarea" :rows="3" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="submitting" @click="handleUserSubmit">
            {{ t('common.save') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="rechargeVisible" :title="t('users.recharge.title')" width="640px" @closed="handleRechargeClosed">
      <el-form :model="rechargeForm" label-width="120px">
        <div class="form-grid">
          <el-form-item :label="t('users.recharge.userId')">
            <el-input v-model="rechargeForm.userId" disabled />
          </el-form-item>
          <el-form-item :label="t('users.recharge.userName')">
            <el-input v-model="rechargeForm.userName" disabled />
          </el-form-item>
          <el-form-item :label="t('users.recharge.store')">
            <el-select v-model="rechargeForm.storeId" filterable clearable style="width: 220px">
              <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="Number(store.id)" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('users.recharge.principalAmount')">
            <el-input-number v-model="rechargeForm.principalAmount" :min="0" :precision="2" :step="1" />
          </el-form-item>
          <el-form-item :label="t('users.recharge.giftAmount')">
            <el-input-number v-model="rechargeForm.giftAmount" :min="0" :precision="2" :step="1" />
          </el-form-item>
          <el-form-item :label="t('users.recharge.remark')" class="form-grid__wide">
            <el-input v-model="rechargeForm.remark" type="textarea" :rows="3" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="rechargeVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="rechargeSubmitting" @click="handleRechargeSubmit">
            {{ t('common.save') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="refundVisible" :title="t('users.refund.title')" width="640px" @closed="handleRefundClosed">
      <el-form :model="refundForm" label-width="120px">
        <div class="form-grid">
          <el-form-item :label="t('users.refund.userId')">
            <el-input v-model="refundForm.userId" disabled />
          </el-form-item>
          <el-form-item :label="t('users.refund.userName')">
            <el-input v-model="refundForm.userName" disabled />
          </el-form-item>
          <el-form-item :label="t('users.refund.store')">
            <el-select v-model="refundForm.storeId" filterable clearable style="width: 220px">
              <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="Number(store.id)" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('users.refund.principalAmount')">
            <el-input-number v-model="refundForm.principalAmount" :min="0" :precision="2" :step="1" />
          </el-form-item>
          <el-form-item :label="t('users.refund.remark')" class="form-grid__wide">
            <el-input v-model="refundForm.remark" type="textarea" :rows="3" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="refundVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="refundSubmitting" @click="handleRefundSubmit">
            {{ t('common.save') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="fineVisible" :title="t('users.fine.title')" width="640px" @closed="handleFineClosed">
      <el-form :model="fineForm" label-width="120px">
        <div class="form-grid">
          <el-form-item :label="t('users.fine.userId')">
            <el-input v-model="fineForm.userId" disabled />
          </el-form-item>
          <el-form-item :label="t('users.fine.userName')">
            <el-input v-model="fineForm.userName" disabled />
          </el-form-item>
          <el-form-item :label="t('users.fine.store')">
            <el-select v-model="fineForm.storeId" filterable clearable style="width: 220px">
              <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="Number(store.id)" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('users.fine.amount')">
            <el-input-number v-model="fineForm.amount" :min="0" :precision="2" :step="1" />
          </el-form-item>
          <el-form-item :label="t('users.fine.remark')" class="form-grid__wide">
            <el-input
              v-model="fineForm.remark"
              type="textarea"
              :rows="3"
              :placeholder="t('users.fine.remarkPlaceholder')"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="fineVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="warning" :loading="fineSubmitting" @click="handleFineSubmit">
            {{ t('common.save') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import type { FormInstance } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { createUser, fetchUserOverview, fetchUserPage, manualFine, manualRecharge, manualRefund, updateUser } from '@/api/user';
import { fetchAdminStoreOptions } from '@/api/store';
import { t } from '@/i18n';
import type {
  AdminManualFinePayload,
  AdminManualRechargePayload,
  AdminManualRefundPayload,
  AdminUserFormPayload,
  AdminUserItem,
  AdminUserOverview,
} from '@/types/user';
import type { StoreOption } from '@/types/store';
import { formatBooleanFlag, formatDateTime, formatRegisterSource, formatUserStatus } from '@/utils/format';
import UserCardManagerDrawer from './UserCardManagerDrawer.vue';
import UserDetailDrawer from './UserDetailDrawer.vue';

type RechargeFormState = AdminManualRechargePayload & { userName?: string };
type RefundFormState = AdminManualRefundPayload & { userName?: string };
type FineFormState = AdminManualFinePayload & { userName?: string };

const loading = ref(false);
const submitting = ref(false);
const rechargeSubmitting = ref(false);
const refundSubmitting = ref(false);
const fineSubmitting = ref(false);
const tableData = ref<AdminUserItem[]>([]);
const detailVisible = ref(false);
const detailData = ref<AdminUserOverview | null>(null);
const cardManagerVisible = ref(false);
const cardManagerUser = ref<AdminUserItem | null>(null);
const formVisible = ref(false);
const formMode = ref<'create' | 'edit'>('create');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const rechargeVisible = ref(false);
const refundVisible = ref(false);
const fineVisible = ref(false);
const storeOptions = ref<StoreOption[]>([]);

const filters = reactive({
  keyword: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const createEmptyUserForm = (): AdminUserFormPayload => ({
  userNo: '',
  nickname: '',
  realName: '',
  mobile: '',
  userStatus: 1,
  remark: '',
});

const createEmptyRechargeForm = (): RechargeFormState => ({
  userId: 0,
  storeId: 0,
  principalAmount: 0,
  giftAmount: 0,
  remark: '',
  userName: '',
});

const createEmptyRefundForm = (): RefundFormState => ({
  userId: 0,
  storeId: 0,
  principalAmount: 0,
  remark: '',
  userName: '',
});

const createEmptyFineForm = (): FineFormState => ({
  userId: 0,
  storeId: 0,
  amount: 0,
  remark: '',
  userName: '',
});

const userForm = reactive<AdminUserFormPayload>(createEmptyUserForm());
const rechargeForm = reactive<RechargeFormState>(createEmptyRechargeForm());
const refundForm = reactive<RefundFormState>(createEmptyRefundForm());
const fineForm = reactive<FineFormState>(createEmptyFineForm());

const formatMemberExpiry = (user: AdminUserItem) => {
  if (!Number(user.isMember)) {
    return '普通用户';
  }
  if (!user.memberExpireTime) {
    return '永久会员';
  }
  return `至 ${formatDateTime(user.memberExpireTime)}`;
};

const formTitle = computed(() => {
  return formMode.value === 'create' ? t('users.dialogs.createTitle') : t('users.dialogs.editTitle');
});

const assignUserForm = (payload: Partial<AdminUserFormPayload>) => {
  Object.assign(userForm, createEmptyUserForm(), payload);
};

const assignRechargeForm = (payload: Partial<RechargeFormState>) => {
  Object.assign(rechargeForm, createEmptyRechargeForm(), payload);
};

const assignRefundForm = (payload: Partial<RefundFormState>) => {
  Object.assign(refundForm, createEmptyRefundForm(), payload);
};

const assignFineForm = (payload: Partial<FineFormState>) => {
  Object.assign(fineForm, createEmptyFineForm(), payload);
};

const loadUsers = async () => {
  loading.value = true;
  try {
    const data = await fetchUserPage({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword || undefined,
    });

    tableData.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error(t('users.messages.loadFailed'));
  } finally {
    loading.value = false;
  }
};

const loadStoreOptions = async () => {
  try {
    storeOptions.value = await fetchAdminStoreOptions();
  } catch (error) {
    ElMessage.error(t('users.messages.loadStoresFailed'));
  }
};

const openCreate = () => {
  formMode.value = 'create';
  editingId.value = null;
  assignUserForm({});
  formVisible.value = true;
};

const openEdit = async (id: number) => {
  try {
    const detail = await fetchUserOverview(id);
    formMode.value = 'edit';
    editingId.value = id;
    assignUserForm({
      nickname: detail.nickname || '',
      realName: detail.realName || '',
      mobile: detail.mobile || '',
      userStatus: detail.userStatus ?? 1,
      remark: detail.remark || '',
    });
    formVisible.value = true;
  } catch (error) {
    ElMessage.error(t('users.messages.loadDetailFailed'));
  }
};

const openDetail = async (id: number) => {
  try {
    detailData.value = await fetchUserOverview(id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(t('users.messages.loadDetailFailed'));
  }
};

const openCardManager = (row: AdminUserItem) => {
  cardManagerUser.value = row;
  cardManagerVisible.value = true;
};

const openRecharge = (row: AdminUserItem) => {
  assignRechargeForm({
    userId: row.id,
    userName: row.nickname || row.userNo || String(row.id),
  });
  rechargeVisible.value = true;
};

const openRefund = (row: AdminUserItem) => {
  assignRefundForm({
    userId: row.id,
    userName: row.nickname || row.userNo || String(row.id),
  });
  refundVisible.value = true;
};

const openFine = (row: AdminUserItem) => {
  assignFineForm({
    userId: row.id,
    userName: row.nickname || row.userNo || String(row.id),
  });
  fineVisible.value = true;
};

const handleUserSubmit = async () => {
  submitting.value = true;
  try {
    if (formMode.value === 'create') {
      await createUser(userForm);
      ElMessage.success(t('users.messages.createSuccess'));
    } else if (editingId.value) {
      await updateUser(editingId.value, userForm);
      ElMessage.success(t('users.messages.updateSuccess'));
    }
    formVisible.value = false;
    await loadUsers();
  } catch (error) {
    ElMessage.error(t('users.messages.saveFailed'));
  } finally {
    submitting.value = false;
  }
};

const handleRechargeSubmit = async () => {
  if (!rechargeForm.userId) {
    ElMessage.error(t('users.messages.rechargeUserRequired'));
    return;
  }
  if (!rechargeForm.storeId) {
    ElMessage.error(t('users.messages.rechargeStoreRequired'));
    return;
  }
  const principal = Number(rechargeForm.principalAmount || 0);
  const gift = Number(rechargeForm.giftAmount || 0);
  if (principal <= 0 && gift <= 0) {
    ElMessage.error(t('users.messages.rechargeAmountRequired'));
    return;
  }

  rechargeSubmitting.value = true;
  try {
    await manualRecharge({
      userId: rechargeForm.userId,
      storeId: rechargeForm.storeId,
      principalAmount: principal,
      giftAmount: gift,
      remark: rechargeForm.remark || undefined,
    });
    ElMessage.success(t('users.messages.rechargeSuccess'));
    rechargeVisible.value = false;
    await loadUsers();
    if (detailVisible.value && detailData.value?.id === rechargeForm.userId) {
      detailData.value = await fetchUserOverview(rechargeForm.userId);
    }
  } catch (error) {
    ElMessage.error(t('users.messages.rechargeFailed'));
  } finally {
    rechargeSubmitting.value = false;
  }
};

const handleRefundSubmit = async () => {
  if (!refundForm.userId) {
    ElMessage.error(t('users.messages.refundUserRequired'));
    return;
  }
  if (!refundForm.storeId) {
    ElMessage.error(t('users.messages.refundStoreRequired'));
    return;
  }
  const principal = Number(refundForm.principalAmount || 0);
  if (principal <= 0) {
    ElMessage.error(t('users.messages.refundAmountRequired'));
    return;
  }

  refundSubmitting.value = true;
  try {
    await manualRefund({
      userId: refundForm.userId,
      storeId: refundForm.storeId,
      principalAmount: principal,
      remark: refundForm.remark || undefined,
    });
    ElMessage.success(t('users.messages.refundSuccess'));
    refundVisible.value = false;
    await loadUsers();
    if (detailVisible.value && detailData.value?.id === refundForm.userId) {
      detailData.value = await fetchUserOverview(refundForm.userId);
    }
  } catch (error) {
    ElMessage.error(t('users.messages.refundFailed'));
  } finally {
    refundSubmitting.value = false;
  }
};

const handleFineSubmit = async () => {
  if (!fineForm.userId) {
    ElMessage.error(t('users.messages.fineUserRequired'));
    return;
  }
  if (!fineForm.storeId) {
    ElMessage.error(t('users.messages.fineStoreRequired'));
    return;
  }
  const amount = Number(fineForm.amount || 0);
  if (amount <= 0) {
    ElMessage.error(t('users.messages.fineAmountRequired'));
    return;
  }

  fineSubmitting.value = true;
  try {
    await manualFine({
      userId: fineForm.userId,
      storeId: fineForm.storeId,
      amount,
      remark: fineForm.remark || undefined,
    });
    ElMessage.success(t('users.messages.fineSuccess'));
    fineVisible.value = false;
    await loadUsers();
    if (detailVisible.value && detailData.value?.id === fineForm.userId) {
      detailData.value = await fetchUserOverview(fineForm.userId);
    }
  } catch (error) {
    ElMessage.error(t('users.messages.fineFailed'));
  } finally {
    fineSubmitting.value = false;
  }
};

const handleCardsChanged = async () => {
  if (detailVisible.value && detailData.value?.id) {
    detailData.value = await fetchUserOverview(detailData.value.id);
  }
};

const handleFormClosed = () => {
  formRef.value?.clearValidate();
  assignUserForm({});
};

const handleRechargeClosed = () => {
  assignRechargeForm({});
};

const handleRefundClosed = () => {
  assignRefundForm({});
};

const handleFineClosed = () => {
  assignFineForm({});
};

const handleSearch = () => {
  pagination.page = 1;
  void loadUsers();
};

const handleReset = () => {
  filters.keyword = '';
  pagination.page = 1;
  void loadUsers();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  void loadUsers();
};

const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.page = 1;
  void loadUsers();
};

onMounted(async () => {
  await loadStoreOptions();
  await loadUsers();
});
</script>

<style scoped lang="scss">
.page-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.toolbar-row {
  display: flex;
  justify-content: flex-end;
  padding-bottom: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 16px;
}

.form-grid :deep(.form-grid__wide) {
  grid-column: 1 / -1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 960px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
