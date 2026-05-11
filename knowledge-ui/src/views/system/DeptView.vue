<template>
  <div class="system-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增部门</el-button>
        </div>
      </template>

      <el-table :data="deptTree" row-key="id" default-expand-all border>
        <el-table-column prop="name" label="部门名称" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select v-model="form.parentId" :data="deptTree" :props="{ label: 'name', value: 'id' }" placeholder="请选择上级部门" clearable check-strictly />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { getDeptTree, getDept, saveDept, updateDept, deleteDept } from '@/api/dept'
import { ElMessage, ElMessageBox } from 'element-plus'

const deptTree = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ name: '', parentId: null, sortOrder: 0, status: 1 })
const rules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

const loadTree = async () => {
  const res = await getDeptTree()
  deptTree.value = res.data || []
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { name: '', parentId: null, sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  const res = await getDept(row.id)
  form.value = { ...res.data }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该部门？', '提示')
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  loadTree()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateDept(form.value)
  } else {
    await saveDept(form.value)
  }
  ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
  dialogVisible.value = false
  loadTree()
}

onMounted(loadTree)
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
</style>
