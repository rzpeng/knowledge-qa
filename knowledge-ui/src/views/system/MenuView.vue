<template>
  <div class="system-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增菜单</el-button>
        </div>
      </template>

      <el-table :data="menuTree" row-key="id" default-expand-all border>
        <el-table-column prop="name" label="菜单名称" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.type === 0" type="info">目录</el-tag>
            <el-tag v-else-if="row.type === 1">菜单</el-tag>
            <el-tag v-else type="warning">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permission" label="权限标识" />
        <el-table-column prop="path" label="路由路径" />
        <el-table-column prop="icon" label="图标" width="80" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select v-model="form.parentId" :data="menuTree" :props="{ label: 'name', value: 'id' }" placeholder="顶级菜单" clearable check-strictly />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限标识" prop="permission">
          <el-input v-model="form.permission" placeholder="如 system:user:list" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path" v-if="form.type !== 2">
          <el-input v-model="form.path" />
        </el-form-item>
        <el-form-item label="组件路径" prop="component" v-if="form.type === 1">
          <el-input v-model="form.component" />
        </el-form-item>
        <el-form-item label="图标" prop="icon" v-if="form.type !== 2">
          <el-input v-model="form.icon" />
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
import { getMenuTree, getMenu, saveMenu, updateMenu, deleteMenu } from '@/api/menu'
import { ElMessage, ElMessageBox } from 'element-plus'

const menuTree = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ name: '', parentId: null, type: 1, permission: '', path: '', component: '', icon: '', sortOrder: 0, status: 1 })
const rules = { name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }] }

const loadTree = async () => {
  const res = await getMenuTree()
  menuTree.value = res.data || []
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { name: '', parentId: null, type: 1, permission: '', path: '', component: '', icon: '', sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  const res = await getMenu(row.id)
  form.value = { ...res.data }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该菜单？', '提示')
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  loadTree()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateMenu(form.value)
  } else {
    await saveMenu(form.value)
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
