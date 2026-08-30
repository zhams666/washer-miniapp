import { createRouter, createWebHistory } from 'vue-router';
import CardUsagePage from '@/views/cards/CardUsagePage.vue';
import DashboardActivityPage from '@/views/dashboard/DashboardActivityPage.vue';
import DashboardPage from '@/views/dashboard/DashboardPage.vue';
import DeviceListPage from '@/views/devices/DeviceListPage.vue';
import FranchiseContactPage from '@/views/franchise/FranchiseContactPage.vue';
import AdminLayout from '@/layout/AdminLayout.vue';
import OrderListPage from '@/views/orders/OrderListPage.vue';
import PaymentDetailPage from '@/views/payments/PaymentDetailPage.vue';
import MiniAdminPermissionPage from '@/views/permissions/MiniAdminPermissionPage.vue';
import MembershipSettingsPage from '@/views/membership/MembershipSettingsPage.vue';
import PointMallProductPage from '@/views/points-mall/PointMallProductPage.vue';
import SettlementBillPage from '@/views/settlements/SettlementBillPage.vue';
import SettlementDetailPage from '@/views/settlements/SettlementDetailPage.vue';
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
          redirect: '/dashboard',
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardPage,
          meta: {
            title: '首页总览',
            eyebrow: 'Operations Dashboard',
            description: '当天次卡、钱包、充值和购卡核心数据可视化。',
          },
        },
        {
          path: 'activities',
          name: 'activities',
          component: DashboardActivityPage,
          meta: {
            title: '业务动态',
            eyebrow: 'Operations Feed',
            description: '集中查看充值、消费、购卡、发卡和核销动态。',
          },
        },
        {
          path: 'orders',
          name: 'orders',
          component: OrderListPage,
          meta: {
            title: 'Orders',
            eyebrow: 'Admin Console',
            description: 'Minimal order operations view',
          },
        },
        {
          path: 'stores',
          name: 'stores',
          component: StoreListPage,
          meta: {
            title: 'Stores',
            eyebrow: 'Master Data',
            description: 'Create, edit, and view store records',
          },
        },
        {
          path: 'franchise-contacts',
          name: 'franchise-contacts',
          component: FranchiseContactPage,
          meta: {
            title: '加盟联系',
            eyebrow: 'Franchise Leads',
            description: '查看小程序提交的加盟姓名和电话号码',
          },
        },
        {
          path: 'devices',
          name: 'devices',
          component: DeviceListPage,
          meta: {
            title: 'Devices',
            eyebrow: 'Master Data',
            description: 'Maintain device ownership and configuration',
          },
        },
        {
          path: 'users',
          name: 'users',
          component: UserListPage,
          meta: {
            title: 'Users',
            eyebrow: 'Operations View',
            description: 'User assets overview and recent activity',
          },
        },
        {
          path: 'mini-admin-permissions',
          name: 'mini-admin-permissions',
          component: MiniAdminPermissionPage,
          meta: {
            title: '管理端权限',
            eyebrow: 'Permission Center',
            description: '按用户编号设置手机端管理端角色、门店范围和启用状态。',
          },
        },
        {
          path: 'membership',
          name: 'membership',
          component: MembershipSettingsPage,
          meta: {
            title: '会员管理',
            eyebrow: 'Membership Center',
            description: '维护会员日、充值方案、会员订单和用户有效期。',
          },
        },
        {
          path: 'point-mall',
          name: 'point-mall',
          component: PointMallProductPage,
          meta: {
            title: '积分商城',
            eyebrow: 'Point Mall',
            description: '管理积分兑换商品的上架规则和展示顺序。',
          },
        },
        {
          path: 'payment-details',
          name: 'payment-details',
          component: PaymentDetailPage,
          meta: {
            title: 'Payment Details',
            eyebrow: 'Payment Center',
            description: 'Order payment details and links',
          },
        },
        {
          path: 'settlement-details',
          name: 'settlement-details',
          component: SettlementDetailPage,
          meta: {
            title: 'Settlement Details',
            eyebrow: 'Settlement Center',
            description: 'Cross-store principal settlement lines',
          },
        },
        {
          path: 'settlement-bills',
          name: 'settlement-bills',
          component: SettlementBillPage,
          meta: {
            title: 'Settlement Bills',
            eyebrow: 'Settlement Center',
            description: 'Period summaries for cross-store settlements',
          },
        },
        {
          path: 'wallet-transactions',
          name: 'wallet-transactions',
          component: WalletTransactionPage,
          meta: {
            title: 'Wallet Transactions',
            eyebrow: 'Payment Center',
            description: 'Wallet transaction history and links',
          },
        },
        {
          path: 'card-usages',
          name: 'card-usages',
          component: CardUsagePage,
          meta: {
            title: 'Card Usages',
            eyebrow: 'Payment Center',
            description: 'Card usage records and linked orders',
          },
        },
      ],
    },
  ],
});

export default router;
