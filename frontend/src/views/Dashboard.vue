<template>
  <div v-loading="loading">
    <div class="page-head">
      <div>
        <h2>{{ greeting }}，{{ store.user?.realName }}</h2>
        <p>{{ today }} · 溯源链运行概况</p>
      </div>
    </div>

    <template v-if="data">
      <!-- 指标 -->
      <div class="kpis">
        <div v-for="k in kpis" :key="k.label" class="kpi card">
          <div class="kpi-label"><el-icon><component :is="k.icon" /></el-icon>{{ k.label }}</div>
          <div class="kpi-value num">{{ fmtNum(k.value) }}</div>
          <div class="kpi-sub">{{ k.sub }}</div>
        </div>
      </div>

      <!-- 批次生命周期 -->
      <div class="card pipeline cloth">
        <div class="pl-title">批次生命周期 <small>共 {{ data.cards.batchCount }} 个批次</small></div>
        <div class="pl-steps">
          <router-link v-for="s in pipeline" :key="s.key" :to="{ path: '/batches', query: { status: s.key } }" class="pl-step" >
            <span class="pl-count num">{{ s.count }}</span>
            <span class="pl-name">{{ s.label }}</span>
          </router-link>
        </div>
      </div>

      <div class="grid">
        <div class="col">
          <div class="card">
            <div class="card-title">
              <h3>近 30 天扫码次数</h3>
              <span class="hint">今日 {{ data.cards.todayScans }} 次 · 30 天合计 {{ trendTotal }} 次</span>
            </div>
            <EChart :option="trendOption" height="260px" label="近 30 天每日扫码次数折线图" />
          </div>
          <div class="pair">
            <div class="card">
              <div class="card-title"><h3>各类别批次数</h3></div>
              <EChart :option="categoryOption" :height="barHeight(data.category)" label="各产品类别的批次数条形图" />
            </div>
            <div class="card">
              <div class="card-title"><h3>扫码最多的产品</h3><span class="hint">累计</span></div>
              <EChart :option="topOption" :height="barHeight(data.topProducts)" label="累计扫码次数最多的产品条形图" />
            </div>
          </div>
        </div>

        <div class="col">
          <div class="card">
            <div class="card-title"><h3>质检合格率</h3><span class="hint">按检测报告计</span></div>
            <div class="rate">
              <span class="rate-num num">{{ passRate }}</span><span class="rate-unit">%</span>
            </div>
            <div class="rate-bar" role="img" :aria-label="`合格 ${inspect.PASS} 份，不合格 ${inspect.FAIL} 份`">
              <span class="ok" :style="{ flex: inspect.PASS || 0 }" />
              <span class="bad" :style="{ flex: inspect.FAIL || 0 }" />
            </div>
            <div class="rate-legend">
              <span class="pill ok">合格 {{ inspect.PASS }} 份</span>
              <span class="pill bad">不合格 {{ inspect.FAIL }} 份</span>
            </div>
          </div>
          <div class="card grow">
            <div class="card-title"><h3>最新动态</h3></div>
            <ul class="acts">
              <li v-for="(a, i) in data.activities" :key="i">
                <span class="dot" :class="a.kind" />
                <div class="act-body">
                  <div class="act-title">{{ a.title }}</div>
                  <div class="faint"><span class="mono">{{ a.batchNo }}</span> · {{ fmtDate(a.time) }}</div>
                </div>
              </li>
              <li v-if="!data.activities.length" class="empty">暂无动态</li>
            </ul>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { dashboardApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { BATCH_STATUS } from '@/utils/dict'
import { fmtDate, fmtNum } from '@/utils/format'
import EChart from '@/components/EChart.vue'

const store = useUserStore()
const data = ref(null)
const loading = ref(false)

const hour = new Date().getHours()
const greeting = hour < 11 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
const today = dayjs().format('YYYY年M月D日')

const kpis = computed(() => {
  const c = data.value.cards
  return [
    { label: '种植基地', value: c.baseCount, sub: '个在册基地', icon: 'Location' },
    { label: '农产品', value: c.productCount, sub: '个品类档案', icon: 'Apple' },
    { label: '生产批次', value: c.batchCount, sub: `${c.growingCount} 个种植中`, icon: 'Box' },
    { label: '溯源码', value: c.traceCodeCount, sub: '个已赋码批次', icon: 'Postcard' },
    { label: '累计扫码', value: c.totalScans, sub: `今日 +${c.todayScans}`, icon: 'View' },
    { label: '在途运单', value: c.inTransit, sub: '单正在运输', icon: 'Van' }
  ]
})

const pipeline = computed(() => {
  const m = Object.fromEntries(data.value.batchStatus.map(r => [r.name, Number(r.value)]))
  return Object.entries(BATCH_STATUS).map(([key, v]) => ({ key, label: v.label, count: m[key] || 0 }))
})

const inspect = computed(() => {
  const m = { PASS: 0, FAIL: 0 }
  data.value.inspectionResult.forEach(r => (m[r.name] = Number(r.value)))
  return m
})
const passRate = computed(() => {
  const total = inspect.value.PASS + inspect.value.FAIL
  return total ? ((inspect.value.PASS / total) * 100).toFixed(1) : '—'
})
const trendTotal = computed(() => data.value.scanTrend.reduce((s, r) => s + Number(r.value), 0))

const barHeight = rows => Math.max(rows.length * 40 + 30, 160) + 'px'

// 折线：单一系列，标题说明含义，不需要图例；悬停显示十字线 + 当天数值
const trendOption = t => ({
  grid: { left: 36, right: 16, top: 16, bottom: 28 },
  tooltip: {
    trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: t.faint, width: 1 } },
    formatter: p => `${p[0].axisValue}<br/><b>${p[0].value}</b> 次扫码`
  },
  xAxis: {
    type: 'category', boundaryGap: false, data: data.value.scanTrend.map(r => r.day.slice(5)),
    axisLine: { lineStyle: { color: t.line } }, axisTick: { show: false },
    axisLabel: { color: t.faint, interval: 6, hideOverlap: true }
  },
  yAxis: {
    type: 'value', minInterval: 1, splitLine: { lineStyle: { color: t.line, type: [3, 4] } },
    axisLabel: { color: t.faint }
  },
  series: [{
    type: 'line', data: data.value.scanTrend.map(r => Number(r.value)), smooth: false,
    symbol: 'circle', symbolSize: 8, showSymbol: false,
    lineStyle: { width: 2, color: t.chart }, itemStyle: { color: t.chart, borderColor: t.paper, borderWidth: 2 },
    areaStyle: { color: t.chartSoft }
  }]
})

// 横向条形：类别名在左，数值直接标在条尾
const barOption = (rows, unit) => t => ({
  grid: { left: 8, right: 44, top: 4, bottom: 4, containLabel: true },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow', shadowStyle: { color: t.tint } }, formatter: p => `${p[0].name}<br/><b>${p[0].value}</b> ${unit}` },
  xAxis: { type: 'value', show: false },
  yAxis: {
    type: 'category', inverse: true, data: rows.map(r => r.name),
    axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: t.text, fontSize: 12 }
  },
  series: [{
    type: 'bar', data: rows.map(r => Number(r.value)), barWidth: 12,
    itemStyle: { color: t.chart, borderRadius: [0, 4, 4, 0] },
    label: { show: true, position: 'right', color: t.sub, fontSize: 12 }
  }]
})
const categoryOption = computed(() => barOption(data.value.category, '个批次'))
const topOption = computed(() => barOption(data.value.topProducts, '次扫码'))

onMounted(async () => {
  loading.value = true
  try {
    data.value = await dashboardApi.overview()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.kpis { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; margin-bottom: 16px; }
.kpi { padding: 14px 16px; }
.kpi + .kpi { margin-top: 0; }
.kpi-label { display: flex; align-items: center; gap: 6px; color: var(--sub); font-size: 13px; }
.kpi-label .el-icon { color: var(--acc); }
.kpi-value { font-size: 28px; font-weight: 600; line-height: 1.3; margin-top: 4px; }
.kpi-sub { font-size: 12px; color: var(--faint); }

.pipeline { padding: 16px 20px; margin-bottom: 16px; }
.pl-title { font-weight: 600; letter-spacing: .06em; color: var(--ink); }
.pl-title small { font-weight: 400; opacity: .7; color: var(--on-cloth); margin-left: 6px; }
.pl-steps { display: grid; grid-template-columns: repeat(6, 1fr); gap: 8px; margin-top: 12px; counter-reset: s; }
.pl-step {
  position: relative; display: flex; flex-direction: column; align-items: flex-start; padding: 10px 14px;
  border-radius: var(--r-s); background: rgba(255,255,255,.08); color: var(--on-cloth); transition: background .15s;
}
.pl-step:hover { background: rgba(255,255,255,.15); }
.pl-count { font-size: 24px; font-weight: 600; color: var(--ink); line-height: 1.2; }
.pl-name { font-size: 12px; opacity: .85; }

.grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(0, 1fr); gap: 16px; }
.col { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
.col .card { margin: 0; }
.col .grow { flex: 1; }
.pair { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }

.rate { display: flex; align-items: baseline; gap: 2px; margin: 8px 0 14px; }
.rate-num { font-size: 46px; font-weight: 600; line-height: 1; color: var(--ok); }
.rate-unit { font-size: 18px; color: var(--sub); }
.rate-bar { display: flex; gap: 2px; height: 10px; border-radius: 5px; overflow: hidden; background: var(--tint); }
.rate-bar .ok { background: var(--ok); }
.rate-bar .bad { background: var(--bad); }
.rate-legend { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }

.acts { list-style: none; margin: 0; padding: 0; }
.acts li { display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px dashed var(--line); }
.acts li:last-child { border-bottom: 0; }
.dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 8px; flex: none; background: var(--faint); }
.dot.farming { background: var(--warn); }
.dot.pass { background: var(--ok); }
.dot.fail { background: var(--bad); }
.dot.logistics { background: var(--info); }
.act-body { min-width: 0; font-size: 13px; }
.act-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.act-body .faint { font-size: 12px; }

@media (max-width: 1200px) {
  .kpis { grid-template-columns: repeat(3, 1fr); }
  .grid { grid-template-columns: minmax(0, 1fr); }
}
@media (max-width: 720px) {
  .kpis { grid-template-columns: repeat(2, 1fr); }
  .pl-steps { grid-template-columns: repeat(3, 1fr); }
  .pair { grid-template-columns: minmax(0, 1fr); }
}
</style>
