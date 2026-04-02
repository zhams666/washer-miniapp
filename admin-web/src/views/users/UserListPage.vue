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
        <el-table-column :label="t('users.table.registerSource')" min-width="120">
          <template #default="{ row }">{{ formatRegisterSource(row.registerSource) }}</template>
        </el-table-column>
        <el-table-column :label="t('users.table.lastConsumeTime')" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastConsumeTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('users.table.createdAt')" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('users.table.actions')" fixed="right" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">
              {{ t('common.view') }}
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
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { fetchUserOverview, fetchUserPage } from '@/api/user';
import { t } from '@/i18n';
import type { AdminUserItem, AdminUserOverview } from '@/types/user';
import { formatBooleanFlag, formatDateTime, formatRegisterSource, formatUserStatus } from '@/utils/format';
import UserDetailDrawer from './UserDetailDrawer.vue';

const loading = ref(false);
const tableData = ref<AdminUserItem[]>([]);
const detailVisible = ref(false);
const detailData = ref<AdminUserOverview | null>(null);

const filters = reactive({
  keyword: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

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

const openDetail = async (id: number) => {
  try {
    detailData.value = await fetchUserOverview(id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(t('users.messages.loadDetailFailed'));
  }
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

onMounted(() => {
  void loadUsers();
});
</script>

<style scoped lang="scss">
.page-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
</style>
