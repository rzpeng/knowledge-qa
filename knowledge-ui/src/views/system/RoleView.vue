<template>
  <div class="system-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增角色</el-button>
        </div>
      </template>

      <el-table :data="roleList" border>
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="code" label="角色编码" />
        <el-table-column label="部门数据权限" width="150">
          <template #default="{ row }">
            {{ deptScopeMap[row.deptDataScope] || '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="地区数据权限" width="150">
          <template #default="{ row }">
            {{ regionScopeMap[row.regionDataScope] || '未知' }}
          </template>
        </el-table-column>
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
            <el-button type="success" link size="small" @click="handlePermission(row)">权限配置</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="部门数据权限" prop="deptDataScope">
          <el-select v-model="form.deptDataScope">
            <el-option :value="1" label="全部数据" />
            <el-option :value="2" label="自定义部门" />
            <el-option :value="3" label="本部门及下属" />
            <el-option :value="4" label="本部门" />
            <el-option :value="5" label="本人" />
          </el-select>
        </el-form-item>
        <el-form-item label="地区数据权限" prop="regionDataScope">
          <el-select v-model="form.regionDataScope">
            <el-option :value="1" label="全部地区" />
            <el-option :value="2" label="自定义地区" />
            <el-option :value="3" label="本用户所属地区" />
          </el-select>
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

    <el-dialog v-model="permDialogVisible" title="权限配置" width="700px">
      <el-tabs>
        <el-tab-pane label="菜单权限">
          <el-tree ref="menuTreeRef" :data="menuTree" show-checkbox node-key="id"
            :props="{ label: 'name', children: 'children' }" default-expand-all />
        </el-tab-pane>
        <el-tab-pane label="部门数据权限">
          <el-tree ref="deptTreeRef" :data="deptTree" show-checkbox node-key="id"
            :props="{ label: 'name', children: 'children' }" default-expand-all />
        </el-tab-pane>
        <el-tab-pane label="地区数据权限">
          <el-select v-model="selectedRegions" multiple placeholder="选择地区" style="width: 100%">
            <el-option v-for="r in regionList" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePerm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRoleList, getRole, saveRole, updateRole, deleteRole,
  assignRoleMenus, getRoleMenuIds, assignRoleDepts, getRoleDeptIds,
  assignRoleRegions, getRoleRegionIds } from '@/api/role'
import { getMenuTree } from '@/api/menu'
import { getDeptTree } from '@/api/dept'
import { getRegionList } from '@/api/region'
import { ElMessage, ElMessageBox } from 'element-plus'

const roleList = ref([])
const menuTree = ref([])
const deptTree = ref([])
const regionList = ref([])
const dialogVisible = ref(false)
const permDialogVisible = ref(false)
const isEdit = ref(false)
const currentRoleId = ref(null)
const menuTreeRef = ref(null)
const deptTreeRef = ref(null)
const selectedRegions = ref([])
const formRef = ref(null)
const form = ref({ name: '', code: '', deptDataScope: 3, regionDataScope: 1, status: 1 })
const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const deptScopeMap = { 1: '全部数据', 2: '自定义部门', 3: '本部门及下属', 4: '本部门', 5: '本人' }
const regionScopeMap = { 1: '全部地区', 2: '自定义地区', 3: '本用户所属地区' }

const loadData = async () => {
  const [roleRes, menuRes, deptRes, regionRes] = await Promise.all([
    getRoleList(), getMenuTree(), getDeptTree(), getRegionList()
  ])
  roleList.value = roleRes.data || []
  menuTree.value = menuRes.data || []
  deptTree.value = deptRes.data || []
  regionList.value = regionRes.data || []
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { name: '', code: '', deptDataScope: 3, regionDataScope: 1, status: 1 }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  const res = await getRole(row.id)
  form.value = { ...res.data }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该角色？', '提示')
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateRole(form.value)
  } else {
    await saveRole(form.value)
  }
  ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
  dialogVisible.value = false
  loadData()
}

const handlePermission = async (row) => {
  currentRoleId.value = row.id
  permDialogVisible.value = true
  selectedRegions.value = []
  await loadPermissionData(row.id)
}

const loadPermissionData = async (roleId) => {
  const [menuIds, deptIds, regionIds] = await Promise.all([
    getRoleMenuIds(roleId), getRoleDeptIds(roleId), getRoleRegionIds(roleId)
  ])
  await new Promise(r => setTimeout(r, 100))
  if (menuTreeRef.value) {
    menuTreeRef.value.setCheckedKeys(menuIds.data || [])
  }
  if (deptTreeRef.value) {
    deptTreeRef.value.setCheckedKeys(deptIds.data || [])
  }
  selectedRegions.value = regionIds.data || []
}

const handleSavePerm = async () => {
  if (!currentRoleId.value) return
  const menuKeys = menuTreeRef.value ? menuTreeRef.value.getCheckedKeys() : []
  const deptKeys = deptTreeRef.value ? deptTreeRef.value.getCheckedKeys() : []
  await Promise.all([
    assignRoleMenus(currentRoleId.value, menuKeys),
    assignRoleDepts(currentRoleId.value, deptKeys),
    assignRoleRegions(currentRoleId.value, selectedRegions.value)
  ])
  ElMessage.success('权限配置保存成功')
  permDialogVisible.value = false
}

onMounted(loadData)
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
