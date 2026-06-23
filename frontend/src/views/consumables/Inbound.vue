<template>
  <div class="inbound-page">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <template #header><span>耗材入库</span></template>
          <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
            <el-form-item label="耗材" prop="consumableId">
              <el-select v-model="form.consumableId" placeholder="请选择耗材" style="width: 100%" filterable>
                <el-option v-for="item in consumableList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="供应商">
              <el-input v-model="form.supplier" placeholder="可选" />
            </el-form-item>
            <el-form-item label="参考单号">
              <el-input v-model="form.referenceNo" placeholder="可选" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="可选" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="submitLoading" @click="handleSubmit">确认入库</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header><span>入库历史</span></template>
          <el-table :data="historyList" v-loading="historyLoading" stripe size="small">
            <el-table-column prop="consumableName" label="耗材名称" min-width="140" />
            <el-table-column prop="quantity" label="入库数量" width="100" />
            <el-table-column prop="supplier" label="供应商" width="120" />
            <el-table-column prop="referenceNo" label="参考单号" width="140" />
            <el-table-column prop="remarks" label="备注" min-width="120" />
            <el-table-column prop="createTime" label="入库时间" width="160">
              <template #default="{ row }">{{ row.createTime?.replace('T', ' ').slice(0, 19) || '-' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="historyList.length === 0 && !historyLoading" description="暂无入库记录" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConsumablePage, inboundConsumable, type ConsumableVO } from '@/api/consumable'
import request from '@/utils/request'

const consumableList = ref<ConsumableVO[]>([])
const submitLoading = ref(false)
const formRef = ref()
const form = reactive({ consumableId: undefined as number | undefined, quantity: 1, supplier: '', referenceNo: '', remarks: '' })
const formRules = {
  consumableId: [{ required: true, message: '请选择耗材', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
}

const historyLoading = ref(false)
const historyList = ref<any[]>([])

const fetchConsumables = async () => {
  try {
    const res = await getConsumablePage({ pageNum: 1, pageSize: 500 })
    consumableList.value = res.list || []
  } catch {}
}

const fetchHistory = async () => {
  historyLoading.value = true
  try {
    const res: any = await request.get('/consumables/inbound/history')
    historyList.value = Array.isArray(res) ? res : res?.data?.list || res?.data || []
  } catch {} finally { historyLoading.value = false }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await inboundConsumable(form.consumableId!, form.quantity, form.referenceNo || undefined, form.remarks || undefined)
    ElMessage.success('入库成功')
    form.consumableId = undefined; form.quantity = 1; form.supplier = ''; form.referenceNo = ''; form.remarks = ''
    fetchHistory()
  } catch {} finally { submitLoading.value = false }
}

onMounted(() => { fetchConsumables(); fetchHistory() })
</script>

<style scoped lang="scss">
.inbound-page { padding: 10px; }
</style>
