<template>
  <div class="nl-page">
    <!-- 免责声明横幅 -->
    <el-alert
      title="Prototype Disclaimer"
      type="warning"
      :closable="false"
      show-icon
      class="disclaimer-banner"
    >
      <template #default>
        <p>
          This NL interface is a <strong>research prototype</strong>.
          It uses rule-based keyword matching (not a trained NLU model) and supports only a fixed set of command templates.
          The underlying causal graph is a <strong>hardcoded 7-edge DAG</strong> over 4 system modules, not a dynamically learned structure.
          This page exists to demonstrate the concept; it is <strong>not production-ready</strong>.
        </p>
      </template>
    </el-alert>

    <el-card shadow="hover" class="nl-card">
      <template #header>
        <div class="card-header">
          <span>Natural Language Command</span>
          <el-tag type="info" size="small">Prototype</el-tag>
        </div>
      </template>

      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          placeholder='Try: "查询设备 EQ-2024-001" or "入库耗材键盘 50个"'
          @keydown.ctrl.enter="handleExecute"
        />
        <div class="button-row">
          <el-button type="primary" :loading="loading" @click="handleExecute">
            Execute
          </el-button>
          <el-button :loading="loadingCausal" @click="handleCausalCheck">
            Execute + Causal Check
          </el-button>
          <span class="hint">Ctrl+Enter to execute</span>
        </div>
      </div>

      <!-- 执行结果 -->
      <div v-if="resultText" class="result-area">
        <el-divider content-position="left">Result</el-divider>
        <el-alert :title="resultText" type="success" :closable="false" show-icon />
      </div>

      <!-- 因果检查结果 -->
      <div v-if="causalResult" class="result-area">
        <el-divider content-position="left">Causal Impact Report</el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="Causal Check Performed">
            <el-tag :type="causalResult.causalCheckPerformed ? 'success' : 'info'" size="small">
              {{ causalResult.causalCheckPerformed ? 'Yes' : 'No' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Entity ID">
            {{ causalResult.entityId ?? 'N/A' }}
          </el-descriptions-item>
          <el-descriptions-item label="Impacted Nodes" :span="2">
            {{ Object.keys(causalResult.impactedNodes).length }}
          </el-descriptions-item>
          <el-descriptions-item label="Execution Result" :span="2">
            {{ causalResult.executionResult }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="Object.keys(causalResult.impactedNodes).length > 0" class="impact-table">
          <el-table :data="impactTableData" size="small" stripe>
            <el-table-column prop="nodeId" label="Node ID" />
            <el-table-column prop="weight" label="Impact Weight" width="140">
              <template #default="{ row }">
                <el-progress
                  :percentage="Math.round(row.weight * 100)"
                  :color="row.weight >= 0.5 ? '#F56C6C' : '#409EFF'"
                  :stroke-width="14"
                  :text-inside="true"
                />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 支持的命令示例 -->
      <el-divider content-position="left">Supported Command Examples</el-divider>
      <div class="examples">
        <el-tag
          v-for="cmd in exampleCommands"
          :key="cmd"
          class="example-tag"
          @click="inputText = cmd"
        >
          {{ cmd }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 自然语言指令输入页面
 * @description 提供NL指令执行和因果影响预测功能。注意：此页面为原型演示，
 *              NL引擎使用基于规则的关键词匹配，因果图谱为硬编码的7条边DAG。
 */
import { ref, computed } from 'vue'
import { executeNL, executeWithCausalCheck, type CausalCheckResult } from '@/api/nl'

const inputText = ref('')
const loading = ref(false)
const loadingCausal = ref(false)
const resultText = ref('')
const causalResult = ref<CausalCheckResult | null>(null)

const exampleCommands = [
  '查询设备 EQ-2024-001',
  '列出所有维护中的设备',
  '给设备 EQ-2024-001 做巡检',
  '入库耗材键盘 50个',
  '查询我的操作日志',
  '给用户张三分配管理员角色',
]

const impactTableData = computed(() => {
  if (!causalResult.value) return []
  return Object.entries(causalResult.value.impactedNodes).map(([nodeId, weight]) => ({
    nodeId,
    weight,
  }))
})

const handleExecute = async () => {
  if (!inputText.value.trim()) return
  loading.value = true
  resultText.value = ''
  causalResult.value = null
  try {
    resultText.value = await executeNL(inputText.value)
  } catch {
    resultText.value = 'Execution failed.'
  } finally {
    loading.value = false
  }
}

const handleCausalCheck = async () => {
  if (!inputText.value.trim()) return
  loadingCausal.value = true
  resultText.value = ''
  causalResult.value = null
  try {
    causalResult.value = await executeWithCausalCheck(inputText.value)
    resultText.value = causalResult.value.executionResult
  } catch {
    resultText.value = 'Execution failed.'
  } finally {
    loadingCausal.value = false
  }
}
</script>

<style scoped lang="scss">
.nl-page {
  max-width: 900px;
  margin: 0 auto;
}

.disclaimer-banner {
  margin-bottom: 16px;

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.6;
  }
}

.nl-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
  }
}

.input-area {
  .button-row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 12px;

    .hint {
      font-size: 12px;
      color: #999;
    }
  }
}

.result-area {
  margin-top: 16px;
}

.impact-table {
  margin-top: 12px;
}

.examples {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .example-tag {
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      color: #409EFF;
      border-color: #409EFF;
    }
  }
}
</style>
