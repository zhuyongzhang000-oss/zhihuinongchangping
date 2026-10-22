<template>
  <div>
    <div class="page-head">
      <div><h2>种植基地</h2><p>登记产地信息，消费者扫码时能看到农产品来自哪里</p></div>
      <el-button v-if="canEdit" type="primary" icon="Plus" @click="openForm()">新增基地</el-button>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="名称 / 编号 / 地址" clearable prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="search">
          <el-option label="使用中" :value="1" /><el-option label="已停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="code" label="编号" width="110"><template #default="{ row }"><span class="mono">{{ row.code }}</span></template></el-table-column>
        <el-table-column prop="name" label="基地名称" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="面积" width="110" align="right"><template #default="{ row }"><span class="num">{{ fmtNum(row.area, '亩') }}</span></template></el-table-column>
        <el-table-column prop="manager" label="负责人" width="90" />
        <el-table-column prop="phone" label="电话" width="125" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><span class="pill" :class="row.status === 1 ? 'ok' : ''">{{ row.status === 1 ? '使用中' : '已停用' }}</span></template>
        </el-table-column>
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

    <el-dialog v-model="dialog" :title="form.id ? '编辑基地' : '新增基地'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :sm="12"><el-form-item label="编号" prop="code"><el-input v-model="form.code" placeholder="如 CQ-FJ-01" /></el-form-item></el-col>
          <el-col :sm="12"><el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :sm="12"><el-form-item label="面积(亩)"><el-input-number v-model="form.area" :min="0" :precision="2" controls-position="right" style="width: 100%" /></el-form-item></el-col>
          <el-col :sm="12"><el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">使用中</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :sm="12"><el-form-item label="负责人"><el-input v-model="form.manager" /></el-form-item></el-col>
          <el-col :sm="12"><el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="环境说明"><el-input v-model="form.environment" placeholder="土壤、水源、气候等" /></el-form-item></el-col>
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
import { baseApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { fmtNum } from '@/utils/format'

const store = useUserStore()
const canEdit = store.can('FARMER')
const { query, pager, rows, loading, load, search, reset } = useTable(baseApi.page, { keyword: '', status: '' })

const dialog = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

function openForm(row) {
  form.value = row ? { ...row } : { status: 1, area: null }
  dialog.value = true
}

async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    if (form.value.id) await baseApi.update(form.value.id, form.value)
    else await baseApi.create(form.value)
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除基地「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await baseApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
