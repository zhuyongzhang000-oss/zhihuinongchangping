import { reactive, ref } from 'vue'

/**
 * 列表页通用逻辑：查询条件、分页、加载状态
 * fetcher 接收 { page, size, ...query }，返回 { records, total }
 */
export function useTable(fetcher, initialQuery = {}) {
  const query = reactive({ ...initialQuery })
  const pager = reactive({ page: 1, size: 10, total: 0 })
  const rows = ref([])
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      const params = { page: pager.page, size: pager.size }
      for (const [k, v] of Object.entries(query)) if (v !== '' && v != null) params[k] = v
      const res = await fetcher(params)
      rows.value = res.records
      pager.total = res.total
    } catch (e) {
      rows.value = []
    } finally {
      loading.value = false
    }
  }

  function search() {
    pager.page = 1
    load()
  }

  function reset() {
    Object.assign(query, initialQuery)
    search()
  }

  return { query, pager, rows, loading, load, search, reset }
}
