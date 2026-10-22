<template>
  <el-dialog :model-value="modelValue" :title="form.id ? '编辑质检记录' : '录入质检结果'" width="600px" destroy-on-close
             @update:model-value="v => emit('update:modelValue', v)">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
      <el-form-item label="批次" prop="batchId">
        <el-select v-model="form.batchId" filterable :disabled="!!batchId || !!form.id" style="width: 100%" placeholder="只列已采收 / 待复检的批次">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo}  ${b.productName}`" :value="b.id" />
        </el-select>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :sm="12"><el-form-item label="报告编号"><el-input v-model="form.reportNo" placeholder="留空自动生成" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="检测日期" prop="inspectDate"><el-date-picker v-model="form.inspectDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="检测机构"><el-input v-model="form.agency" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="检测人"><el-input v-model="form.inspector" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="农药残留"><el-input v-model="form.pesticideResidue" placeholder="如 未检出" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="重金属"><el-input v-model="form.heavyMetal" placeholder="如 铅 0.02 mg/kg" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="其他项目"><el-input v-model="form.otherItems" placeholder="糖度、水分等" /></el-form-item></el-col>
        <el-col :span="24">
          <el-form-item label="检测结论" prop="result">
            <el-radio-group v-model="form.result">
              <el-radio-button value="PASS">合格</el-radio-button>
              <el-radio-button value="FAIL">不合格</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="24"><el-form-item label="结论说明"><el-input v-model="form.conclusion" type="textarea" :rows="2" /></el-form-item></el-col>
      </el-row>
      <p class="faint tip">保存后批次状态按最近一次检测结论更新：合格可发运、可生成溯源码；不合格则禁止流通。</p>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { batchApi, inspectionApi } from '@/api'
import { useUserStore } from '@/stores/user'

const props = defineProps({ modelValue: Boolean, row: { type: Object, default: null }, batchId: { type: Number, default: null } })
const emit = defineEmits(['update:modelValue', 'saved'])
const store = useUserStore()

const formRef = ref()
const form = ref({})
const saving = ref(false)
const batches = ref([])
const rules = {
  batchId: [{ required: true, message: '请选择批次', trigger: 'change' }],
  inspectDate: [{ required: true, message: '请选择检测日期', trigger: 'change' }],
  result: [{ required: true, message: '请选择检测结论', trigger: 'change' }]
}

watch(() => props.modelValue, async v => {
  if (!v) return
  form.value = props.row ? { ...props.row }
    : { batchId: props.batchId, inspectDate: dayjs().format('YYYY-MM-DD'), result: 'PASS', inspector: store.user?.realName }
  batches.value = await batchApi.options(['HARVESTED', 'INSPECTED', 'REJECTED'])
}, { immediate: true })

async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    if (form.value.id) await inspectionApi.update(form.value.id, form.value)
    else await inspectionApi.create(form.value)
    ElMessage.success('已保存')
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip { margin: 0 0 0 86px; font-size: 12px; }
</style>
