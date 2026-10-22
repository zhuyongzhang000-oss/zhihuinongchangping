import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const TOKEN_KEY = 'agri-token'

const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE || '/api', timeout: 15000 })

http.interceptors.request.use(cfg => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

// 后端统一返回 { code, msg, data }：code=200 直接给 data，其它弹出 msg
http.interceptors.response.use(
  resp => {
    const body = resp.data
    if (body && body.code === 200) return body.data
    return fail(body?.code, body?.msg, resp.config)
  },
  err => {
    const body = err.response?.data
    return fail(body?.code || err.response?.status, body?.msg || (err.code === 'ECONNABORTED' ? '请求超时' : '网络连接失败'), err.config)
  }
)

function fail(code, msg, config) {
  if (code === 401 && !config?.url?.startsWith('/public')) {
    localStorage.removeItem(TOKEN_KEY)
    if (router.currentRoute.value.name !== 'login') {
      router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    }
  }
  if (!config?.silent) ElMessage.error(msg || '操作失败')
  const e = new Error(msg || '操作失败')
  e.code = code
  return Promise.reject(e)
}

export default http
