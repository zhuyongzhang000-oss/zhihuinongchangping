<template>
  <div>
    <div class="page-head">
      <div><h2>溯源码</h2><p>一批一码。打印标签贴在包装上，消费者扫码即可查看完整来源</p></div>
      <el-button v-if="store.can('FARMER')" type="primary" icon="Plus" @click="openGen">为批次赋码</el-button>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="溯源码 / 批次号" clearable prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
          <el-option label="有效" :value="1" /><el-option label="已作废" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="溯源码" width="150"><template #default="{ row }"><span class="mono strong">{{ row.code }}</span></template></el-table-column>
        <el-table-column label="批次" width="150"><template #default="{ row }"><router-link :to="'/batches/' + row.batchId" class="mono">{{ row.batchNo }}</router-link></template></el-table-column>
        <el-table-column prop="productName" label="产品" min-width="130" />
        <el-table-column prop="baseName" label="基地" min-width="150" show-overflow-tooltip />
        <el-table-column label="批次状态" width="120"><template #default="{ row }"><Pill :dict="BATCH_STATUS" :value="row.batchStatus" /></template></el-table-column>
        <el-table-column label="扫码次数" width="100" align="right"><template #default="{ row }"><span class="num">{{ row.scanCount }}</span></template></el-table-column>
        <el-table-column label="最近扫码" width="150"><template #default="{ row }">{{ fmtTime(row.lastScanAt) }}</template></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><span class="pill" :class="row.status === 1 ? 'ok' : 'bad'">{{ row.status === 1 ? '有效' : '已作废' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showLabel(row)">标签</el-button>
            <el-button link type="primary" @click="openTrace(row)">消费者页</el-button>
            <el-button v-if="store.isAdmin" link :type="row.status === 1 ? 'danger' : 'primary'" @click="toggle(row)">{{ row.status === 1 ? '作废' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="pager.page" v-model:page-size="pager.size" :total="pager.total"
                       layout="total, sizes, prev, pager, next" background @change="load" />
      </div>
    </div>

    <el-dialog v-model="labelDialog" title="溯源码标签" width="340px" destroy-on-close>
      <TraceLabel v-if="current" ref="labelRef" :code="current.code" :product-name="current.productName" :batch-no="current.batchNo" />
      <template #footer>
        <el-button @click="labelDialog = false">关闭</el-button>
        <el-button type="primary" icon="Download" @click="labelRef.download()">下载 PNG</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="genDialog" title="为批次赋码" width="460px" destroy-on-close>
      <p class="muted" style="margin-top: 0">只列出质检合格、还没有溯源码的批次。</p>
      <el-select v-model="genBatch" filterable placeholder="选择批次" style="width: 100%">
        <el-option v-for="b in candidates" :key="b.id" :label="`${b.batchNo}  ${b.productName}`" :value="b.id" />
      </el-select>
      <div v-if="!candidates.length" class="faint" style="margin-top: 10px">暂无可赋码的批次</div>
      <template #footer>
        <el-button @click="genDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!genBatch" :loading="generating" @click="generate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchApi, traceCodeApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { BATCH_STATUS } from '@/utils/dict'
import { fmtTime } from '@/utils/format'
import Pill from '@/components/Pill.vue'
import TraceLabel from '@/components/TraceLabel.vue'

const store = useUserStore()
const { query, pager, rows, loading, load, search, reset } = useTable(traceCodeApi.page, { keyword: '', status: '' })

const labelDialog = ref(false)
const labelRef = ref()
const current = ref(null)
const genDialog = ref(false)
const genBatch = ref(null)
const candidates = ref([])
const generating = ref(false)

function showLabel(row) {
  current.value = row
  labelDialog.value = true
}

function openTrace(row) {
  window.open('/trace/' + row.code, '_blank')
}

async function toggle(row) {
  const to = row.status === 1 ? 0 : 1
  if (to === 0) await ElMessageBox.confirm(`作废后消费者扫描 ${row.code} 会看到「已作废」提示，确定吗？`, '作废溯源码', { type: 'warning' })
  await traceCodeApi.setStatus(row.id, to)
  ElMessage.success(to ? '已启用' : '已作废')
  load()
}

async function openGen() {
  genBatch.value = null
  genDialog.value = true
  const list = await batchApi.options(['INSPECTED', 'SHIPPING', 'ON_SALE'])
  candidates.value = list.filter(b => !b.traceCode)
}

async function generate() {
  generating.value = true
  try {
    const c = await traceCodeApi.generate(genBatch.value)
    ElMessage.success('已生成溯源码 ' + c.code)
    genDialog.value = false
    load()
  } finally {
    generating.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.strong { font-weight: 700; }
</style>
