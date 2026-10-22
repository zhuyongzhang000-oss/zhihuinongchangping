<template>
  <div v-loading="loading">
    <template v-if="d">
      <div class="hero card cloth">
        <div class="hero-main">
          <router-link to="/batches" class="back"><el-icon><ArrowLeft /></el-icon> 返回批次列表</router-link>
          <div class="hero-title">
            <h2>{{ d.product?.name }}</h2>
            <span class="mono">{{ d.batch.batchNo }}</span>
          </div>
          <div class="hero-meta">
            <span><el-icon><Location /></el-icon>{{ d.base?.name }}</span>
            <span v-if="d.batch.quantity"><el-icon><Box /></el-icon>{{ fmtNum(d.batch.quantity, d.product?.unit) }}</span>
          </div>
        </div>
        <div class="hero-side">
          <Pill :dict="BATCH_STATUS" :value="d.batch.status" class="big" />
          <el-dropdown v-if="store.isAdmin" trigger="click" @command="setStatus">
            <el-button size="small" class="ghost">手动改状态 <el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="o in options(BATCH_STATUS)" :key="o.value" :command="o.value" :disabled="o.value === d.batch.status">{{ o.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 流程进度 -->
      <div class="card steps">
        <div v-for="(s, i) in steps" :key="s.label" class="step" :class="s.state">
          <span class="step-dot">{{ s.state === 'done' ? '✓' : s.state === 'bad' ? '!' : i + 1 }}</span>
          <span>{{ s.label }}</span>
        </div>
      </div>

      <div class="layout">
        <div class="col-main">
          <div class="card">
            <div class="card-title"><h3>批次信息</h3></div>
            <dl class="kv">
              <div><dt>产品 / 品种</dt><dd>{{ d.product?.name }} · {{ d.product?.variety || '—' }}</dd></div>
              <div><dt>类别</dt><dd>{{ d.product?.category }}</dd></div>
              <div><dt>种植日期</dt><dd>{{ fmtDate(d.batch.plantDate) }}</dd></div>
              <div><dt>采收日期</dt><dd>{{ fmtDate(d.batch.harvestDate) }}</dd></div>
              <div><dt>产地</dt><dd>{{ d.base?.address || '—' }}</dd></div>
              <div><dt>基地负责人</dt><dd>{{ d.base?.manager || '—' }} {{ d.base?.phone }}</dd></div>
              <div><dt>产地环境</dt><dd>{{ d.base?.environment || '—' }}</dd></div>
              <div><dt>备注</dt><dd>{{ d.batch.remark || '—' }}</dd></div>
            </dl>
          </div>

          <div class="card">
            <div class="card-title">
              <h3>溯源时间线</h3>
              <div class="actions">
                <el-button v-if="store.can('FARMER') && canFarm" size="small" icon="Plus" @click="dlg.farming = true">记录农事</el-button>
                <el-button v-if="store.can('INSPECTOR') && canInspect" size="small" icon="DocumentChecked" @click="dlg.inspection = true">录入质检</el-button>
                <el-button v-if="store.can('LOGISTICS') && canShip" size="small" icon="Van" @click="dlg.logistics = true">登记发运</el-button>
              </div>
            </div>
            <TraceTimeline :detail="d" />
          </div>
        </div>

        <div class="col-side">
          <div class="card">
            <div class="card-title"><h3>溯源码</h3></div>
            <template v-if="d.traceCode">
              <TraceLabel ref="labelRef" :code="d.traceCode.code" :product-name="d.product?.name" :batch-no="d.batch.batchNo" />
              <div class="code-stats">
                <div><b class="num">{{ d.traceCode.scanCount }}</b><span>次扫码</span></div>
                <div><b>{{ d.traceCode.status === 1 ? '有效' : '已作废' }}</b><span>状态</span></div>
              </div>
              <div class="code-actions">
                <el-button icon="Download" @click="labelRef.download()">下载标签</el-button>
                <el-button icon="View" @click="openTrace">预览消费者页</el-button>
              </div>
            </template>
            <div v-else class="no-code">
              <el-icon class="big-icon"><Postcard /></el-icon>
              <p v-if="passed">批次已质检合格，可以生成溯源码，贴在包装上供消费者扫码。</p>
              <p v-else class="faint">批次质检合格后才能生成溯源码。</p>
              <el-button v-if="passed && store.can('FARMER')" type="primary" :loading="generating" @click="generate">生成溯源码</el-button>
            </div>
          </div>
        </div>
      </div>

      <FarmingForm v-model="dlg.farming" :batch-id="d.batch.id" @saved="load" />
      <InspectionForm v-model="dlg.inspection" :batch-id="d.batch.id" @saved="load" />
      <LogisticsForm v-model="dlg.logistics" :batch-id="d.batch.id" @saved="load" />
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchApi, traceCodeApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { BATCH_STATUS, options } from '@/utils/dict'
import { fmtDate, fmtNum } from '@/utils/format'
import Pill from '@/components/Pill.vue'
import TraceTimeline from '@/components/TraceTimeline.vue'
import TraceLabel from '@/components/TraceLabel.vue'
import FarmingForm from '../farming/FarmingForm.vue'
import InspectionForm from '../inspection/InspectionForm.vue'
import LogisticsForm from '../logistics/LogisticsForm.vue'

const route = useRoute()
const store = useUserStore()
const d = ref(null)
const loading = ref(false)
const generating = ref(false)
const labelRef = ref()
const dlg = reactive({ farming: false, inspection: false, logistics: false })

const status = computed(() => d.value?.batch.status)
const passed = computed(() => ['INSPECTED', 'SHIPPING', 'ON_SALE'].includes(status.value))
const canFarm = computed(() => ['GROWING', 'HARVESTED'].includes(status.value))
const canInspect = computed(() => ['HARVESTED', 'INSPECTED', 'REJECTED'].includes(status.value))
const canShip = passed

// 种植 → 采收 → 质检 → 运输 → 上市
const steps = computed(() => {
  const order = ['GROWING', 'HARVESTED', 'INSPECTED', 'SHIPPING', 'ON_SALE']
  const s = status.value === 'REJECTED' ? 'INSPECTED' : status.value
  const idx = order.indexOf(s)
  return ['种植', '采收', '质检', '运输', '上市'].map((label, i) => ({
    label: i === 2 && status.value === 'REJECTED' ? '质检不合格' : label,
    state: i === 2 && status.value === 'REJECTED' ? 'bad' : i < idx || (i === idx && i === 4) ? 'done' : i === idx ? 'now' : ''
  }))
})

async function load() {
  loading.value = true
  try {
    d.value = await batchApi.get(route.params.id)
  } finally {
    loading.value = false
  }
}

async function generate() {
  generating.value = true
  try {
    await traceCodeApi.generate(d.value.batch.id)
    ElMessage.success('溯源码已生成')
    load()
  } finally {
    generating.value = false
  }
}

async function setStatus(s) {
  await ElMessageBox.confirm(`把状态改为「${BATCH_STATUS[s].label}」？一般情况下状态会随业务自动流转，只在纠错时手动修改。`, '手动改状态', { type: 'warning' })
  await batchApi.setStatus(d.value.batch.id, s)
  ElMessage.success('已修改')
  load()
}

function openTrace() {
  window.open('/trace/' + d.value.traceCode.code, '_blank')
}

onMounted(load)
</script>

<style scoped>
.hero { border: 0; display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; padding: 20px 24px; }
.back { color: var(--on-cloth); opacity: .75; font-size: 13px; display: inline-flex; align-items: center; gap: 4px; }
.back:hover { opacity: 1; }
.hero-title { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-top: 6px; }
.hero-title h2 { margin: 0; font-size: 26px; color: var(--ink); }
.hero-title .mono { opacity: .8; }
.hero-meta { display: flex; gap: 18px; flex-wrap: wrap; margin-top: 6px; opacity: .85; font-size: 13px; }
.hero-meta span { display: inline-flex; align-items: center; gap: 4px; }
.hero-side { display: flex; flex-direction: column; align-items: flex-end; justify-content: space-between; gap: 10px; }
.pill.big { height: 28px; font-size: 14px; padding: 0 14px; }
.ghost { background: rgba(255,255,255,.1); border-color: rgba(255,255,255,.25); color: var(--on-cloth); }

.steps { display: flex; gap: 6px; padding: 12px 16px; }
.step { flex: 1; display: flex; align-items: center; gap: 8px; color: var(--faint); font-size: 13px; position: relative; }
.step:not(:last-child)::after { content: ""; flex: 1; height: 2px; background: var(--line); margin: 0 4px; }
.step-dot { width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center; font-size: 12px; font-weight: 700; background: var(--tint); flex: none; }
.step.done { color: var(--ok); }
.step.done .step-dot { background: var(--ok); color: var(--paper); }
.step.done:not(:last-child)::after { background: var(--ok); }
.step.now { color: var(--text); font-weight: 600; }
.step.now .step-dot { background: var(--acc); color: var(--paper); box-shadow: 0 0 0 4px var(--el-color-primary-light-8); }
.step.bad { color: var(--bad); font-weight: 600; }
.step.bad .step-dot { background: var(--bad); color: var(--paper); }

.layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; margin-top: 16px; align-items: start; }
.layout .card { margin: 0 0 16px; }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.actions .el-button + .el-button { margin-left: 0; }
.code-stats { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin: 14px 0; }
.code-stats div { background: var(--tint); border-radius: var(--r-s); padding: 8px 12px; display: flex; flex-direction: column; }
.code-stats b { font-size: 18px; }
.code-stats span { font-size: 12px; color: var(--faint); }
.code-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.code-actions .el-button { margin: 0; }
.no-code { text-align: center; padding: 10px 0; }
.big-icon { font-size: 42px; color: var(--faint); }
.no-code p { font-size: 13px; margin: 8px 0 14px; }

@media (max-width: 1000px) {
  .layout { grid-template-columns: minmax(0, 1fr); }
}
@media (max-width: 640px) {
  .steps { flex-wrap: wrap; }
  .step:not(:last-child)::after { display: none; }
  .hero-side { align-items: flex-start; }
}
</style>
