<template>
  <div>
    <div class="page-head">
      <div><h2>物流运输</h2><p>质检合格的批次登记发运；确认到达后批次自动变为「已上市」</p></div>
      <el-button v-if="canEdit" type="primary" icon="Plus" @click="openForm()">登记发运</el-button>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="运单号 / 承运单位 / 车牌 / 目的地" clearable prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
          <el-option v-for="o in options(LOGISTICS_STATUS)" :key="o.value" v-bind="o" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="运单号" width="130"><template #default="{ row }"><span class="mono">{{ row.waybillNo }}</span></template></el-table-column>
        <el-table-column label="批次" width="150"><template #default="{ row }"><router-link :to="'/batches/' + row.batchId" class="mono">{{ row.batchNo }}</router-link></template></el-table-column>
        <el-table-column prop="productName" label="产品" width="120" />
        <el-table-column label="线路" min-width="220">
          <template #default="{ row }">{{ row.fromAddr }} <span class="faint">→</span> {{ row.toAddr }}</template>
        </el-table-column>
        <el-table-column label="承运 / 车辆" min-width="170">
          <template #default="{ row }">{{ row.carrier || '—' }}<div class="faint">{{ row.vehicleNo }} {{ row.driver }}</div></template>
        </el-table-column>
        <el-table-column prop="temperature" label="温度" width="80" />
        <el-table-column label="发车 / 到达" width="150">
          <template #default="{ row }">{{ fmtTime(row.departTime) }}<div class="faint">{{ row.arriveTime ? fmtTime(row.arriveTime) : '未到达' }}</div></template>
        </el-table-column>
        <el-table-column label="状态" width="90"><template #default="{ row }"><Pill :dict="LOGISTICS_STATUS" :value="row.status" /></template></el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'IN_TRANSIT'" link type="primary" @click="arrive(row)">确认到达</el-button>
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
    <LogisticsForm v-model="dialog" :row="editing" @saved="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logisticsApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { LOGISTICS_STATUS, options } from '@/utils/dict'
import { fmtTime } from '@/utils/format'
import Pill from '@/components/Pill.vue'
import LogisticsForm from './LogisticsForm.vue'

const store = useUserStore()
const canEdit = store.can('LOGISTICS')
const { query, pager, rows, loading, load, search, reset } = useTable(logisticsApi.page, { keyword: '', status: '' })
const dialog = ref(false)
const editing = ref(null)

function openForm(row) {
  editing.value = row || null
  dialog.value = true
}

async function arrive(row) {
  await ElMessageBox.confirm(`确认运单 ${row.waybillNo} 已到达「${row.toAddr}」？到达时间记为现在。`, '确认到达', { type: 'info' })
  await logisticsApi.arrive(row.id)
  ElMessage.success('已确认到达')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除运单 ${row.waybillNo} 吗？`, '删除确认', { type: 'warning' })
  await logisticsApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
