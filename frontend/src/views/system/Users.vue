<template>
  <div class="users-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增用户</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="statusText" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-for="role in (row.roles || [])" :key="role" size="small" style="margin: 2px;">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleResetPassword(row)">重置密码</el-button>
            <el-switch
              :model-value="row.status === 1"
              active-text="启用"
              inactive-text="禁用"
              @change="(val: boolean) => handleToggleStatus(row, val)"
              style="margin-left: 8px;"
            />
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

    <!-- Add/Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>

    <!-- Reset password dialog -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="400px" destroy-on-close>
      <el-form :model="resetPwdForm" label-width="80px">
        <el-form-item label="用户">
          <span>{{ resetPwdForm.username }}</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="confirmResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { register, type UserVO } from '@/api/auth'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref<UserVO[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const saveLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)
const editingId = ref<number>()

const form = reactive({ username: '', password: '', realName: '', email: '', phone: '' })
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
}

const resetPwdVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdForm = reactive({ id: 0, username: '', newPassword: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/users', { params: { pageNum: pagination.pageNum, pageSize: pagination.pageSize } })
    const data = res?.data || res
    tableData.value = data?.list || (Array.isArray(data) ? data : [])
    pagination.total = data?.total || 0
  } catch {} finally { loading.value = false }
}

const handleAdd = () => {
  isEdit.value = false; editingId.value = undefined
  Object.assign(form, { username: '', password: '', realName: '', email: '', phone: '' })
  dialogTitle.value = '新增用户'; dialogVisible.value = true
}

const handleEdit = (row: UserVO) => {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, { username: row.username, password: '', realName: row.realName, email: row.email || '', phone: row.phone || '' })
  dialogTitle.value = '编辑用户'; dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value && editingId.value) {
      await request.put(`/users/${editingId.value}`, { realName: form.realName, email: form.email, phone: form.phone })
      ElMessage.success('更新成功')
    } else {
      await register({ username: form.username, password: form.password, realName: form.realName, email: form.email || undefined, phone: form.phone || undefined })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false; fetchData()
  } catch {} finally { saveLoading.value = false }
}

const handleToggleStatus = async (row: UserVO, val: boolean) => {
  try {
    await request.patch(`/users/${row.id}/status`, { status: val ? 1 : 0 })
    ElMessage.success('状态更新成功'); fetchData()
  } catch {}
}

const handleResetPassword = (row: UserVO) => {
  resetPwdForm.id = row.id; resetPwdForm.username = row.username; resetPwdForm.newPassword = ''
  resetPwdVisible.value = true
}

const confirmResetPassword = async () => {
  if (!resetPwdForm.newPassword) { ElMessage.warning('请输入新密码'); return }
  resetPwdLoading.value = true
  try {
    await request.put(`/users/${resetPwdForm.id}/password`, { newPassword: resetPwdForm.newPassword })
    ElMessage.success('密码重置成功'); resetPwdVisible.value = false
  } catch {} finally { resetPwdLoading.value = false }
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.users-page { padding: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
