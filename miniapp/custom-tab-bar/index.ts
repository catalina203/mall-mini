Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/home/index', text: '首页' },
      { pagePath: '/pages/category/index', text: '分类' },
      { pagePath: '/pages/cart/index', text: '购物车' },
      { pagePath: '/pages/profile/index', text: '我的' },
    ],
  },
  methods: {
    switchTab(event: WechatMiniprogram.TouchEvent) {
      const { path, index } = event.currentTarget.dataset as { path: string; index: number }
      wx.switchTab({ url: path })
      this.setData({ selected: index })
    },
  },
})
