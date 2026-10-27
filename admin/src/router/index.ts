import { createRouter, createWebHistory } from 'vue-router'
import { ADMIN_PROFILE_KEY, ADMIN_TOKEN_KEY } from '@/stores/auth'
import AdminLayout from '@/layouts/AdminLayout.vue'

function hasPermission(permission: unknown): boolean {
  if (typeof permission !== 'string' || !permission) {
    return true
  }
  const raw = localStorage.getItem(ADMIN_PROFILE_KEY)
  if (!raw) {
    // 有 token 但本地 profile 缺失时，允许进入，由 AdminLayout 调用 /admin/me 补齐
    return true
  }
  try {
    const profile = JSON.parse(raw) as { role?: string; permissions?: string[] }
    if (!Array.isArray(profile.permissions)) {
      // 兼容旧版本缓存（没有 permissions 字段）
      return true
    }
    return profile.role === 'SUPER_ADMIN' || profile.permissions.includes(permission)
  } catch {
    return true
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '管理员登录', public: true },
    },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '仪表盘', permission: 'DASHBOARD_READ' },
        },
        {
          path: 'products',
          name: 'products',
          component: () => import('@/views/products/ProductListView.vue'),
          meta: { title: '商品管理', permission: 'PRODUCT_READ' },
        },
        {
          path: 'categories',
          name: 'categories',
          component: () => import('@/views/categories/CategoryListView.vue'),
          meta: { title: '分类管理', permission: 'CATEGORY_READ' },
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/orders/OrderListView.vue'),
          meta: { title: '订单管理', permission: 'ORDER_READ' },
        },
        {
          path: 'after-sales',
          name: 'after-sales',
          component: () => import('@/views/after-sales/AfterSaleListView.vue'),
          meta: { title: '售后管理', permission: 'AFTER_SALE_READ' },
        },
        {
          path: 'stock',
          name: 'stock',
          component: () => import('@/views/stock/StockListView.vue'),
          meta: { title: '库存中心', permission: 'STOCK_READ' },
        },
        {
          path: 'shipping',
          name: 'shipping',
          component: () => import('@/views/shipping/ShippingTemplateView.vue'),
          meta: { title: '运费模板', permission: 'PRODUCT_READ' },
        },
        {
          path: 'coupons',
          name: 'coupons',
          component: () => import('@/views/coupons/CouponListView.vue'),
          meta: { title: '优惠券', permission: 'COUPON_READ' },
        },
        {
          path: 'reviews',
          name: 'reviews',
          component: () => import('@/views/reviews/ReviewListView.vue'),
          meta: { title: '评价管理', permission: 'REVIEW_READ' },
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/views/notifications/NotificationListView.vue'),
          meta: { title: '通知记录', permission: 'NOTIFICATION_READ' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/users/UserListView.vue'),
          meta: { title: '用户管理', permission: 'USER_READ' },
        },
        {
          path: 'support',
          name: 'support',
          component: () => import('@/views/support/SupportTicketView.vue'),
          meta: { title: '客服工单', permission: 'SUPPORT_READ' },
        },
        {
          path: 'banners',
          name: 'banners',
          component: () => import('@/views/banners/BannerListView.vue'),
          meta: { title: '轮播图管理', permission: 'BANNER_READ' },
        },
        {
          path: 'admins',
          name: 'admins',
          component: () => import('@/views/admins/AdminListView.vue'),
          meta: { title: '管理员管理', permission: 'ADMIN_MANAGE' },
        },
        {
          path: 'operation-logs',
          name: 'operation-logs',
          component: () => import('@/views/logs/OperationLogView.vue'),
          meta: { title: '操作日志', permission: 'AUDIT_READ' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard',
    },
  ],
})

router.beforeEach((to) => {
  const hasToken = Boolean(localStorage.getItem(ADMIN_TOKEN_KEY))
  if (to.meta.requiresAuth && !hasToken) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && hasToken) {
    return { path: '/dashboard' }
  }
  if (!hasPermission(to.meta.permission)) {
    if (to.path === '/dashboard') {
      return true
    }
    return { path: '/dashboard' }
  }
  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '管理后台')} · 优选商城`
})

export default router
