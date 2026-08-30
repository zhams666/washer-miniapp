<template>
  <div class="page-stack point-mall-page">
    <div class="hero-panel">
      <div>
        <p class="hero-panel__eyebrow">Point Mall</p>
        <h3>积分商城</h3>
        <span>维护积分兑换商品的库存、限兑规则、展示顺序和上架时段。</span>
      </div>
      <div class="hero-panel__metrics">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>商品总数</span>
        </div>
        <div>
          <strong>{{ publishedCount }}</strong>
          <span>本页上架</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="商品名称">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="输入商品名称"
            style="width: 230px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="商品类型">
          <el-select v-model="filters.productType" clearable placeholder="全部类型" style="width: 150px">
            <el-option v-for="item in productTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 130px">
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openCreate">新增积分商品</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="products" border>
        <el-table-column label="封面" width="92">
          <template #default="{ row }">
            <el-image
              v-if="row.coverImage"
              class="product-cover"
              :src="row.coverImage"
              :preview-src-list="[row.coverImage]"
              fit="cover"
              preview-teleported
            />
            <span v-else class="cover-placeholder">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="商品名称" min-width="180">
          <template #default="{ row }">
            <div class="product-title">{{ row.title }}</div>
            <div v-if="row.description" class="product-description">{{ row.description }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" min-width="110">
          <template #default="{ row }">{{ productTypeLabel(row.productType) }}</template>
        </el-table-column>
        <el-table-column label="兑换积分" min-width="110" align="right">
          <template #default="{ row }">{{ row.pointsPrice }}</template>
        </el-table-column>
        <el-table-column label="库存" min-width="90" align="right">
          <template #default="{ row }">{{ row.stockTotal }}</template>
        </el-table-column>
        <el-table-column label="每人限兑" min-width="110" align="right">
          <template #default="{ row }">{{ row.limitPerUser || '不限' }}</template>
        </el-table-column>
        <el-table-column label="上架时段" min-width="190">
          <template #default="{ row }">{{ availabilityText(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="170">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form label-width="108px" class="product-form">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：10 元洗车抵扣券" />
        </el-form-item>
        <el-form-item label="商品类型" required>
          <el-select v-model="form.productType" style="width: 220px">
            <el-option v-for="item in productTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图片">
          <el-input v-model="form.coverImage" maxlength="500" placeholder="请输入 HTTPS 图片地址" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="兑换积分" required>
              <el-input-number v-model="form.pointsPrice" :min="1" :max="999999" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可兑换库存" required>
              <el-input-number v-model="form.stockTotal" :min="0" :max="999999" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="每人限兑">
              <el-input-number v-model="form.limitPerUser" :min="0" :max="999999" controls-position="right" />
              <span class="form-hint">0 为不限</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="展示排序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="999999" controls-position="right" />
              <span class="form-hint">越小越靠前</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="生效时间">
          <el-date-picker
            v-model="form.effectiveTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            placeholder="留空则立即生效"
            clearable
          />
        </el-form-item>
        <el-form-item label="失效时间">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            placeholder="留空则长期有效"
            clearable
          />
        </el-form-item>
        <el-form-item label="商品状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
        <el-form-item label="商品说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="说明兑换后可获得的权益或领取方式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import {
  createPointMallProduct,
  fetchPointMallProducts,
  updatePointMallProduct,
  updatePointMallProductStatus,
} from '@/api/point-mall';
import type { PointMallProduct, PointMallProductType } from '@/types/point-mall';

const productTypes: Array<{ label: string; value: PointMallProductType }> = [
  { label: '洗车权益', value: 'wash_service' },
  { label: '优惠券', value: 'coupon' },
  { label: '实物礼品', value: 'physical' },
];

const createDefaultProduct = (): PointMallProduct => ({
  title: '',
  description: '',
  coverImage: '',
  productType: 'wash_service',
  pointsPrice: 10,
  stockTotal: 1,
  limitPerUser: 0,
  effectiveTime: '',
  expireTime: '',
  status: 0,
  sortOrder: 0,
});

const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const editingId = ref<number>();
const products = ref<PointMallProduct[]>([]);
const filters = reactive<{ keyword: string; status?: number; productType?: PointMallProductType }>({
  keyword: '',
  status: undefined,
  productType: undefined,
});
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});
const form = reactive<PointMallProduct>(createDefaultProduct());

const publishedCount = computed(() => products.value.filter((item) => item.status === 1).length);
const dialogTitle = computed(() => (editingId.value ? '编辑积分商品' : '新增积分商品'));

const loadProducts = async () => {
  loading.value = true;
  try {
    const result = await fetchPointMallProducts({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status,
      productType: filters.productType,
    });
    products.value = result.records || [];
    pagination.total = Number(result.total || 0);
    pagination.page = Number(result.current || pagination.page);
    pagination.size = Number(result.size || pagination.size);
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '积分商品加载失败'));
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  editingId.value = undefined;
  Object.assign(form, createDefaultProduct());
  dialogVisible.value = true;
};

const openEdit = (product: PointMallProduct) => {
  editingId.value = product.id;
  Object.assign(form, createDefaultProduct(), product, {
    description: product.description || '',
    coverImage: product.coverImage || '',
    effectiveTime: product.effectiveTime || '',
    expireTime: product.expireTime || '',
  });
  dialogVisible.value = true;
};

const validateProduct = () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入商品名称');
    return false;
  }
  if (!Number.isInteger(form.pointsPrice) || form.pointsPrice <= 0) {
    ElMessage.warning('兑换积分必须为正整数');
    return false;
  }
  if (!Number.isInteger(form.stockTotal) || form.stockTotal < 0) {
    ElMessage.warning('库存不能小于 0');
    return false;
  }
  if (form.status === 1 && form.stockTotal === 0) {
    ElMessage.warning('库存为 0 的商品不能上架');
    return false;
  }
  if (!Number.isInteger(form.limitPerUser) || form.limitPerUser < 0) {
    ElMessage.warning('每人限兑次数不能小于 0');
    return false;
  }
  if (!Number.isInteger(form.sortOrder) || form.sortOrder < 0) {
    ElMessage.warning('排序不能小于 0');
    return false;
  }
  if (form.effectiveTime && form.expireTime && form.expireTime <= form.effectiveTime) {
    ElMessage.warning('失效时间必须晚于生效时间');
    return false;
  }
  return true;
};

const saveProduct = async () => {
  if (!validateProduct()) {
    return;
  }
  saving.value = true;
  const payload: PointMallProduct = {
    ...form,
    title: form.title.trim(),
    description: form.description?.trim() || '',
    coverImage: form.coverImage?.trim() || '',
    effectiveTime: form.effectiveTime || '',
    expireTime: form.expireTime || '',
  };
  try {
    if (editingId.value) {
      await updatePointMallProduct(editingId.value, payload);
      ElMessage.success('积分商品已更新');
    } else {
      await createPointMallProduct(payload);
      ElMessage.success('积分商品已创建');
    }
    dialogVisible.value = false;
    loadProducts();
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '积分商品保存失败'));
  } finally {
    saving.value = false;
  }
};

const toggleStatus = async (product: PointMallProduct) => {
  const targetStatus: 0 | 1 = product.status === 1 ? 0 : 1;
  const action = targetStatus === 1 ? '上架' : '下架';
  try {
    await ElMessageBox.confirm(`确定${action}“${product.title}”吗？`, `${action}积分商品`, {
      confirmButtonText: `确认${action}`,
      cancelButtonText: '取消',
      type: 'warning',
    });
    await updatePointMallProductStatus(product.id as number, targetStatus);
    ElMessage.success(`商品已${action}`);
    loadProducts();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(resolveErrorMessage(error, `商品${action}失败`));
    }
  }
};

const handleSearch = () => {
  pagination.page = 1;
  loadProducts();
};

const handleReset = () => {
  filters.keyword = '';
  filters.status = undefined;
  filters.productType = undefined;
  pagination.page = 1;
  loadProducts();
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  loadProducts();
};

const handleSizeChange = (size: number) => {
  pagination.size = size;
  pagination.page = 1;
  loadProducts();
};

const productTypeLabel = (value?: string) =>
  productTypes.find((item) => item.value === value)?.label || '洗车权益';

const formatDateTime = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '无');

const availabilityText = (product: PointMallProduct) => {
  const start = formatDateTime(product.effectiveTime);
  const end = formatDateTime(product.expireTime);
  if (start === '无' && end === '无') {
    return '长期有效';
  }
  return `${start === '无' ? '立即生效' : start} 至 ${end === '无' ? '长期有效' : end}`;
};

const resolveErrorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

onMounted(loadProducts);
</script>

<style scoped>
.product-cover {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  background: #f1f5f9;
}

.cover-placeholder {
  display: inline-flex;
  width: 52px;
  height: 52px;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.product-title {
  font-weight: 600;
}

.product-description {
  max-width: 260px;
  margin-top: 4px;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-hint {
  margin-left: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.product-form :deep(.el-input-number) {
  width: 150px;
}
</style>
