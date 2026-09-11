<template>
  <div class="page-stack">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">Ranking Operations</p>
        <h3>排行榜管理</h3>
        <span>后台可查看 24 小时榜、30 日榜、总榜，并为单个用户设置最终展示洗车时长。</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ rankingRows.length }}</strong>
          <span>榜单用户</span>
        </div>
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>调整记录</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <div class="scope-tabs">
        <button
          v-for="scope in scopeOptions"
          :key="scope.value"
          class="scope-tab"
          :class="{ active: activeScope === scope.value }"
          @click="handleScopeChange(scope.value)"
        >
          {{ scope.label }}
        </button>
      </div>
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="用户关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="昵称、手机号、用户编号或姓名"
            style="width: 280px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="section-toolbar">
        <div>
          <h4>{{ activeScopeLabel }}明细</h4>
          <p>展示时长由真实洗车时长和后台调整差值组成，不影响真实订单和结算。</p>
        </div>
        <el-button type="primary" @click="loadPage">刷新</el-button>
      </div>

      <el-table v-loading="rankingLoading" :data="filteredRankingRows" border>
        <el-table-column prop="rank" label="排名" width="84" />
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="34" :src="row.avatarUrl">{{ avatarText(row.nickname) }}</el-avatar>
              <div>
                <strong>{{ row.nickname || '--' }}</strong>
                <span>ID {{ row.userId }} / {{ row.userNo || '--' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="手机号" min-width="140">
          <template #default="{ row }">{{ row.mobile || '--' }}</template>
        </el-table-column>
        <el-table-column prop="realDurationText" label="真实洗车时长" min-width="150" />
        <el-table-column prop="durationText" label="榜单显示时长" min-width="150" />
        <el-table-column prop="displayAdjustmentText" label="后台调整" min-width="130" />
        <el-table-column prop="orderCount" label="真实订单数" min-width="120" />
        <el-table-column label="最近结束/调整时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.latestEndTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="132">
          <template #default="{ row }">
            <el-button link type="primary" @click="openSetDialog(row)">设置时长</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="table-card">
      <div class="section-toolbar">
        <div>
          <h4>{{ activeScopeLabel }}调整记录</h4>
          <p>每次设置会替换该用户在当前榜单的旧调整记录。</p>
        </div>
      </div>

      <el-table v-loading="adjustmentLoading" :data="adjustmentRows" border>
        <el-table-column prop="id" label="记录 ID" min-width="100" />
        <el-table-column prop="scopeName" label="榜单" min-width="110" />
        <el-table-column prop="userId" label="用户 ID" min-width="100" />
        <el-table-column prop="userNo" label="用户编号" min-width="150">
          <template #default="{ row }">{{ row.userNo || '--' }}</template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="130">
          <template #default="{ row }">{{ row.nickname || '--' }}</template>
        </el-table-column>
        <el-table-column prop="mobile" label="手机号" min-width="140">
          <template #default="{ row }">{{ row.mobile || '--' }}</template>
        </el-table-column>
        <el-table-column prop="durationText" label="后台调整" min-width="150" />
        <el-table-column prop="remark" label="备注" min-width="200">
          <template #default="{ row }">{{ row.remark || '--' }}</template>
        </el-table-column>
        <el-table-column label="设置时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.occurredAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :current-page="pagination.page"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="setDialogVisible" title="设置排行榜展示时长" width="560px" @closed="resetSetForm">
      <el-alert
        title="这里设置的是当前榜单最终显示的分钟数，系统会自动换算与真实洗车时长的差值。"
        type="info"
        :closable="false"
        class="form-alert"
      />
      <el-form ref="setFormRef" :model="setForm" :rules="rules" label-width="128px">
        <el-form-item label="当前榜单">
          <el-tag type="success">{{ activeScopeLabel }}</el-tag>
        </el-form-item>
        <el-form-item label="用户">
          <span>{{ selectedUserLabel }}</span>
        </el-form-item>
        <el-form-item label="显示分钟数" prop="durationMinutes">
          <el-input-number
            v-model="setForm.durationMinutes"
            :min="0"
            :max="100000"
            :precision="0"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="setForm.remark" maxlength="255" show-word-limit type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="setDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSetSubmit">确认保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import {
  deleteRankingDisplayAdjustment,
  fetchRankingDisplayAdjustments,
  fetchRankingDurationRows,
  setRankingDisplayAdjustment,
} from '@/api/ranking-display-adjustment';
import type {
  AdminRankingDurationItem,
  RankingDisplayAdjustmentCreatePayload,
  RankingDisplayAdjustmentItem,
} from '@/types/ranking-display-adjustment';
import { formatDateTime } from '@/utils/format';

const scopeOptions = [
  { value: 'day', label: '24小时榜' },
  { value: 'month', label: '30日榜' },
  { value: 'total', label: '总榜' },
];

const activeScope = ref('day');
const rankingLoading = ref(false);
const adjustmentLoading = ref(false);
const submitting = ref(false);
const setDialogVisible = ref(false);
const setFormRef = ref<FormInstance>();
const rankingRows = ref<AdminRankingDurationItem[]>([]);
const adjustmentRows = ref<RankingDisplayAdjustmentItem[]>([]);

const filters = reactive({
  keyword: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const createEmptySetForm = (): RankingDisplayAdjustmentCreatePayload => ({
  userId: 0,
  scope: activeScope.value,
  durationMinutes: 0,
  remark: '',
});

const setForm = reactive<RankingDisplayAdjustmentCreatePayload>(createEmptySetForm());

const rules: FormRules<RankingDisplayAdjustmentCreatePayload> = {
  durationMinutes: [{ required: true, message: '请输入显示分钟数', trigger: 'change' }],
};

const activeScopeLabel = computed(() => {
  return scopeOptions.find((scope) => scope.value === activeScope.value)?.label || '总榜';
});

const selectedUserLabel = computed(() => {
  const row = rankingRows.value.find((item) => item.userId === setForm.userId);
  if (!row) {
    return `用户 ID ${setForm.userId || '--'}`;
  }
  return `${row.nickname || '--'} / ID ${row.userId} / ${row.mobile || '无手机号'}`;
});

const filteredRankingRows = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  if (!keyword) {
    return rankingRows.value;
  }
  return rankingRows.value.filter((row) => {
    return [row.userId, row.userNo, row.nickname, row.mobile]
      .map((value) => String(value || '').toLowerCase())
      .some((value) => value.includes(keyword));
  });
});

const avatarText = (nickname?: string) => {
  const text = String(nickname || '').trim();
  return text ? text.slice(0, 1) : '用';
};

const resolveErrorMessage = (error: unknown, fallback: string) => {
  return error instanceof Error && error.message.trim() ? error.message.trim() : fallback;
};

const loadRankingRows = async () => {
  rankingLoading.value = true;
  try {
    rankingRows.value = await fetchRankingDurationRows({
      scope: activeScope.value,
      limit: 500,
    });
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '排行榜明细加载失败'));
  } finally {
    rankingLoading.value = false;
  }
};

const loadAdjustments = async () => {
  adjustmentLoading.value = true;
  try {
    const data = await fetchRankingDisplayAdjustments({
      page: pagination.page,
      size: pagination.size,
      scope: activeScope.value,
      keyword: filters.keyword.trim() || undefined,
    });
    adjustmentRows.value = data.records || [];
    pagination.total = Number(data.total || 0);
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '排行榜调整记录加载失败'));
  } finally {
    adjustmentLoading.value = false;
  }
};

const loadPage = async () => {
  await Promise.all([loadRankingRows(), loadAdjustments()]);
};

const handleScopeChange = (scope: string) => {
  activeScope.value = scope;
  pagination.page = 1;
  void loadPage();
};

const openSetDialog = (row: AdminRankingDurationItem) => {
  Object.assign(setForm, {
    userId: row.userId,
    scope: activeScope.value,
    durationMinutes: Number(row.durationMinutes || 0),
    remark: '后台设置排行榜显示时长',
  });
  setDialogVisible.value = true;
};

const handleSetSubmit = async () => {
  if (!setFormRef.value) {
    return;
  }
  const valid = await setFormRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    await setRankingDisplayAdjustment({
      userId: Number(setForm.userId),
      scope: activeScope.value,
      durationMinutes: Number(setForm.durationMinutes),
      remark: setForm.remark?.trim() || undefined,
    });
    ElMessage.success('排行榜展示时长已保存');
    setDialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '保存排行榜展示时长失败'));
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (row: RankingDisplayAdjustmentItem) => {
  try {
    await ElMessageBox.confirm(
      `删除后将移除${row.scopeName || activeScopeLabel.value}的这条后台调整，真实订单不会受影响。`,
      '删除调整记录',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    );
    await deleteRankingDisplayAdjustment(row.id);
    ElMessage.success('排行榜调整记录已删除');
    if (adjustmentRows.value.length === 1 && pagination.page > 1) {
      pagination.page -= 1;
    }
    await loadPage();
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    ElMessage.error(resolveErrorMessage(error, '删除排行榜调整记录失败'));
  }
};

const handleSearch = () => {
  pagination.page = 1;
  void loadAdjustments();
};

const handleReset = () => {
  filters.keyword = '';
  pagination.page = 1;
  void loadAdjustments();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  void loadAdjustments();
};

const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.page = 1;
  void loadAdjustments();
};

const resetSetForm = () => {
  setFormRef.value?.clearValidate();
  Object.assign(setForm, createEmptySetForm());
};

onMounted(() => {
  void loadPage();
});
</script>

<style scoped lang="scss">
.page-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.scope-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.scope-tab {
  min-width: 96px;
  height: 36px;
  border: 1px solid #d8e3e5;
  border-radius: 8px;
  background: #ffffff;
  color: #46616a;
  cursor: pointer;
  font-weight: 700;
}

.scope-tab.active {
  border-color: #2f9aa5;
  background: #e8f7f8;
  color: #237b84;
}

.section-toolbar,
.dialog-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.section-toolbar {
  align-items: flex-start;
  padding-bottom: 16px;
}

.section-toolbar h4 {
  margin: 0;
  color: #1f2933;
  font-size: 17px;
}

.section-toolbar p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell strong,
.user-cell span {
  display: block;
}

.user-cell strong {
  color: #1f2933;
  font-size: 14px;
}

.user-cell span {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.dialog-footer {
  justify-content: flex-end;
}

.form-alert {
  margin-bottom: 18px;
}
</style>
