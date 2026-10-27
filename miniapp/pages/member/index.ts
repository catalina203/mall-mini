import { getMember, listPointsLogs } from '../../services/member'
import { isLoggedIn } from '../../store/auth'
import type { MemberInfo, PointsLog } from '../../types/api'
import { formatDateTime } from '../../utils/format'

const TYPE_TEXT: Record<string, string> = {
  ORDER_REWARD: '订单奖励',
  ORDER_REFUND_REVOKE: '退款扣回',
  ADMIN_ADJUST: '管理员调整',
}

interface LogView extends PointsLog {
  typeText: string
  createdText: string
  deltaText: string
}

Page({
  data: {
    loggedIn: false,
    loading: false,
    error: '',
    member: null as MemberInfo | null,
    progress: 0,
    logs: [] as LogView[],
  },

  onShow() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (loggedIn) {
      void this.load()
    } else {
      this.setData({ member: null, logs: [], loading: false })
    }
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [member, logs] = await Promise.all([getMember(), listPointsLogs(1, 30)])
      const progress = member.nextLevelPoints
        ? Math.min(Math.round((member.totalPoints / member.nextLevelPoints) * 100), 100)
        : 100
      this.setData({
        member,
        progress,
        logs: logs.items.map((log) => this.toLogView(log)),
        loading: false,
      })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '会员信息加载失败',
      })
    }
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },

  toLogView(log: PointsLog): LogView {
    return {
      ...log,
      typeText: TYPE_TEXT[log.changeType] ?? log.changeType,
      createdText: formatDateTime(log.createdAt),
      deltaText: `${log.pointsDelta > 0 ? '+' : ''}${log.pointsDelta}`,
    }
  },
})
