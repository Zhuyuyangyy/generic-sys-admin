<template>
  <div class="nl-assistant-page">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <span>NL 助手</span>
          <el-tag :type="dryRunMode ? 'warning' : 'success'" size="small">
            {{ dryRunMode ? 'Dry Run 模式' : '直接执行模式' }}
          </el-tag>
          <el-switch v-model="dryRunMode" active-text="Dry Run" inactive-text="直接执行" style="margin-left: 12px;" />
        </div>
      </template>

      <!-- Message history -->
      <div class="chat-messages" ref="messagesRef">
        <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
          <div class="message-content">
            <div v-if="msg.role === 'user'" class="msg-text">{{ msg.content }}</div>
            <div v-else-if="msg.role === 'dry-run'" class="msg-dry-run">
              <div class="parsed-intent"><strong>解析意图：</strong>{{ msg.data?.parsedIntent }}</div>
              <div class="affected-entities" v-if="msg.data?.affectedEntities?.length">
                <strong>受影响实体：</strong>
                <el-tag v-for="e in msg.data.affectedEntities" :key="e.id" size="small" style="margin: 2px;">
                  {{ e.type }}: {{ e.name }} ({{ e.change }})
                </el-tag>
              </div>
              <div class="warnings" v-if="msg.data?.warnings?.length">
                <strong>警告：</strong>
                <div v-for="(w, i) in msg.data.warnings" :key="i" style="color: #E6A23C; font-size: 13px;">{{ w }}</div>
              </div>
              <div class="estimated-impact"><strong>预估影响：</strong>{{ msg.data?.estimatedImpact }}</div>
              <div class="risk-level">
                <strong>风险等级：</strong>
                <el-tag :color="riskColor(msg.data?.riskLevel)" style="color: #fff; border: none;" size="small">
                  {{ msg.data?.riskLevel || 'UNKNOWN' }}
                </el-tag>
              </div>
              <el-button type="primary" size="small" style="margin-top: 8px;" @click="handleConfirmExecute(msg)">
                确认执行
              </el-button>
            </div>
            <div v-else-if="msg.role === 'result'" class="msg-result">
              <el-tag :type="msg.data?.success ? 'success' : 'danger'" size="small">{{ msg.data?.success ? '执行成功' : '执行失败' }}</el-tag>
              <div style="margin-top: 4px;">{{ msg.data?.message }}</div>
              <div v-if="msg.data?.results?.length" style="margin-top: 8px;">
                <div v-for="(r, i) in msg.data.results" :key="i" style="font-size: 13px; color: #666;">
                  {{ r.action }} - {{ r.target }}: {{ r.status }} {{ r.detail || '' }}
                </div>
              </div>
              <div v-if="msg.data?.causalWarnings?.length" style="margin-top: 8px;">
                <div v-for="(w, i) in msg.data.causalWarnings" :key="i" style="font-size: 13px;">
                  <el-tag :color="riskColor(w.severity)" style="color: #fff; border: none;" size="small">{{ w.severity }}</el-tag>
                  {{ w.description }}
                </div>
              </div>
            </div>
            <div v-else class="msg-text" style="color: #999;">{{ msg.content }}</div>
          </div>
        </div>
        <div v-if="messages.length === 0" class="empty-chat">
          <el-empty description="输入自然语言指令开始交互" :image-size="80" />
        </div>
      </div>

      <!-- Test commands -->
      <div class="test-commands" v-if="testCommands.length > 0">
        <span style="font-size: 12px; color: #999; margin-right: 8px;">快捷指令：</span>
        <el-tag
          v-for="(cmd, idx) in testCommands"
          :key="idx"
          size="small"
          effect="plain"
          style="cursor: pointer; margin: 2px;"
          @click="inputText = cmd"
        >{{ cmd }}</el-tag>
      </div>

      <!-- Input area -->
      <div class="chat-input">
        <el-input
          v-model="inputText"
          placeholder="输入自然语言指令..."
          @keyup.enter="handleSend"
          :disabled="sending"
        >
          <template #append>
            <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
          </template>
        </el-input>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { dryRun, executeConfirmed, getTestCommands } from '@/api/nl'
import type { DryRunResult, ExecuteResult } from '@/utils/types'

interface ChatMessage {
  role: 'user' | 'dry-run' | 'result' | 'system'
  content: string
  data?: any
}

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const sending = ref(false)
const dryRunMode = ref(true)
const messagesRef = ref()
const testCommands = ref<string[]>([])

const riskColor = (level?: string) => {
  const map: Record<string, string> = { LOW: '#67C23A', MEDIUM: '#E6A23C', HIGH: '#F56C6C', CRITICAL: '#8B0000' }
  return map[level || ''] || '#909399'
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  messages.value.push({ role: 'user', content: text })
  scrollToBottom()

  sending.value = true
  try {
    if (dryRunMode.value) {
      const result: DryRunResult = await dryRun(text)
      messages.value.push({
        role: 'dry-run',
        content: '',
        data: result,
      })
    } else {
      const result: ExecuteResult = await executeConfirmed(text, '')
      messages.value.push({ role: 'result', content: '', data: result })
    }
  } catch (err: any) {
    messages.value.push({ role: 'system', content: `错误: ${err?.message || '请求失败'}` })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

const handleConfirmExecute = async (msg: ChatMessage) => {
  sending.value = true
  try {
    const result: ExecuteResult = await executeConfirmed(msg.data.input, msg.data.confirmationId)
    messages.value.push({ role: 'result', content: '', data: result })
  } catch (err: any) {
    messages.value.push({ role: 'system', content: `执行错误: ${err?.message || '请求失败'}` })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

const fetchTestCommands = async () => {
  try {
    const res: any = await getTestCommands()
    testCommands.value = Array.isArray(res) ? res : res?.data || []
  } catch {}
}

onMounted(() => { fetchTestCommands() })
</script>

<style scoped lang="scss">
.nl-assistant-page { padding: 10px; height: calc(100vh - 100px); }
.chat-card { height: 100%; display: flex; flex-direction: column; }
.card-header { display: flex; justify-content: space-between; align-items: center; }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 12px; min-height: 400px; max-height: calc(100vh - 320px);
  .message { margin-bottom: 12px; display: flex;
    &.user { justify-content: flex-end; .message-content { background: #409EFF; color: #fff; border-radius: 12px 12px 2px 12px; padding: 8px 14px; max-width: 70%; } }
    &.dry-run, &.result { justify-content: flex-start; .message-content { background: #f5f7fa; border-radius: 12px 12px 12px 2px; padding: 10px 14px; max-width: 80%; } }
    &.system { justify-content: center; .message-content { color: #999; font-size: 13px; } }
  }
  .msg-dry-run, .msg-result { font-size: 13px; line-height: 1.6; }
}

.test-commands { padding: 8px 12px; border-top: 1px solid #f0f0f0; }

.chat-input { padding: 8px 0 0; border-top: 1px solid #f0f0f0; }

.empty-chat { display: flex; align-items: center; justify-content: center; height: 300px; }
</style>
