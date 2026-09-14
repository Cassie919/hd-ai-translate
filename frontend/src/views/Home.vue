<template>
  <div class="chatgpt-layout">
    <div class="app-splitter-wrapper">
    <el-splitter style="height: 100vh;">
      <el-splitter-panel :size="260" :min="200">
        <!-- 侧边栏 -->
        <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="sidebar-title">Novel Translate</h1>
      </div>
      
      <div class="sidebar-actions">
        <el-upload
          class="upload-btn"
          :auto-upload="false"
          :on-change="handleFileChange"
          :show-file-list="false"
          accept=".txt"
        >
          <el-button class="new-chat-btn">
            <span class="btn-icon">+</span>
            上传文件
          </el-button>
        </el-upload>
        <div v-if="uploadedFileName" class="uploaded-file-name">{{ uploadedFileName }}</div>
      </div>

      <div class="sidebar-section">
        <div class="section-title">当前章节</div>
        <el-select
          v-model="selectedChapter"
          placeholder="请先上传文件"
          class="chapter-dropdown"
          :disabled="chapters.length === 0"
        >
          <el-option
            v-for="chapter in chapters"
            :key="chapter"
            :label="chapter"
            :value="chapter"
          />
        </el-select>
      </div>

      <div class="sidebar-section">
        <div class="section-title">AI 工具</div>
        <router-link to="/" class="chat-nav-link">
          <span class="btn-icon">🏠</span>
          返回首页
        </router-link>
        <router-link to="/chat" class="chat-nav-link">
          <span class="btn-icon">💬</span>
          AI 对话
        </router-link>
      </div>

      <div class="sidebar-footer">
        <el-button class="clear-btn" @click="handleClearSession">
          清空会话
        </el-button>
      </div>
    </aside>
      </el-splitter-panel>
      <el-splitter-panel>
    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部导航 -->
      <header class="top-nav">
        <div class="nav-left">
          <el-button
            class="translate-btn-header"
            type="primary"
            :disabled="!selectedChapter || translating"
            :loading="translating"
            @click="handleTranslate"
          >
            {{ translating ? '翻译中...' : '开始翻译' }}
          </el-button>
          <el-button
            class="chunk-btn-header"
            type="primary"
            :disabled="list.length === 0 || chunking"
            :loading="chunking"
            @click="handlePhraseChunk"
          >
            {{ chunking ? '切分中...' : '意群切分' }}
          </el-button>
          <el-button
            class="chunk-btn-header"
            type="primary"
            :disabled="!hasChunks"
            @click="handleClearChunks"
          >
            清除切分
          </el-button>
        </div>
        <div class="nav-center">
          <el-button
            class="toggle-en-btn"
            :class="{ 'is-active': showEnglishOnly }"
            :disabled="list.length === 0"
            @click="toggleEnglishOnly"
          >
            {{ showEnglishOnly ? '中英对照' : '仅英文' }}
          </el-button>
        </div>
        <div class="nav-actions">
          <el-dropdown trigger="click" @command="handleMenuCommand">
            <el-avatar class="user-avatar" :size="32">
              {{ userId ? userId.charAt(0).toUpperCase() : 'U' }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="memo">墨墨配置</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 聊天内容区 -->
      <div class="chat-container">
        <div v-if="list.length === 0" class="empty-state">
          <div class="empty-icon">📄</div>
          <h2>开始翻译</h2>
          <p>上传 TXT 文件，选择章节，即可开始翻译</p>
        </div>

        <div v-else class="result-wrapper">
          <!-- 当前页内容 -->
          <div class="result-container">
            <div class="result-page">
              <div
                v-for="item in pages[currentPage]"
                :key="item.index"
                class="result-item"
              >
                <div class="en-text">
                  <template v-if="item.chunks && item.chunks.length > 1">
                    <span v-for="(chunk, ci) in item.chunks" :key="ci">
                      <span v-if="ci > 0" class="chunk-separator"> | </span>{{ chunk }}
                    </span>
                  </template>
                  <template v-else>{{ item.en }}</template>
                </div>
                <div v-show="!showEnglishOnly" class="cn-text">{{ item.cn }}</div>
              </div>
            </div>

            <!-- 底部翻页控制栏 -->
            <div class="page-control-bar">
              <button
                class="page-btn"
                :disabled="currentPage === 0"
                @click="prevPage"
              >
                ‹ 上一页
              </button>
              <div class="page-info-center">
                <span v-if="list.length > 0" class="page-segment-info">当前章节 {{ list.length }} 段</span>
                <span v-if="pages.length > 1" class="page-indicator">
                  {{ currentPage + 1 }} / {{ pages.length }}
                </span>
              </div>
              <button
                class="page-btn"
                :disabled="currentPage === pages.length - 1"
                @click="nextPage"
              >
                下一页 ›
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
      </el-splitter-panel>
      <el-splitter-panel :size="showAiPanel ? 380 : 0" :min="showAiPanel ? 280 : 0" :resizable="showAiPanel">
        <!-- 右侧 AI 面板 -->
        <aside class="ai-panel">
      <div class="ai-menu">
        <div
          class="ai-menu-item"
          :class="{ active: aiActiveTab === 'vocab' }"
          @click="aiActiveTab = 'vocab'"
        >
          <span class="menu-icon">📖</span>
          <span>生词分析</span>
        </div>
        <div
          class="ai-menu-item"
          :class="{ active: aiActiveTab === 'qa' }"
          @click="aiActiveTab = 'qa'"
        >
          <span class="menu-icon">💬</span>
          <span>AI问答</span>
        </div>
      </div>

      <!-- 生词分析内容 -->
      <div v-show="aiActiveTab === 'vocab'" class="panel-content">
        <el-button
          type="primary"
          :disabled="list.length === 0 || predicting"
          :loading="predicting"
          class="predict-btn"
          @click="handleVocabPrediction"
        >
          {{ predicting ? '分析中...' : '开始分析' }}
        </el-button>
        <div v-if="vocabWords.length === 0" class="vocab-empty">
          点击上方按钮对当前章节进行生词分析
        </div>
        <template v-else>
          <div class="vocab-list">
            <div class="vocab-check-all">
              <el-checkbox
                v-model="vocabCheckAll"
                :indeterminate="vocabIndeterminate"
                @change="handleVocabCheckAll"
              >
                全选 ({{ vocabChecked.length }}/{{ vocabWords.length }})
              </el-checkbox>
            </div>
            <div class="vocab-grid">
              <div
                v-for="(word, idx) in vocabWords"
                :key="idx"
                class="vocab-item"
                :class="{ 'vocab-item-active': selectedWordIndex === idx }"
                @click="selectVocabWord(idx)"
              >
                <el-checkbox
                  :model-value="vocabChecked.includes(idx)"
                  @change="handleVocabCheck(idx)"
                >
                  {{ word.lemma || word.word }}
                </el-checkbox>
              </div>
            </div>
          </div>

          <!-- 单词详情区 -->
          <div v-if="selectedWordIndex !== null" class="vocab-detail">
            <div class="vocab-detail-header">
              <span class="vocab-detail-word">{{ selectedWord.word }}</span>
              <span v-if="selectedWord.lemma && selectedWord.lemma !== selectedWord.word" class="vocab-detail-lemma">({{ selectedWord.lemma }})</span>
              <span v-if="selectedWord.phonetic" class="vocab-detail-phonetic">{{ selectedWord.phonetic }}</span>
            </div>
            <div class="vocab-detail-body">
              <div class="vocab-detail-sentence">{{ selectedWord.sentence }}</div>
              <div class="vocab-detail-translation">{{ selectedWord.translation }}</div>
            </div>
          </div>
          <div v-else class="vocab-detail vocab-detail-placeholder">
            <span>点击上方单词查看例句和翻译</span>
          </div>

          <!-- 底部固定栏 -->
          <div class="panel-bottom-bar">
            <span class="selected-count">已选 {{ vocabChecked.length }} 个</span>
            <el-dropdown
              trigger="click"
              :disabled="vocabChecked.length === 0 || pushing"
              @command="handlePushToMomo"
            >
              <el-button
                type="primary"
                class="push-btn"
                :disabled="vocabChecked.length === 0 || pushing"
                :loading="pushing"
              >
                {{ pushing ? '推送中...' : '推送到墨墨' }}
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="words-only">仅推送单词</el-dropdown-item>
                  <el-dropdown-item command="words-sentences">推送单词和例句</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </div>

      <!-- AI问答内容 -->
      <div v-show="aiActiveTab === 'qa'" class="panel-content qa-panel">
        <div v-if="qaMessages.length === 0" class="qa-empty">
          <span class="qa-empty-icon">💬</span>
          <p>AI 智能问答</p>
          <p class="qa-empty-hint">输入你的问题，AI 将为你解答</p>
        </div>
        <div v-else class="qa-chat-body" ref="qaMessageContainer">
          <div
            v-for="(msg, idx) in qaMessages"
            :key="idx"
            class="qa-message"
            :class="msg.role === 'user' ? 'qa-message-user' : 'qa-message-assistant'"
          >
            <div class="qa-message-avatar">
              <span v-if="msg.role === 'user'" class="qa-avatar-user">U</span>
              <span v-else class="qa-avatar-ai">AI</span>
            </div>
            <div class="qa-message-content">
              <div class="qa-message-text">{{ msg.content }}</div>
            </div>
          </div>
          <div v-if="qaLoading" class="qa-message qa-message-assistant">
            <div class="qa-message-avatar">
              <span class="qa-avatar-ai">AI</span>
            </div>
            <div class="qa-message-content">
              <div class="qa-typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
        <div class="qa-chat-input-area">
          <el-input
            v-model="qaInput"
            type="textarea"
            :rows="2"
            placeholder="输入你的问题..."
            class="qa-input"
            :disabled="qaLoading"
            @keydown.enter.exact.prevent="handleQaSend"
          />
          <el-button
            type="primary"
            class="qa-send-btn"
            :disabled="!qaInput.trim() || qaLoading"
            :loading="qaLoading"
            @click="handleQaSend"
          >
            <span v-if="!qaLoading">发送</span>
          </el-button>
        </div>
      </div>
    </aside>
      </el-splitter-panel>
    </el-splitter>
    <div
      class="drawer-toggle"
      :style="{ right: showAiPanel ? '380px' : '4px' }"
      @click="showAiPanel = !showAiPanel"
      :title="showAiPanel ? '收起面板' : '展开面板'"
    >
      <span>{{ showAiPanel ? '▶' : '◀' }}</span>
    </div>
    </div>

    <!-- 墨墨配置面板 -->
    <transition name="settings-fade">
      <div v-if="showSettings" class="settings-overlay" @click.self="closeSettings">
        <div class="settings-panel">
          <main class="settings-content">
            <button class="settings-close-btn" @click="closeSettings">✕</button>

            <!-- 墨墨配置 -->
            <div class="settings-section">
              <h2 class="settings-section-title">墨墨配置</h2>
              <div class="settings-form">
                <div class="form-group">
                  <label class="form-label">用户 ID</label>
                  <div class="form-display">{{ userId || '加载中...' }}</div>
                  <p class="form-hint">系统自动分配，用于标识您的唯一身份</p>
                </div>
                <div class="form-group">
                  <label class="form-label">墨墨 API Token</label>
                  <el-input
                    v-model="memoToken"
                    :type="showToken ? 'text' : 'password'"
                    placeholder="请输入墨墨 API Token"
                    class="form-input"
                  >
                    <template #suffix>
                      <el-button
                        class="token-toggle-btn"
                        :icon="showToken ? View : Hide"
                        link
                        @click="showToken = !showToken"
                      />
                    </template>
                  </el-input>
                  <p class="form-hint">用于推送单词到墨墨背单词云词本，首次使用必须先设置 Token</p>
                </div>
                <div class="form-actions">
                  <el-button
                    type="primary"
                    :loading="savingToken"
                    :disabled="!memoToken"
                    @click="saveMemoToken"
                  >
                    保存 Token
                  </el-button>
                </div>
              </div>
            </div>
          </main>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Hide } from '@element-plus/icons-vue'
import axios from 'axios'
import sessionManager from '@/utils/session'

const chapters = ref([])
const selectedChapter = ref('')
const list = ref([])
const translating = ref(false)
const chunking = ref(false)
const sessionId = ref('')
const sessionCheckTimer = ref(null)
const currentPage = ref(0)
const uploadedFileName = ref('')
const showEnglishOnly = ref(false)
const predicting = ref(false)
const vocabWords = ref([])
const aiActiveTab = ref('vocab')
const vocabChecked = ref([])
const vocabCheckAll = ref(false)
const selectedWordIndex = ref(null)
const showAiPanel = ref(true)
// 墨墨配置相关
const showSettings = ref(false)
const userId = ref('')
const memoToken = ref('')
const savingToken = ref(false)
const showToken = ref(false)

// AI 问答相关
const qaMessages = ref([])
const qaInput = ref('')
const qaLoading = ref(false)
const qaChatId = ref('')
const qaMessageContainer = ref(null)

function generateChatId() {
  return 'chat_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 8)
}

function scrollQaToBottom() {
  setTimeout(() => {
    if (qaMessageContainer.value) {
      qaMessageContainer.value.scrollTop = qaMessageContainer.value.scrollHeight
    }
  }, 100)
}

async function handleQaSend() {
  const message = qaInput.value.trim()
  if (!message || qaLoading.value) return

  if (!qaChatId.value) {
    qaChatId.value = generateChatId()
  }

  qaMessages.value.push({ role: 'user', content: message })
  qaInput.value = ''
  scrollQaToBottom()
  qaLoading.value = true

  try {
    const res = await axios.post('/api/do-chat/sync', null, {
      params: { message, chatId: qaChatId.value, sessionId: sessionId.value, chapterTitle: selectedChapter.value }
    })
    qaMessages.value.push({ role: 'assistant', content: res.data || '' })
    scrollQaToBottom()
  } catch (error) {
    ElMessage.error('AI 回复失败: ' + (error.response?.data?.message || error.message))
  } finally {
    qaLoading.value = false
  }
}

const selectedWord = computed(() => {
  if (selectedWordIndex.value !== null && vocabWords.value[selectedWordIndex.value]) {
    return vocabWords.value[selectedWordIndex.value]
  }
  return null
})

function selectVocabWord(idx) {
  selectedWordIndex.value = selectedWordIndex.value === idx ? null : idx
}

function toggleEnglishOnly() {
  showEnglishOnly.value = !showEnglishOnly.value
  try {
    localStorage.setItem('show_english_only', showEnglishOnly.value ? '1' : '0')
  } catch (e) {
    // ignore
  }
}

const CACHE_KEY = 'translate_result'
const VOCAB_CACHE_KEY = 'vocab_prediction'

const pages = computed(() => {
  // A4纸可容纳的字符数估算（考虑边距、字体大小、行距）
  // A4纸内容区域约170mm × 257mm，按12px字体、1.6行距估算
  return paginateByCharacters(list.value, 2500)
})

const hasChunks = computed(() => {
  return list.value.some(item => item.chunks && item.chunks.length > 0)
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
      currentPage.value = 0

      // 将sessionId保存到sessionManager
      sessionManager.setSessionId(sessionId.value)

      uploadedFileName.value = file.name
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
      currentPage.value = 0
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

function handlePhraseChunk() {
  if (list.value.length === 0) {
    ElMessage.warning('没有可切分的内容')
    return
  }

  chunking.value = true

  const items = list.value.map((item, index) => ({
    index: index + 1,
    en: item.en,
    cn: item.cn
  }))

  axios.post('/api/phrase-chunk', { items })
    .then(response => {
      const chunkedItems = response.data.items || []
      // 保留原始翻译的 en/cn，仅叠加 chunks（避免 AI 回传文本差异覆盖原文）
      list.value = list.value.map((item, i) => {
        const chunked = chunkedItems[i]
        return chunked?.chunks?.length > 0
          ? { ...item, chunks: chunked.chunks }
          : item
      })
      currentPage.value = 0
      saveToCache(list.value)
      ElMessage.success('意群切分完成')
    })
    .catch(error => {
      ElMessage.error('意群切分失败: ' + (error.response?.data?.message || error.message))
    })
    .finally(() => {
      chunking.value = false
    })
}

function handleClearChunks() {
  list.value = list.value.map(item => {
    const { chunks, ...rest } = item
    return rest
  })
  saveToCache(list.value)
  ElMessage.success('已清除意群切分')
}

function saveVocabToCache() {
  try {
    sessionStorage.setItem(VOCAB_CACHE_KEY, JSON.stringify({
      chapter: selectedChapter.value,
      words: vocabWords.value,
      checked: vocabChecked.value,
      timestamp: Date.now()
    }))
  } catch (e) {
    // ignore
  }
}

function loadVocabFromCache() {
  if (!selectedChapter.value) return
  try {
    const cached = sessionStorage.getItem(VOCAB_CACHE_KEY)
    if (cached) {
      const parsed = JSON.parse(cached)
      if (parsed.words && parsed.chapter === selectedChapter.value) {
        vocabWords.value = parsed.words
        vocabChecked.value = parsed.checked || []
        vocabCheckAll.value = vocabChecked.value.length === vocabWords.value.length
      }
    }
  } catch (e) {
    // ignore
  }
}

function handleVocabPrediction() {
  if (list.value.length === 0) {
    ElMessage.warning('没有可预测的内容')
    return
  }

  predicting.value = true

  // 将所有英文文本拼接成一段完整文本
  const text = list.value.map(item => item.en).join('\n')

  axios.post('/api/vocab-prediction', { text })
    .then(response => {
      vocabWords.value = response.data.words || []
      vocabChecked.value = []
      vocabCheckAll.value = false
      saveVocabToCache()
      if (vocabWords.value.length > 0) {
        ElMessage.success(`生词分析完成，共 ${vocabWords.value.length} 个生词`)
      } else {
        ElMessage.info('未发现值得学习的生词')
      }
    })
    .catch(error => {
      ElMessage.error('生词分析失败: ' + (error.response?.data?.message || error.message))
    })
    .finally(() => {
      predicting.value = false
    })
}

const vocabIndeterminate = computed(() => {
  return vocabChecked.value.length > 0 && vocabChecked.value.length < vocabWords.value.length
})

function handleVocabCheckAll(val) {
  vocabChecked.value = val ? vocabWords.value.map((_, i) => i) : []
}

function handleVocabCheck(idx) {
  const pos = vocabChecked.value.indexOf(idx)
  if (pos >= 0) {
    vocabChecked.value.splice(pos, 1)
  } else {
    vocabChecked.value.push(idx)
  }
  vocabChecked.value.sort((a, b) => a - b)
  vocabCheckAll.value = vocabChecked.value.length === vocabWords.value.length
}

const pushing = ref(false)

async function handlePushToMomo(command) {
  if (!memoToken.value) {
    ElMessage.warning('请先在「墨墨配置」中设置 Token')
    return
  }

  pushing.value = true
  try {
    if (command === 'words-only') {
      await handlePushWordsOnly()
    } else if (command === 'words-sentences') {
      await handlePushWordsAndSentences()
    }
  } finally {
    pushing.value = false
  }
}

async function handlePushWordsOnly() {
  const words = vocabChecked.value.map(i => vocabWords.value[i].word)
  try {
    const res = await axios.post('/api/maiMemo/add-words', { words })
    const data = res.data
    if (data.success) {
      const uniq = data.uniqueWordsCount || data.uniqueWords?.length || 0
      const dup = data.duplicateWordsCount || data.duplicateWords?.length || 0
      ElMessage.success(`推送完成：新增 ${uniq} 个，已存在 ${dup} 个`)
    } else {
      ElMessage.error('推送失败: ' + (data.message || '未知错误'))
    }
  } catch (error) {
    ElMessage.error('推送单词失败: ' + (error.response?.data?.message || error.message))
  }
}

async function handlePushWordsAndSentences() {
  const items = vocabChecked.value.map(i => ({
    word: vocabWords.value[i].word,
    sentence: vocabWords.value[i].sentence || '',
    translation: vocabWords.value[i].translation || ''
  }))
  try {
    const res = await axios.post('/api/maiMemo/push', { items })
    const data = res.data
    if (data.success) {
      const wordRes = data.addWordsResult || {}
      const uniq = wordRes.uniqueWordsCount || wordRes.uniqueWords?.length || 0
      const dup = wordRes.duplicateWordsCount || wordRes.duplicateWords?.length || 0
      const sentCount = data.sentenceSummary?.success || 0
      const sentFail = data.sentenceSummary?.fail || 0
      let msg = `推送完成：单词新增 ${uniq} 个，已存在 ${dup} 个`
      if (sentCount > 0 || sentFail > 0) {
        msg += `；例句成功 ${sentCount} 个`
        if (sentFail > 0) msg += `，失败 ${sentFail} 个`
      }
      ElMessage.success(msg)
    } else {
      ElMessage.error('推送失败: ' + (data.message || '未知错误'))
    }
  } catch (error) {
    ElMessage.error('推送失败: ' + (error.response?.data?.message || error.message))
  }
}

// 墨墨配置相关
const MEMO_TOKEN_KEY = 'translate_memo_token'

function initUserId() {
  axios.get('/api/user/info')
    .then(response => {
      userId.value = response.data.userId || ''
      // 同时加载已保存的 Token
      try {
        memoToken.value = localStorage.getItem(MEMO_TOKEN_KEY) || ''
      } catch (e) {
        // ignore
      }
    })
    .catch(() => {
      ElMessage.warning('获取用户信息失败')
    })
}

function handleMenuCommand() {
  showSettings.value = true
}

function closeSettings() {
  showSettings.value = false
}

function saveMemoToken() {
  if (!memoToken.value) {
    ElMessage.warning('请输入墨墨 API Token')
    return
  }

  savingToken.value = true
  axios.post('/api/maiMemo/set-token', {
    token: memoToken.value
  })
    .then(() => {
      try {
        localStorage.setItem(MEMO_TOKEN_KEY, memoToken.value)
      } catch (e) {
        // ignore
      }
      ElMessage.success('配置保存成功')
      closeSettings()
    })
    .catch(error => {
      ElMessage.error('Token 保存失败: ' + (error.response?.data?.message || error.message))
    })
    .finally(() => {
      savingToken.value = false
    })
}

function handleExportCommand(command) {
  if (command === 'word') {
    handleExportWord()
  } else if (command === 'pdf') {
    handleExportPdf()
  }
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

function handleExportPdf() {
  if (list.value.length === 0) {
    ElMessage.warning('没有可导出的内容')
    return
  }

  // 确保有会话ID
  sessionId.value = sessionManager.getSessionId()

  axios.post('/api/export/pdf', {
    title: selectedChapter.value || '翻译结果',
    content: list.value,
    sessionId: sessionId.value
  }, {
    responseType: 'blob'
  })
    .then(response => {
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${selectedChapter.value || '翻译结果'}.pdf`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
      ElMessage.success('PDF导出成功')
    })
    .catch(error => {
      ElMessage.error('PDF导出失败: ' + (error.response?.data?.message || error.message))
    })
}

function prevPage() {
  if (currentPage.value > 0) {
    currentPage.value--
  }
}

function nextPage() {
  if (currentPage.value < pages.value.length - 1) {
    currentPage.value++
  }
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
 * 清空会话 - 同时清空前端和后端的会话数据
 */
function handleClearSession() {
  ElMessageBox.confirm('确定要清空当前会话的所有数据吗？此操作不可恢复。', '警告', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'error'
  }).then(() => {
    // 清空本地状态
    uploadedFileName.value = ''
    chapters.value = []
    selectedChapter.value = ''
    list.value = []
    currentPage.value = 0
    vocabWords.value = []
    vocabChecked.value = []
    vocabCheckAll.value = false
    selectedWordIndex.value = null
    
    // 清空缓存
    try {
      localStorage.removeItem(CACHE_KEY)
      sessionStorage.removeItem(VOCAB_CACHE_KEY)
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

  // 恢复生词分析缓存
  loadVocabFromCache()

  // 尝试从本地存储恢复会话ID
  const savedSessionId = sessionManager.loadSessionId()
  if (savedSessionId) {
    sessionId.value = savedSessionId
  }

  // 恢复仅英文展示偏好
  try {
    showEnglishOnly.value = localStorage.getItem('show_english_only') === '1'
  } catch (e) {
    // ignore
  }

  // 从后端获取用户 ID
  initUserId()

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

/* ChatGPT 亮色风格布局 */
.chatgpt-layout {
  background: #ffffff;
  color: #343541;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 侧边栏 */
.sidebar {
  height: 100%;
  width: 100%;
  background: #f7f7f8;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e5e5e5;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e5e5e5;
}

.sidebar-title {
  font-size: 18px;
  font-weight: 500;
  color: #343541;
  margin: 0;
}

.sidebar-actions {
  padding: 16px;
}

.upload-btn {
  width: 100%;
}

.upload-btn :deep(.el-button) {
  width: 100%;
  justify-content: center;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
  padding: 12px 16px;
  border-radius: 8px;
  transition: all 0.2s;
}

.upload-btn :deep(.el-button:hover) {
  background: #ececf1;
  border-color: #c7c7cc;
}

.uploaded-file-name {
  font-size: 12px;
  color: #8e8e93;
  margin-top: 8px;
  padding-left: 4px;
  word-break: break-all;
}

.sidebar-section {
  padding: 16px;
}

.sidebar-section:last-of-type {
  flex: none;
}

.section-title {
  font-size: 12px;
  color: #6b6b6f;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.chapter-dropdown {
  width: 100%;
}

.chapter-dropdown :deep(.el-select) {
  width: 100%;
}

.chapter-dropdown :deep(.el-input__wrapper) {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  box-shadow: none;
}

.chapter-dropdown :deep(.el-input__wrapper:hover) {
  border-color: #c7c7cc;
}

.chapter-dropdown :deep(.el-input__inner) {
  color: #343541;
}

.chapter-dropdown :deep(.el-select__placeholder) {
  color: #6b6b6f;
}

.chapter-dropdown :deep(.el-select-dropdown) {
  background: #ffffff;
  border: 1px solid #e5e5e5;
}

.chapter-dropdown :deep(.el-select-dropdown__item) {
  color: #343541;
}

.chapter-dropdown :deep(.el-select-dropdown__item:hover) {
  background: #ececf1;
}

.chapter-dropdown :deep(.el-select-dropdown__item.is-selected) {
  background: #ffffff;
  color: #10a37f;
}

/* AI 对话导航入口 */
.chat-nav-link {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px 16px;
  font-size: 14px;
  color: #343541;
  text-decoration: none;
  background: transparent;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  transition: all 0.2s;
  cursor: pointer;
}

.chat-nav-link:hover {
  background: rgba(16, 163, 127, 0.06);
  border-color: #10a37f;
  color: #10a37f;
}

.btn-icon {
  font-size: 16px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #e5e5e5;
}

.clear-btn {
  width: 100%;
  justify-content: center;
  padding: 0 18px;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
  border-radius: 8px;
}

.clear-btn:hover {
  background: #ececf1;
  border-color: #c7c7cc;
}

.clear-btn:hover {
  background: #ececf1;
}

/* 主内容区 */
.main-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  overflow: hidden;
}

/* 顶部导航 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #e5e5e5;
  background: #ffffff;
  overflow: hidden;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-center {
  display: flex;
  align-items: center;
}

.toggle-en-btn {
  height: 36px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-en-btn:hover:not(:disabled) {
  background: #ececf1;
  border-color: #c7c7cc;
}

.toggle-en-btn.is-active {
  background: #10a37f;
  border-color: #10a37f;
  color: #ffffff;
}

.toggle-en-btn.is-active:hover:not(:disabled) {
  background: #0d8c6d;
  border-color: #0d8c6d;
}

.toggle-en-btn:disabled {
  color: #c7c7cc;
  border-color: #e5e5e5;
  background: transparent;
  cursor: not-allowed;
}



.translate-btn-header {
  height: 36px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
  white-space: nowrap;
}

.translate-btn-header:hover:not(:disabled) {
  background: #ececf1;
  border-color: #c7c7cc;
}

.translate-btn-header:disabled {
  color: #c7c7cc;
  border-color: #e5e5e5;
  background: transparent;
}

.chunk-btn-header {
  height: 36px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
  white-space: nowrap;
}

.chunk-btn-header:hover:not(:disabled) {
  background: #ececf1;
  border-color: #c7c7cc;
}

.chunk-btn-header:disabled {
  color: #c7c7cc;
  border-color: #e5e5e5;
  background: transparent;
}

.nav-actions {
  display: flex;
  gap: 12px;
}

.nav-actions .action-btn {
  padding: 0 18px;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
  border-radius: 8px;
}

.nav-actions .action-btn:hover {
  background: #ececf1;
  border-color: #c7c7cc;
}

/* 聊天容器 */
.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 32px 24px;
  background: #f7f7f8;
}

.chat-container::-webkit-scrollbar {
  width: 8px;
}

.chat-container::-webkit-scrollbar-track {
  background: transparent;
}

.chat-container::-webkit-scrollbar-thumb {
  background: #c7c7cc;
  border-radius: 4px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #6b6b6f;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.5;
}

.empty-state h2 {
  font-size: 24px;
  font-weight: 500;
  color: #343541;
  margin-bottom: 12px;
}

.empty-state p {
  font-size: 14px;
}

/* 结果包裹器 */
.result-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 结果容器 */
.result-container {
  width: 100%;
  max-width: 800px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.result-page {
  width: 210mm;
  min-height: 297mm;
  padding: 20mm;
  background: #ffffff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06), 0 4px 12px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  page-break-after: always;
}

.result-item {
  padding: 0;
  margin-bottom: 24px;
}

.en-text {
  font-size: 16px;
  color: #2c3e50;
  margin-bottom: 8px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.chunk-separator {
  color: #cc0033;
  font-weight: bold;
}

.cn-text {
  color: #666;
  font-size: 15px;
  line-height: 1.6;
}

/* 底部翻页控制栏 */
.page-control-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 210mm;
  padding: 16px 0;
  margin-top: 4px;
}

.page-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  color: #343541;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: #f5f5f5;
  border-color: #10a37f;
  color: #10a37f;
}

.page-btn:disabled {
  color: #c7c7cc;
  border-color: #e5e5e5;
  cursor: not-allowed;
  background: #fafafa;
}

.page-info-center {
  display: flex;
  align-items: center;
  gap: 20px;
  color: #6b6b6f;
  font-size: 13px;
}

.page-indicator {
  font-size: 14px;
  font-weight: 500;
  color: #343541;
}

/* 覆盖 Element Plus 样式 */
::deep(.el-button--primary) {
  --el-button-bg-color: #10a37f;
  --el-button-border-color: #10a37f;
  --el-button-hover-bg-color: #0d8c6d;
  --el-button-hover-border-color: #0d8c6d;
  --el-button-active-bg-color: #0a7058;
  --el-button-active-border-color: #0a7058;
}

::deep(.el-select-dropdown) {
  background: #ffffff !important;
  border: 1px solid #e5e5e5 !important;
}

::deep(.el-select-dropdown__item) {
  color: #343541 !important;
}

::deep(.el-select-dropdown__item:hover) {
  background: #ececf1 !important;
}

::deep(.el-select-dropdown__item.is-selected) {
  color: #10a37f !important;
}

::deep(.el-popper.is-light) {
  background: #ffffff !important;
  border: 1px solid #e5e5e5 !important;
}

::deep(.el-popper.is-light .el-popper__arrow::before) {
  background: #ffffff !important;
  border: 1px solid #e5e5e5 !important;
}

::deep(.el-empty__description) {
  color: #6b6b6f;
}

/* 抽屉包裹器 */
.app-splitter-wrapper {
  position: relative;
}

/* 抽屉拉手按钮 */
.drawer-toggle {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #d9d9db;
  border-radius: 8px 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 30;
  transition: right 0.3s ease, background 0.2s, color 0.2s, box-shadow 0.2s;
  user-select: none;
  font-size: 12px;
  color: #8e8e93;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.06);
}

.drawer-toggle:hover {
  background: #fff;
  color: #10a37f;
  border-color: #10a37f;
  box-shadow: -2px 0 14px rgba(16, 163, 127, 0.10);
}

/* 右侧 AI 面板 */
.ai-panel {
  height: 100%;
  background: #f7f7f8;
  border-left: 1px solid #e5e5e5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 右侧面板菜单 */
.ai-menu {
  display: flex;
  border-bottom: 1px solid #e5e5e5;
  background: #f7f7f8;
  flex-shrink: 0;
}

.ai-menu-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 8px;
  font-size: 13px;
  color: #6b6b6f;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  user-select: none;
}

.ai-menu-item:hover {
  color: #343541;
  background: rgba(16, 163, 127, 0.04);
}

.ai-menu-item.active {
  color: #10a37f;
  border-bottom-color: #10a37f;
}

.menu-icon {
  font-size: 15px;
}

/* 面板通用内容区 */
.panel-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  overflow: hidden;
}

.predict-btn {
  width: 100%;
  margin-bottom: 16px;
  border-radius: 8px;
  flex-shrink: 0;
}

/* 生词分析列表 */
.vocab-empty {
  text-align: center;
  padding: 40px 20px;
  color: #8e8e93;
  font-size: 14px;
  line-height: 1.6;
}

.vocab-list {
  flex: 7;
  overflow-y: auto;
  min-height: 0;
}

.vocab-list::-webkit-scrollbar {
  width: 6px;
}

.vocab-list::-webkit-scrollbar-thumb {
  background: #c7c7cc;
  border-radius: 3px;
}

.vocab-check-all {
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid #e5e5e5;
}

.vocab-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.vocab-item {
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid transparent;
  transition: all 0.15s;
}

.vocab-item:hover {
  background: rgba(16, 163, 127, 0.06);
  border-color: rgba(16, 163, 127, 0.2);
}

.vocab-item-active {
  background: rgba(16, 163, 127, 0.12);
  border-color: #10a37f;
}

.vocab-item :deep(.el-checkbox) {
  display: flex;
  align-items: center;
}

.vocab-item :deep(.el-checkbox__label) {
  font-size: 14px;
  color: #343541;
}

/* 单词详情区 */
.vocab-detail {
  flex: 3;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  overflow-y: auto;
  min-height: 0;
}

.vocab-detail::-webkit-scrollbar {
  width: 6px;
}

.vocab-detail::-webkit-scrollbar-thumb {
  background: #c7c7cc;
  border-radius: 3px;
}

.vocab-detail-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8e8e93;
  font-size: 13px;
}

.vocab-detail-header {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e5e5;
}

.vocab-detail-word {
  font-size: 20px;
  font-weight: 600;
  color: #10a37f;
}

.vocab-detail-lemma {
  font-size: 13px;
  color: #8e8e93;
}

.vocab-detail-phonetic {
  font-size: 13px;
  color: #8e8e93;
  font-style: italic;
  margin-left: 4px;
}

.vocab-detail-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.vocab-detail-sentence {
  font-size: 15px;
  color: #2c3e50;
  line-height: 1.7;
}

.vocab-detail-translation {
  font-size: 14px;
  color: #6b6b6f;
  line-height: 1.7;
}

/* 面板底部固定栏 */
.panel-bottom-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0 0;
  margin-top: auto;
  border-top: 1px solid #e5e5e5;
}

.selected-count {
  font-size: 13px;
  color: #6b6b6f;
  font-weight: 500;
}

.push-btn {
  border-radius: 8px;
  font-size: 14px;
  padding: 8px 20px;
}

/* AI问答面板 */
.qa-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.qa-empty {
  text-align: center;
  color: #8e8e93;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.qa-empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
  opacity: 0.4;
}

.qa-empty p {
  font-size: 14px;
  margin: 0 0 4px;
}

.qa-empty-hint {
  font-size: 12px !important;
  opacity: 0.7;
}

/* 聊天消息区 */
.qa-chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.qa-chat-body::-webkit-scrollbar {
  width: 6px;
}

.qa-chat-body::-webkit-scrollbar-thumb {
  background: #c7c7cc;
  border-radius: 3px;
}

/* 单条消息 */
.qa-message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.qa-message-user {
  flex-direction: row-reverse;
}

.qa-message-avatar {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.qa-avatar-user {
  background: #10a37f;
  color: #ffffff;
  width: 100%;
  height: 100%;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qa-avatar-ai {
  background: #ececf1;
  color: #343541;
  width: 100%;
  height: 100%;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qa-message-content {
  max-width: 85%;
  display: flex;
  flex-direction: column;
}

.qa-message-user .qa-message-content {
  align-items: flex-end;
}

.qa-message-assistant .qa-message-content {
  align-items: flex-start;
}

.qa-message-text {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.qa-message-user .qa-message-text {
  background: #10a37f;
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.qa-message-assistant .qa-message-text {
  background: #ffffff;
  color: #343541;
  border: 1px solid #e5e5e5;
  border-bottom-left-radius: 4px;
}

/* 打字动画指示器 */
.qa-typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  border-bottom-left-radius: 4px;
}

.qa-typing-indicator span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #c7c7cc;
  animation: qa-typing-bounce 1.4s ease-in-out infinite both;
}

.qa-typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.qa-typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

.qa-typing-indicator span:nth-child(3) {
  animation-delay: 0s;
}

@keyframes qa-typing-bounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 底部输入区 */
.qa-chat-input-area {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 12px 0 0;
  border-top: 1px solid #e5e5e5;
}

.qa-input {
  flex: 1;
}

.qa-input :deep(.el-textarea__inner) {
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  resize: none;
  box-shadow: none;
  transition: border-color 0.2s;
}

.qa-input :deep(.el-textarea__inner:hover) {
  border-color: #c7c7cc;
}

.qa-input :deep(.el-textarea__inner:focus) {
  border-color: #10a37f;
  box-shadow: 0 0 0 2px rgba(16, 163, 127, 0.15);
}

.qa-send-btn {
  flex-shrink: 0;
  height: 40px;
  min-width: 60px;
  border-radius: 10px;
  font-size: 14px;
}

/* 用户头像 */
.user-avatar {
  cursor: pointer;
  background: #10a37f;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  transition: box-shadow 0.2s;
  flex-shrink: 0;
}

.user-avatar:hover {
  box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.3);
}

/* 设置面板遮罩 */
.settings-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.32);
  display: flex;
  align-items: center;
  justify-content: center;
}

.settings-panel {
  width: 520px;
  max-width: 90vw;
  height: auto;
  max-height: 80vh;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* 设置面板内容 */
.settings-content {
  padding: 24px 32px;
  overflow-y: auto;
  position: relative;
}

.settings-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #57606a;
  font-size: 18px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.settings-close-btn:hover {
  background: #f3f4f6;
  color: #24292f;
}

.settings-section {
  max-width: 420px;
}

.settings-section-title {
  font-size: 20px;
  font-weight: 600;
  color: #24292f;
  margin: 0 0 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #d0d7de;
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: #24292f;
}

.form-display {
  padding: 8px 12px;
  font-size: 14px;
  color: #343541;
  background: #f6f8fa;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-family: monospace;
  word-break: break-all;
}

.form-input {
  width: 100%;
}

.form-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #d0d7de inset;
  border-radius: 6px;
}

.form-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #0969da inset;
}

.form-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #0969da inset;
}

.form-hint {
  font-size: 12px;
  color: #57606a;
  line-height: 1.4;
  margin: 0;
}

.form-actions {
  padding-top: 4px;
}

.form-actions .el-button--primary {
  border-radius: 6px;
  font-size: 14px;
  padding: 5px 16px;
  background: #10a37f;
  border-color: #10a37f;
}

.form-actions .el-button--primary:hover {
  background: #0d8c6d;
  border-color: #0d8c6d;
}

.token-toggle-btn {
  font-size: 16px;
  color: #57606a;
}

.token-toggle-btn:hover {
  color: #24292f;
}

/* 设置面板过渡动画 */
.settings-fade-enter-active {
  transition: all 0.2s ease-out;
}

.settings-fade-leave-active {
  transition: all 0.15s ease-in;
}

.settings-fade-enter-from,
.settings-fade-leave-to {
  opacity: 0;
}

.settings-fade-enter-from .settings-panel,
.settings-fade-leave-to .settings-panel {
  transform: scale(0.96);
  opacity: 0;
}

.settings-panel {
  transition: transform 0.2s ease-out, opacity 0.2s ease-out;
}
</style>
