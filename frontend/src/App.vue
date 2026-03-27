<template>
  <div class="app-container">
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <h1>AI小说翻译工具</h1>
        </div>
      </el-header>
      
      <el-main>
        <div class="control-panel">
          <el-upload
            class="upload-area"
            :auto-upload="false"
            :on-change="handleFileChange"
            :show-file-list="false"
            accept=".txt"
          >
            <el-button type="primary" :icon="Upload">上传TXT文件</el-button>
          </el-upload>
          
          <el-select
            v-model="selectedChapter"
            placeholder="选择章节"
            class="chapter-select"
            :disabled="chapters.length === 0"
          >
            <el-option
              v-for="chapter in chapters"
              :key="chapter"
              :label="chapter"
              :value="chapter"
            />
          </el-select>
          
          <el-button
            type="success"
            :icon="MagicStick"
            :disabled="!selectedChapter || translating"
            :loading="translating"
            @click="handleTranslate"
          >
            {{ translating ? '翻译中...' : '翻译' }}
          </el-button>
          
          <el-button
            type="info"
            :icon="Document"
            :disabled="list.length === 0"
            @click="handleExportWord"
          >
            导出Word
          </el-button>

          <el-divider direction="vertical" />

          <el-button
            type="primary"
            plain
            :icon="RefreshRight"
            @click="handleRestart"
          >
            重新开始
          </el-button>

          <el-button
            type="danger"
            plain
            :icon="Delete"
            @click="handleClearSession"
          >
            清空会话
          </el-button>
        </div>
        
        <div class="content-area">
          <div v-if="list.length === 0" class="empty-tip">
            <el-empty description="请上传文件并选择章节进行翻译" />
          </div>
          
          <div v-else class="container">
            <div
              v-for="(page, pageIndex) in pages"
              :key="pageIndex"
              class="page"
            >
              <div
                v-for="item in page"
                :key="item.index"
                class="paragraph"
              >
                <div class="en">{{ item.en }}</div>
                <div class="cn">{{ item.cn }}</div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="list.length > 0" class="page-info">
          共 {{ pages.length }} 页，当前章节 {{ list.length }} 段
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { Upload, MagicStick, Document, RefreshRight, Delete } from '@element-plus/icons-vue'
import sessionManager from '@/utils/session'

const chapters = ref([])
const selectedChapter = ref('')
const list = ref([])
const translating = ref(false)
const sessionId = ref('')
const sessionCheckTimer = ref(null)

const CACHE_KEY = 'translate_result'

const pages = computed(() => {
  // A4纸可容纳的字符数估算（考虑边距、字体大小、行距）
  // A4纸内容区域约170mm × 257mm，按12px字体、1.6行距估算
  return paginateByCharacters(list.value, 2500)
})

function handleFileChange(file) {
  console.log('handleFileChange 触发:', file.name, file)

  const formData = new FormData()
  formData.append('file', file.raw)

  axios.post('/api/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
    .then(response => {
      // 从上传接口响应中获取后端生成的sessionId
      sessionId.value = response.data.sessionId
      chapters.value = response.data.chapters
      selectedChapter.value = ''
      list.value = []

      // 将sessionId保存到sessionManager
      sessionManager.setSessionId(sessionId.value)

      ElMessage.success('文件解析成功')

      // 保存会话元数据
      sessionManager.saveSessionMeta({
        chapters: chapters.value,
        lastUploadTime: Date.now()
      })
    })
    .catch(error => {
      ElMessage.error('文件上传失败: ' + (error.response?.data?.message || error.message))
    })
}

function handleTranslate() {
  if (!selectedChapter.value) {
    ElMessage.warning('请先选择章节')
    return
  }

  if (!sessionId.value) {
    ElMessage.error('会话ID不存在，请先上传文件')
    return
  }

  translating.value = true

  // 使用上传接口返回的sessionId调用翻译接口
  axios.post('/api/translate', {
    chapterTitle: selectedChapter.value,
    sessionId: sessionId.value
  })
    .then(response => {
      list.value = response.data
      saveToCache(response.data)
      ElMessage.success('翻译完成')

      // 保存会话元数据
      sessionManager.saveSessionMeta({
        selectedChapter: selectedChapter.value,
        lastTranslateTime: Date.now()
      })
    })
    .catch(error => {
      ElMessage.error('翻译失败: ' + (error.response?.data?.message || error.message))
    })
    .finally(() => {
      translating.value = false
    })
}

function handleExportWord() {
  if (list.value.length === 0) {
    ElMessage.warning('没有可导出的内容')
    return
  }

  // 确保有会话ID
  sessionId.value = sessionManager.getSessionId()

  axios.post('/api/export/word', {
    title: selectedChapter.value || '翻译结果',
    content: list.value,
    sessionId: sessionId.value
  }, {
    responseType: 'blob'
  })
    .then(response => {
      const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${selectedChapter.value || '翻译结果'}.docx`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
      ElMessage.success('Word导出成功')
    })
    .catch(error => {
      ElMessage.error('Word导出失败: ' + (error.response?.data?.message || error.message))
    })
}

function paginateByCharacters(items, charLimit) {
  const result = []
  let currentPageItems = []
  let currentCharCount = 0

  for (const item of items) {
    const itemLength = (item.en?.length || 0) + (item.cn?.length || 0)
    
    if (currentPageItems.length > 0 && currentCharCount + itemLength > charLimit) {
      result.push(currentPageItems)
      currentPageItems = []
      currentCharCount = 0
    }

    currentPageItems.push(item)
    currentCharCount += itemLength
  }

  if (currentPageItems.length > 0) {
    result.push(currentPageItems)
  }

  return result
}

function saveToCache(data) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({
      sessionId: sessionId.value,
      chapter: selectedChapter.value,
      data: data,
      timestamp: Date.now()
    }))
  } catch (error) {
    console.error('保存缓存失败:', error)
  }
}

function loadFromCache() {
  try {
    const cached = localStorage.getItem(CACHE_KEY)
    if (cached) {
      const parsed = JSON.parse(cached)
      if (parsed.data) {
        sessionId.value = parsed.sessionId || sessionManager.getSessionId()
        selectedChapter.value = parsed.chapter || ''
        list.value = parsed.data
        chapters.value = [parsed.chapter || '默认章节']
      }
    }
  } catch (error) {
    console.error('加载缓存失败:', error)
  }
}

/**
 * 重新开始 - 清空当前状态，等待上传新文件获取新会话
 */
function handleRestart() {
  ElMessageBox.confirm('确定要重新开始吗？这将清空当前所有数据。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 清空本地状态
    chapters.value = []
    selectedChapter.value = ''
    list.value = []
    sessionId.value = ''

    // 清空缓存
    try {
      localStorage.removeItem(CACHE_KEY)
    } catch (error) {
      console.error('清空缓存失败:', error)
    }

    // 清空会话管理器
    sessionManager.clearSession()

    ElMessage.success('已重置，请上传新文件开始')
  }).catch(() => {
    // 取消操作
  })
}

/**
 * 清空会话 - 同时清空前端和后端的会话数据
 */
function handleClearSession() {
  ElMessageBox.confirm('确定要清空当前会话的所有数据吗？此操作不可恢复。', '警告', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'error'
  }).then(() => {
    // 清空本地状态
    chapters.value = []
    selectedChapter.value = ''
    list.value = []
    
    // 清空缓存
    try {
      localStorage.removeItem(CACHE_KEY)
    } catch (error) {
      console.error('清空缓存失败:', error)
    }
    
    // 调用后端接口清空会话数据
    if (sessionId.value) {
      axios.post('/api/session/clear', {
        sessionId: sessionId.value
      })
        .then(() => {
          // 清空会话管理器
          sessionManager.clearSession()
          sessionId.value = ''
          ElMessage.success('会话已清空')
        })
        .catch(error => {
          console.error('清空会话失败:', error)
          ElMessage.warning('本地已清空，但后端清空失败，请稍后再试')
        })
    } else {
      ElMessage.success('会话已清空')
    }
  }).catch(() => {
    // 取消操作
  })
}

/**
 * 检查会话状态
 * 定期检查会话是否仍然有效，如果无效则提示用户
 */
function checkSessionStatus() {
  if (!sessionId.value) return

  axios.post('/api/session/check', {
    sessionId: sessionId.value
  })
    .then(response => {
      if (!response.data.valid) {
        ElMessage.warning('会话已失效，建议重新开始')
        // 可选：自动刷新会话
        // sessionId.value = sessionManager.getSessionId(true)
      }
    })
    .catch(error => {
      console.error('检查会话状态失败:', error)
    })
}

onMounted(() => {
  // 加载缓存数据
  loadFromCache()

  // 尝试从本地存储恢复会话ID
  const savedSessionId = sessionManager.loadSessionId()
  if (savedSessionId) {
    sessionId.value = savedSessionId
  }

  // 开始定期检查会话状态
  sessionCheckTimer.value = setInterval(checkSessionStatus, 30*1000*60) // 每30min检查一次
})

onBeforeUnmount(() => {
  // 清除定时器
  if (sessionCheckTimer.value) {
    clearInterval(sessionCheckTimer.value)
  }
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.app-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 0 24px;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.header h1 {
  font-size: 28px;
  font-weight: 500;
  margin: 0;
}

.control-panel {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 20px 24px;
  background: white;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-wrap: wrap;
}

.upload-area {
  display: inline-block;
}

.chapter-select {
  min-width: 200px;
  flex: 1;
  max-width: 300px;
}

.content-area {
  background: white;
  border-radius: 8px;
  padding: 24px;
  min-height: 500px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow-y: auto;
  gap: 24px;
  padding: 20px;
  max-height: calc(100vh - 300px);
  scroll-behavior: smooth;
}

.container::-webkit-scrollbar {
  width: 8px;
}

.container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.container::-webkit-scrollbar-thumb:hover {
  background: #555;
}

.page {
  width: 210mm;
  min-height: 297mm;
  padding: 20mm;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
  page-break-after: always;
}

.paragraph {
  margin-bottom: 24px;
  line-height: 1.8;
}

.en {
  font-weight: bold;
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 8px;
  line-height: 1.6;
}

.cn {
  color: #666;
  font-size: 15px;
  line-height: 1.6;
}

.page-info {
  margin-top: 16px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.el-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.el-divider--vertical {
  margin: 0 8px;
}
</style>
