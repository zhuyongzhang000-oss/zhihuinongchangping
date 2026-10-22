import http from './request'

/** 生成一套标准的增删改查接口 */
const crud = base => ({
  page: params => http.get(base, { params }),
  get: id => http.get(`${base}/${id}`),
  create: data => http.post(base, data),
  update: (id, data) => http.put(`${base}/${id}`, data),
  remove: id => http.delete(`${base}/${id}`)
})

export const authApi = {
  login: data => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  profile: data => http.put('/auth/profile', data),
  password: data => http.put('/auth/password', data)
}

export const userApi = { ...crud('/users'), resetPassword: id => http.put(`/users/${id}/reset-password`) }
export const baseApi = { ...crud('/bases'), options: () => http.get('/bases/options') }
export const productApi = { ...crud('/products'), options: () => http.get('/products/options') }
export const batchApi = {
  ...crud('/batches'),
  options: status => http.get('/batches/options', { params: { status: status?.join(',') } }),
  setStatus: (id, status) => http.put(`/batches/${id}/status`, { status })
}
export const farmingApi = crud('/farming-records')
export const inspectionApi = crud('/inspections')
export const logisticsApi = { ...crud('/logistics'), arrive: (id, arriveTime) => http.put(`/logistics/${id}/arrive`, { arriveTime }) }
export const traceCodeApi = {
  page: params => http.get('/trace-codes', { params }),
  generate: batchId => http.post(`/trace-codes/batch/${batchId}`),
  setStatus: (id, status) => http.put(`/trace-codes/${id}/status`, { status })
}
export const dashboardApi = { overview: () => http.get('/dashboard/overview') }
export const publicApi = { trace: code => http.get(`/public/trace/${encodeURIComponent(code)}`, { silent: true }) }
