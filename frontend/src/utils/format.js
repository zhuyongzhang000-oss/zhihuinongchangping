import dayjs from 'dayjs'

export const fmtDate = v => (v ? dayjs(v).format('YYYY-MM-DD') : '—')
export const fmtTime = v => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—')
export const fmtNum = (v, unit = '') => (v == null || v === '' ? '—' : Number(v).toLocaleString('zh-CN') + (unit ? ' ' + unit : ''))

/** 消费者页和二维码里用的溯源地址 */
export const traceUrl = code => `${location.origin}/trace/${code}`
