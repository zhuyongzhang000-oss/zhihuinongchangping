<template>
  <div>
    <div class="page-head">
      <div><h2>农产品</h2><p>品类档案：品种、规格、保质期和储存方式</p></div>
      <el-button v-if="canEdit" type="primary" icon="Plus" @click="openForm()">新增产品</el-button>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="名称 / 品种" clearable prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="query.category" placeholder="全部类别" clearable @change="search">
          <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="name" label="产品名称" min-width="140" />
        <el-table-column label="类别" width="90"><template #default="{ row }"><span class="pill plain">{{ row.category }}</span></template></el-table-column>
        <el-table-column prop="variety" label="品种" width="110" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="保质期" width="90" align="right"><template #default="{ row }">{{ row.shelfLifeDays ? row.shelfLifeDays + ' 天' : '—' }}</template></el-table-column>
        <el-table-column prop="storage" label="储存方式" width="140" />
        <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="canEdit" label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-if="store.isAdmin" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="pager.page" v-model:page-size="pager.size" :total="pager.total"
                       layout="total, sizes, prev, pager, next" background @change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑产品' : '新增产品'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="16">
          <el-col :sm="12"><el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :sm="12">
            <el-form-item label="类别" prop="category">
              <el-select v-model="form.category" style="width: 100%"><el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" /></el-select>
            </el-form-item>
          </el-col>
          <el-col :sm="12"><el-form-item label="品种"><el-input v-model="form.variety" /></el-form-item></el-col>
          <el-col :sm="12"><el-form-item label="单位"><el-input v-model="form.unit" placeholder="kg" /></el-form-item></el-col>
          <el-col :sm="12"><el-form-item label="保质期"><el-input-number v-model="form.shelfLifeDays" :min="0" controls-position="right" style="width: 100%" /></el-form-item></el-col>
          <el-col :sm="12"><el-form-item label="储存"><el-input v-model="form.storage" placeholder="如 0~5℃ 冷藏" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { CATEGORIES } from '@/utils/dict'

const store = useUserStore()
const canEdit = store.can('FARMER')
const { query, pager, rows, loading, load, search, reset } = useTable(productApi.page, { keyword: '', category: '' })

const dialog = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }]
}

function openForm(row) {
  form.value = row ? { ...row } : { unit: 'kg', shelfLifeDays: null }
  dialog.value = true
}

async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    if (form.value.id) await productApi.update(form.value.id, form.value)
    else await productApi.create(form.value)
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除产品「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await productApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
