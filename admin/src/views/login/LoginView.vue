<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, CircleCheck, Lock, TrendCharts, User } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({
  username: '',
  password: '',
})
const loading = ref(false)
const error = ref('')

const features = [
  { icon: TrendCharts, title: '经营数据', text: '订单、销售额与待处理事项一目了然' },
  { icon: CircleCheck, title: '交易闭环', text: '商品、库存、支付与履约统一管理' },
  { icon: Lock, title: '权限隔离', text: '管理员与用户身份分离，接口独立鉴权' },
]

async function submit() {
  if (loading.value) {
    return
  }
  if (!form.username.trim() || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const session = await login(form.username.trim(), form.password)
    auth.setSession(session.token, session.admin)
    ElMessage.success(`欢迎回来，${session.admin.displayName || session.admin.username}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <section class="story-panel">
      <div class="story-orb story-orb-one"></div>
      <div class="story-orb story-orb-two"></div>
      <div class="story-content">
        <div class="brand">
          <div class="brand-mark">M</div>
          <div>
            <strong>优选商城</strong>
            <span>MERCHANT CONSOLE</span>
          </div>
        </div>

        <div class="story-main">
          <p class="story-eyebrow">MallMini Commerce Suite</p>
          <h1>把每一笔生意，<br />经营得更有质感。</h1>
          <p class="story-desc">
            面向中小商家的轻量运营后台。从商品上架到订单履约，把常用的电商流程收进一个清晰、克制的工作台。
          </p>
        </div>

        <div class="feature-list">
          <div v-for="item in features" :key="item.title" class="feature">
            <span class="feature-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.text }}</p>
            </div>
          </div>
        </div>

        <p class="story-foot">MallMini · 可复用国内电商脚手架</p>
      </div>
    </section>

    <section class="form-panel">
      <div class="form-card">
        <p class="eyebrow">WELCOME BACK</p>
        <h2>登录运营后台</h2>
        <p class="form-desc">使用管理员账号进入工作台，管理商品、订单与运营内容。</p>

        <el-form label-position="top" class="login-form" @submit.prevent="submit">
          <el-form-item label="管理员账号">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="请输入用户名"
              autocomplete="username"
              :prefix-icon="User"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-form-item label="登录密码">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="submit"
            />
          </el-form-item>

          <transition name="fade">
            <div v-if="error" class="form-error">{{ error }}</div>
          </transition>

          <el-button
            type="primary"
            size="large"
            class="submit"
            :loading="loading"
            @click="submit"
          >
            进入控制台
            <el-icon class="submit-icon"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="form-note">
          <span class="note-dot"></span>
          管理员账号由服务端初始化，仓库不内置固定密码。
        </div>
      </div>

      <p class="copyright">© {{ new Date().getFullYear() }} 优选商城 · 管理后台</p>
    </section>
  </div>
</template>

<style scoped>
.login-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.92fr);
  min-height: 100vh;
  background: var(--mall-bg);
}

.story-panel {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 100vh;
  padding: 64px clamp(40px, 6vw, 96px);
  overflow: hidden;
  color: #f7f0e9;
  background:
    radial-gradient(circle at 85% 12%, rgb(217 84 54 / 34%), transparent 28rem),
    radial-gradient(circle at 8% 90%, rgb(47 125 99 / 18%), transparent 24rem),
    linear-gradient(150deg, #241c16 0%, #1b1510 58%, #211912 100%);
}

.story-panel::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(rgb(255 255 255 / 3%) 1px, transparent 1px);
  background-size: 100% 72px;
  content: '';
  mask-image: linear-gradient(180deg, transparent, #000 30%, #000 70%, transparent);
}

.story-orb {
  position: absolute;
  border: 1px solid rgb(255 255 255 / 8%);
  border-radius: 50%;
}

.story-orb-one {
  top: -180px;
  right: -140px;
  width: 520px;
  height: 520px;
}

.story-orb-two {
  right: 12%;
  bottom: -260px;
  width: 640px;
  height: 640px;
  border-color: rgb(239 138 99 / 12%);
}

.story-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 620px;
  min-height: calc(100vh - 128px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 23px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(145deg, #ef8a63, #c8472b);
  border-radius: 13px;
  box-shadow: 0 12px 30px rgb(200 71 43 / 35%);
}

.brand strong {
  display: block;
  font-size: 16px;
  letter-spacing: 1px;
}

.brand span {
  display: block;
  margin-top: 3px;
  color: rgb(247 240 233 / 38%);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 1.8px;
}

.story-main {
  margin: auto 0;
  padding: 70px 0 50px;
}

.story-eyebrow {
  margin: 0 0 18px;
  color: #e79a7d;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 3px;
  text-transform: uppercase;
}

.story-main h1 {
  margin: 0;
  font-family: Georgia, 'Songti SC', 'Times New Roman', serif;
  font-size: clamp(36px, 4.2vw, 58px);
  font-weight: 400;
  line-height: 1.22;
  letter-spacing: -1.2px;
}

.story-desc {
  max-width: 470px;
  margin: 24px 0 0;
  color: rgb(247 240 233 / 56%);
  font-size: 14px;
  line-height: 1.9;
}

.feature-list {
  display: grid;
  gap: 12px;
  margin-bottom: 44px;
}

.feature {
  display: flex;
  align-items: flex-start;
  gap: 13px;
  max-width: 430px;
}

.feature-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: #e79a7d;
  background: rgb(255 255 255 / 5%);
  border: 1px solid rgb(255 255 255 / 7%);
  border-radius: 9px;
}

.feature strong {
  display: block;
  font-size: 12px;
  font-weight: 600;
}

.feature p {
  margin: 3px 0 0;
  color: rgb(247 240 233 / 40%);
  font-size: 11px;
  line-height: 1.6;
}

.story-foot {
  margin: 0;
  color: rgb(247 240 233 / 22%);
  font-size: 10px;
  letter-spacing: 1px;
}

.form-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 32px 28px;
  background:
    radial-gradient(circle at 100% 0%, rgb(217 84 54 / 7%), transparent 24rem),
    var(--mall-bg);
}

.form-card {
  width: 100%;
  max-width: 420px;
  padding: 38px;
  background: var(--mall-surface);
  border: 1px solid rgb(235 226 215 / 90%);
  border-radius: 24px;
  box-shadow: 0 26px 70px rgb(56 42 31 / 10%);
}

.form-card h2 {
  margin: 0;
  color: var(--mall-ink);
  font-size: 25px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.form-desc {
  margin: 8px 0 28px;
  color: var(--mall-muted);
  font-size: 12px;
  line-height: 1.7;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: var(--mall-ink-soft);
  font-size: 12px;
  font-weight: 600;
}

.login-form :deep(.el-input__wrapper) {
  padding: 3px 14px;
  background: var(--mall-surface-soft);
  border-radius: 11px;
  box-shadow: 0 0 0 1px var(--mall-line) inset;
}

.form-error {
  padding: 10px 13px;
  margin-bottom: 16px;
  color: var(--mall-danger);
  font-size: 12px;
  background: #fbeae6;
  border: 1px solid #f4d4cd;
  border-radius: 10px;
}

.submit {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  font-size: 13px;
  letter-spacing: 0.4px;
  border-radius: 11px;
}

.submit-icon {
  margin-left: 6px;
  transition: transform 0.2s ease;
}

.submit:hover .submit-icon {
  transform: translateX(3px);
}

.form-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding-top: 20px;
  margin-top: 24px;
  color: var(--mall-faint);
  font-size: 10px;
  line-height: 1.6;
  border-top: 1px solid var(--mall-line);
}

.note-dot {
  flex-shrink: 0;
  width: 5px;
  height: 5px;
  margin-top: 5px;
  background: var(--mall-primary);
  border-radius: 50%;
}

.copyright {
  margin: 26px 0 0;
  color: var(--mall-faint);
  font-size: 10px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .login-shell {
    display: block;
  }

  .story-panel {
    display: none;
  }

  .form-panel {
    min-height: 100vh;
    padding: 28px 18px;
  }
}

@media (max-width: 480px) {
  .form-card {
    padding: 26px 22px;
    border-radius: 18px;
  }
}
</style>
