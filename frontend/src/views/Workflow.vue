<template>
  <div class="workflow-layout">
    <!-- 顶部导航 -->
    <header class="top-nav">
      <div class="nav-left">
        <router-link to="/" class="back-link">← 返回首页</router-link>
        <h1 class="app-title">翻译工作流</h1>
      </div>
      <div class="nav-actions">
        <el-button class="nav-btn" @click="handleConfig">配置</el-button>
        <el-button class="nav-btn" @click="handleUser">用户</el-button>
      </div>
    </header>

    <div class="workflow-body">
      <el-splitter style="height: 100%;">
        <!-- 左侧栏 -->
        <el-splitter-panel :size="260" :min="200">
          <aside class="sidebar">
            <div class="sidebar-actions">
              <el-button class="new-task-btn" @click="openUploadDialog">
                <span class="btn-icon">+</span>
                选择文档
              </el-button>
            </div>

            <div v-if="documentName" class="document-section">
              <div class="section-title">当前文档</div>
              <div class="document-name" :title="documentName">{{ documentName }}</div>
            </div>

            <div class="chapter-section">
              <div class="section-title">章节列表</div>
              <div v-if="chapters.length === 0" class="chapter-empty">暂无章节</div>
              <div v-else class="chapter-list">
                <div
                  v-for="chapter in chapters"
                  :key="chapter"
                  class="chapter-item"
                  :class="{ 'is-active': chapter === selectedChapter }"
                  :title="chapter"
                  @click="selectChapter(chapter)"
                >
                  {{ chapter }}
                </div>
              </div>
            </div>
          </aside>
        </el-splitter-panel>

        <!-- 右侧主内容区 -->
        <el-splitter-panel>
          <main class="main-content">
            <!-- 步骤条 -->
            <div class="step-bar">
              <template v-for="(step, index) in steps" :key="step">
                <div
                  class="step-item"
                  :class="{
                    'is-active': currentStep === index,
                    'is-done': index < currentStep
                  }"
                  @click="selectStep(index)"
                >
                  {{ step }}
                </div>
                <span
                  v-if="index < steps.length - 1"
                  class="step-arrow"
                  :class="{ 'is-done': index < currentStep }"
                >→</span>
              </template>
            </div>

            <!-- 步骤内容区 -->
            <div class="step-content">
              <div v-show="currentStep === 0" class="step-panel">
                <div v-if="chapterLoading" class="chapter-loading">
                  <el-skeleton :rows="8" animated />
                </div>
                <div v-else-if="chapterContent" class="chapter-content">
                  <h2 class="chapter-content-title">{{ selectedChapter }}</h2>
                  <div class="chapter-content-body">{{ chapterContent }}</div>
                </div>
                <div v-else class="panel-placeholder">
                  <div class="placeholder-icon">📄</div>
                  <h2>文档</h2>
                  <p>请从左侧章节列表选择章节，在此展示章节内容</p>
                </div>
              </div>

              <div v-show="currentStep === 1" class="step-panel">
                <div v-if="translating" class="chapter-loading">
                  <el-skeleton :rows="8" animated />
                </div>
                <div v-else-if="translateItems.length > 0" class="result-card">
                  <div v-for="item in translateItems" :key="item.index" class="result-item">
                    <div class="result-en">
                      <template v-for="(token, ti) in tokenize(item.en)" :key="ti">
                        <span
                          v-if="token.type === 'word'"
                          class="word"
                          :class="{ 'is-marked': markedWords.has(token.value.toLowerCase()) }"
                          @click="toggleWord(token.value)"
                        >{{ token.value }}</span>
                        <template v-else>{{ token.value }}</template>
                      </template>
                    </div>
                    <div class="result-cn">{{ item.cn }}</div>
                  </div>
                </div>
                <div v-else class="panel-placeholder">
                  <div class="placeholder-icon">🌐</div>
                  <h2>翻译</h2>
                  <p>请先选择章节，点击"翻译"后在此展示翻译结果</p>
                </div>
              </div>

              <div v-show="currentStep === 2" class="step-panel">
                <div v-if="chunking" class="chapter-loading">
                  <el-skeleton :rows="8" animated />
                </div>
                <div v-else-if="phraseChunkItems.length > 0" class="result-card">
                  <div v-for="item in phraseChunkItems" :key="item.index" class="result-item">
                    <div class="result-en">
                      <template v-if="item.chunks && item.chunks.length > 0">
                        <template v-for="(chunk, ci) in item.chunks" :key="ci">
                          <span v-if="ci > 0" class="chunk-separator"> | </span>
                          <template v-for="(token, ti) in tokenize(chunk)" :key="ti">
                            <span
                              v-if="token.type === 'word'"
                              class="word"
                              :class="{ 'is-marked': markedWords.has(token.value.toLowerCase()) }"
                              @click="toggleWord(token.value)"
                            >{{ token.value }}</span>
                            <template v-else>{{ token.value }}</template>
                          </template>
                        </template>
                      </template>
                      <template v-else>
                        <template v-for="(token, ti) in tokenize(item.en)" :key="ti">
                          <span
                            v-if="token.type === 'word'"
                            class="word"
                            :class="{ 'is-marked': markedWords.has(token.value.toLowerCase()) }"
                            @click="toggleWord(token.value)"
                          >{{ token.value }}</span>
                          <template v-else>{{ token.value }}</template>
                        </template>
                      </template>
                    </div>
                    <div class="result-cn">{{ item.cn }}</div>
                  </div>
                </div>
                <div v-else class="panel-placeholder">
                  <div class="placeholder-icon">✂️</div>
                  <h2>意群</h2>
                  <p>翻译完成后在此展示意群切分结果</p>
                </div>
              </div>

              <div v-show="currentStep === 3" class="step-panel">
                <div v-if="markedWords.size > 0" class="result-card">
                  <div class="word-header">
                    <span class="word-count">
                      已标记 {{ displayMarkedWords.length }} 个生词
                      <span v-if="lemmatizing" class="lemmatizing-hint">还原词性中...</span>
                    </span>
                    <el-button class="clear-word-btn" size="small" @click="clearMarkedWords">清空</el-button>
                  </div>
                  <div class="word-list">
                    <span
                      v-for="word in displayMarkedWords"
                      :key="word"
                      class="marked-word"
                      :title="lemmaWordTip(word)"
                      @click="toggleWordByLemma(word)"
                    >
                      {{ word }}
                      <span class="remove-icon">×</span>
                    </span>
                  </div>

                  <!-- 例句与例句译文 -->
                  <div class="word-examples">
                    <div class="examples-section-title">例句与译文</div>
                    <div v-if="examplesLoading" class="examples-loading">
                      <el-skeleton :rows="3" animated />
                    </div>
                    <template v-else>
                      <div v-if="wordExamples.length > 0" class="example-list">
                        <div
                          v-for="example in wordExamples"
                          :key="example.word"
                          class="example-item"
                        >
                          <div class="example-word">{{ example.word }}</div>
                          <div class="example-sentence">
                            <template v-for="(token, ti) in tokenize(example.sentence)" :key="ti">
                              <span
                                v-if="token.type === 'word' && token.value.toLowerCase() === example.word.toLowerCase()"
                                class="hit"
                              >{{ token.value }}</span>
                              <template v-else>{{ token.value }}</template>
                            </template>
                          </div>
                          <div class="example-translation">{{ example.translation }}</div>
                        </div>
                      </div>
                      <div v-else class="examples-empty">暂未匹配到例句</div>
                      <div v-if="wordExamplesNotFound.length > 0" class="examples-notfound">
                        未在译文中找到例句：{{ wordExamplesNotFound.join('、') }}
                      </div>
                    </template>
                  </div>
                </div>
                <div v-else class="panel-placeholder">
                  <div class="placeholder-icon">📖</div>
                  <h2>单词</h2>
                  <p>在翻译 / 意群中点击英文单词即可标记生词</p>
                </div>
              </div>

              <div v-show="currentStep === 4" class="step-panel">
                <div v-if="momoTokenSet === false" class="momo-token-warning">
                  尚未配置墨墨 Token，请点击右上角「配置」填写后再推送。
                </div>
                <div v-if="sortedMarkedWords.length > 0" class="result-card">
                  <div class="word-header">
                    <span class="word-count">
                      已标记 {{ displayMarkedWords.length }} 个生词
                      <span v-if="lemmatizing" class="lemmatizing-hint">还原词性中...</span>
                    </span>
                    <span class="momo-hint">推送到墨墨</span>
                  </div>

                  <div class="word-list">
                    <span
                      v-for="word in displayMarkedWords"
                      :key="word"
                      class="marked-word is-static"
                      :title="lemmaWordTip(word)"
                    >{{ word }}</span>
                  </div>

                  <div class="momo-actions">
                    <el-button
                      class="momo-btn"
                      :loading="pushingWords"
                      :disabled="pushingWordsAndExamples"
                      @click="pushWordsOnly"
                    >仅推送单词</el-button>
                    <el-button
                      class="momo-btn momo-btn-primary"
                      type="primary"
                      :loading="pushingWordsAndExamples"
                      :disabled="pushingWords"
                      @click="pushWordsAndExamples"
                    >推送单词和例句</el-button>
                  </div>

                  <div
                    v-if="momoResult"
                    class="momo-result"
                    :class="momoResult.success ? 'is-success' : 'is-error'"
                  >
                    <div class="momo-result-title">{{ momoResult.message }}</div>
                    <div v-if="momoResult.uniqueWords.length > 0" class="momo-result-line">
                      新增单词（{{ momoResult.uniqueWords.length }}）：{{ momoResult.uniqueWords.join('、') }}
                    </div>
                    <div v-if="momoResult.duplicateWords.length > 0" class="momo-result-line">
                      已存在（{{ momoResult.duplicateWords.length }}）：{{ momoResult.duplicateWords.join('、') }}
                    </div>
                    <div v-if="momoResult.sentenceSummary" class="momo-result-line">
                      例句：成功 {{ momoResult.sentenceSummary.success }} / 共 {{ momoResult.sentenceSummary.total }}
                      <template v-if="momoResult.sentenceSummary.fail > 0">
                        ，失败 {{ momoResult.sentenceSummary.fail }}
                      </template>
                    </div>
                  </div>
                </div>
                <div v-else class="panel-placeholder">
                  <div class="placeholder-icon">📮</div>
                  <h2>墨墨</h2>
                  <p>请先在"单词"步骤标记生词，再推送到墨墨</p>
                </div>
              </div>
            </div>
          </main>
        </el-splitter-panel>
      </el-splitter>
    </div>

    <!-- 上传中文文档弹窗 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传中文文档"
      width="440px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <div class="upload-dialog-body">
        <p class="upload-hint">支持 txt</p>

        <el-upload
          class="upload-area"
          :auto-upload="false"
          :show-file-list="false"
          accept=".txt"
          :on-change="handleFileChange"
        >
          <el-button class="upload-select-btn">选择文件</el-button>
        </el-upload>

        <div v-if="selectedFile" class="selected-file">
          <span class="selected-file-icon">📄</span>
          <span class="selected-file-name" :title="selectedFile.name">{{ selectedFile.name }}</span>
        </div>
        <div v-else class="selected-file-empty">尚未选择文件</div>
      </div>

      <template #footer>
        <el-button class="dialog-btn" @click="handleUploadCancel">取消</el-button>
        <el-button class="dialog-btn dialog-btn-primary" type="primary" :loading="uploading" @click="handleUploadConfirm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 配置墨墨 Token 弹窗 -->
    <el-dialog
      v-model="showConfigDialog"
      title="配置"
      width="440px"
      :close-on-click-modal="false"
    >
      <div class="config-dialog-body">
        <div class="config-field-label">墨墨 API Token</div>
        <el-input
          v-model="momoToken"
          type="password"
          show-password
          clearable
          placeholder="请输入墨墨 API Token"
          @keyup.enter="handleConfigConfirm"
        />
        <p class="config-hint">
          获取方式：墨墨背单词 App → 我的 → 更多设置 → 实验功能 → 开放 API。
          保存时会自动校验 Token 有效性。
        </p>
      </div>

      <template #footer>
        <el-button class="dialog-btn" @click="handleConfigCancel">取消</el-button>
        <el-button class="dialog-btn dialog-btn-primary" type="primary" :loading="savingToken" @click="handleConfigConfirm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import sessionManager from '@/utils/session'
import { getChapterContent, translateChapter, phraseChunk, getWordExamples, lemmatizeWords, addMaiMemoWords, pushMaiMemo, setMaiMemoToken, getMaiMemoTokenStatus } from '@/utils/api'

// 步骤定义
const steps = ['文档', '翻译', '意群', '单词', '墨墨']
const currentStep = ref(0)

// 章节列表（由后端上传接口解析返回）
const chapters = ref([])
const selectedChapter = ref('')
// 当前选中章节的正文内容与加载状态
const chapterContent = ref('')
const chapterLoading = ref(false)

// 翻译与意群划分（两个独立的异步流程，各自维护加载态与结果）
const translating = ref(false)
const translateItems = ref([])
const chunking = ref(false)
const phraseChunkItems = ref([])

// 当前文档与会话
const documentName = ref('')
const sessionId = ref('')

// 上传文档弹窗
const showUploadDialog = ref(false)
const selectedFile = ref(null)
const uploading = ref(false)

// ---------- 英文分词 + 生词标记 ----------
// 已标记的生词（小写原词，用于和原文匹配例句）
const markedWords = ref(new Set())
// 原词 -> 原型（用于"单词"步骤展示与墨墨推送），小写 key
const lemmaMap = ref(new Map())
const lemmatizing = ref(false)

// 已标记生词的例句与译文（由后端从会话逐句译文中匹配）
const wordExamples = ref([])
// 译文中未匹配到例句的单词
const wordExamplesNotFound = ref([])
const examplesLoading = ref(false)

// ---------- 墨墨推送 ----------
// 两个推送动作各自的加载态（互斥）
const pushingWords = ref(false)
const pushingWordsAndExamples = ref(false)
// 最近一次推送结果（用于面板内展示）
const momoResult = ref(null)

// ---------- 墨墨 Token 配置 ----------
// 配置弹窗与输入内容
const showConfigDialog = ref(false)
const momoToken = ref('')
const savingToken = ref(false)
// 是否已配置墨墨 Token：true 已配置 / false 未配置 / null 未知
const momoTokenSet = ref(null)

// 优先使用浏览器内置的 Intl.Segmenter（按 Unicode 词边界切分，更准），不支持时退回正则
const wordSegmenter =
  typeof Intl !== 'undefined' && Intl.Segmenter
    ? new Intl.Segmenter('en', { granularity: 'word' })
    : null

/**
 * 把英文文本切成 { type: 'word' | 'text', value } 序列
 * word 类型渲染成可点击的 span，text 类型（空格、标点等）原样输出
 */
function tokenize(text) {
  if (!text) {
    return []
  }
  const tokens = []
  if (wordSegmenter) {
    for (const { segment, isWordLike } of wordSegmenter.segment(text)) {
      tokens.push({ type: isWordLike ? 'word' : 'text', value: segment })
    }
    return tokens
  }
  // 兜底：Unicode 感知正则，支持带重音的字母
  const re = /[\p{L}\p{M}]+(?:[''][\p{L}\p{M}]+)*/gu
  let last = 0
  let match
  while ((match = re.exec(text)) !== null) {
    if (match.index > last) {
      tokens.push({ type: 'text', value: text.slice(last, match.index) })
    }
    tokens.push({ type: 'word', value: match[0] })
    last = match.index + match[0].length
  }
  if (last < text.length) {
    tokens.push({ type: 'text', value: text.slice(last) })
  }
  return tokens
}

/**
 * 点击单词：切换标记状态（以小写作为去重键）
 */
function toggleWord(word) {
  const key = word.toLowerCase()
  const set = markedWords.value
  if (set.has(key)) {
    set.delete(key)
  } else {
    set.add(key)
  }
  // 标记变化后立即持久化，刷新后仍可恢复
  saveWorkflowState()
}

// 已标记生词（按字母排序，供"单词"步骤展示）
const sortedMarkedWords = computed(() => [...markedWords.value].sort())

// 用于"单词"步骤展示与墨墨推送：优先用原型，缺失时回退原词
const displayMarkedWords = computed(() =>
  sortedMarkedWords.value.map((w) => lemmaMap.value.get(w) || w)
)

// 反向索引：原型 -> 原词列表（同一原型可能对应多个原词）
const originByLemma = computed(() => {
  const m = new Map()
  for (const origin of sortedMarkedWords.value) {
    const lemma = lemmaMap.value.get(origin) || origin
    if (!m.has(lemma)) {
      m.set(lemma, [])
    }
    m.get(lemma).push(origin)
  }
  return m
})

/**
 * 清空全部已标记生词
 */
function clearMarkedWords() {
  markedWords.value.clear()
  lemmaMap.value.clear()
  wordExamples.value = []
  wordExamplesNotFound.value = []
  momoResult.value = null
  saveWorkflowState()
}

/**
 * 获取已标记生词的例句与译文（调用后端接口）
 */
async function fetchWordExamples() {
  const words = sortedMarkedWords.value
  if (words.length === 0 || !sessionId.value) {
    wordExamples.value = []
    wordExamplesNotFound.value = []
    return
  }

  examplesLoading.value = true
  try {
    const data = await getWordExamples(sessionId.value, words)
    wordExamples.value = Array.isArray(data?.matched) ? data.matched : []
    wordExamplesNotFound.value = Array.isArray(data?.notFound) ? data.notFound : []
  } catch (error) {
    wordExamples.value = []
    wordExamplesNotFound.value = []
    ElMessage.error('获取单词例句失败: ' + (error.response?.data?.message || error.message))
  } finally {
    examplesLoading.value = false
  }
}

/**
 * 批量还原词性：调用后端接口，把原词 -> 原型写入 lemmaMap
 * 已存在的词不会重复请求；失败时静默回退原词
 */
async function fetchLemmas(words) {
  if (!Array.isArray(words) || words.length === 0) {
    return
  }
  const pending = words.filter((w) => !lemmaMap.value.has(w))
  if (pending.length === 0) {
    return
  }
  lemmatizing.value = true
  try {
    const lemmas = await lemmatizeWords(pending)
    for (let i = 0; i < pending.length; i++) {
      const origin = pending[i]
      const lemma = (Array.isArray(lemmas) && lemmas[i]) || origin
      lemmaMap.value.set(origin, lemma)
    }
  } catch (error) {
    // 还原失败：保留原词，不阻塞用户操作
    ElMessage.warning('词性还原失败，将显示原词: ' + (error.response?.data?.message || error.message))
  } finally {
    lemmatizing.value = false
  }
}

/**
 * 确保当前所有已标记词都尝试过原型还原
 */
async function ensureLemmas() {
  await fetchLemmas(sortedMarkedWords.value)
}

/**
 * 单个原型 chip 的悬浮提示：展示原词信息
 */
function lemmaWordTip(lemma) {
  const origins = originByLemma.value.get(lemma) || []
  const others = origins.filter((o) => o !== lemma)
  if (others.length === 0) {
    return lemma
  }
  return `${lemma}（原词：${others.join('、')}）`
}

/**
 * 在单词步骤按原型 chip 点击：根据该原型对应的所有原词统一切换标记状态
 */
function toggleWordByLemma(lemma) {
  const origins = originByLemma.value.get(lemma) || []
  if (origins.length === 0) {
    return
  }
  const set = markedWords.value
  const someUnmarked = origins.some((o) => !set.has(o))
  for (const origin of origins) {
    if (someUnmarked) {
      set.add(origin)
    } else {
      set.delete(origin)
    }
  }
  // 标记变化后立即持久化
  saveWorkflowState()
}

/**
 * 仅推送单词：把"单词"步骤标记的生词（按原型）追加到墨墨云词本（后端自动去重）
 */
async function pushWordsOnly() {
  if (sortedMarkedWords.value.length === 0) {
    ElMessage.warning('请先在"单词"步骤标记生词')
    return
  }
  // 推送前确保原型已还原（用户可能跳过单词步骤直接到墨墨）
  await ensureLemmas()
  const lemmas = displayMarkedWords.value
  if (lemmas.length === 0) {
    ElMessage.warning('没有可推送的单词')
    return
  }

  pushingWords.value = true
  momoResult.value = null
  try {
    const data = await addMaiMemoWords(lemmas)
    const uniqueWords = Array.isArray(data?.uniqueWords) ? data.uniqueWords : []
    const duplicateWords = Array.isArray(data?.duplicateWords) ? data.duplicateWords : []
    momoResult.value = {
      success: true,
      message: `已推送 ${lemmas.length} 个单词到墨墨`,
      uniqueWords,
      duplicateWords,
      sentenceSummary: null
    }
    ElMessage.success(`已推送 ${lemmas.length} 个单词到墨墨`)
  } catch (error) {
    ElMessage.error('推送单词失败: ' + (error.response?.data?.message || error.message))
  } finally {
    pushingWords.value = false
  }
}

/**
 * 推送单词和例句：单词按原型，例句保持原句原译；未匹配到例句的单词忽略
 */
async function pushWordsAndExamples() {
  if (sortedMarkedWords.value.length === 0) {
    ElMessage.warning('请先在"单词"步骤标记生词')
    return
  }
  // 推送前确保原型已还原
  await ensureLemmas()

  const lemmas = displayMarkedWords.value
  if (lemmas.length === 0) {
    ElMessage.warning('没有可推送的单词')
    return
  }

  // 例句按原词（lowercase）索引：wordExamples[i].word 是小写原词
  const exampleMap = new Map(
    wordExamples.value.map((item) => [String(item.word).toLowerCase(), item])
  )

  // 对每个原型：找到其对应的若干原词，挨个查找第一个能匹配到例句的
  const items = []
  for (const lemma of lemmas) {
    const origins = originByLemma.value.get(lemma) || []
    const matched = origins
      .map((o) => exampleMap.get(o))
      .find((ex) => ex && ex.sentence)
    if (matched) {
      items.push({
        word: lemma,
        sentence: matched.sentence,
        translation: matched.translation || ''
      })
    }
  }

  if (items.length === 0) {
    ElMessage.warning('没有匹配到例句的单词')
    return
  }

  pushingWordsAndExamples.value = true
  momoResult.value = null
  try {
    const data = await pushMaiMemo(items)
    const addWordsResult = data?.addWordsResult || {}
    const uniqueWords = Array.isArray(addWordsResult.uniqueWords) ? addWordsResult.uniqueWords : []
    const duplicateWords = Array.isArray(addWordsResult.duplicateWords)
      ? addWordsResult.duplicateWords
      : []
    momoResult.value = {
      success: true,
      message: `已推送 ${items.length} 个单词及例句到墨墨`,
      uniqueWords,
      duplicateWords,
      sentenceSummary: data?.sentenceSummary || null
    }
    ElMessage.success('推送完成')
  } catch (error) {
    ElMessage.error('推送失败: ' + (error.response?.data?.message || error.message))
  } finally {
    pushingWordsAndExamples.value = false
  }
}

async function selectChapter(chapter) {
  // 重复点击已选中的章节不重新请求
  if (chapter === selectedChapter.value) {
    return
  }
  selectedChapter.value = chapter
  chapterContent.value = ''
  // 切换章节后清空上一章节的翻译与意群结果
  translateItems.value = []
  phraseChunkItems.value = []

  if (!sessionId.value) {
    ElMessage.warning('会话已失效，请重新上传文档')
    return
  }

  chapterLoading.value = true
  try {
    const data = await getChapterContent(sessionId.value, chapter)
    // 防止快速切换章节时旧请求覆盖新结果
    if (selectedChapter.value !== chapter) {
      return
    }
    chapterContent.value = data?.content || ''
  } catch (error) {
    if (selectedChapter.value === chapter) {
      chapterContent.value = ''
    }
    ElMessage.error('获取章节内容失败: ' + (error.response?.data?.message || error.message))
  } finally {
    if (selectedChapter.value === chapter) {
      chapterLoading.value = false
    }
  }
}

function selectStep(index) {
  currentStep.value = index
  // 点击"翻译"步骤时，先翻译章节，再对翻译结果做意群划分
  if (index === 1) {
    runTranslate()
  }
}

async function runTranslate() {
  if (translating.value) {
    return
  }
  if (!selectedChapter.value) {
    ElMessage.warning('请先选择章节')
    return
  }
  if (!sessionId.value) {
    ElMessage.warning('会话已失效，请重新上传文档')
    return
  }
  // 已翻译过则直接展示，避免重复请求
  if (translateItems.value.length > 0) {
    return
  }

  translating.value = true
  try {
    // 1. 翻译章节：返回后立即渲染翻译面板
    const items = await translateChapter(sessionId.value, selectedChapter.value)
    translateItems.value = items || []
  } catch (error) {
    ElMessage.error('翻译失败: ' + (error.response?.data?.message || error.message))
    return
  } finally {
    translating.value = false
  }

  // 2. 翻译完成后，独立异步发起意群划分
  runPhraseChunk()
}

async function runPhraseChunk() {
  if (chunking.value) {
    return
  }
  // 意群划分依赖翻译结果，且结果已存在时不重复请求
  if (translateItems.value.length === 0 || phraseChunkItems.value.length > 0) {
    return
  }

  chunking.value = true
  try {
    // items 字段与翻译结果结构一致，可直接复用
    const result = await phraseChunk(translateItems.value)
    phraseChunkItems.value = result?.items || []
  } catch (error) {
    ElMessage.error('意群划分失败: ' + (error.response?.data?.message || error.message))
  } finally {
    chunking.value = false
  }
}

function openUploadDialog() {
  selectedFile.value = null
  showUploadDialog.value = true
}

function handleFileChange(file) {
  // el-upload 的 file 对象，真实文件在 raw 中
  selectedFile.value = file.raw || file
}

async function handleUploadConfirm() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  const fileName = selectedFile.value.name
  const formData = new FormData()
  formData.append('file', selectedFile.value)

  uploading.value = true
  try {
    const response = await axios.post('/api/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    // 后端解析后返回 sessionId 与章节列表
    sessionId.value = response.data.sessionId
    chapters.value = response.data.chapters || []
    // 上传成功后清空已选章节及其内容
    selectedChapter.value = ''
    chapterContent.value = ''
    chapterLoading.value = false
    translateItems.value = []
    phraseChunkItems.value = []
    wordExamples.value = []
    wordExamplesNotFound.value = []
    lemmaMap.value.clear()
    documentName.value = response.data.fileName || fileName

    sessionManager.setSessionId(sessionId.value)
    sessionManager.saveSessionMeta({
      chapters: chapters.value,
      lastUploadTime: Date.now()
    })

    showUploadDialog.value = false
    ElMessage.success('文件解析成功')
  } catch (error) {
    ElMessage.error('文件上传失败: ' + (error.response?.data?.message || error.message))
  } finally {
    uploading.value = false
  }
}

function handleUploadCancel() {
  showUploadDialog.value = false
}

function handleDialogClosed() {
  selectedFile.value = null
}

function handleConfig() {
  // 打开配置弹窗，每次进入清空输入框，避免残留上一次内容
  momoToken.value = ''
  showConfigDialog.value = true
}

function handleConfigCancel() {
  showConfigDialog.value = false
}

async function handleConfigConfirm() {
  const token = momoToken.value.trim()
  if (!token) {
    ElMessage.warning('请输入墨墨 API Token')
    return
  }

  savingToken.value = true
  try {
    // 后端会先探活校验 Token，无效则返回错误、不会保存
    const data = await setMaiMemoToken(token)
    ElMessage.success(data?.message || 'Token 设置成功')
    momoTokenSet.value = true
    showConfigDialog.value = false
  } catch (error) {
    ElMessage.error('Token 设置失败: ' + (error.response?.data?.message || error.message))
  } finally {
    savingToken.value = false
  }
}

/**
 * 查询墨墨 Token 是否已配置（进入"墨墨"步骤时调用）
 */
async function fetchTokenStatus() {
  try {
    const data = await getMaiMemoTokenStatus()
    momoTokenSet.value = !!data?.hasToken
  } catch (error) {
    // 查询失败时保持未知状态，避免误提示
    momoTokenSet.value = null
  }
}

async function handleUser() {
  try {
    const response = await axios.get('/api/user/info')
    const userId = response.data?.userId
    if (userId) {
      ElMessage.info(`当前用户 ID：${userId}`)
    } else {
      ElMessage.warning('未获取到用户信息，请先登录')
    }
  } catch (error) {
    ElMessage.error('获取用户信息失败: ' + (error.response?.data?.message || error.message))
  }
}

// 工作流状态缓存 key
const CACHE_KEY = 'translate_workflow_state'

/**
 * 保存工作流状态到本地缓存（刷新后可恢复）
 */
function saveWorkflowState() {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({
      sessionId: sessionId.value,
      documentName: documentName.value,
      chapters: chapters.value,
      selectedChapter: selectedChapter.value,
      chapterContent: chapterContent.value,
      translateItems: translateItems.value,
      phraseChunkItems: phraseChunkItems.value,
      currentStep: currentStep.value,
      timestamp: Date.now()
    }))
  } catch (error) {
    console.error('保存工作流缓存失败:', error)
  }
}

/**
 * 从本地缓存恢复工作流状态
 */
function loadWorkflowState() {
  try {
    const cached = localStorage.getItem(CACHE_KEY)
    if (!cached) {
      return
    }
    const state = JSON.parse(cached)
    // 会话不匹配则丢弃缓存，避免串会话
    if (!state.sessionId || state.sessionId !== sessionId.value) {
      return
    }
    documentName.value = state.documentName || ''
    chapters.value = state.chapters || []
    selectedChapter.value = state.selectedChapter || ''
    chapterContent.value = state.chapterContent || ''
    translateItems.value = state.translateItems || []
    phraseChunkItems.value = state.phraseChunkItems || []
    currentStep.value = state.currentStep || 0
    // 标记的生词不持久化，刷新后清空
  } catch (error) {
    console.error('恢复工作流缓存失败:', error)
  }
}

/**
 * 校验后端会话是否仍然有效
 */
async function checkSessionValidity() {
  if (!sessionId.value) {
    return
  }
  try {
    const response = await axios.get(`/api/session/${sessionId.value}`)
    if (!response.data?.exists) {
      ElMessage.warning('会话已失效，建议重新上传文档')
    }
  } catch (error) {
    console.error('检查会话状态失败:', error)
  }
}

// 状态变化时自动持久化
watch(
  [documentName, chapters, selectedChapter, chapterContent, translateItems, phraseChunkItems, currentStep],
  saveWorkflowState,
  { deep: true }
)

// 进入"单词"/"墨墨"步骤或标记变化时，重新拉取例句与译文
watch(
  () => [currentStep.value, sortedMarkedWords.value.join('|')],
  () => {
    if (currentStep.value === 3 || currentStep.value === 4) {
      fetchWordExamples()
    }
    // 进入"单词"步骤时，确保已标记词已尝试还原为原型
    if (currentStep.value === 3) {
      ensureLemmas()
    }
    // 进入"墨墨"步骤时查询 Token 配置状态，未配置则给出提示；并补一次原型还原
    if (currentStep.value === 4) {
      fetchTokenStatus()
      ensureLemmas()
    }
  }
)

onMounted(() => {
  // 先恢复会话 ID，再据此恢复工作流状态
  const savedSessionId = sessionManager.loadSessionId()
  if (savedSessionId) {
    sessionId.value = savedSessionId
  }
  loadWorkflowState()
  checkSessionValidity()
  // 刷新后若停留在"单词"/"墨墨"步骤，恢复例句展示
  if (currentStep.value === 3 || currentStep.value === 4) {
    fetchWordExamples()
  }
  // 刷新后若停留在"单词"/"墨墨"步骤，补一次原型还原（缓存可能为空）
  if (currentStep.value === 3 || currentStep.value === 4) {
    ensureLemmas()
  }
  // 刷新后若停留在"墨墨"步骤，同步 Token 配置状态
  if (currentStep.value === 4) {
    fetchTokenStatus()
  }
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.workflow-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  color: #343541;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 顶部导航 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #e5e5e5;
  background: #ffffff;
  flex-shrink: 0;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-link {
  font-size: 14px;
  color: #6b6b6f;
  text-decoration: none;
  transition: color 0.2s;
  white-space: nowrap;
}

.back-link:hover {
  color: #10a37f;
}

.app-title {
  font-size: 18px;
  font-weight: 500;
  color: #343541;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-btn {
  height: 36px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
}

.nav-btn:hover {
  background: #ececf1;
  border-color: #c7c7cc;
}

/* 主体 */
.workflow-body {
  flex: 1;
  min-height: 0;
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

.sidebar-actions {
  padding: 16px;
  border-bottom: 1px solid #e5e5e5;
}

.new-task-btn {
  width: 100%;
  height: 40px;
  justify-content: center;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
}

.new-task-btn:hover {
  background: #ececf1;
  border-color: #c7c7cc;
}

.btn-icon {
  font-size: 16px;
  margin-right: 6px;
}

.chapter-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.section-title {
  font-size: 12px;
  color: #6b6b6f;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

/* 当前文档 */
.document-section {
  padding: 16px;
  border-bottom: 1px solid #e5e5e5;
  flex-shrink: 0;
}

.document-name {
  font-size: 14px;
  font-weight: 500;
  color: #343541;
  padding: 8px 12px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chapter-empty {
  font-size: 13px;
  color: #8e8e93;
  padding: 8px 4px;
}

.chapter-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chapter-list::-webkit-scrollbar {
  width: 6px;
}

.chapter-list::-webkit-scrollbar-thumb {
  background: #c7c7cc;
  border-radius: 3px;
}

.chapter-item {
  padding: 10px 12px;
  font-size: 14px;
  color: #343541;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
  line-height: 1.5;
  word-break: break-all;
  white-space: normal;
}

.chapter-item:hover {
  background: rgba(16, 163, 127, 0.06);
  border-color: rgba(16, 163, 127, 0.2);
}

.chapter-item.is-active {
  background: rgba(16, 163, 127, 0.12);
  border-color: #10a37f;
  color: #10a37f;
  font-weight: 500;
}

/* 主内容区 */
.main-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  overflow: hidden;
}

/* 步骤条 */
.step-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e5e5;
  flex-shrink: 0;
}

.step-item {
  padding: 8px 18px;
  font-size: 14px;
  font-weight: 500;
  color: #343541;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.step-item:hover {
  background: #ececf1;
  border-color: #c7c7cc;
}

.step-item.is-done {
  color: #10a37f;
  border-color: rgba(16, 163, 127, 0.4);
  background: rgba(16, 163, 127, 0.06);
}

.step-item.is-active {
  color: #ffffff;
  background: #10a37f;
  border-color: #10a37f;
}

.step-arrow {
  font-size: 14px;
  color: #c7c7cc;
}

.step-arrow.is-done {
  color: #10a37f;
}

/* 步骤内容区 */
.step-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 32px 24px;
  background: #f7f7f8;
}

.step-panel {
  height: 100%;
}

.panel-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #6b6b6f;
}

/* 章节内容 / 翻译与意群结果 */
.result-card {
  max-width: 880px;
  margin: 0 auto;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  padding: 28px 32px;
}

.result-item {
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.result-item:last-child {
  padding-bottom: 0;
  margin-bottom: 0;
  border-bottom: none;
}

.result-en {
  font-size: 16px;
  line-height: 1.8;
  color: #2c3e50;
  margin-bottom: 8px;
  word-break: break-word;
}

.result-cn {
  font-size: 15px;
  line-height: 1.8;
  color: #6b6b6f;
  word-break: break-word;
}

.chunk-separator {
  color: #d03050;
  font-weight: 700;
}

/* 英文单词：可点击标记 */
.word {
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s, color 0.15s;
}

.word:hover {
  background: rgba(16, 163, 127, 0.12);
}

.word.is-marked {
  color: #0d8c6d;
  font-weight: 600;
  background: rgba(16, 163, 127, 0.16);
  box-shadow: inset 0 -2px 0 #10a37f;
}

/* 单词步骤：已标记生词列表 */
.word-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.word-count {
  font-size: 14px;
  color: #6b6b6f;
}

.lemmatizing-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #8e8e93;
}

.clear-word-btn {
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
  border-radius: 8px;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
}

.clear-word-btn:hover {
  background: #ececf1;
  border-color: #c7c7cc;
}

.word-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.marked-word {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  font-size: 14px;
  color: #0d8c6d;
  background: rgba(16, 163, 127, 0.1);
  border: 1px solid rgba(16, 163, 127, 0.35);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.marked-word:hover {
  background: rgba(16, 163, 127, 0.18);
  border-color: #10a37f;
}

.remove-icon {
  font-weight: 700;
  color: #8e8e93;
}

.marked-word:hover .remove-icon {
  color: #d03050;
}

/* 单词步骤：例句与译文 */
.word-examples {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.examples-section-title {
  font-size: 12px;
  color: #6b6b6f;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
}

.examples-loading {
  padding: 4px 0;
}

.example-item {
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.example-item:last-child {
  padding-bottom: 0;
  margin-bottom: 0;
  border-bottom: none;
}

.example-word {
  font-size: 16px;
  font-weight: 600;
  color: #10a37f;
  margin-bottom: 6px;
}

.example-sentence {
  font-size: 15px;
  line-height: 1.8;
  color: #2c3e50;
  word-break: break-word;
}

.example-sentence .hit {
  color: #0d8c6d;
  font-weight: 600;
  border-radius: 4px;
  background: rgba(16, 163, 127, 0.16);
  box-shadow: inset 0 -2px 0 #10a37f;
}

.example-translation {
  font-size: 14px;
  line-height: 1.8;
  color: #6b6b6f;
  margin-top: 6px;
  word-break: break-word;
}

.examples-empty {
  font-size: 13px;
  color: #8e8e93;
}

.examples-notfound {
  margin-top: 12px;
  font-size: 13px;
  color: #8e8e93;
}

/* 墨墨步骤：单词列表使用只读样式 */
.marked-word.is-static {
  cursor: default;
}

.marked-word.is-static:hover {
  background: rgba(16, 163, 127, 0.1);
  border-color: rgba(16, 163, 127, 0.35);
}

/* 墨墨步骤：推送操作 */
.momo-hint {
  font-size: 12px;
  color: #10a37f;
  background: rgba(16, 163, 127, 0.1);
  border-radius: 999px;
  padding: 3px 10px;
}

.momo-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.momo-btn {
  min-width: 140px;
}

.momo-btn.el-button {
  border-radius: 8px;
  font-size: 14px;
}

.momo-result {
  margin-top: 20px;
  padding: 16px 18px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.8;
}

.momo-result.is-success {
  background: rgba(16, 163, 127, 0.08);
  border: 1px solid rgba(16, 163, 127, 0.3);
  color: #0d8c6d;
}

.momo-result.is-error {
  background: rgba(208, 48, 80, 0.08);
  border: 1px solid rgba(208, 48, 80, 0.3);
  color: #d03050;
}

.momo-result-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.momo-result-line {
  color: #6b6b6f;
  word-break: break-word;
}

/* 章节内容 */
.chapter-loading,
.chapter-content {
  max-width: 880px;
  margin: 0 auto;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  padding: 28px 32px;
}

.chapter-loading {
  min-height: 200px;
}

.chapter-content-title {
  font-size: 20px;
  font-weight: 600;
  color: #343541;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #e5e5e5;
}

.chapter-content-body {
  font-size: 15px;
  line-height: 1.9;
  color: #343541;
  white-space: pre-wrap;
  word-break: break-word;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.5;
}

.panel-placeholder h2 {
  font-size: 24px;
  font-weight: 500;
  color: #343541;
  margin-bottom: 12px;
}

.panel-placeholder p {
  font-size: 14px;
}

/* 上传弹窗 */
.upload-dialog-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 8px 0 4px;
}

.upload-hint {
  font-size: 13px;
  color: #6b6b6f;
}

.upload-area {
  display: flex;
  justify-content: center;
}

.upload-select-btn {
  height: 40px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
}

.upload-select-btn:hover {
  background: #ececf1;
  border-color: #c7c7cc;
}

.selected-file {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(16, 163, 127, 0.08);
  border: 1px solid rgba(16, 163, 127, 0.3);
}

.selected-file-icon {
  font-size: 16px;
}

.selected-file-name {
  font-size: 13px;
  color: #10a37f;
  max-width: 280px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.selected-file-empty {
  font-size: 13px;
  color: #8e8e93;
}

.dialog-btn {
  height: 36px;
  padding: 0 20px;
  font-size: 14px;
  border-radius: 8px;
  background: transparent;
  border: 1px solid #e5e5e5;
  color: #343541;
}

.dialog-btn:hover {
  background: #ececf1;
  border-color: #c7c7cc;
}

.dialog-btn-primary {
  color: #ffffff;
  background: #10a37f;
  border-color: #10a37f;
}

.dialog-btn-primary:hover {
  color: #ffffff;
  background: #0d8c6d;
  border-color: #0d8c6d;
}

/* 配置弹窗 */
.config-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 8px 0 4px;
}

.config-field-label {
  font-size: 13px;
  font-weight: 500;
  color: #343541;
}

.config-hint {
  font-size: 12px;
  line-height: 1.7;
  color: #8e8e93;
}

/* 墨墨步骤：未配置 Token 提示 */
.momo-token-warning {
  max-width: 880px;
  margin: 0 auto 16px;
  padding: 10px 16px;
  font-size: 13px;
  line-height: 1.7;
  color: #d08a00;
  background: rgba(240, 173, 0, 0.1);
  border: 1px solid rgba(240, 173, 0, 0.35);
  border-radius: 10px;
}

/* 覆盖 Element Plus 主色 */
:deep(.el-button--primary) {
  --el-button-bg-color: #10a37f;
  --el-button-border-color: #10a37f;
  --el-button-hover-bg-color: #0d8c6d;
  --el-button-hover-border-color: #0d8c6d;
}
</style>
