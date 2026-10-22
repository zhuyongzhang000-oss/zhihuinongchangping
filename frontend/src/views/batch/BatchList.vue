<template>
  <div>
    <div class="page-head">
      <div><h2>生产批次</h2><p>溯源的基本单位：一个批次 = 一块地上同一季收获的同一种产品</p></div>
      <el-button v-if="canEdit" type="primary" icon="Plus" @click="openForm()">新建批次</el-button>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="批次号 / 产品 / 备注" clearable prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
          <el-option v-for="o in options(BATCH_STATUS)" :key="o.value" v-bind="o" />
        </el-select>
        <el-select v-model="query.productId" placeholder="全部产品" clearable filterable @change="search">
          <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select v-model="query.baseId" placeholder="全部基地" clearable filterable @change="search">
          <el-option v-for="b in bases" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe @row-dblclick="row => router.push('/batches/' + row.id)">
        <el-table-column label="批次号" width="140">
          <template #default="{ row }"><router-link :to="'/batches/' + row.id" class="mono">{{ row.batchNo }}</router-link></template>
        </el-table-column>
        <el-table-column prop="productName" label="产品" min-width="110" />
        <el-table-column prop="baseName" label="基地" min-width="140" show-overflow-tooltip />
        <el-table-column label="种植日期" width="115"><template #default="{ row }">{{ fmtDate(row.plantDate) }}</template></el-table-column>
        <el-table-column label="采收日期" width="115"><template #default="{ row }">{{ fmtDate(row.harvestDate) }}</template></el-table-column>
        <el-table-column label="产量" width="110" align="right"><template #default="{ row }"><span class="num">{{ fmtNum(row.quantity, row.unit) }}</span></template></el-table-column>
        <el-table-column label="状态" width="120"><template #default="{ row }"><Pill :dict="BATCH_STATUS" :value="row.status" /></template></el-table-column>
        <el-table-column label="溯源码" width="140">
          <template #default="{ row }"><span v-if="row.traceCode" class="mono">{{ row.traceCode }}</span><span v-else class="faint">未赋码</span></template>
        </el-table-column>
        <el-table-column label="操作" :width="store.isAdmin ? 150 : 110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push('/batches/' + row.id)">详情</el-button>
            <el-button v-if="canEdit" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-if="store.isAdmin" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="pager.page" v-model:page-size="pager.size" :total="pager.total"
                       layout="total, sizes, prev, pager, next" background @change="load" />
      </div>
    </div>
    <BatchForm v-model="dialog" :row="editing" @saved="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { baseApi, batchApi, productApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { BATCH_STATUS, options } from '@/utils/dict'
import { fmtDate, fmtNum } from '@/utils/format'
import Pill from '@/components/Pill.vue'
import BatchForm from './BatchForm.vue'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const canEdit = store.can('FARMER')
const { query, pager, rows, loading, load, search, reset } = useTable(batchApi.page, { keyword: '', status: '', productId: '', baseId: '' })
// 从看板点进来时带着状态
if (route.query.status) query.status = route.query.status

const products = ref([])
const bases = ref([])
const dialog = ref(false)
const editing = ref(null)

function openForm(row) {
  editing.value = row || null
  dialog.value = true
}

async function remove(row) {
  await ElMessageBox.confirm(`删除批次 ${row.batchNo} 会同时删除它的农事、质检、物流记录和溯源码，确定吗？`, '删除确认', { type: 'warning' })
  await batchApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  ;[products.value, bases.value] = await Promise.all([productApi.options(), baseApi.options()])
})
</script>
