<template>
  <div class="trace">
    <div v-if="loading" class="state"><el-icon class="is-loading"><Loading /></el-icon> 正在查询…</div>

    <div v-else-if="error" class="state">
      <el-icon class="state-icon"><WarningFilled /></el-icon>
      <h2>{{ error }}</h2>
      <p class="mono">{{ route.params.code }}</p>
      <router-link to="/trace" class="btn">重新查询</router-link>
    </div>

    <template v-else-if="d">
      <header class="head cloth">
        <div class="head-in">
          <div class="brand"><img src="/icon.svg" alt="" /> 农产品溯源</div>
          <div class="cat">{{ d.product?.category }} · {{ d.product?.variety }}</div>
          <h1>{{ d.product?.name }}</h1>
          <div class="verdict" :class="verdict.tone">
            <el-icon><component :is="verdict.icon" /></el-icon>{{ verdict.text }}
          </div>
          <div class="scan">溯源码 <b class="mono">{{ d.traceCode?.code }}</b> · 已被查询 <b class="num">{{ d.traceCode?.scanCount }}</b> 次</div>
        </div>
      </header>

      <main class="body">
        <section class="sec">
          <h3>产品信息</h3>
          <dl class="kv">
            <div><dt>批次号</dt><dd class="mono">{{ d.batch.batchNo }}</dd></div>
            <div><dt>采收日期</dt><dd>{{ fmtDate(d.batch.harvestDate) }}</dd></div>
            <div><dt>保质期</dt><dd>{{ d.product?.shelfLifeDays ? d.product.shelfLifeDays + ' 天' : '—' }}</dd></div>
            <div><dt>储存方式</dt><dd>{{ d.product?.storage || '—' }}</dd></div>
          </dl>
          <p v-if="d.product?.description" class="desc">{{ d.product.description }}</p>
        </section>

        <section class="sec">
          <h3>产地信息</h3>
          <div class="origin">
            <el-icon><Location /></el-icon>
            <div>
              <b>{{ d.base?.name }}</b>
              <div class="muted">{{ d.base?.address }}</div>
            </div>
          </div>
          <dl class="kv">
            <div><dt>基地面积</dt><dd>{{ fmtNum(d.base?.area, '亩') }}</dd></div>
            <div><dt>负责人</dt><dd>{{ d.base?.manager || '—' }}</dd></div>
            <div class="wide"><dt>产地环境</dt><dd>{{ d.base?.environment || '—' }}</dd></div>
          </dl>
        </section>

        <section class="sec">
          <h3>从田间到餐桌</h3>
          <TraceTimeline :detail="d" />
        </section>

        <footer class="foot">
          <p>本页信息由生产、检测、物流各环节如实登记</p>
          <router-link to="/trace">查询其他溯源码</router-link>
        </footer>
      </main>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { publicApi } from '@/api'
import { fmtDate, fmtNum } from '@/utils/format'
import TraceTimeline from '@/components/TraceTimeline.vue'

const route = useRoute()
const d = ref(null)
const loading = ref(true)
const error = ref('')

const verdict = computed(() => {
  const s = d.value.batch.status
  if (s === 'REJECTED') return { tone: 'bad', icon: 'WarningFilled', text: '该批次质检不合格，请勿食用' }
  if (['INSPECTED', 'SHIPPING', 'ON_SALE'].includes(s)) return { tone: 'ok', icon: 'CircleCheckFilled', text: '质检合格 · 来源可查' }
  return { tone: 'warn', icon: 'InfoFilled', text: '该批次尚未完成质检' }
})

onMounted(async () => {
  try {
    d.value = await publicApi.trace(route.params.code)
    document.title = d.value.product?.name + ' · 溯源信息'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.trace { min-height: 100vh; background: var(--desk); }
.state { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 24px; text-align: center; color: var(--sub); }
.state-icon { font-size: 48px; color: var(--warn); }
.state h2 { margin: 0; color: var(--text); font-size: 18px; }
.btn { margin-top: 10px; padding: 8px 20px; border-radius: var(--r-s); background: var(--cloth); color: var(--on-cloth); }

.head { padding: 22px 16px 26px; }
.head-in, .body { max-width: 640px; margin: 0 auto; }
.brand { display: flex; align-items: center; gap: 8px; font-size: 13px; letter-spacing: .1em; opacity: .85; }
.brand img { width: 24px; height: 24px; border-radius: 6px; }
.cat { margin-top: 22px; font-size: 13px; opacity: .75; }
.head h1 { margin: 2px 0 12px; font-size: 30px; color: var(--ink); letter-spacing: .04em; }
.verdict { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 999px; font-weight: 600; font-size: 14px; background: var(--paper); }
.verdict.ok { color: var(--ok); }
.verdict.bad { color: var(--bad); }
.verdict.warn { color: var(--warn); }
.scan { margin-top: 12px; font-size: 13px; opacity: .85; }
.scan b { color: var(--ink); }

.body { padding: 0 12px 30px; margin-top: -10px; }
.sec { background: var(--paper); border: 1px solid var(--line); border-radius: var(--r); padding: 16px 18px; margin-bottom: 12px; }
.sec h3 { margin: 0 0 12px; font-size: 15px; display: flex; align-items: center; gap: 8px; }
.sec h3::before { content: ""; width: 4px; height: 15px; border-radius: 2px; background: var(--acc); }
.kv { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.kv .wide { grid-column: 1 / -1; }
.desc { margin: 12px 0 0; padding-top: 12px; border-top: 1px dashed var(--line); color: var(--sub); font-size: 13px; }
.origin { display: flex; gap: 10px; align-items: flex-start; margin-bottom: 12px; }
.origin > .el-icon { font-size: 22px; color: var(--acc); margin-top: 2px; }
.foot { text-align: center; color: var(--faint); font-size: 12px; padding: 10px 0; }
.foot p { margin: 0 0 6px; }
</style>
