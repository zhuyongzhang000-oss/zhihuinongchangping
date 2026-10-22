<template>
  <div class="layout" :class="{ open: drawer }">
    <aside class="side cloth">
      <router-link to="/dashboard" class="brand" @click="drawer = false">
        <img src="/icon.svg" alt="" />
        <div>
          <b>农品溯源</b>
          <small>智慧农业管理平台</small>
        </div>
      </router-link>
      <nav class="nav">
        <template v-for="g in groups" :key="g.name">
          <div v-if="g.name" class="nav-group">{{ g.name }}</div>
          <router-link v-for="r in g.items" :key="r.name" :to="'/' + r.path" class="nav-item"
                       :class="{ active: activeName === r.name }" @click="drawer = false">
            <el-icon><component :is="r.meta.icon" /></el-icon>
            <span>{{ r.meta.title }}</span>
          </router-link>
        </template>
      </nav>
      <router-link to="/trace" target="_blank" class="nav-foot">
        <el-icon><Search /></el-icon> 消费者查询页
      </router-link>
    </aside>
    <div class="scrim-mask" @click="drawer = false" />

    <section class="main">
      <header class="topbar">
        <button class="icon-btn menu-btn" aria-label="打开菜单" @click="drawer = true">
          <el-icon><Menu /></el-icon>
        </button>
        <div class="crumb">{{ route.meta.title }}</div>
        <div class="grow" />
        <button class="icon-btn" :aria-label="theme.dark ? '切换到日间' : '切换到夜间'" :title="theme.dark ? '日间' : '夜间'" @click="theme.toggle()">
          <el-icon><Sunny v-if="theme.dark" /><Moon v-else /></el-icon>
        </button>
        <el-dropdown trigger="click" @command="onCommand">
          <button class="user-btn">
            <span class="avatar">{{ (store.user?.realName || '?').slice(-1) }}</span>
            <span class="who">
              <b>{{ store.user?.realName }}</b>
              <small>{{ ROLES[store.user?.role]?.label }}</small>
            </span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile" icon="User">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" icon="SwitchButton" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { menuRoutes } from '@/router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { ROLES } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const theme = useThemeStore()
theme.watchSystem()
const drawer = ref(false)

const activeName = computed(() => route.meta.active || route.name)
const groups = computed(() => {
  const list = []
  for (const r of menuRoutes) {
    if (!r.meta.menu || (r.meta.roles && !store.can(...r.meta.roles))) continue
    const name = r.meta.group || ''
    let g = list.find(x => x.name === name)
    if (!g) list.push((g = { name, items: [] }))
    g.items.push(r)
  }
  return list
})

async function onCommand(cmd) {
  if (cmd === 'profile') return router.push('/profile')
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  store.logout()
  router.replace('/login')
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.side {
  width: 228px; flex: none; position: sticky; top: 0; height: 100vh; display: flex; flex-direction: column;
  padding: 18px 12px; z-index: 30;
}
.brand { display: flex; align-items: center; gap: 10px; padding: 4px 10px 18px; color: var(--on-cloth); }
.brand img { width: 38px; height: 38px; border-radius: 10px; box-shadow: 0 0 0 1px rgba(255,255,255,.18); }
.brand b { display: block; font-size: 17px; letter-spacing: .08em; color: var(--ink); }
.brand small { display: block; font-size: 11px; opacity: .72; letter-spacing: .1em; }
.nav { flex: 1; overflow-y: auto; }
.nav-group { font-size: 11px; letter-spacing: .16em; opacity: .55; padding: 14px 12px 6px; }
.nav-item {
  position: relative; display: flex; align-items: center; gap: 10px; height: 40px; padding: 0 12px; margin: 2px 0;
  border-radius: var(--r-s); color: var(--on-cloth); opacity: .86; transition: background .15s, opacity .15s;
}
.nav-item:hover { background: rgba(255,255,255,.07); opacity: 1; }
.nav-item.active { background: rgba(255,255,255,.13); color: var(--ink); opacity: 1; font-weight: 600; }
.nav-item.active::before { content: ""; position: absolute; left: -12px; top: 10px; bottom: 10px; width: 3px; border-radius: 0 3px 3px 0; background: var(--ink); }
.nav-item .el-icon { font-size: 17px; }
.nav-foot { display: flex; align-items: center; gap: 8px; padding: 10px 12px; margin-top: 8px; border-top: 1px dashed rgba(255,255,255,.2); color: var(--on-cloth); opacity: .75; font-size: 13px; }
.nav-foot:hover { opacity: 1; }

.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.topbar {
  position: sticky; top: 0; z-index: 20; height: 58px; display: flex; align-items: center; gap: 8px; padding: 0 22px;
  background: color-mix(in srgb, var(--paper) 92%, transparent); backdrop-filter: blur(8px); border-bottom: 1px solid var(--line);
}
.crumb { font-weight: 600; font-size: 15px; }
.grow { flex: 1; }
.icon-btn {
  width: 36px; height: 36px; border: 0; border-radius: var(--r-s); background: transparent; color: var(--sub);
  display: inline-grid; place-items: center; cursor: pointer; font-size: 18px;
}
.icon-btn:hover { background: var(--tint); color: var(--text); }
.menu-btn { display: none; }
.user-btn {
  display: flex; align-items: center; gap: 8px; border: 0; background: transparent; cursor: pointer; padding: 4px 8px;
  border-radius: var(--r-s); color: var(--text); font: inherit;
}
.user-btn:hover { background: var(--tint); }
.avatar {
  width: 32px; height: 32px; border-radius: 50%; display: grid; place-items: center; font-weight: 600;
  background-color: var(--cloth); background-image: var(--linen); color: var(--ink);
}
.who { display: flex; flex-direction: column; align-items: flex-start; line-height: 1.2; }
.who b { font-size: 13px; font-weight: 600; }
.who small { font-size: 11px; color: var(--faint); }
.content { padding: 22px; width: 100%; max-width: 1440px; margin: 0 auto; }
.scrim-mask { display: none; }

.fade-enter-active, .fade-leave-active { transition: opacity .15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 960px) {
  .side { position: fixed; left: 0; transform: translateX(-100%); transition: transform .22s; box-shadow: 4px 0 24px rgba(0,0,0,.2); }
  .open .side { transform: none; }
  .open .scrim-mask { display: block; position: fixed; inset: 0; background: var(--scrim); z-index: 25; }
  .menu-btn { display: inline-grid; }
  .topbar { padding: 0 12px; }
  .content { padding: 16px; }
  .who { display: none; }
}
</style>
