<template>
  <el-dialog :model-value="modelValue" :title="form.id ? '编辑农事记录' : '记录农事'" width="560px" destroy-on-close
             @update:model-value="v => emit('update:modelValue', v)">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="批次" prop="batchId">
        <el-select v-model="form.batchId" filterable :disabled="!!batchId || !!form.id" style="width: 100%" placeholder="只列种植中 / 已采收的批次">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo}  ${b.productName}`" :value="b.id" />
        </el-select>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :sm="12">
          <el-form-item label="类型" prop="type">
            <el-select v-model="form.type" style="width: 100%">
              <el-option v-for="o in options(FARMING_TYPES)" :key="o.value" v-bind="o" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :sm="12"><el-form-item label="日期" prop="opDate"><el-date-picker v-model="form.opDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="投入品"><el-input v-model="form.material" placeholder="种子 / 肥料 / 农药" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="用量"><el-input v-model="form.dosage" placeholder="如 40 kg/亩" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="作业人"><el-input v-model="form.operator" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item></el-col>
      </el-row>
      <p v-if="form.type === 'HARVEST'" class="faint tip">登记「采收」后，种植中的批次会自动变为「已采收」，可以送检。</p>
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
import { batchApi, farmingApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { FARMING_TYPES, options } from '@/utils/dict'

const props = defineProps({ modelValue: Boolean, row: { type: Object, default: null }, batchId: { type: Number, default: null } })
const emit = defineEmits(['update:modelValue', 'saved'])
const store = useUserStore()

const formRef = ref()
const form = ref({})
const saving = ref(false)
const batches = ref([])
const rules = {
  batchId: [{ required: true, message: '请选择批次', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  opDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

watch(() => props.modelValue, async v => {
  if (!v) return
  form.value = props.row ? { ...props.row } : { batchId: props.batchId, type: 'FERTILIZE', opDate: dayjs().format('YYYY-MM-DD'), operator: store.user?.realName }
  batches.value = await batchApi.options(['GROWING', 'HARVESTED'])
}, { immediate: true })

async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    if (form.value.id) await farmingApi.update(form.value.id, form.value)
    else await farmingApi.create(form.value)
    ElMessage.success('已保存')
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip { margin: 0 0 0 80px; font-size: 12px; }
</style>
