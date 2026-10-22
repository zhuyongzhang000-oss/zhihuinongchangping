import { defineStore } from 'pinia'

const KEY = 'agri-theme'
const media = window.matchMedia('(prefers-color-scheme: dark)')

function apply(mode) {
  const dark = mode === 'dark' || (mode === 'auto' && media.matches)
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
  return dark
}

/** 日间 / 夜间 / 跟随系统 */
export const useThemeStore = defineStore('theme', {
  state: () => {
    let mode = 'auto'
    try { mode = localStorage.getItem(KEY) || 'auto' } catch (e) { /* 隐私模式 */ }
    return { mode, dark: apply(mode) }
  },
  actions: {
    set(mode) {
      this.mode = mode
      try { localStorage.setItem(KEY, mode) } catch (e) { /* 隐私模式 */ }
      this.dark = apply(mode)
    },
    toggle() {
      this.set(this.dark ? 'light' : 'dark')
    },
    watchSystem() {
      media.addEventListener('change', () => { if (this.mode === 'auto') this.dark = apply('auto') })
    }
  }
})
