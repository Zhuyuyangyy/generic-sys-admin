<template>
  <div class="reports-page">
    <el-card>
      <template #header><span>报表中心</span></template>

      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
        <!-- Asset Summary -->
        <el-tab-pane label="资产汇总" name="asset">
          <div class="report-toolbar">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 280px" />
            <el-button type="primary" :loading="reportLoading" @click="generateReport('asset')">生成报表</el-button>
          </div>
          <el-descriptions v-if="assetReport" :column="2" border size="small">
            <el-descriptions-item label="资产总数">{{ assetReport.totalAssets }}</el-descriptions-item>
            <el-descriptions-item label="资产总值">¥{{ assetReport.totalValue?.toLocaleString() }}</el-descriptions-item>
            <el-descriptions-item label="统计周期">{{ assetReport.period }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="assetReport?.byCategory?.length" :data="assetReport.byCategory" stripe size="small" style="margin-top: 12px;">
            <el-table-column prop="category" label="类别" />
            <el-table-column prop="count" label="数量" width="100" />
            <el-table-column prop="value" label="价值" width="140">
              <template #default="{ row }">¥{{ row.value?.toLocaleString() }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!assetReport && !reportLoading" description="请选择日期范围并生成报表" :image-size="60" />
        </el-tab-pane>

        <!-- Inventory Summary -->
        <el-tab-pane label="库存汇总" name="inventory">
          <div class="report-toolbar">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 280px" />
            <el-button type="primary" :loading="reportLoading" @click="generateReport('inventory')">生成报表</el-button>
          </div>
          <el-descriptions v-if="inventoryReport" :column="2" border size="small">
            <el-descriptions-item label="耗材种类">{{ inventoryReport.totalConsumables }}</el-descriptions-item>
            <el-descriptions-item label="库存总值">¥{{ inventoryReport.totalValue?.toLocaleString() }}</el-descriptions-item>
            <el-descriptions-item label="总库存量">{{ inventoryReport.totalStock }}</el-descriptions-item>
            <el-descriptions-item label="统计周期">{{ inventoryReport.period }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="inventoryReport?.lowStockItems?.length" :data="inventoryReport.lowStockItems" stripe size="small" style="margin-top: 12px;">
            <el-table-column prop="name" label="耗材名称" />
            <el-table-column prop="category" label="类别" width="100" />
            <el-table-column prop="currentStock" label="当前库存" width="100" />
            <el-table-column prop="minStockLevel" label="最低库存" width="100" />
            <el-table-column prop="shortage" label="缺口" width="80" />
          </el-table>
          <el-empty v-if="!inventoryReport && !reportLoading" description="请选择日期范围并生成报表" :image-size="60" />
        </el-tab-pane>

        <!-- Maintenance History -->
        <el-tab-pane label="维保历史" name="maintenance">
          <div class="report-toolbar">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 280px" />
            <el-button type="primary" :loading="reportLoading" @click="generateReport('maintenance')">生成报表</el-button>
          </div>
          <el-descriptions v-if="maintenanceReport" :column="2" border size="small">
            <el-descriptions-item label="总计划数">{{ maintenanceReport.totalPlans }}</el-descriptions-item>
            <el-descriptions-item label="已完成">{{ maintenanceReport.completedPlans }}</el-descriptions-item>
            <el-descriptions-item label="逾期">{{ maintenanceReport.overduePlans }}</el-descriptions-item>
            <el-descriptions-item label="平均完成天数">{{ maintenanceReport.averageCompletionDays }}</el-descriptions-item>
            <el-descriptions-item label="统计周期">{{ maintenanceReport.period }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="maintenanceReport?.byEquipment?.length" :data="maintenanceReport.byEquipment" stripe size="small" style="margin-top: 12px;">
            <el-table-column prop="equipmentName" label="设备名称" />
            <el-table-column prop="totalPlans" label="总计划" width="80" />
            <el-table-column prop="completedPlans" label="已完成" width="80" />
            <el-table-column prop="averageCompletionDays" label="平均天数" width="90" />
          </el-table>
          <el-empty v-if="!maintenanceReport && !reportLoading" description="请选择日期范围并生成报表" :image-size="60" />
        </el-tab-pane>

        <!-- Audit Trail -->
        <el-tab-pane label="审计轨迹" name="audit">
          <div class="report-toolbar">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 280px" />
            <el-button type="primary" :loading="reportLoading" @click="generateReport('audit')">生成报表</el-button>
          </div>
          <el-descriptions v-if="auditReport" :column="2" border size="small">
            <el-descriptions-item label="总日志数">{{ auditReport.totalLogs }}</el-descriptions-item>
            <el-descriptions-item label="统计周期">{{ auditReport.period }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="auditReport?.byModule?.length" :data="auditReport.byModule" stripe size="small" style="margin-top: 12px;">
            <el-table-column prop="module" label="模块" />
            <el-table-column prop="count" label="操作次数" width="100" />
          </el-table>
          <el-empty v-if="!auditReport && !reportLoading" description="请选择日期范围并生成报表" :image-size="60" />
        </el-tab-pane>

        <!-- Stock Movement -->
        <el-tab-pane label="库存变动" name="stock">
          <div class="report-toolbar">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 280px" />
            <el-button type="primary" :loading="reportLoading" @click="generateReport('stock')">生成报表</el-button>
          </div>
          <el-descriptions v-if="stockReport" :column="2" border size="small">
            <el-descriptions-item label="总入库">{{ stockReport.totalInbound }}</el-descriptions-item>
            <el-descriptions-item label="总出库">{{ stockReport.totalOutbound }}</el-descriptions-item>
            <el-descriptions-item label="调整次数">{{ stockReport.totalAdjustments }}</el-descriptions-item>
            <el-descriptions-item label="净变动">{{ stockReport.netChange }}</el-descriptions-item>
            <el-descriptions-item label="统计周期">{{ stockReport.period }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="stockReport?.byConsumable?.length" :data="stockReport.byConsumable" stripe size="small" style="margin-top: 12px;">
            <el-table-column prop="consumableName" label="耗材名称" />
            <el-table-column prop="inbound" label="入库" width="80" />
            <el-table-column prop="outbound" label="出库" width="80" />
            <el-table-column prop="adjustments" label="调整" width="80" />
            <el-table-column prop="netChange" label="净变动" width="80" />
          </el-table>
          <el-empty v-if="!stockReport && !reportLoading" description="请选择日期范围并生成报表" :image-size="60" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getAssetSummaryReport, getInventorySummaryReport, getMaintenanceHistoryReport,
  getAuditTrailReport, getStockMovementReport,
} from '@/api/report'
import type {
  AssetSummaryReport, InventorySummaryReport, MaintenanceHistoryReport,
  AuditTrailReport, StockMovementReport,
} from '@/utils/types'

const activeTab = ref('asset')
const dateRange = ref<string[]>([])
const reportLoading = ref(false)

const assetReport = ref<AssetSummaryReport | null>(null)
const inventoryReport = ref<InventorySummaryReport | null>(null)
const maintenanceReport = ref<MaintenanceHistoryReport | null>(null)
const auditReport = ref<AuditTrailReport | null>(null)
const stockReport = ref<StockMovementReport | null>(null)

const getParams = () => ({
  startDate: dateRange.value?.[0] || undefined,
  endDate: dateRange.value?.[1] || undefined,
})

const generateReport = async (type: string) => {
  reportLoading.value = true
  try {
    const params = getParams()
    const res: any = await ({
      asset: () => getAssetSummaryReport(params),
      inventory: () => getInventorySummaryReport(params),
      maintenance: () => getMaintenanceHistoryReport(params),
      audit: () => getAuditTrailReport(params),
      stock: () => getStockMovementReport(params),
    } as Record<string, () => any>)[type]()
    const data = res?.data || res
    if (type === 'asset') assetReport.value = data
    else if (type === 'inventory') inventoryReport.value = data
    else if (type === 'maintenance') maintenanceReport.value = data
    else if (type === 'audit') auditReport.value = data
    else if (type === 'stock') stockReport.value = data
  } catch {} finally { reportLoading.value = false }
}

const handleTabChange = () => {
  // Tab change only, no auto-load
}
</script>

<style scoped lang="scss">
.reports-page { padding: 10px; }
.report-toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
</style>
