<script setup lang="ts">
defineProps<{
  title: string
  description: string
  stage: string
}>()
</script>

<template>
  <section class="page-card">
    <h2 class="page-title">{{ title }}</h2>
    <p class="page-desc">{{ description }}</p>
    <el-alert
      class="notice"
      type="info"
      :closable="false"
      :title="`将在${stage}接入`"
      description="当前为阶段 1 工程骨架，页面与接口尚未实现，不提供可点击的伪功能。"
    />
  </section>
</template>

<style scoped>
.notice {
  margin-top: 20px;
}
</style>
