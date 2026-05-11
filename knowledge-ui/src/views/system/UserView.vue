<template>
  <div class="system-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增用户</el-button>
        </div>
      </template>

      <el-table :data="userList" border v-loading="loading">
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="handleAssign(row)">关联配置</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" v-model:current="current" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="loadList" class="pagination" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
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

    <el-dialog v-model="assignDialogVisible" title="关联配置" width="700px">
      <el-tabs>
        <el-tab-pane label="部门">
          <el-tree ref="assignDeptTreeRef" :data="deptTree" show-checkbox node-key="id"
            :props="{ label: 'name', children: 'children' }" default-expand-all />
          <div style="margin-top: 10px">
            <span style="font-size: 13px; color: #909399;">勾选设为部门负责人：</span>
            <el-checkbox-group v-model="leaderDeptIds">
              <el-checkbox v-for="id in checkedDeptIds" :key="id" :value="id" :label="id">
                {{ getDeptName(id) }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </el-tab-pane>
        <el-tab-pane label="地区">
          <el-select v-model="selectedRegionIds" multiple placeholder="选择地区" style="width: 100%">
            <el-option v-for="r in regionList" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-tab-pane>
        <el-tab-pane label="角色">
          <el-checkbox-group v-model="selectedRoleIds">
            <el-checkbox v-for="r in roleList" :key="r.id" :value="r.id" :label="r.id">
              {{ r.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserPage, getUser, saveUser, updateUser, deleteUser,
  assignUserDepts, getUserDeptIds, assignUserRegions, getUserRegionIds,
  assignUserRoles, getUserRoleIds } from '@/api/user'
import { getDeptTree } from '@/api/dept'
import { getRegionList } from '@/api/region'
import { getRoleList } from '@/api/role'
import { ElMessage, ElMessageBox } from 'element-plus'

const userList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const assignDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const currentUserId = ref(null)
const current = ref(1)
const pageSize = ref(10)
const total = ref(0)
const form = ref({ name: '', phone: '', email: '', status: 1 })
const rules = { name: [{ required: true, message: '请输入姓名', trigger: 'blur' }] }

// Assignment
const deptTree = ref([])
const regionList = ref([])
const roleList = ref([])
const assignDeptTreeRef = ref(null)
const checkedDeptIds = computed(() => assignDeptTreeRef.value?.getCheckedKeys() || [])
const leaderDeptIds = ref([])
const selectedRegionIds = ref([])
const selectedRoleIds = ref([])

const deptNameMap = ref({})

const getDeptName = (id) => deptNameMap.value[id] || id

const loadList = async () => {
  loading.value = true
  try {
    const res = await getUserPage(current.value, pageSize.value)
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadAssignData = async () => {
  const [deptRes, regionRes, roleRes] = await Promise.all([
    getDeptTree(), getRegionList(), getRoleList()
  ])
  deptTree.value = deptRes.data || []
  regionList.value = regionRes.data || []
  roleList.value = roleRes.data || []
  // Build name map
  const flatten = (nodes) => {
    for (const n of nodes) {
      deptNameMap.value[n.id] = n.name
      if (n.children) flatten(n.children)
    }
  }
  flatten(deptTree.value)
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { name: '', phone: '', email: '', status: 1 }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  const res = await getUser(row.id)
  form.value = { ...res.data }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该用户？', '提示')
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadList()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateUser(form.value)
  } else {
    await saveUser(form.value)
  }
  ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
  dialogVisible.value = false
  loadList()
}

const handleAssign = async (row) => {
  currentUserId.value = row.id
  assignDialogVisible.value = true
  selectedRegionIds.value = []
  selectedRoleIds.value = []
  leaderDeptIds.value = []
  await new Promise(r => setTimeout(r, 100))

  const [deptIds, regionIds, roleIds] = await Promise.all([
    getUserDeptIds(row.id), getUserRegionIds(row.id), getUserRoleIds(row.id)
  ])
  if (assignDeptTreeRef.value) {
    assignDeptTreeRef.value.setCheckedKeys(deptIds.data || [])
  }
  selectedRegionIds.value = regionIds.data || []
  selectedRoleIds.value = roleIds.data || []
}

const handleSaveAssign = async () => {
  if (!currentUserId.value) return
  const deptKeys = assignDeptTreeRef.value ? assignDeptTreeRef.value.getCheckedKeys() : []
  await Promise.all([
    assignUserDepts(currentUserId.value, deptKeys, leaderDeptIds.value),
    assignUserRegions(currentUserId.value, selectedRegionIds.value),
    assignUserRoles(currentUserId.value, selectedRoleIds.value)
  ])
  ElMessage.success('关联配置保存成功')
  assignDialogVisible.value = false
}

onMounted(() => {
  loadList()
  loadAssignData()
})
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
