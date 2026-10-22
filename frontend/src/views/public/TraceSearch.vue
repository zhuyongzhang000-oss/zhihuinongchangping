<template>
  <div class="search cloth">
    <div class="box">
      <img src="/icon.svg" alt="" class="logo" />
      <h1>农产品溯源查询</h1>
      <p>输入包装上的溯源码，查看这份农产品从田间到餐桌的全过程</p>
      <form class="field" @submit.prevent="go">
        <input v-model.trim="code" class="mono" maxlength="32" placeholder="如 T6QK8M3NXA2P" aria-label="溯源码" autocomplete="off" />
        <button type="submit" :disabled="!code">查询</button>
      </form>
      <div class="demo">示例：<a v-for="c in demos" :key="c" class="mono" @click="code = c">{{ c }}</a></div>
    </div>
    <router-link to="/login" class="admin">管理后台登录 →</router-link>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const code = ref('')
const demos = ['T6QK8M3NXA2P', 'T7LJ2C9TGW8H']
const go = () => code.value && router.push('/trace/' + code.value.toUpperCase())
</script>

<style scoped>
.search { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px 16px; }
.box { width: 100%; max-width: 440px; text-align: center; }
.logo { width: 64px; height: 64px; border-radius: 16px; box-shadow: 0 0 0 1px rgba(255,255,255,.2); }
h1 { margin: 18px 0 6px; color: var(--ink); font-size: 26px; letter-spacing: .06em; }
p { margin: 0 0 26px; opacity: .8; font-size: 14px; }
.field { display: flex; background: var(--paper); border-radius: 14px; padding: 6px; box-shadow: 0 10px 30px rgba(0,0,0,.18); }
.field input { flex: 1; min-width: 0; border: 0; outline: 0; background: transparent; color: var(--text); font-size: 18px; padding: 0 12px; text-transform: uppercase; letter-spacing: .06em; }
.field button { border: 0; border-radius: 10px; padding: 0 22px; height: 46px; font-size: 16px; cursor: pointer; background: var(--cloth-deep); color: var(--ink); font-weight: 600; font-family: inherit; }
.field button:disabled { opacity: .5; cursor: default; }
.demo { margin-top: 16px; font-size: 13px; opacity: .85; }
.demo a { color: var(--ink); margin: 0 6px; cursor: pointer; text-decoration: underline dotted; }
.admin { position: absolute; bottom: 20px; color: var(--on-cloth); opacity: .6; font-size: 13px; }
.admin:hover { opacity: 1; }
</style>
