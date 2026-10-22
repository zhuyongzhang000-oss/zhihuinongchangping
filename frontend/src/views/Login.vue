<template>
  <div class="login">
    <section class="hero cloth">
      <div class="hero-in">
        <div class="logo"><img src="/icon.svg" alt="" /> 农品溯源</div>
        <h1>智慧农业<br />农产品溯源管理系统</h1>
        <p>从田间到餐桌，每一步都有据可查。</p>
        <ol class="flow">
          <li v-for="(s, i) in steps" :key="s"><span class="num">{{ i + 1 }}</span>{{ s }}</li>
        </ol>
      </div>
      <small class="foot">Spring Boot · Vue 3 · MySQL</small>
    </section>

    <section class="panel">
      <div class="box">
        <h2>登录</h2>
        <p class="muted">使用分配给你的账号登录后台</p>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" autocomplete="username" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password autocomplete="current-password" />
          </el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">登 录</el-button>
        </el-form>

        <div class="demo">
          <div class="faint">演示账号（密码均为 123456），点一下自动填入：</div>
          <div class="chips">
            <button v-for="d in demos" :key="d.u" type="button" @click="fill(d.u)">
              <b>{{ d.label }}</b><span class="mono">{{ d.u }}</span>
            </button>
          </div>
        </div>
        <router-link to="/trace" class="consumer"><el-icon><Search /></el-icon> 我是消费者，查询溯源码</router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const steps = ['种植基地', '农事记录', '质量检测', '物流运输', '扫码溯源']
const demos = [
  { label: '管理员', u: 'admin' },
  { label: '生产者', u: 'farmer' },
  { label: '质检员', u: 'inspector' },
  { label: '物流员', u: 'logistics' }
]

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fill(u) {
  form.username = u
  form.password = '123456'
}

async function submit() {
  if (!(await formRef.value.validate().catch(() => false))) return
  loading.value = true
  try {
    await store.login(form)
    ElMessage.success('欢迎回来，' + store.user.realName)
    router.replace(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login { min-height: 100vh; display: grid; grid-template-columns: 1.1fr 1fr; }
.hero { position: relative; display: flex; flex-direction: column; justify-content: center; padding: 48px 56px; overflow: hidden; }
.hero::after {
  /* 一道道田垄 */
  content: ""; position: absolute; right: -80px; bottom: -80px; width: 420px; height: 420px; border-radius: 50%;
  background: repeating-radial-gradient(circle at 100% 100%, rgba(242,212,155,.10) 0 2px, transparent 2px 22px);
  pointer-events: none;
}
.hero-in { position: relative; max-width: 460px; }
.logo { display: flex; align-items: center; gap: 10px; font-size: 16px; letter-spacing: .12em; color: var(--ink); }
.logo img { width: 36px; height: 36px; border-radius: 10px; box-shadow: 0 0 0 1px rgba(255,255,255,.2); }
.hero h1 { font-size: 34px; line-height: 1.35; margin: 34px 0 10px; letter-spacing: .04em; font-weight: 700; }
.hero p { margin: 0; opacity: .82; font-size: 15px; }
.flow { list-style: none; padding: 0; margin: 34px 0 0; display: flex; flex-wrap: wrap; gap: 8px; }
.flow li { display: flex; align-items: center; gap: 6px; padding: 6px 12px 6px 6px; border-radius: 999px; background: rgba(255,255,255,.09); font-size: 13px; }
.flow .num { width: 22px; height: 22px; border-radius: 50%; display: grid; place-items: center; background: var(--ink); color: var(--cloth-deep); font-size: 12px; font-weight: 700; }
.foot { position: absolute; left: 56px; bottom: 24px; opacity: .5; letter-spacing: .08em; }

.panel { display: grid; place-items: center; padding: 32px 20px; background: var(--paper); }
.box { width: 100%; max-width: 380px; }
.box h2 { margin: 0; font-size: 24px; }
.box > p { margin: 4px 0 26px; }
.submit { width: 100%; height: 44px; font-size: 15px; letter-spacing: .2em; }
.demo { margin-top: 26px; padding-top: 18px; border-top: 1px dashed var(--line); font-size: 12px; }
.chips { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 10px; }
.chips button {
  display: flex; justify-content: space-between; align-items: center; gap: 6px; padding: 8px 12px; cursor: pointer;
  border: 1px solid var(--line); border-radius: var(--r-s); background: var(--tint); color: var(--text); font: inherit; font-size: 13px;
}
.chips button:hover { border-color: var(--acc); }
.chips span { color: var(--faint); font-size: 12px; }
.consumer { display: inline-flex; align-items: center; gap: 6px; margin-top: 22px; font-size: 13px; }

@media (max-width: 860px) {
  .login { grid-template-columns: 1fr; }
  .hero { padding: 32px 20px 40px; }
  .hero h1 { font-size: 26px; margin-top: 22px; }
  .flow { margin-top: 22px; }
  .foot { display: none; }
}
</style>
