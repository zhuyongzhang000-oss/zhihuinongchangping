<template>
  <ol class="tl">
    <li v-for="(e, i) in events" :key="i" :class="e.tone">
      <span class="tl-dot" />
      <div class="tl-time">{{ e.time }}</div>
      <div class="tl-card">
        <div class="tl-head">
          <span class="tl-stage">{{ e.stage }}</span>
          <b>{{ e.title }}</b>
          <span v-if="e.pill" class="pill" :class="e.pill.tone">{{ e.pill.label }}</span>
        </div>
        <div v-for="(l, j) in e.lines" :key="j" class="tl-line">{{ l }}</div>
      </div>
    </li>
    <li v-if="!events.length" class="empty">暂无记录</li>
  </ol>
</template>

<script setup>
import { computed } from 'vue'
import { FARMING_TYPES, INSPECT_RESULT } from '@/utils/dict'
import { fmtDate, fmtTime } from '@/utils/format'

/** 把批次详情（农事 / 质检 / 物流）排成一条时间线，后台详情页和消费者页共用 */
const props = defineProps({ detail: { type: Object, required: true } })

const join = (...parts) => parts.filter(Boolean).join(' · ')

const events = computed(() => {
  const d = props.detail
  const list = []
  d.farmingRecords.forEach(r => list.push({
    sort: r.opDate, time: fmtDate(r.opDate), stage: '种植', tone: r.type === 'HARVEST' ? 'warn' : '',
    title: FARMING_TYPES[r.type]?.label || r.type,
    lines: [join(r.material && '投入品：' + r.material, r.dosage && '用量：' + r.dosage, r.operator && '作业人：' + r.operator), r.description].filter(Boolean)
  }))
  d.inspections.forEach(r => list.push({
    sort: r.inspectDate, time: fmtDate(r.inspectDate), stage: '质检', tone: r.result === 'PASS' ? 'ok' : 'bad',
    title: r.agency || '质量检测', pill: INSPECT_RESULT[r.result],
    lines: [join('报告编号：' + r.reportNo, r.inspector && '检测人：' + r.inspector),
      join(r.pesticideResidue && '农药残留：' + r.pesticideResidue, r.heavyMetal && '重金属：' + r.heavyMetal, r.otherItems),
      r.conclusion].filter(Boolean)
  }))
  d.logistics.forEach(r => {
    list.push({
      sort: r.departTime, time: fmtTime(r.departTime), stage: '物流', tone: 'info',
      title: `${r.fromAddr} → ${r.toAddr}`,
      lines: [join(r.carrier, r.vehicleNo, r.driver && '司机：' + r.driver), join(r.temperature && '运输温度：' + r.temperature, '运单：' + r.waybillNo)].filter(Boolean)
    })
    if (r.arriveTime) {
      list.push({ sort: r.arriveTime, time: fmtTime(r.arriveTime), stage: '到达', tone: 'ok', title: '已送达 ' + r.toAddr, lines: [] })
    }
  })
  return list.sort((a, b) => String(a.sort).localeCompare(String(b.sort)))
})
</script>

<style scoped>
.tl { list-style: none; margin: 0; padding: 0 0 0 4px; }
.tl li { position: relative; padding: 0 0 18px 26px; --c: var(--faint); }
.tl li::before { content: ""; position: absolute; left: 5px; top: 14px; bottom: -4px; width: 2px; background: var(--line); }
.tl li:last-child::before { display: none; }
.tl li.ok { --c: var(--ok); }
.tl li.bad { --c: var(--bad); }
.tl li.warn { --c: var(--warn); }
.tl li.info { --c: var(--info); }
.tl-dot { position: absolute; left: 0; top: 4px; width: 12px; height: 12px; border-radius: 50%; background: var(--paper); border: 3px solid var(--c); }
.tl-time { font-size: 12px; color: var(--faint); font-family: var(--f-num); }
.tl-card { margin-top: 4px; padding: 10px 14px; background: var(--tint); border-radius: var(--r-s); }
.tl-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tl-stage { font-size: 11px; padding: 0 6px; border-radius: 4px; color: var(--c); border: 1px solid currentColor; line-height: 18px; }
.tl-line { font-size: 13px; color: var(--sub); margin-top: 2px; }
.tl li.empty { padding-left: 0; }
.tl li.empty::before { display: none; }
</style>
