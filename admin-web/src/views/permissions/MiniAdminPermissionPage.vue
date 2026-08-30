<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">Permission Center</p>
        <h3>管理端权限配置</h3>
        <span>通过用户编号绑定手机端管理权限，保存后会同步到小程序商家管理端。</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ currentPermission.exists ? '已配置' : '未配置' }}</strong>
          <span>当前状态</span>
        </div>
        <div>
          <strong>{{ currentPermission.permissions.length }}</strong>
          <span>权限点</span>
        </div>
      </div>
    </div>

    <div class="permission-layout">
      <section class="table-card permission-query">
        <h4>查询用户</h4>
        <el-form label-position="top">
          <el-form-item label="用户编号">
            <el-input
              v-model="queryUserNo"
              clearable
              placeholder="请输入用户编号，如 Uxxxxxxxx"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-button type="primary" :loading="loading" @click="handleQuery">
            查询权限
          </el-button>
        </el-form>

        <el-descriptions v-if="currentPermission.userNo" class="user-summary" :column="1" border>
          <el-descriptions-item label="用户编号">{{ currentPermission.userNo }}</el-descriptions-item>
          <el-descriptions-item label="用户昵称">{{ currentPermission.nickname || '--' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentPermission.mobile || '--' }}</el-descriptions-item>
          <el-descriptions-item label="微信标识">{{ currentPermission.openId || '--' }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="table-card permission-form-card">
        <div class="toolbar-row permission-toolbar">
          <div>
            <h4>权限设置</h4>
            <p>角色层级会决定手机端管理端可用功能，门店范围会限制可查询的数据。</p>
          </div>
          <div>
            <el-button
              type="danger"
              plain
              :disabled="!currentPermission.exists"
              :loading="deleting"
              @click="handleDelete"
            >
              删除权限
            </el-button>
            <el-button
              type="primary"
              :disabled="!currentPermission.userNo"
              :loading="saving"
              @click="handleSave"
            >
              保存权限
            </el-button>
          </div>
        </div>

        <el-empty v-if="!currentPermission.userNo" description="请先输入用户编号查询" />

        <el-form v-else label-width="110px" class="permission-form">
          <div class="form-grid">
            <el-form-item label="员工名称">
              <el-input v-model="form.staffName" placeholder="默认使用昵称、姓名或手机号" />
            </el-form-item>
            <el-form-item label="启用状态">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="停用"
              />
            </el-form-item>
            <el-form-item label="角色层级">
              <el-select v-model="form.roleCode" class="full-width" @change="handleRoleChange">
                <el-option
                  v-for="role in options.roles"
                  :key="role.value"
                  :label="role.label"
                  :value="role.value"
                >
                  <span>{{ role.label }}</span>
                  <small>{{ role.description }}</small>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="数据范围">
              <el-select v-model="form.dataScope" class="full-width">
                <el-option
                  v-for="scope in options.dataScopes"
                  :key="scope.value"
                  :label="scope.label"
                  :value="scope.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="加盟主体ID" v-if="form.dataScope === 'franchisee'">
              <el-input-number v-model="form.franchiseeId" :min="1" :controls="false" class="full-width" />
            </el-form-item>
            <el-form-item label="绑定门店" class="form-grid__wide">
              <el-select
                v-model="form.storeIds"
                class="full-width"
                multiple
                collapse-tags
                collapse-tags-tooltip
                :disabled="form.dataScope !== 'store'"
                placeholder="指定门店权限时请选择门店"
              >
                <el-option
                  v-for="store in options.stores"
                  :key="store.id"
                  :label="store.storeName"
                  :value="store.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="备注" class="form-grid__wide">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可填写权限调整原因" />
            </el-form-item>
          </div>
        </el-form>
      </section>
    </div>

    <section class="table-card" v-if="currentPermission.userNo">
      <div class="permission-preview">
        <div>
          <h4>手机端同步预览</h4>
          <p>
            {{ previewRoleName }} · {{ previewScopeName }}
            <template v-if="form.dataScope === 'store'"> · {{ form.storeIds.length }} 家门店</template>
          </p>
        </div>
        <el-tag :type="form.status === 1 ? 'success' : 'info'" effect="light" round>
          {{ form.status === 1 ? '启用' : '停用' }}
        </el-tag>
      </div>

      <div class="permission-tags">
        <el-tag
          v-for="permission in previewPermissions"
          :key="permission.value"
          effect="light"
          round
        >
          {{ permission.label }}
        </el-tag>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  deleteMiniAdminPermission,
  fetchMiniAdminPermission,
  fetchMiniAdminPermissionOptions,
  saveMiniAdminPermission,
} from '@/api/mini-admin-permission';
import type {
  MiniAdminPermissionItem,
  MiniAdminPermissionOptions,
  MiniAdminPermissionPayload,
} from '@/types/mini-admin-permission';

const emptyPermission = (): MiniAdminPermissionItem => ({
  exists: false,
  userId: 0,
  userNo: '',
  nickname: '',
  mobile: '',
  openId: '',
  roleCode: 'store_manager',
  roleName: '店长',
  dataScope: 'store',
  dataScopeName: '指定门店',
  status: 1,
  remark: '',
  storeIds: [],
  stores: [],
  permissions: [],
});

const options = reactive<MiniAdminPermissionOptions>({
  roles: [],
  dataScopes: [],
  permissions: [],
  stores: [],
});

const form = reactive<MiniAdminPermissionPayload>({
  userNo: '',
  staffName: '',
  roleCode: 'store_manager',
  dataScope: 'store',
  franchiseeId: 1,
  storeIds: [],
  status: 1,
  remark: '',
});

const queryUserNo = ref('');
const currentPermission = ref<MiniAdminPermissionItem>(emptyPermission());
const loading = ref(false);
const saving = ref(false);
const deleting = ref(false);

const permissionMap = computed(() => {
  return new Map(options.permissions.map((item) => [item.value, item]));
});

const roleMap = computed(() => {
  return new Map(options.roles.map((item) => [item.value, item]));
});

const scopeMap = computed(() => {
  return new Map(options.dataScopes.map((item) => [item.value, item]));
});

const previewRoleName = computed(() => {
  return roleMap.value.get(form.roleCode)?.label || currentPermission.value.roleName || '--';
});

const previewScopeName = computed(() => {
  return scopeMap.value.get(form.dataScope)?.label || currentPermission.value.dataScopeName || '--';
});

const previewPermissions = computed(() => {
  return resolvePermissions(form.roleCode).map((code) => {
    return permissionMap.value.get(code) || { value: code, label: code };
  });
});

const loadOptions = async () => {
  const result = await fetchMiniAdminPermissionOptions();
  options.roles = result.roles || [];
  options.dataScopes = result.dataScopes || [];
  options.permissions = result.permissions || [];
  options.stores = result.stores || [];
};

const applyPermission = (item: MiniAdminPermissionItem) => {
  currentPermission.value = item;
  form.userNo = item.userNo || '';
  form.staffName = item.staffName || item.nickname || item.mobile || '';
  form.roleCode = item.roleCode || 'store_manager';
  form.dataScope = item.dataScope || 'store';
  form.franchiseeId = 1;
  form.storeIds = Array.isArray(item.storeIds) ? item.storeIds.slice() : [];
  form.status = item.status === 0 ? 0 : 1;
  form.remark = item.remark || '';
};

const handleQuery = async () => {
  const userNo = queryUserNo.value.trim();
  if (!userNo) {
    ElMessage.warning('请输入用户编号');
    return;
  }
  loading.value = true;
  try {
    const result = await fetchMiniAdminPermission(userNo);
    applyPermission(result);
    if (!result.openId) {
      ElMessage.warning('该用户尚未绑定微信身份，需要先在小程序登录');
    }
  } finally {
    loading.value = false;
  }
};

const handleRoleChange = () => {
  if (form.roleCode === 'platform_admin') {
    form.dataScope = 'platform';
    form.storeIds = [];
  } else if (form.roleCode === 'franchisee_owner') {
    form.dataScope = 'franchisee';
    form.storeIds = [];
  } else if (form.dataScope === 'platform') {
    form.dataScope = 'store';
  }
};

const validateForm = () => {
  if (!form.userNo.trim()) {
    ElMessage.warning('请先查询用户编号');
    return false;
  }
  if (!currentPermission.value.openId) {
    ElMessage.warning('该用户尚未绑定微信身份，不能设置手机端管理权限');
    return false;
  }
  if (form.dataScope === 'store' && form.storeIds.length === 0) {
    ElMessage.warning('指定门店权限至少需要选择一个门店');
    return false;
  }
  return true;
};

const handleSave = async () => {
  if (!validateForm()) {
    return;
  }
  saving.value = true;
  try {
    const result = await saveMiniAdminPermission({
      userNo: form.userNo.trim(),
      staffName: form.staffName?.trim(),
      roleCode: form.roleCode,
      dataScope: form.dataScope,
      franchiseeId: form.franchiseeId || 1,
      storeIds: form.dataScope === 'store' ? form.storeIds : [],
      status: form.status,
      remark: form.remark?.trim(),
    });
    applyPermission(result);
    ElMessage.success('管理权限已保存');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async () => {
  if (!currentPermission.value.exists || !form.userNo) {
    return;
  }
  try {
    await ElMessageBox.confirm('删除后该用户将无法进入手机端管理端，确认删除？', '删除权限', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  deleting.value = true;
  try {
    const result = await deleteMiniAdminPermission(form.userNo);
    applyPermission(result);
    ElMessage.success('管理权限已删除');
  } finally {
    deleting.value = false;
  }
};

const resolvePermissions = (roleCode: string) => {
  const permissions = new Set(['dashboard:view', 'device:view', 'order:view', 'activity:view']);
  if (roleCode === 'platform_admin' || roleCode === 'franchisee_owner') {
    [
      'device:control',
      'user:view',
      'wallet:adjust',
      'card:adjust',
      'finance:view',
      'settlement:view',
      'store:edit',
      'staff:manage',
    ].forEach((item) => permissions.add(item));
  } else if (roleCode === 'store_manager') {
    ['device:control', 'user:view', 'wallet:adjust', 'card:adjust', 'finance:view', 'store:edit'].forEach((item) =>
      permissions.add(item),
    );
  } else if (roleCode === 'store_staff') {
    ['device:control', 'user:view', 'wallet:adjust', 'card:adjust'].forEach((item) => permissions.add(item));
  } else if (roleCode === 'finance') {
    ['user:view', 'finance:view', 'settlement:view'].forEach((item) => permissions.add(item));
  } else if (roleCode === 'operator') {
    ['device:control', 'user:view'].forEach((item) => permissions.add(item));
  }
  return Array.from(permissions);
};

onMounted(loadOptions);
</script>

<style scoped lang="scss">
.permission-layout {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 18px;
}

.permission-query h4,
.permission-form-card h4,
.permission-preview h4 {
  margin: 0;
  color: #102033;
  font-size: 18px;
}

.permission-form-card p,
.permission-preview p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
}

.user-summary {
  margin-top: 18px;
}

.permission-toolbar {
  align-items: flex-start;
}

.permission-form {
  margin-top: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 18px;
}

.form-grid__wide {
  grid-column: 1 / -1;
}

.full-width {
  width: 100%;
}

.permission-preview {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.permission-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

:deep(.el-select-dropdown__item) {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

:deep(.el-select-dropdown__item small) {
  color: #98a2b3;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .permission-layout {
    grid-template-columns: 1fr;
  }
}
</style>
