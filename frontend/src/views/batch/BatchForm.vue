<template>
  <el-dialog :model-value="modelValue" :title="form.id ? '编辑批次' : '新建批次'" width="580px" destroy-on-close
             @update:model-value="v => emit('update:modelValue', v)">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="批次号">
            <el-input v-model="form.batchNo" placeholder="留空自动生成，如 B20261022-001" :disabled="!!form.id" />
          </el-form-item>
        </el-col>
        <el-col :sm="12">
          <el-form-item label="产品" prop="productId">
            <el-select v-model="form.productId" filterable style="width: 100%">
              <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :sm="12">
          <el-form-item label="种植基地" prop="baseId">
            <el-select v-model="form.baseId" filterable style="width: 100%">
              <el-option v-for="b in bases" :key="b.id" :label="b.name" :value="b.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :sm="12"><el-form-item label="种植日期"><el-date-picker v-model="form.plantDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="采收日期"><el-date-picker v-model="form.harvestDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        <el-col :sm="12"><el-form-item label="产量"><el-input-number v-model="form.quantity" :min="0" :precision="2" controls-position="right" style="width: 100%" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
      </el-row>
      <p class="faint tip">填了采收日期的新批次直接记为「已采收」；也可以以后在农事记录里登记「采收」。</p>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { baseApi, batchApi, productApi } from '@/api'

const props = defineProps({ modelValue: Boolean, row: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const form = ref({})
const saving = ref(false)
const products = ref([])
const bases = ref([])
const rules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  baseId: [{ required: true, message: '请选择基地', trigger: 'change' }]
}

watch(() => props.modelValue, v => { if (v) form.value = props.row ? { ...props.row } : { quantity: null } }, { immediate: true })

onMounted(async () => {
  ;[products.value, bases.value] = await Promise.all([productApi.options(), baseApi.options()])
})

async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  try {
    const res = form.value.id ? await batchApi.update(form.value.id, form.value) : await batchApi.create(form.value)
    ElMessage.success(form.value.id ? '已保存' : `已创建批次 ${res.batchNo}`)
    emit('update:modelValue', false)
    emit('saved', res)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip { margin: 0 0 0 86px; font-size: 12px; }
</style>
