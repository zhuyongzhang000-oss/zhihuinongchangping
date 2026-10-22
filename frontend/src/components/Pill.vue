<template>
  <span class="pill" :class="item.tone">{{ item.label }}</span>
</template>

<script setup>
import { computed } from 'vue'

// 用字典渲染状态：<Pill :dict="BATCH_STATUS" :value="row.status" />
const props = defineProps({ dict: { type: Object, required: true }, value: { type: [String, Number], default: '' } })
const item = computed(() => props.dict[props.value] || { label: props.value || '—', tone: '' })
</script>
