<template>
  <el-drawer :model-value="visible" :title="drawerTitle" size="1120px" @close="emit('close')">
    <div class="card-manager">
      <div class="manager-toolbar">
        <el-form :inline="true" :model="filters" class="filter-form">
          <el-form-item label="门店">
            <el-select v-model="filters.storeId" clearable filterable style="width: 180px">
              <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="String(store.id)" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filters.status" clearable style="width: 140px">
              <el-option label="有效" value="active" />
              <el-option label="已锁定" value="locked" />
              <el-option label="已用完" value="used_up" />
              <el-option label="已过期" value="expired" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
          </el-form-item>
          <el-form-item label="卡号">
            <el-input v-model="filters.cardNo" clearable style="width: 220px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <div class="manager-toolbar__actions">
          <el-button type="primary" @click="openAddDialog">人工增加</el-button>
          <el-button type="danger" plain @click="openReduceDialog()">人工减少</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="cardNo" label="卡号" min-width="190" />
        <el-table-column prop="storeName" label="门店" min-width="140" />
        <el-table-column prop="sourceChannel" label="来源" min-width="110" />
        <el-table-column prop="cardType" label="类型" min-width="100" />
        <el-table-column prop="remainingTimes" label="剩余" min-width="80" align="center" />
        <el-table-column prop="usedTimes" label="已用" min-width="80" align="center" />
        <el-table-column prop="totalTimes" label="总次" min-width="80" align="center" />
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">{{ formatCardStatus(row.status) }}</template>
        </el-table-column>
        <el-table-column label="有效期" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="danger" :disabled="!isReducible(row)" @click="openReduceDialog(row)">减少</el-button>
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

    <el-dialog v-model="addVisible" title="人工增加次卡" width="560px" append-to-body @closed="resetAddForm">
      <el-form :model="addForm" label-width="110px">
        <el-form-item label="门店">
          <el-select v-model="addForm.storeId" filterable clearable style="width: 260px">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="Number(store.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="发放张数">
          <el-input-number v-model="addForm.count" :min="1" :max="200" :step="1" />
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="addForm.effectiveTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 260px" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker v-model="addForm.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 260px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAddSubmit">确认增加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reduceVisible" title="人工减少次卡" width="560px" append-to-body @closed="resetReduceForm">
      <el-alert
        v-if="reduceForm.userCardIds.length"
        type="warning"
        :closable="false"
        show-icon
        class="reduce-alert"
        title="将作废当前选中的单张次卡。"
      />
      <el-form :model="reduceForm" label-width="110px">
        <el-form-item v-if="!reduceForm.userCardIds.length" label="门店">
          <el-select v-model="reduceForm.storeId" filterable clearable style="width: 260px">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.storeName" :value="Number(store.id)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!reduceForm.userCardIds.length" label="减少张数">
          <el-input-number v-model="reduceForm.count" :min="1" :max="200" :step="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="reduceForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reduceVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleReduceSubmit">确认减少</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="次卡详情" width="900px" append-to-body>
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="卡号">{{ detailData.card.cardNo || '--' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ formatCardStatus(detailData.card.status) }}</el-descriptions-item>
          <el-descriptions-item label="门店">{{ detailData.card.storeName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ detailData.card.sourceChannel || '--' }}</el-descriptions-item>
          <el-descriptions-item label="次数">
            {{ detailData.card.remainingTimes || 0 }} / {{ detailData.card.totalTimes || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="外部单号">{{ detailData.card.externalOrderNo || '--' }}</el-descriptions-item>
          <el-descriptions-item label="生效时间">{{ formatDateTime(detailData.card.effectiveTime) }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ formatDateTime(detailData.card.expireTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.card.remark || '--' }}</el-descriptions-item>
        </el-descriptions>

        <h3 class="usage-title">使用记录</h3>
        <el-table v-if="detailData.usageRecords.length" :data="detailData.usageRecords" border>
          <el-table-column prop="usageNo" label="使用记录号" min-width="170" />
          <el-table-column prop="storeName" label="门店" min-width="140" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="usedTimes" label="使用次数" width="100" />
          <el-table-column label="使用时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.usageTime) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无使用记录" />
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, reactive, ref, watch } from 'vue';
import {
  fetchUserCardDetail,
  fetchUserCards,
  manualAddUserCards,
  manualReduceUserCards,
} from '@/api/user';
import type {
  AdminUserCardDetail,
  AdminUserCardPageItem,
  AdminUserItem,
} from '@/types/user';
import type { StoreOption } from '@/types/store';
import { formatCardStatus, formatDateTime } from '@/utils/format';

const props = defineProps<{
  visible: boolean;
  user: AdminUserItem | null;
  storeOptions: StoreOption[];
}>();

const emit = defineEmits<{
  close: [];
  changed: [];
}>();

const loading = ref(false);
const submitting = ref(false);
const addVisible = ref(false);
const reduceVisible = ref(false);
const detailVisible = ref(false);
const tableData = ref<AdminUserCardPageItem[]>([]);
const detailData = ref<AdminUserCardDetail | null>(null);

const filters = reactive({
  storeId: '',
  status: '',
  cardNo: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const addForm = reactive({
  storeId: 0,
  count: 1,
  effectiveTime: '',
  expireTime: '',
  remark: '',
});

const reduceForm = reactive({
  storeId: 0,
  count: 1,
  userCardIds: [] as number[],
  remark: '',
});

const drawerTitle = computed(() => {
  const name = props.user?.nickname || props.user?.userNo || props.user?.id || '';
  return name ? `用户次卡 - ${name}` : '用户次卡';
});

const currentUserId = computed(() => Number(props.user?.id || 0));

const parseOptionalNumber = (value: string) => {
  const parsed = Number(value);
  return value && !Number.isNaN(parsed) ? parsed : undefined;
};

const loadCards = async () => {
  if (!currentUserId.value) {
    return;
  }
  loading.value = true;
  try {
    const data = await fetchUserCards(currentUserId.value, {
      page: pagination.page,
      size: pagination.size,
      storeId: parseOptionalNumber(filters.storeId),
      status: filters.status || undefined,
      cardNo: filters.cardNo || undefined,
    });
    tableData.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error('用户次卡加载失败');
  } finally {
    loading.value = false;
  }
};

const openAddDialog = () => {
  addVisible.value = true;
};

const openReduceDialog = (card?: AdminUserCardPageItem) => {
  resetReduceForm();
  if (card) {
    reduceForm.userCardIds = [card.id];
    reduceForm.storeId = Number(card.storeId || 0);
  }
  reduceVisible.value = true;
};

const openDetail = async (card: AdminUserCardPageItem) => {
  if (!currentUserId.value) {
    return;
  }
  try {
    detailData.value = await fetchUserCardDetail(currentUserId.value, card.id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error('次卡详情加载失败');
  }
};

const handleAddSubmit = async () => {
  if (!currentUserId.value) {
    return;
  }
  if (!addForm.storeId) {
    ElMessage.error('请选择门店');
    return;
  }
  if (!addForm.count || addForm.count <= 0) {
    ElMessage.error('请输入发放张数');
    return;
  }

  submitting.value = true;
  try {
    await manualAddUserCards(currentUserId.value, {
      storeId: addForm.storeId,
      count: addForm.count,
      effectiveTime: normalizeDateTimeForApi(addForm.effectiveTime),
      expireTime: normalizeDateTimeForApi(addForm.expireTime),
      remark: addForm.remark || undefined,
    });
    ElMessage.success('次卡已发放');
    addVisible.value = false;
    emit('changed');
    await loadCards();
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '次卡发放失败'));
  } finally {
    submitting.value = false;
  }
};

const handleReduceSubmit = async () => {
  if (!currentUserId.value) {
    return;
  }
  if (!reduceForm.userCardIds.length && (!reduceForm.count || reduceForm.count <= 0)) {
    ElMessage.error('请输入减少张数');
    return;
  }

  submitting.value = true;
  try {
    await manualReduceUserCards(currentUserId.value, {
      storeId: reduceForm.userCardIds.length ? undefined : reduceForm.storeId || undefined,
      count: reduceForm.userCardIds.length ? undefined : reduceForm.count,
      userCardIds: reduceForm.userCardIds.length ? reduceForm.userCardIds : undefined,
      remark: reduceForm.remark || undefined,
    });
    ElMessage.success('次卡已减少');
    reduceVisible.value = false;
    emit('changed');
    await loadCards();
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '次卡减少失败，请确认可用次卡数量是否充足'));
  } finally {
    submitting.value = false;
  }
};

const isReducible = (card: AdminUserCardPageItem) => {
  return card.status === 'active' && Number(card.remainingTimes || 0) > 0;
};

const normalizeDateTimeForApi = (value: string) => {
  return value ? value.replace(' ', 'T') : undefined;
};

const extractErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
};

const handleSearch = () => {
  pagination.page = 1;
  void loadCards();
};

const handleReset = () => {
  filters.storeId = '';
  filters.status = '';
  filters.cardNo = '';
  pagination.page = 1;
  void loadCards();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  void loadCards();
};

const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.page = 1;
  void loadCards();
};

const resetAddForm = () => {
  Object.assign(addForm, {
    storeId: 0,
    count: 1,
    effectiveTime: '',
    expireTime: '',
    remark: '',
  });
};

function resetReduceForm() {
  Object.assign(reduceForm, {
    storeId: 0,
    count: 1,
    userCardIds: [],
    remark: '',
  });
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      pagination.page = 1;
      void loadCards();
    }
  }
);
</script>

<style scoped lang="scss">
.card-manager {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.manager-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.manager-toolbar__actions {
  display: flex;
  gap: 10px;
  padding-top: 1px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 10px;
}

.reduce-alert {
  margin-bottom: 16px;
}

.usage-title {
  margin: 18px 0 12px;
}

@media (max-width: 960px) {
  .manager-toolbar {
    flex-direction: column;
  }
}
</style>
