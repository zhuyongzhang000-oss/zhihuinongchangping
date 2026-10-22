<template>
  <div>
    <div class="page-head">
      <div><h2>农事记录</h2><p>播种、施肥、灌溉、用药、采收……每一次田间作业都留痕</p></div>
      <el-button v-if="canEdit" type="primary" icon="Plus" @click="openForm()">记录农事</el-button>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.batchId" placeholder="全部批次" clearable filterable @change="search">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo} ${b.productName}`" :value="b.id" />
        </el-select>
        <el-select v-model="query.type" placeholder="全部类型" clearable @change="search">
          <el-option v-for="o in options(FARMING_TYPES)" :key="o.value" v-bind="o" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="日期" width="110"><template #default="{ row }">{{ fmtDate(row.opDate) }}</template></el-table-column>
        <el-table-column label="批次" width="150"><template #default="{ row }"><router-link :to="'/batches/' + row.batchId" class="mono">{{ row.batchNo }}</router-link></template></el-table-column>
        <el-table-column prop="productName" label="产品" width="130" />
        <el-table-column label="类型" width="110"><template #default="{ row }"><span class="pill" :class="{ warn: row.type === 'HARVEST' }">{{ FARMING_TYPES[row.type]?.label }}</span></template></el-table-column>
        <el-table-column prop="material" label="投入品" min-width="120" />
        <el-table-column prop="dosage" label="用量" width="110" />
        <el-table-column prop="operator" label="作业人" width="90" />
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
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
    <FarmingForm v-model="dialog" :row="editing" @saved="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchApi, farmingApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { FARMING_TYPES, options } from '@/utils/dict'
import { fmtDate } from '@/utils/format'
import FarmingForm from './FarmingForm.vue'

const store = useUserStore()
const canEdit = store.can('FARMER')
const { query, pager, rows, loading, load, search, reset } = useTable(farmingApi.page, { batchId: '', type: '' })
const batches = ref([])
const dialog = ref(false)
const editing = ref(null)

function openForm(row) {
  editing.value = row || null
  dialog.value = true
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除这条农事记录吗？', '删除确认', { type: 'warning' })
  await farmingApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  batches.value = await batchApi.options()
})
</script>
