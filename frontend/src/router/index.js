import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppLayout from '@/layout/AppLayout.vue'

// meta.menu：出现在侧栏；meta.roles：哪些角色能看到（管理员总能看到）
export const menuRoutes = [
  { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '数据看板', icon: 'DataBoard', menu: true } },
  { path: 'bases', name: 'bases', component: () => import('@/views/base/BaseList.vue'), meta: { title: '种植基地', icon: 'Location', menu: true, group: '档案' } },
  { path: 'products', name: 'products', component: () => import('@/views/product/ProductList.vue'), meta: { title: '农产品', icon: 'Apple', menu: true, group: '档案' } },
  { path: 'batches', name: 'batches', component: () => import('@/views/batch/BatchList.vue'), meta: { title: '生产批次', icon: 'Box', menu: true, group: '溯源链' } },
  { path: 'batches/:id', name: 'batch-detail', component: () => import('@/views/batch/BatchDetail.vue'), meta: { title: '批次详情', active: 'batches' } },
  { path: 'farming', name: 'farming', component: () => import('@/views/farming/FarmingList.vue'), meta: { title: '农事记录', icon: 'Sunny', menu: true, group: '溯源链' } },
  { path: 'inspections', name: 'inspections', component: () => import('@/views/inspection/InspectionList.vue'), meta: { title: '质量检测', icon: 'DocumentChecked', menu: true, group: '溯源链' } },
  { path: 'logistics', name: 'logistics', component: () => import('@/views/logistics/LogisticsList.vue'), meta: { title: '物流运输', icon: 'Van', menu: true, group: '溯源链' } },
  { path: 'trace-codes', name: 'trace-codes', component: () => import('@/views/trace/TraceCodeList.vue'), meta: { title: '溯源码', icon: 'Postcard', menu: true, group: '溯源链' } },
  { path: 'users', name: 'users', component: () => import('@/views/user/UserList.vue'), meta: { title: '用户管理', icon: 'User', menu: true, group: '系统', roles: ['ADMIN'] } },
  { path: 'profile', name: 'profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true, title: '登录' } },
    // 消费者扫码页面，不需要登录
    { path: '/trace', name: 'trace-search', component: () => import('@/views/public/TraceSearch.vue'), meta: { public: true, title: '农产品溯源查询' } },
    { path: '/trace/:code', name: 'trace', component: () => import('@/views/public/TraceResult.vue'), meta: { public: true, title: '溯源信息' } },
    { path: '/', component: AppLayout, redirect: '/dashboard', children: menuRoutes },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async to => {
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '智慧农业农产品溯源管理系统'
  if (to.meta.public) return true
  const store = useUserStore()
  if (!store.token) return { name: 'login', query: { redirect: to.fullPath } }
  if (!store.user) {
    try {
      await store.fetchMe()
    } catch (e) {
      store.logout()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  if (to.meta.roles && !store.can(...to.meta.roles)) return { name: 'dashboard' }
  return true
})

export default router
