<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">{{ t('devices.phase') }}</p>
        <h3>{{ t('devices.heroTitle') }}</h3>
        <span>{{ t('devices.heroDesc') }}</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ tableData.length }}</strong>
          <span>{{ t('devices.totalDevices') }}</span>
        </div>
        <div>
          <strong>{{ activeStoreLabel }}</strong>
          <span>{{ t('devices.currentStore') }}</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item :label="t('devices.filters.store')">
          <el-select v-model="filters.storeId" clearable style="width: 220px" :placeholder="t('devices.filters.allStores')">
            <el-option
              v-for="store in storeOptions"
              :key="store.id"
              :label="store.storeName"
              :value="store.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('devices.filters.keyword')">
          <el-input
            v-model="filters.keyword"
            :placeholder="t('devices.filters.keywordPlaceholder')"
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
          {{ t('devices.actions.create') }}
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="deviceName" :label="t('devices.table.deviceName')" min-width="180">
          <template #default="{ row }">
            {{ row.deviceName || t('common.noData') }}
          </template>
        </el-table-column>
        <el-table-column prop="deviceCode" :label="t('devices.table.deviceCode')" min-width="180" />
        <el-table-column :label="t('devices.table.store')" min-width="160">
          <template #default="{ row }">
            {{ row.storeName || resolveStoreName(row.storeId) }}
          </template>
        </el-table-column>
        <el-table-column prop="deviceType" :label="t('devices.table.deviceType')" min-width="120">
          <template #default="{ row }">
            {{ row.deviceType || t('common.noData') }}
          </template>
        </el-table-column>
        <el-table-column prop="deviceRole" :label="t('devices.table.deviceRole')" min-width="120">
          <template #default="{ row }">
            {{ row.deviceRole || t('common.noData') }}
          </template>
        </el-table-column>
        <el-table-column :label="t('devices.table.deviceStatus')" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getDeviceTagType(row.deviceStatus || row.status)" effect="light" round>
              {{ formatDeviceStatus(row.deviceStatus || row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="protocolType" :label="t('devices.table.protocolType')" min-width="120">
          <template #default="{ row }">
            {{ row.protocolType || t('common.noData') }}
          </template>
        </el-table-column>
        <el-table-column :label="t('devices.table.updatedAt')" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('devices.table.actions')" fixed="right" width="180">
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
    </div>

    <el-dialog v-model="detailVisible" :title="t('devices.detail.title')" width="760px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="t('devices.form.deviceName')">
            {{ detailData.deviceName || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.form.deviceCode')">
            {{ detailData.deviceCode || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.form.storeId')">
            {{ detailData.storeId }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.form.store')">
            {{ detailData.storeName || resolveStoreName(detailData.storeId) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.form.deviceType')">
            {{ detailData.deviceType || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.form.deviceRole')">
            {{ detailData.deviceRole || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.form.deviceStatus')">
            {{ formatDeviceStatus(detailData.deviceStatus || detailData.status) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.form.protocolType')">
            {{ detailData.protocolType || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.form.firmwareVersion')">
            {{ detailData.firmwareVersion || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.form.remark')">
            {{ detailData.remark || t('common.noData') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.detail.createdAt')">
            {{ formatDateTime(detailData.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('devices.detail.updatedAt')">
            {{ formatDateTime(detailData.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <el-dialog v-model="formVisible" :title="formTitle" width="760px" @closed="handleFormClosed">
      <el-form ref="formRef" :model="deviceForm" :rules="deviceRules" label-width="110px">
        <div class="form-grid">
          <el-form-item :label="t('devices.form.storeId')" prop="storeId">
            <el-select v-model="deviceForm.storeId" style="width: 100%">
              <el-option
                v-for="store in storeOptions"
                :key="store.id"
                :label="store.storeName"
                :value="store.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('devices.form.deviceCode')">
            <el-input v-model="deviceForm.deviceCode" :placeholder="t('devices.form.deviceCodePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('devices.form.deviceName')">
            <el-input v-model="deviceForm.deviceName" />
          </el-form-item>
          <el-form-item :label="t('devices.form.deviceType')">
            <el-select v-model="deviceForm.deviceType" style="width: 100%">
              <el-option v-for="item in deviceTypeOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('devices.form.deviceRole')">
            <el-select v-model="deviceForm.deviceRole" style="width: 100%">
              <el-option v-for="item in deviceRoleOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('devices.form.deviceStatus')">
            <el-select v-model="deviceForm.deviceStatus" style="width: 100%">
              <el-option
                v-for="item in deviceStatusOptions"
                :key="item"
                :label="formatDeviceStatus(item)"
                :value="item"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('devices.form.protocolType')">
            <el-input v-model="deviceForm.protocolType" />
          </el-form-item>
          <el-form-item :label="t('devices.form.firmwareVersion')">
            <el-input v-model="deviceForm.firmwareVersion" />
          </el-form-item>
          <el-form-item :label="t('devices.form.remark')" class="form-grid__wide">
            <el-input v-model="deviceForm.remark" type="textarea" :rows="3" />
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
import { createDevice, fetchDeviceDetail, fetchDeviceList, updateDevice } from '@/api/device';
import { fetchAdminStoreOptions } from '@/api/store';
import { t } from '@/i18n';
import type { DeviceFormPayload, DeviceItem } from '@/types/device';
import type { StoreOption } from '@/types/store';
import { formatDateTime, formatDeviceStatus } from '@/utils/format';

const loading = ref(false);
const submitting = ref(false);
const tableData = ref<DeviceItem[]>([]);
const detailVisible = ref(false);
const formVisible = ref(false);
const detailData = ref<DeviceItem | null>(null);
const storeOptions = ref<StoreOption[]>([]);
const editingId = ref<number | null>(null);
const formMode = ref<'create' | 'edit'>('create');
const formRef = ref<FormInstance>();

const filters = reactive({
  storeId: undefined as number | undefined,
  keyword: '',
});

const deviceTypeOptions = ['washer', 'controller', 'gateway'];
const deviceRoleOptions = ['main', 'assistant'];
const deviceStatusOptions = ['offline', 'idle', 'running', 'paused', 'fault', 'disabled'];

const createEmptyDeviceForm = (): DeviceFormPayload => ({
  storeId: 0,
  deviceCode: '',
  deviceName: '',
  deviceType: 'washer',
  deviceRole: 'main',
  deviceStatus: 'offline',
  protocolType: '',
  firmwareVersion: '',
  remark: '',
});

const deviceForm = reactive<DeviceFormPayload>(createEmptyDeviceForm());

const deviceRules: FormRules<DeviceFormPayload> = {
  storeId: [
    {
      required: true,
      validator: (_rule, value, callback) => {
        if (Number(value) > 0) {
          callback();
          return;
        }
        callback(new Error(t('devices.messages.storeRequired')));
      },
      trigger: 'change',
    },
  ],
};

const formTitle = computed(() => {
  return formMode.value === 'create' ? t('devices.dialogs.createTitle') : t('devices.dialogs.editTitle');
});

const activeStoreLabel = computed(() => {
  if (!filters.storeId) {
    return t('devices.filters.allStores');
  }

  return resolveStoreName(filters.storeId);
});

const assignForm = (payload: Partial<DeviceFormPayload>) => {
  Object.assign(deviceForm, createEmptyDeviceForm(), payload);
};

const resolveStoreName = (storeId?: number) => {
  const matched = storeOptions.value.find((item) => item.id === storeId);
  return matched?.storeName || t('common.noData');
};

const getDeviceTagType = (status?: string) => {
  if (status === 'running') {
    return 'primary';
  }
  if (status === 'idle') {
    return 'success';
  }
  if (status === 'fault' || status === 'disabled') {
    return 'danger';
  }
  return 'info';
};

const loadStoreOptions = async () => {
  try {
    storeOptions.value = await fetchAdminStoreOptions();
  } catch (error) {
    ElMessage.error(t('devices.messages.loadStoresFailed'));
  }
};

const loadDevices = async () => {
  loading.value = true;
  try {
    tableData.value = await fetchDeviceList({
      storeId: filters.storeId,
      keyword: filters.keyword || undefined,
    });
  } catch (error) {
    ElMessage.error(t('devices.messages.loadFailed'));
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  formMode.value = 'create';
  editingId.value = null;
  assignForm({
    storeId: filters.storeId || storeOptions.value[0]?.id || 0,
  });
  formVisible.value = true;
};

const openEdit = async (id: number) => {
  try {
    const device = await fetchDeviceDetail(id);
    formMode.value = 'edit';
    editingId.value = id;
    assignForm({
      storeId: device.storeId,
      deviceCode: device.deviceCode,
      deviceName: device.deviceName,
      deviceType: device.deviceType,
      deviceRole: device.deviceRole,
      deviceStatus: device.deviceStatus || device.status,
      protocolType: device.protocolType,
      firmwareVersion: device.firmwareVersion,
      remark: device.remark,
    });
    formVisible.value = true;
  } catch (error) {
    ElMessage.error(t('devices.messages.loadDetailFailed'));
  }
};

const openDetail = async (id: number) => {
  try {
    detailData.value = await fetchDeviceDetail(id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(t('devices.messages.loadDetailFailed'));
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
      await createDevice(deviceForm);
      ElMessage.success(t('devices.messages.createSuccess'));
    } else if (editingId.value) {
      await updateDevice(editingId.value, deviceForm);
      ElMessage.success(t('devices.messages.updateSuccess'));
    }

    formVisible.value = false;
    await loadDevices();
  } catch (error) {
    ElMessage.error(t('devices.messages.saveFailed'));
  } finally {
    submitting.value = false;
  }
};

const handleFormClosed = () => {
  formRef.value?.clearValidate();
  assignForm({
    storeId: filters.storeId || storeOptions.value[0]?.id || 0,
  });
};

const handleSearch = () => {
  void loadDevices();
};

const handleReset = () => {
  filters.storeId = undefined;
  filters.keyword = '';
  void loadDevices();
};

onMounted(async () => {
  await loadStoreOptions();
  await loadDevices();
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
