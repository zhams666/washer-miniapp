<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">{{ t('stores.phase') }}</p>
        <h3>{{ t('stores.heroTitle') }}</h3>
        <span>{{ t('stores.heroDesc') }}</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>{{ t('stores.totalStores') }}</span>
        </div>
        <div>
          <strong>{{ tableData.length }}</strong>
          <span>{{ t('stores.rowsOnPage') }}</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item :label="t('stores.filters.keyword')">
          <el-input
            v-model="filters.keyword"
            :placeholder="t('stores.filters.keywordPlaceholder')"
            clearable
            style="width: 240px"
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
          {{ t('stores.actions.create') }}
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="storeName" :label="t('stores.table.storeName')" min-width="180" />
        <el-table-column prop="storeCode" :label="t('stores.table.storeCode')" min-width="160" />
        <el-table-column :label="t('stores.table.status')" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.storeStatus === 1 ? 'success' : 'info'" effect="light" round>
              {{ formatStoreStatus(row.storeStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('stores.table.address')" min-width="260">
          <template #default="{ row }">
            {{ formatStoreAddress(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="contactPhone" :label="t('stores.table.contactPhone')" min-width="140">
          <template #default="{ row }">
            {{ row.contactPhone || t('common.noData') }}
          </template>
        </el-table-column>
        <el-table-column prop="businessHours" :label="t('stores.table.businessHours')" min-width="140">
          <template #default="{ row }">
            {{ row.businessHours || t('common.noData') }}
          </template>
        </el-table-column>
        <el-table-column :label="t('stores.table.updatedAt')" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('stores.table.actions')" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">
              {{ t('common.view') }}
            </el-button>
            <el-button link type="primary" @click="openEdit(row.id)">
              {{ t('common.edit') }}
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

    <el-dialog v-model="detailVisible" :title="t('stores.detail.title')" width="760px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="t('stores.form.storeName')">{{ detailData.storeName }}</el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.storeCode')">
            {{ detailData.storeCode || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.storeStatus')">
            {{ formatStoreStatus(detailData.storeStatus) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.contactPhone')">
            {{ detailData.contactPhone || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.province')">
            {{ detailData.province || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.city')">
            {{ detailData.city || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.district')">
            {{ detailData.district || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.contactName')">
            {{ detailData.contactName || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.address')" :span="2">
            {{ detailData.address || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.businessHours')">
            {{ detailData.businessHours || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.form.remark')">
            {{ detailData.remark || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.detail.createdAt')">
            {{ formatDateTime(detailData.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('stores.detail.updatedAt')">
            {{ formatDateTime(detailData.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <el-dialog v-model="formVisible" :title="formTitle" width="760px" @closed="handleFormClosed">
      <el-form ref="formRef" :model="storeForm" :rules="storeRules" label-width="110px">
        <div class="form-grid">
          <el-form-item :label="t('stores.form.storeName')" prop="storeName">
            <el-input v-model="storeForm.storeName" />
          </el-form-item>
          <el-form-item :label="t('stores.form.storeCode')">
            <el-input v-model="storeForm.storeCode" :placeholder="t('stores.form.storeCodePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('stores.form.storeStatus')">
            <el-switch
              v-model="storeForm.storeStatus"
              :active-value="1"
              :inactive-value="0"
              :active-text="t('stores.status.enabled')"
              :inactive-text="t('stores.status.disabled')"
            />
          </el-form-item>
          <el-form-item :label="t('stores.form.contactName')">
            <el-input v-model="storeForm.contactName" />
          </el-form-item>
          <el-form-item :label="t('stores.form.contactPhone')">
            <el-input v-model="storeForm.contactPhone" />
          </el-form-item>
          <el-form-item :label="t('stores.form.businessHours')">
            <el-input v-model="storeForm.businessHours" />
          </el-form-item>
          <el-form-item :label="t('stores.form.province')">
            <el-input v-model="storeForm.province" />
          </el-form-item>
          <el-form-item :label="t('stores.form.city')">
            <el-input v-model="storeForm.city" />
          </el-form-item>
          <el-form-item :label="t('stores.form.district')">
            <el-input v-model="storeForm.district" />
          </el-form-item>
          <el-form-item :label="t('stores.form.address')" class="form-grid__wide">
            <el-input v-model="storeForm.address" />
          </el-form-item>
          <el-form-item :label="t('stores.form.remark')" class="form-grid__wide">
            <el-input v-model="storeForm.remark" type="textarea" :rows="3" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ t('common.save') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { createStore, fetchStoreDetail, fetchStorePage, updateStore } from '@/api/store';
import { t } from '@/i18n';
import type { StoreFormPayload, StoreItem } from '@/types/store';
import { formatDateTime, formatStoreStatus } from '@/utils/format';

const loading = ref(false);
const submitting = ref(false);
const tableData = ref<StoreItem[]>([]);
const detailVisible = ref(false);
const formVisible = ref(false);
const detailData = ref<StoreItem | null>(null);
const editingId = ref<number | null>(null);
const formMode = ref<'create' | 'edit'>('create');
const formRef = ref<FormInstance>();

const filters = reactive({
  keyword: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const createEmptyStoreForm = (): StoreFormPayload => ({
  storeCode: '',
  storeName: '',
  storeStatus: 1,
  province: '',
  city: '',
  district: '',
  address: '',
  contactName: '',
  contactPhone: '',
  businessHours: '',
  remark: '',
});

const storeForm = reactive<StoreFormPayload>(createEmptyStoreForm());

const storeRules: FormRules<StoreFormPayload> = {
  storeName: [
    {
      required: true,
      message: t('stores.messages.storeNameRequired'),
      trigger: 'blur',
    },
  ],
};

const formTitle = computed(() => {
  return formMode.value === 'create' ? t('stores.dialogs.createTitle') : t('stores.dialogs.editTitle');
});

const assignForm = (payload: Partial<StoreFormPayload>) => {
  Object.assign(storeForm, createEmptyStoreForm(), payload);
};

const formatStoreAddress = (store: StoreItem) => {
  const parts = [store.province, store.city, store.district, store.address].filter(Boolean);
  return parts.length > 0 ? parts.join(' ') : t('common.noData');
};

const loadStores = async () => {
  loading.value = true;
  try {
    const data = await fetchStorePage({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword || undefined,
    });

    tableData.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error(t('stores.messages.loadFailed'));
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  formMode.value = 'create';
  editingId.value = null;
  assignForm({});
  formVisible.value = true;
};

const openEdit = async (id: number) => {
  try {
    const store = await fetchStoreDetail(id);
    formMode.value = 'edit';
    editingId.value = id;
    assignForm({
      ...store,
      storeStatus: store.storeStatus ?? 1,
    });
    formVisible.value = true;
  } catch (error) {
    ElMessage.error(t('stores.messages.loadDetailFailed'));
  }
};

const openDetail = async (id: number) => {
  try {
    detailData.value = await fetchStoreDetail(id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(t('stores.messages.loadDetailFailed'));
  }
};

const handleSubmit = async () => {
  if (!formRef.value) {
    return;
  }

  const isValid = await formRef.value.validate().catch(() => false);
  if (!isValid) {
    return;
  }

  submitting.value = true;
  try {
    if (formMode.value === 'create') {
      await createStore(storeForm);
      ElMessage.success(t('stores.messages.createSuccess'));
    } else if (editingId.value) {
      await updateStore(editingId.value, storeForm);
      ElMessage.success(t('stores.messages.updateSuccess'));
    }

    formVisible.value = false;
    await loadStores();
  } catch (error) {
    ElMessage.error(t('stores.messages.saveFailed'));
  } finally {
    submitting.value = false;
  }
};

const handleFormClosed = () => {
  formRef.value?.clearValidate();
  assignForm({});
};

const handleSearch = () => {
  pagination.page = 1;
  void loadStores();
};

const handleReset = () => {
  filters.keyword = '';
  pagination.page = 1;
  void loadStores();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  void loadStores();
};

const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.page = 1;
  void loadStores();
};

onMounted(() => {
  void loadStores();
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
