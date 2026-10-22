// 字典：代码 → 中文 + 标签样式（ok / bad / warn / info / 空）

export const ROLES = {
  ADMIN: { label: '管理员', tone: 'warn' },
  FARMER: { label: '生产者', tone: 'ok' },
  INSPECTOR: { label: '质检员', tone: 'info' },
  LOGISTICS: { label: '物流员', tone: '' }
}

// 批次生命周期，顺序即流程顺序
export const BATCH_STATUS = {
  GROWING: { label: '种植中', tone: '' },
  HARVESTED: { label: '已采收', tone: 'warn' },
  INSPECTED: { label: '质检合格', tone: 'ok' },
  REJECTED: { label: '质检不合格', tone: 'bad' },
  SHIPPING: { label: '运输中', tone: 'info' },
  ON_SALE: { label: '已上市', tone: 'ok' }
}

export const FARMING_TYPES = {
  SOW: { label: '播种/定植' },
  FERTILIZE: { label: '施肥' },
  IRRIGATE: { label: '灌溉' },
  PEST: { label: '病虫害防治' },
  WEED: { label: '除草' },
  HARVEST: { label: '采收' },
  OTHER: { label: '其他' }
}

export const INSPECT_RESULT = {
  PASS: { label: '合格', tone: 'ok' },
  FAIL: { label: '不合格', tone: 'bad' }
}

export const LOGISTICS_STATUS = {
  IN_TRANSIT: { label: '运输中', tone: 'info' },
  ARRIVED: { label: '已到达', tone: 'ok' }
}

export const CATEGORIES = ['蔬菜', '水果', '粮油', '茶叶', '调味品', '其他']

export const options = dict => Object.entries(dict).map(([value, v]) => ({ value, label: v.label }))
