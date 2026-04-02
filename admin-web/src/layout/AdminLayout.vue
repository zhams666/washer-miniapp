<template>
  <div class="admin-shell">
    <aside class="admin-shell__sidebar">
      <div class="brand-card">
        <p class="brand-card__eyebrow">{{ t('layout.brandEyebrow') }}</p>
        <h1>{{ t('layout.title') }}</h1>
        <span>{{ t('layout.subtitle') }}</span>
      </div>
      <RouterLink class="nav-link" to="/orders" active-class="is-active">
        {{ t('layout.menuOrders') }}
      </RouterLink>
      <RouterLink class="nav-link" to="/stores" active-class="is-active">
        {{ t('layout.menuStores') }}
      </RouterLink>
      <RouterLink class="nav-link" to="/devices" active-class="is-active">
        {{ t('layout.menuDevices') }}
      </RouterLink>
      <RouterLink class="nav-link" to="/users" active-class="is-active">
        {{ t('layout.menuUsers') }}
      </RouterLink>
      <RouterLink class="nav-link" to="/payment-details" active-class="is-active">
        {{ t('layout.menuPaymentDetails') }}
      </RouterLink>
      <RouterLink class="nav-link" to="/wallet-transactions" active-class="is-active">
        {{ t('layout.menuWalletTransactions') }}
      </RouterLink>
      <RouterLink class="nav-link" to="/card-usages" active-class="is-active">
        {{ t('layout.menuCardUsages') }}
      </RouterLink>
    </aside>

    <main class="admin-shell__content">
      <header class="page-header">
        <div>
          <p class="page-header__eyebrow">{{ routeEyebrow }}</p>
          <h2>{{ routeTitle }}</h2>
          <p class="page-header__desc">{{ routeDescription }}</p>
        </div>
      </header>
      <section class="page-body">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { t } from '@/i18n';

const route = useRoute();

const routeEyebrow = computed(() => String(route.meta.eyebrow || t('layout.headerEyebrow')));
const routeTitle = computed(() => String(route.meta.title || t('layout.headerTitle')));
const routeDescription = computed(() => String(route.meta.description || t('layout.headerDesc')));
</script>
