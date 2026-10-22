<template>
  <div class="profile">
    <div class="page-head"><div><h2>个人中心</h2><p>修改个人资料和登录密码</p></div></div>
    <div class="grid">
      <div class="card">
        <div class="card-title"><h3>个人资料</h3></div>
        <el-form :model="info" label-width="70px">
          <el-form-item label="用户名"><el-input :model-value="store.user?.username" disabled /></el-form-item>
          <el-form-item label="角色"><Pill :dict="ROLES" :value="store.user?.role" /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="info.realName" /></el-form-item>
          <el-form-item label="电话"><el-input v-model="info.phone" /></el-form-item>
          <el-form-item><el-button type="primary" :loading="saving1" @click="saveInfo">保存资料</el-button></el-form-item>
        </el-form>
      </div>
      <div class="card">
        <div class="card-title"><h3>修改密码</h3></div>
        <el-form ref="pwdRef" :model="pwd" :rules="pwdRules" label-width="80px">
          <el-form-item label="原密码" prop="oldPassword"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwd.newPassword" type="password" show-password placeholder="6~32 位" /></el-form-item>
          <el-form-item label="确认密码" prop="confirm"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
          <el-form-item><el-button type="primary" :loading="saving2" @click="savePwd">修改密码</el-button></el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ROLES } from '@/utils/dict'
import Pill from '@/components/Pill.vue'

const store = useUserStore()
const router = useRouter()
const info = reactive({ realName: store.user?.realName, phone: store.user?.phone })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRef = ref()
const saving1 = ref(false)
const saving2 = ref(false)
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, max: 32, message: '新密码 6~32 位', trigger: 'blur' }],
  confirm: [{ validator: (r, v, cb) => (v === pwd.newPassword ? cb() : cb(new Error('两次输入不一致'))), trigger: 'blur' }]
}

async function saveInfo() {
  saving1.value = true
  try {
    store.user = await authApi.profile(info)
    ElMessage.success('资料已保存')
  } finally {
    saving1.value = false
  }
}

async function savePwd() {
  if (!(await pwdRef.value.validate().catch(() => false))) return
  saving2.value = true
  try {
    await authApi.password({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    store.logout()
    router.replace('/login')
  } finally {
    saving2.value = false
  }
}
</script>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; max-width: 980px; }
.grid .card { margin: 0; }
@media (max-width: 800px) { .grid { grid-template-columns: minmax(0, 1fr); } }
</style>
