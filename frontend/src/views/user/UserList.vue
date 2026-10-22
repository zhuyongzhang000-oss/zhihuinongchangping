<template>
  <div>
    <div class="page-head">
      <div><h2>用户管理</h2><p>按角色分工：生产者记农事、质检员出报告、物流员管运输</p></div>
      <el-button type="primary" icon="Plus" @click="openForm()">新增用户</el-button>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="用户名 / 姓名 / 电话" clearable prefix-icon="Search" @keyup.enter="search" @clear="search" />
        <el-select v-model="query.role" placeholder="全部角色" clearable @change="search">
          <el-option v-for="o in options(ROLES)" :key="o.value" v-bind="o" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="username" label="用户名" width="130"><template #default="{ row }"><span class="mono">{{ row.username }}</span></template></el-table-column>
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="角色" width="100"><template #default="{ row }"><Pill :dict="ROLES" :value="row.role" /></template></el-table-column>
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><span class="pill" :class="row.status === 1 ? 'ok' : 'bad'">{{ row.status === 1 ? '启用' : '停用' }}</span></template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="150"><template #default="{ row }">{{ fmtTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="primary" @click="resetPwd(row)">重置密码</el-button>
            <el-button link type="danger" :disabled="row.id === store.user.id" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="pager.page" v-model:page-size="pager.size" :total="pager.total"
                       layout="total, sizes, prev, pager, next" background @change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑用户' : '新增用户'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码"><el-input v-model="form.password" type="password" show-password placeholder="留空则为 123456" /></el-form-item>
        <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio v-for="o in options(ROLES)" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" /></el-form-item>
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
import { userApi } from '@/api'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/stores/user'
import { ROLES, options } from '@/utils/dict'
import { fmtTime } from '@/utils/format'
import Pill from '@/components/Pill.vue'

const store = useUserStore()
const { query, pager, rows, loading, load, search, reset } = useTable(userApi.page, { keyword: '', role: '' })
const dialog = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function openForm(row) {
  form.value = row ? { ...row } : { role: 'FARMER', status: 1 }
  dialog.value = true
}

async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    if (form.value.id) await userApi.update(form.value.id, form.value)
    else await userApi.create(form.value)
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function resetPwd(row) {
  await ElMessageBox.confirm(`把「${row.realName}」的密码重置为 123456？`, '重置密码', { type: 'warning' })
  await userApi.resetPassword(row.id)
  ElMessage.success('密码已重置为 123456')
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.realName}」吗？`, '删除确认', { type: 'warning' })
  await userApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
