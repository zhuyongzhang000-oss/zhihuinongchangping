<template>
  <div class="label">
    <div class="label-head cloth">
      <span>农产品溯源码</span>
      <b>{{ productName }}</b>
    </div>
    <div class="label-body">
      <canvas ref="qr" />
      <div class="code mono">{{ code }}</div>
      <div class="faint">批次 {{ batchNo }} · 扫码查看来源</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import QRCode from 'qrcode'
import { traceUrl } from '@/utils/format'

const props = defineProps({
  code: { type: String, required: true },
  productName: { type: String, default: '' },
  batchNo: { type: String, default: '' }
})
const qr = ref()

const draw = () => QRCode.toCanvas(qr.value, traceUrl(props.code), { width: 180, margin: 1, color: { dark: '#2e302b', light: '#ffffff' } })
onMounted(draw)
watch(() => props.code, draw)

/** 导出一张可打印的标签 PNG（固定浅色，打印更清楚） */
async function download() {
  const W = 480, H = 600, c = document.createElement('canvas')
  c.width = W
  c.height = H
  const g = c.getContext('2d')
  const font = getComputedStyle(document.body).fontFamily
  g.fillStyle = '#f7f4ed'
  g.fillRect(0, 0, W, H)
  g.fillStyle = '#4f6b4a'
  g.fillRect(0, 0, W, 120)
  g.fillStyle = '#f1efe6'
  g.font = `16px ${font}`
  g.fillText('农产品溯源码', 32, 46)
  g.fillStyle = '#f2d49b'
  g.font = `bold 30px ${font}`
  g.fillText(props.productName, 32, 92)
  const img = await QRCode.toDataURL(traceUrl(props.code), { width: 300, margin: 1 })
  await new Promise(res => {
    const im = new Image()
    im.onload = () => { g.drawImage(im, (W - 300) / 2, 150, 300, 300); res() }
    im.src = img
  })
  g.fillStyle = '#2e302b'
  g.textAlign = 'center'
  g.font = `bold 26px Consolas, Menlo, monospace`
  g.fillText(props.code, W / 2, 500)
  g.fillStyle = '#8f8b80'
  g.font = `15px ${font}`
  g.fillText(`批次 ${props.batchNo} · 微信 / 浏览器扫码查看来源`, W / 2, 540)
  const a = document.createElement('a')
  a.href = c.toDataURL('image/png')
  a.download = `溯源码-${props.code}.png`
  a.click()
}

defineExpose({ download })
</script>

<style scoped>
.label { width: 260px; border-radius: var(--r); overflow: hidden; border: 1px solid var(--line); background: var(--paper); margin: 0 auto; }
.label-head { padding: 14px 18px; display: flex; flex-direction: column; }
.label-head span { font-size: 12px; opacity: .8; }
.label-head b { font-size: 18px; color: var(--ink); }
.label-body { padding: 16px; text-align: center; }
.label-body canvas { border-radius: 8px; display: block; margin: 0 auto; }
.code { font-size: 18px; font-weight: 700; margin-top: 10px; letter-spacing: .08em; }
.label-body .faint { font-size: 12px; }
</style>
