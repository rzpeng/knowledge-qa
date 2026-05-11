<template>
  <div class="system-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>地区管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增地区</el-button>
        </div>
      </template>

      <el-table :data="regionList" border>
        <el-table-column prop="name" label="地区名称" />
        <el-table-column prop="code" label="地区编码" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑地区' : '新增地区'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="地区名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="地区编码" prop="code">
          <el-input v-model="form.code" />
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
import { getRegionList, getRegion, saveRegion, updateRegion, deleteRegion } from '@/api/region'
import { ElMessage, ElMessageBox } from 'element-plus'

const regionList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ name: '', code: '' })
const rules = { name: [{ required: true, message: '请输入地区名称', trigger: 'blur' }] }

const loadList = async () => {
  const res = await getRegionList()
  regionList.value = res.data || []
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { name: '', code: '' }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  const res = await getRegion(row.id)
  form.value = { ...res.data }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该地区？', '提示')
  await deleteRegion(row.id)
  ElMessage.success('删除成功')
  loadList()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateRegion(form.value)
  } else {
    await saveRegion(form.value)
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
</style>
