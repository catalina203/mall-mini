<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataBoard,
  Fold,
  Expand,
  Goods,
  FolderOpened,
  Picture,
  SwitchButton,
  Tickets,
  User,
} from '@element-plus/icons-vue'
import { getMe } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const menuOpen = ref(false)

const navGroups = [
  {
    label: '总览',
    items: [{ path: '/dashboard', label: '仪表盘', icon: DataBoard }],
  },
  {
    label: '商品运营',
    items: [
      { path: '/products', label: '商品管理', icon: Goods },
      { path: '/categories', label: '分类管理', icon: FolderOpened },
      { path: '/banners', label: '轮播图管理', icon: Picture },
    ],
  },
  {
    label: '交易与用户',
    items: [
      { path: '/orders', label: '订单管理', icon: Tickets },
      { path: '/users', label: '用户管理', icon: User },
    ],
  },
]

const displayName = computed(
  () => auth.admin?.displayName || auth.admin?.username || '管理员',
)
const initials = computed(() => displayName.value.slice(0, 1).toUpperCase())
const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())

watch(
  () => route.path,
  () => {
    menuOpen.value = false
  },
)

async function logout() {
  try {
    await ElMessageBox.confirm('退出当前管理员账号？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  auth.clearToken()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

onMounted(async () => {
  if (!auth.token || auth.admin) {
    return
  }
  try {
    auth.setAdmin(await getMe())
  } catch {
    auth.clearToken()
    router.replace('/login')
  }
})
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ open: menuOpen }">
      <div class="brand">
        <div class="brand-mark">M</div>
        <div class="brand-text">
          <strong>优选商城</strong>
          <span>MERCHANT CONSOLE</span>
        </div>
      </div>

      <nav class="nav">
        <div v-for="group in navGroups" :key="group.label" class="nav-group">
          <p class="nav-label">{{ group.label }}</p>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: route.path === item.path }"
          >
            <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
            <span class="nav-marker"></span>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-foot">
        <div class="user-card">
          <div class="avatar">{{ initials }}</div>
          <div class="user-info">
            <strong>{{ displayName }}</strong>
            <span>店铺管理员</span>
          </div>
          <button class="icon-button" title="退出登录" @click="logout">
            <el-icon><SwitchButton /></el-icon>
          </button>
        </div>
        <p class="version">MallMini · v0.1</p>
      </div>
    </aside>

    <transition name="fade">
      <div v-if="menuOpen" class="sidebar-overlay" @click="menuOpen = false"></div>
    </transition>

    <div class="workspace">
      <header class="topbar">
        <button class="menu-toggle" @click="menuOpen = !menuOpen">
          <el-icon><component :is="menuOpen ? Fold : Expand" /></el-icon>
        </button>
        <div class="crumb">
          <span>控制台</span>
          <span class="crumb-sep">/</span>
          <strong>{{ String(route.meta.title ?? '') }}</strong>
        </div>
        <div class="topbar-right">
          <span class="date">{{ today }}</span>
          <span class="console-pill">运营控制台</span>
          <div class="topbar-avatar">{{ initials }}</div>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100%;
}

.sidebar {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 80;
  display: flex;
  flex-direction: column;
  width: 252px;
  padding: 22px 16px 18px;
  color: #f3ece5;
  background:
    radial-gradient(circle at 20% 0%, rgb(217 84 54 / 24%), transparent 22rem),
    linear-gradient(180deg, #1e1813 0%, #241c15 55%, #1a1510 100%);
  box-shadow: 18px 0 50px rgb(35 29 24 / 8%);
  transition: transform 0.25s ease;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 8px 22px;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(145deg, #ef8a63, #c8472b);
  border-radius: 13px;
  box-shadow: 0 10px 20px rgb(200 71 43 / 30%);
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-text strong {
  font-size: 16px;
  letter-spacing: 1px;
}

.brand-text span {
  color: rgb(243 236 229 / 38%);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 1.8px;
}

.nav {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 22px;
  padding: 8px 4px;
  overflow-y: auto;
}

.nav-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-label {
  padding: 0 12px;
  margin: 0 0 6px;
  color: rgb(243 236 229 / 32%);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.4px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px 12px;
  overflow: hidden;
  color: rgb(243 236 229 / 68%);
  font-size: 13px;
  font-weight: 500;
  border-radius: 11px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
}

.nav-item:hover {
  color: #fff;
  background: rgb(255 255 255 / 6%);
}

.nav-item.active {
  color: #fff;
  background: linear-gradient(90deg, rgb(217 84 54 / 24%), rgb(255 255 255 / 5%));
}

.nav-item.active::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 3px;
  height: 18px;
  background: #ef8a63;
  border-radius: 0 4px 4px 0;
  transform: translateY(-50%);
  content: '';
}

.nav-icon {
  font-size: 16px;
  opacity: 0.85;
}

.nav-item.active .nav-icon {
  color: #f5a181;
  opacity: 1;
}

.nav-marker {
  width: 4px;
  height: 4px;
  margin-left: auto;
  background: transparent;
  border-radius: 50%;
}

.nav-item.active .nav-marker {
  background: #f5a181;
  box-shadow: 0 0 0 4px rgb(245 161 129 / 12%);
}

.sidebar-foot {
  padding: 16px 4px 0;
  border-top: 1px solid rgb(255 255 255 / 7%);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: rgb(255 255 255 / 5%);
  border: 1px solid rgb(255 255 255 / 6%);
  border-radius: 13px;
}

.avatar,
.topbar-avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #c8472b;
  font-size: 14px;
  font-weight: 700;
  background: linear-gradient(145deg, #fdf2ec, #f1d4c8);
  border-radius: 11px;
}

.user-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.user-info strong {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-info span {
  color: rgb(243 236 229 / 40%);
  font-size: 10px;
}

.icon-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: rgb(243 236 229 / 55%);
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 8px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
}

.icon-button:hover {
  color: #fff;
  background: rgb(255 255 255 / 8%);
}

.version {
  margin: 12px 0 0;
  color: rgb(243 236 229 / 22%);
  font-size: 9px;
  letter-spacing: 1.4px;
  text-align: center;
}

.workspace {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  margin-left: 252px;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  gap: 16px;
  height: 64px;
  padding: 0 30px;
  background: rgb(244 240 234 / 82%);
  border-bottom: 1px solid rgb(222 210 196 / 65%);
  backdrop-filter: blur(14px);
}

.menu-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: var(--mall-ink);
  cursor: pointer;
  background: var(--mall-surface);
  border: 1px solid var(--mall-line);
  border-radius: 10px;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
}

.crumb span {
  color: var(--mall-muted);
}

.crumb-sep {
  color: var(--mall-faint) !important;
}

.crumb strong {
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: auto;
}

.date {
  color: var(--mall-muted);
  font-size: 11px;
}

.console-pill {
  padding: 5px 11px;
  color: var(--mall-primary-dark);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  background: var(--mall-primary-soft);
  border-radius: 999px;
}

.topbar-avatar {
  width: 32px;
  height: 32px;
  font-size: 13px;
  border: 1px solid #eedbcf;
}

.content {
  flex: 1;
  padding: 28px 30px 52px;
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  z-index: 70;
  background: rgb(28 23 19 / 42%);
  backdrop-filter: blur(2px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 960px) {
  .sidebar {
    transform: translateX(-100%);
  }

  .sidebar.open {
    transform: translateX(0);
  }

  .workspace {
    margin-left: 0;
  }

  .menu-toggle {
    display: flex;
  }

  .topbar {
    padding: 0 18px;
  }

  .content {
    padding: 20px 18px 40px;
  }
}

@media (max-width: 640px) {
  .date,
  .console-pill {
    display: none;
  }

  .topbar {
    height: 58px;
  }

  .content {
    padding: 16px 14px 36px;
  }
}
</style>
