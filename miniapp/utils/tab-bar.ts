interface TabBarLike {
  setData(data: Record<string, unknown>): void
}

interface PageWithTabBar {
  getTabBar?: () => TabBarLike | undefined
}

export function syncTabBar(page: unknown, selected: number): void {
  const tabBar = (page as PageWithTabBar).getTabBar?.()
  tabBar?.setData({ selected })
}
