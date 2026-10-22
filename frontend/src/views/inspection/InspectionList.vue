<template>
  <div>
    <div class="page-head">
      <div><h2>质量检测</h2><p>采收后送检，合格才能发运和赋码；不合格的批次禁止流通</p></div>
      <el-button v-if="canEdit" type="primary" icon="Plus" @click="openForm()">录入质检</el-button>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="报告编号 / 检测机构" clearable prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="query.batchId" placeholder="全部批次" clearable filterable @change="search">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo} ${b.productName}`" :value="b.id" />
        </el-select>
        <el-select v-model="query.result" placeholder="全部结论" clearable @change="search">
          <el-option v-for="o in options(INSPECT_RESULT)" :key="o.value" v-bind="o" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="报告编号" width="130"><template #default="{ row }"><span class="mono">{{ row.reportNo }}</span></template></el-table-column>
        <el-table-column label="批次" width="150"><template #default="{ row }"><router-link :to="'/batches/' + row.batchId" class="mono">{{ row.batchNo }}</router-link></template></el-table-column>
        <el-table-column prop="productName" label="产品" width="130" />
        <el-table-column label="检测日期" width="110"><template #default="{ row }">{{ fmtDate(row.inspectDate) }}</template></el-table-column>
        <el-table-column prop="agency" label="检测机构" min-width="170" show-overflow-tooltip />
        <el-table-column prop="pesticideResidue" label="农药残留" width="130" show-overflow-tooltip />
        <el-table-column prop="heavyMetal" label="重金属" width="120" show-overflow-tooltip />
        <el-table-column label="结论" width="90"><template #default="{ row }"><Pill :dict="INSPECT_RESULT" :value="row.result" /></template></el-table-column>
        <el-table-column prop="inspector" label="检测人" width="80" />
        <el-table-column v-if="canEdit" label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="pager.page" v-model:page-size="pager.size" :total="pager.total"
                       layout="total, sizes, prev, pager, next" background @change="load" />
      </div>
    </div>
    <InspectionForm v-model="dialog" :row="editing" @saved="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchApi, inspectionApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { INSPECT_RESULT, options } from '@/utils/dict'
import { fmtDate } from '@/utils/format'
import Pill from '@/components/Pill.vue'
import InspectionForm from './InspectionForm.vue'

const store = useUserStore()
const canEdit = store.can('INSPECTOR')
const { query, pager, rows, loading, load, search, reset } = useTable(inspectionApi.page, { keyword: '', batchId: '', result: '' })
const batches = ref([])
const dialog = ref(false)
const editing = ref(null)

function openForm(row) {
  editing.value = row || null
  dialog.value = true
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除检测报告 ${row.reportNo} 吗？批次状态会按剩下的检测记录重新计算。`, '删除确认', { type: 'warning' })
  await inspectionApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  batches.value = await batchApi.options()
})
</script>
