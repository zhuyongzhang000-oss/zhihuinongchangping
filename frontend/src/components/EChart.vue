<template>
  <div ref="el" class="echart" :style="{ height }" role="img" :aria-label="label" />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useThemeStore } from '@/stores/theme'

echarts.use([BarChart, LineChart, GridComponent, TooltipComponent, CanvasRenderer])

/**
 * option 传一个函数 (t) => echartsOption，t 是当前主题的颜色（从 CSS 变量里读），
 * 这样日间 / 夜间切换时图表会跟着重画。
 */
const props = defineProps({
  option: { type: Function, required: true },
  height: { type: String, default: '280px' },
  label: { type: String, default: '图表' }
})

const el = ref()
const theme = useThemeStore()
let chart
let ro

function tokens() {
  const s = getComputedStyle(document.documentElement)
  const v = k => s.getPropertyValue(k).trim()
  return {
    chart: v('--chart'), chartSoft: v('--chart-soft'), text: v('--text'), sub: v('--sub'), faint: v('--faint'),
    line: v('--line'), paper: v('--paper'), tint: v('--tint'), font: v('--f-body')
  }
}

function render() {
  if (!chart) return
  const t = tokens()
  chart.setOption({
    textStyle: { fontFamily: t.font, color: t.sub },
    tooltip: {
      backgroundColor: t.paper, borderColor: t.line, borderWidth: 1, padding: [8, 12],
      textStyle: { color: t.text, fontSize: 12 }, extraCssText: 'border-radius:10px;box-shadow:0 6px 20px rgba(0,0,0,.12);'
    },
    ...props.option(t)
  }, true)
}

onMounted(() => {
  chart = echarts.init(el.value)
  render()
  ro = new ResizeObserver(() => chart?.resize())
  ro.observe(el.value)
})
onBeforeUnmount(() => {
  ro?.disconnect()
  chart?.dispose()
})
watch(() => theme.dark, () => requestAnimationFrame(render))
watch(() => props.option, render)
</script>

<style scoped>
.echart { width: 100%; }
</style>
