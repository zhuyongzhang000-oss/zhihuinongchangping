<template>
  <el-dialog :model-value="modelValue" :title="form.id ? '编辑运单' : '登记发运'" width="620px" destroy-on-close
             @update:model-value="v => emit('update:modelValue', v)">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
      <el-form-item label="批次" prop="batchId">
        <el-select v-model="form.batchId" filterable :disabled="!!batchId || !!form.id" style="width: 100%" placeholder="只列质检合格的批次" @change="fillFrom">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo}  ${b.productName}`" :value="b.id" />
        </el-select>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :sm="12"><el-form-item label="运单号"><el-input v-model="form.waybillNo" placeholder="留空自动生成" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="承运单位"><el-input v-model="form.carrier" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="起运地" prop="fromAddr"><el-input v-model="form.fromAddr" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="目的地" prop="toAddr"><el-input v-model="form.toAddr" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="车牌号"><el-input v-model="form.vehicleNo" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="运输温度"><el-input v-model="form.temperature" placeholder="如 3~5℃ / 常温" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="司机"><el-input v-model="form.driver" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="司机电话"><el-input v-model="form.driverPhone" /></el-form-item></el-col>
        <el-col :sm="12">
          <el-form-item label="发车时间" prop="departTime">
            <el-date-picker v-model="form.departTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :sm="12">
          <el-form-item label="到达时间">
            <el-date-picker v-model="form.arriveTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm" placeholder="未到达留空" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item></el-col>
      </el-row>
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
import { batchApi, logisticsApi } from '@/api'
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
  fromAddr: [{ required: true, message: '请填写起运地', trigger: 'blur' }],
  toAddr: [{ required: true, message: '请填写目的地', trigger: 'blur' }],
  departTime: [{ required: true, message: '请选择发车时间', trigger: 'change' }]
}

watch(() => props.modelValue, async v => {
  if (!v) return
  form.value = props.row ? { ...props.row }
    : { batchId: props.batchId, departTime: dayjs().format('YYYY-MM-DD HH:mm:ss'), driver: store.user?.realName, driverPhone: store.user?.phone }
  batches.value = await batchApi.options(['INSPECTED', 'SHIPPING', 'ON_SALE'])
  fillFrom()
}, { immediate: true })

// 起运地默认填批次所在基地
function fillFrom() {
  const b = batches.value.find(x => x.id === form.value.batchId)
  if (b && !form.value.fromAddr) form.value.fromAddr = b.baseName
}

async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    if (form.value.id) await logisticsApi.update(form.value.id, form.value)
    else await logisticsApi.create(form.value)
    ElMessage.success('已保存')
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
