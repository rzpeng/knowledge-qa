<template>
  <div class="system-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>账号管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增账号</el-button>
        </div>
      </template>

      <el-table :data="accountList" border v-loading="loading">
        <el-table-column prop="username" label="登录名" />
        <el-table-column prop="userId" label="关联用户ID" width="120" />
        <el-table-column prop="accountType" label="账号类型" width="120" />
        <el-table-column label="超级管理员" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isSuperAdmin === 1 ? 'danger' : 'info'" size="small">
              {{ row.isSuperAdmin === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" v-model:current="current" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="loadList" class="pagination" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑账号' : '新增账号'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="关联用户" prop="userId">
          <el-input-number v-model="form.userId" :min="1" placeholder="用户ID" />
        </el-form-item>
        <el-form-item label="登录名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password" :required="!isEdit">
          <el-input v-model="form.password" type="password" show-password
            :placeholder="isEdit ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="账号类型" prop="accountType">
          <el-input v-model="form.accountType" placeholder="PASSWORD" />
        </el-form-item>
        <el-form-item label="超级管理员" prop="isSuperAdmin">
          <el-switch v-model="form.isSuperAdmin" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAccountPage, getAccount, saveAccount, updateAccount, deleteAccount } from '@/api/account'
import { ElMessage, ElMessageBox } from 'element-plus'

const accountList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const current = ref(1)
const pageSize = ref(10)
const total = ref(0)

const form = ref({ userId: null, username: '', password: '', accountType: 'PASSWORD', isSuperAdmin: 0, status: 1 })
const rules = {
  userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }]
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAccountPage(current.value, pageSize.value)
    accountList.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { userId: null, username: '', password: '', accountType: 'PASSWORD', isSuperAdmin: 0, status: 1 }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  const res = await getAccount(row.id)
  form.value = { ...res.data, password: '' }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该账号？', '提示')
  await deleteAccount(row.id)
  ElMessage.success('删除成功')
  loadList()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    const data = { ...form.value }
    if (!data.password) delete data.password
    await updateAccount(data)
  } else {
    await saveAccount(form.value)
  }
  ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
  dialogVisible.value = false
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.system-view {
  padding: 20px;
}
.pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>
