import { createRouter, createWebHistory } from 'vue-router';
import CardUsagePage from '@/views/cards/CardUsagePage.vue';
import DeviceListPage from '@/views/devices/DeviceListPage.vue';
import AdminLayout from '@/layout/AdminLayout.vue';
import OrderListPage from '@/views/orders/OrderListPage.vue';
import PaymentDetailPage from '@/views/payments/PaymentDetailPage.vue';
import StoreListPage from '@/views/stores/StoreListPage.vue';
import UserListPage from '@/views/users/UserListPage.vue';
import WalletTransactionPage from '@/views/wallets/WalletTransactionPage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AdminLayout,
      children: [
        {
          path: '',
          redirect: '/orders',
        },
        {
          path: 'orders',
          name: 'orders',
          component: OrderListPage,
          meta: {
            title: '订单管理',
            eyebrow: '独立后台前端',
            description: '围绕订单运营与业务过程查看的最小后台页',
          },
        },
        {
          path: 'stores',
          name: 'stores',
          component: StoreListPage,
          meta: {
            title: '门店管理',
            eyebrow: '基础资料维护',
            description: '用于新增、编辑和查看门店基础信息',
          },
        },
        {
          path: 'devices',
          name: 'devices',
          component: DeviceListPage,
          meta: {
            title: '设备管理',
            eyebrow: '基础资料维护',
            description: '用于维护设备归属门店和基础配置',
          },
        },
        {
          path: 'users',
          name: 'users',
          component: UserListPage,
          meta: {
            title: '用户资产总览',
            eyebrow: '运营排查视图',
            description: '用于查看用户基本信息、钱包、次卡和最近使用情况',
          },
        },
        {
          path: 'payment-details',
          name: 'payment-details',
          component: PaymentDetailPage,
          meta: {
            title: '支付明细',
            eyebrow: '支付与流水中心',
            description: '用于查看订单支付明细和支付链路关联信息',
          },
        },
        {
          path: 'wallet-transactions',
          name: 'wallet-transactions',
          component: WalletTransactionPage,
          meta: {
            title: '钱包流水',
            eyebrow: '支付与流水中心',
            description: '用于查看钱包流水和关联订单信息',
          },
        },
        {
          path: 'card-usages',
          name: 'card-usages',
          component: CardUsagePage,
          meta: {
            title: '次卡使用记录',
            eyebrow: '支付与流水中心',
            description: '用于查看次卡使用记录和关联订单信息',
          },
        },
      ],
    },
  ],
});

export default router;
