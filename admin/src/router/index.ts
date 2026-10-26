import { createRouter, createWebHistory } from 'vue-router'
import { ADMIN_TOKEN_KEY } from '@/stores/auth'
import AdminLayout from '@/layouts/AdminLayout.vue'

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
          meta: { title: '仪表盘' },
        },
        {
          path: 'products',
          name: 'products',
          component: () => import('@/views/products/ProductListView.vue'),
          meta: { title: '商品管理' },
        },
        {
          path: 'categories',
          name: 'categories',
          component: () => import('@/views/categories/CategoryListView.vue'),
          meta: { title: '分类管理' },
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/orders/OrderListView.vue'),
          meta: { title: '订单管理' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/users/UserListView.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'banners',
          name: 'banners',
          component: () => import('@/views/banners/BannerListView.vue'),
          meta: { title: '轮播图管理' },
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
  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '管理后台')} · 优选商城`
})

export default router
