<template>
  <div class="documents-container">
    <div class="page-header">
      <h2>文档管理</h2>
      <el-upload
        ref="uploadRef"
        :show-file-list="false"
        :before-upload="beforeUpload"
        :http-request="handleUpload"
        accept=".txt,.md,.pdf,.docx"
      >
        <el-button type="primary">
          <el-icon><Upload /></el-icon>
          上传文档
        </el-button>
      </el-upload>
    </div>

    <el-alert
      title="支持的文档格式：TXT、Markdown、PDF、DOCX"
      type="info"
      :closable="false"
      style="margin-bottom: 16px"
    />

    <el-table :data="documents" v-loading="loading" stripe>
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="fileName" label="文件名" min-width="180" />
      <el-table-column prop="fileType" label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ row.fileType.toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="100">
        <template #default="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="getStatusType(row.status)"
            size="small"
          >
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="切片数" width="80" />
      <el-table-column prop="createTime" label="上传时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            type="danger"
            size="small"
            text
            @click="handleDelete(row)"
            :disabled="row.status === 0"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="documents.length === 0 && !loading" class="empty-state">
      <el-empty description="暂无文档，请上传" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { documentApi } from '@/api/document'
import { ElMessage, ElMessageBox } from 'element-plus'

const documents = ref([])
const loading = ref(false)
const uploadRef = ref(null)

onMounted(() => {
  loadDocuments()
})

const loadDocuments = async () => {
  loading.value = true
  try {
    documents.value = await documentApi.list()
  } catch (e) {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

const beforeUpload = (file) => {
  const allowedTypes = ['txt', 'md', 'pdf', 'docx']
  const ext = file.name.split('.').pop().toLowerCase()
  if (!allowedTypes.includes(ext)) {
    ElMessage.error('不支持的文件格式')
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  return true
}

const handleUpload = async ({ file }) => {
  try {
    await documentApi.upload(file)
    ElMessage.success('上传成功，正在处理...')
    setTimeout(loadDocuments, 1000)
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该文档吗？相关向量数据也将被删除。', '提示', {
      type: 'warning'
    })
    await documentApi.delete(row.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(1) + ' ' + units[i]
}

const getStatusType = (status) => {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

const getStatusText = (status) => {
  return { 0: '处理中', 1: '成功', 2: '失败' }[status] || '未知'
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped>
.documents-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.empty-state {
  padding: 40px 0;
}
</style>
