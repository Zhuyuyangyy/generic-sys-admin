<template>
  <div class="batches-page">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="批次号">
          <el-input v-model="searchForm.batchNo" placeholder="批次号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="耗材名称">
          <el-input v-model="searchForm.consumableName" placeholder="耗材名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="在库" value="IN_STOCK" />
            <el-option label="已出库" value="OUT_OF_STOCK" />
            <el-option label="已过期" value="EXPIRED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>批次管理</span>
          <div class="header-actions">
            <el-tag type="warning" size="small" style="margin-right: 8px;">
              即将过期: {{ expiringCount }}
            </el-tag>
            <el-tag type="danger" size="small">已过期: {{ expiredCount }}</el-tag>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border
        :row-class-name="rowClassName"
      >
        <el-table-column prop="batchNo" label="批次号" min-width="140" />
        <el-table-column prop="consumableName" label="耗材名称" min-width="140" />
        <el-table-column prop="quantity" label="入库数量" width="100" />
        <el-table-column prop="remainingQuantity" label="剩余数量" width="100">
          <template #default="{ row }">
            <span :class="{ 'low-stock': row.remainingQuantity <= 0 }">{{ row.remainingQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="supplier" label="供应商" width="120" />
        <el-table-column prop="inboundDate" label="入库日期" width="110">
          <template #default="{ row }">{{ row.inboundDate?.slice(0, 10) }}</template>
        </el-table-column>
        <el-table-column prop="expirationDate" label="有效期至" width="110">
          <template #default="{ row }">
            <span :class="expiryClass(row)">{{ row.expirationDate?.slice(0, 10) || '无限期' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.daysUntilExpiry != null" :type="daysTagType(row.daysUntilExpiry)" size="small">
              {{ row.daysUntilExpiry }}天
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="batchStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import type { BatchVO } from '@/utils/types'

const batchStatusType = (s: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    IN_STOCK: 'success', OUT_OF_STOCK: 'info', EXPIRED: 'danger',
  }
  return map[s] || 'info'
}

const daysTagType = (days: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  if (days < 0) return 'danger'
  if (days <= 30) return 'warning'
  if (days <= 90) return 'primary'
  return 'success'
}

const expiryClass = (row: BatchVO) => {
  if (row.daysUntilExpiry == null) return ''
  if (row.daysUntilExpiry < 0) return 'expired'
  if (row.daysUntilExpiry <= 30) return 'expiring-soon'
  return ''
}

const rowClassName = ({ row }: { row: BatchVO }) => {
  if (row.daysUntilExpiry != null && row.daysUntilExpiry < 0) return 'expired-row'
  return ''
}

const searchForm = reactive({ batchNo: '', consumableName: '', status: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const loading = ref(false)
const tableData = ref<BatchVO[]>([])

const expiringCount = computed(() => tableData.value.filter(b => b.daysUntilExpiry != null && b.daysUntilExpiry > 0 && b.daysUntilExpiry <= 30).length)
const expiredCount = computed(() => tableData.value.filter(b => b.daysUntilExpiry != null && b.daysUntilExpiry < 0).length)

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/consumables/batches', {
      params: {
        pageNum: pagination.pageNum, pageSize: pagination.pageSize,
        batchNo: searchForm.batchNo || undefined,
        consumableName: searchForm.consumableName || undefined,
        status: searchForm.status || undefined,
      },
    })
    const data = res?.data || res
    tableData.value = data?.list || (Array.isArray(data) ? data : [])
    pagination.total = data?.total || 0
  } catch {} finally { loading.value = false }
}

const handleSearch = () => { pagination.pageNum = 1; fetchData() }
const handleReset = () => {
  searchForm.batchNo = ''; searchForm.consumableName = ''; searchForm.status = ''
  pagination.pageNum = 1; fetchData()
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.batches-page { display: flex; flex-direction: column; gap: 12px; padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; .header-actions { display: flex; align-items: center; } }
.pagination { margin-top: 16px; justify-content: flex-end; }
.low-stock { color: #f56c6c; font-weight: bold; }
.expired { color: #f56c6c; font-weight: bold; }
.expiring-soon { color: #e6a23c; font-weight: 600; }

:deep(.expired-row) {
  background-color: #fef0f0 !important;
}
</style>
