<template>
  <div class="chat-page">
    <!-- 左侧历史会话侧栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="handleNewChat">
          <span class="new-chat-icon">＋</span>
          <span>新建会话</span>
        </button>
      </div>
      <div class="session-list">
        <div v-if="sessions.length === 0" class="session-empty">
          暂无历史会话
        </div>
        <div
          v-for="s in sessions"
          :key="s.chatId"
          class="session-item"
          :class="{ active: s.chatId === chatId }"
          :title="s.title"
          @click="handleSessionClick(s.chatId)"
        >
          <span class="session-icon">💬</span>
          <span class="session-title">{{ s.title }}</span>
        </div>
      </div>
    </aside>

    <!-- 右侧聊天区 -->
    <div class="chat-main">
      <header class="chat-header">
        <router-link to="/" class="back-link">← 返回首页</router-link>
        <h1 class="chat-title">AI 对话</h1>
        <div class="header-spacer"></div>
      </header>

      <!-- 消息列表区 -->
      <div class="messages-container" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-icon">💬</div>
        <h2>开始对话</h2>
        <p>在下方输入你的问题，AI 将为你解答</p>
      </div>

      <div v-else class="messages-list">
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="message"
          :class="msg.role"
        >
          <div class="message-body">
            <div
              v-if="msg.role === 'user'"
              class="message-text"
            >{{ msg.content }}</div>
            <div
              v-else
              class="message-text markdown-body"
              v-html="renderMarkdown(msg.content)"
            ></div>
          </div>
        </div>

        <!-- 加载动画 -->
        <div v-if="loading" class="message assistant">
          <div class="message-body">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="input-area">
      <div class="input-wrapper">
        <textarea
          v-model="input"
          ref="inputRef"
          class="chat-input"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          :disabled="loading"
          rows="1"
          @keydown.enter.exact.prevent="handleSend"
          @input="autoResize"
        ></textarea>
        <button
          class="send-btn"
          :disabled="!input.trim() || loading"
          @click="handleSend"
        >
          <span v-if="loading" class="send-loading"></span>
          <span v-else>发送</span>
        </button>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createChat, doChat, getChatHistory, listChatIds } from '@/utils/api'

const messages = ref([])
const input = ref('')
const loading = ref(false)
const chatId = ref('')
const messagesContainer = ref(null)
const inputRef = ref(null)
const route = useRoute()
const router = useRouter()

// 历史会话列表（chatId + 标题）
const sessions = ref([])

// 创建新会话：调用后端 /api/chat/create 获取 chatId
async function initChat() {
  loading.value = true
  try {
    chatId.value = await createChat()
  } catch (error) {
    chatId.value = ''
    ElMessage.error('创建会话失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

// 加载会话列表：并发拉取每个会话的第一条用户消息作为标题
async function loadSessions() {
  try {
    const ids = (await listChatIds()) || []
    sessions.value = await Promise.all(
      ids.map(async (id) => {
        try {
          const hist = await getChatHistory(id)
          const firstUser = (hist?.messages || []).find((m) => m.role === 'user')
          return { chatId: id, title: firstUser?.content || '新会话' }
        } catch (e) {
          return { chatId: id, title: '会话 ' + id.slice(0, 8) }
        }
      })
    )
  } catch (error) {
    ElMessage.error('加载会话列表失败: ' + (error.response?.data?.message || error.message))
  }
}

// 点击左侧会话：加载其历史记录并同步 URL
async function handleSessionClick(id) {
  if (id === chatId.value) return
  chatId.value = id
  messages.value = []
  loading.value = true
  try {
    const data = await getChatHistory(id)
    messages.value = data?.messages || []
    router.replace({ path: '/chat', query: { chatId: id } })
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载历史记录失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

// 对话窗口打开时：优先打开 URL 指定的会话，否则打开第一个，没有则新建
onMounted(async () => {
  await loadSessions()
  const queryId = route.query.chatId ? String(route.query.chatId) : ''
  const exists = sessions.value.find((s) => s.chatId === queryId)
  if (exists) {
    await handleSessionClick(queryId)
  } else if (sessions.value.length > 0) {
    await handleSessionClick(sessions.value[0].chatId)
  } else {
    await initChat()
  }
})

// ---------- 轻量 Markdown 渲染（不依赖第三方库） ----------
// 先转义 HTML，防止 AI 返回内容插入原始标签（XSS）
function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// 行内语法：行内代码 / 加粗 / 斜体 / 删除线 / 链接
function renderInline(text) {
  const codePlaceholders = []
  // 先保护行内代码，避免其中的 *、_ 被误解析
  let result = text.replace(/`([^`]+)`/g, (m, code) => {
    codePlaceholders.push(`<code>${code}</code>`)
    return `\u0000${codePlaceholders.length - 1}\u0000`
  })
  result = result.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  result = result.replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>')
  result = result.replace(/~~([^~]+)~~/g, '<del>$1</del>')
  result = result.replace(
    /\[([^\]]+)\]\(([^)\s]+)\)/g,
    '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>'
  )
  result = result.replace(/\u0000(\d+)\u0000/g, (m, i) => codePlaceholders[+i])
  return result
}

function renderMarkdown(text) {
  if (!text) return ''
  const lines = escapeHtml(text).split('\n')
  let html = ''
  let currentList = null // 'ul' | 'ol' | null
  let inCodeBlock = false
  const codeLines = []

  const flushCodeBlock = () => {
    if (codeLines.length) {
      html += `<pre><code>${codeLines.join('\n')}</code></pre>`
      codeLines.length = 0
    }
  }

  const closeList = () => {
    if (currentList) {
      html += `</${currentList}>`
      currentList = null
    }
  }

  for (const rawLine of lines) {
    // 代码块
    if (/^```/.test(rawLine.trim())) {
      if (inCodeBlock) {
        flushCodeBlock()
        inCodeBlock = false
      } else {
        closeList()
        flushCodeBlock()
        inCodeBlock = true
      }
      continue
    }
    if (inCodeBlock) {
      codeLines.push(rawLine)
      continue
    }

    const line = rawLine.trim()
    if (!line) {
      closeList()
      continue
    }

    // 标题
    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      closeList()
      const level = heading[1].length
      html += `<h${level}>${renderInline(heading[2])}</h${level}>`
      continue
    }

    // 分割线
    if (/^(-{3,}|\*{3,})$/.test(line)) {
      closeList()
      html += '<hr>'
      continue
    }

    // 引用
    if (/^&gt;/.test(line)) {
      closeList()
      html += `<blockquote>${renderInline(line.replace(/^&gt;\s?/, ''))}</blockquote>`
      continue
    }

    // 无序 / 有序列表
    const ulMatch = line.match(/^([-*+])\s+(.+)$/)
    const olMatch = line.match(/^\d+[.)]\s+(.+)$/)
    if (ulMatch || olMatch) {
      const type = ulMatch ? 'ul' : 'ol'
      const content = ulMatch ? ulMatch[2] : olMatch[1]
      if (currentList !== type) {
        closeList()
        html += `<${type}>`
        currentList = type
      }
      html += `<li>${renderInline(content)}</li>`
      continue
    }

    // 普通段落
    closeList()
    html += `<p>${renderInline(line)}</p>`
  }

  closeList()
  flushCodeBlock()
  return html
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function autoResize() {
  const el = inputRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 200) + 'px'
  }
}

async function handleSend() {
  const message = input.value.trim()
  if (!message || loading.value) return

  // 没有 chatId 时先创建会话
  if (!chatId.value) {
    await initChat()
    if (!chatId.value) return
  }

  messages.value.push({ role: 'user', content: message })
  input.value = ''

  // 重置 textarea 高度
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.style.height = 'auto'
    }
  })

  scrollToBottom()
  loading.value = true

  try {
    const content = await doChat(message, chatId.value)
    messages.value.push({ role: 'assistant', content: content || '' })
    refreshCurrentSession()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('AI 回复失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

// 更新当前会话在左侧列表中的标题（取第一条用户消息）
function refreshCurrentSession() {
  const firstUser = messages.value.find((m) => m.role === 'user')
  const idx = sessions.value.findIndex((s) => s.chatId === chatId.value)
  if (idx >= 0) {
    if (firstUser) sessions.value[idx].title = firstUser.content
  } else if (firstUser) {
    sessions.value.unshift({ chatId: chatId.value, title: firstUser.content })
  }
}

async function handleNewChat() {
  messages.value = []
  input.value = ''
  chatId.value = ''
  router.replace({ path: '/chat' })
  await initChat()
  if (chatId.value) {
    sessions.value.unshift({ chatId: chatId.value, title: '新会话' })
  }
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.chat-page {
  display: flex;
  flex-direction: row;
  height: 100vh;
  background: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #343541;
}

/* 左侧历史会话侧栏 */
.sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #f7f7f8;
  border-right: 1px solid #e5e5e5;
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #e5e5e5;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
  font-size: 14px;
  color: #343541;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: #ececf1;
  border-color: #c7c7cc;
}

.new-chat-icon {
  font-size: 16px;
  line-height: 1;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-list::-webkit-scrollbar {
  width: 6px;
}

.session-list::-webkit-scrollbar-thumb {
  background: #c7c7cc;
  border-radius: 3px;
}

.session-empty {
  padding: 24px 12px;
  text-align: center;
  color: #8e8e93;
  font-size: 13px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 13px;
  color: #343541;
}

.session-item:hover {
  background: rgba(16, 163, 127, 0.06);
}

.session-item.active {
  background: rgba(16, 163, 127, 0.12);
  color: #10a37f;
}

.session-icon {
  flex-shrink: 0;
  font-size: 15px;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 右侧聊天区 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 顶部导航栏 */
.chat-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #e5e5e5;
  background: #ffffff;
  flex-shrink: 0;
}

.back-link {
  font-size: 14px;
  color: #6b6b6f;
  text-decoration: none;
  transition: color 0.2s;
}

.back-link:hover {
  color: #10a37f;
}

.chat-title {
  font-size: 16px;
  font-weight: 500;
  color: #343541;
}

/* 消息容器 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  background: #f7f7f8;
}

.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #c7c7cc;
  border-radius: 3px;
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

/* 消息列表 */
.messages-list {
  max-width: 768px;
  margin: 0 auto;
  padding: 24px 16px;
}

/* 单条消息 */
.message {
  display: flex;
  padding: 16px 0;
}

.message.user {
  justify-content: flex-end;
}

.message-body {
  max-width: 85%;
  min-width: 0;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.7;
  word-break: break-word;
}

.message.user .message-text {
  background: #10a37f;
  color: #ffffff;
  border-bottom-right-radius: 4px;
  white-space: pre-wrap;
}

.message.assistant .message-text {
  background: #ffffff;
  color: #343541;
  border: 1px solid #e5e5e5;
  border-bottom-left-radius: 4px;
}

/* Markdown 内容样式 */
.markdown-body p {
  margin: 4px 0;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  margin: 10px 0 6px;
  font-weight: 600;
  color: #202123;
  line-height: 1.4;
}

.markdown-body h1 { font-size: 20px; }
.markdown-body h2 { font-size: 18px; }
.markdown-body h3 { font-size: 16px; }
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 { font-size: 15px; }

.markdown-body strong {
  font-weight: 600;
  color: #202123;
}

.markdown-body code {
  padding: 2px 6px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  color: #d63384;
  background: #f4f4f5;
  border-radius: 4px;
}

.markdown-body pre {
  margin: 8px 0;
  padding: 12px 14px;
  overflow-x: auto;
  background: #f4f4f5;
  border-radius: 8px;
  border: 1px solid #e5e5e5;
}

.markdown-body pre code {
  padding: 0;
  background: transparent;
  color: #343541;
}

.markdown-body ul,
.markdown-body ol {
  margin: 4px 0;
  padding-left: 24px;
}

.markdown-body li {
  margin: 2px 0;
}

.markdown-body blockquote {
  margin: 8px 0;
  padding: 6px 12px;
  color: #6b6b6f;
  border-left: 3px solid #10a37f;
  background: #f7f7f8;
  border-radius: 0 6px 6px 0;
}

.markdown-body a {
  color: #10a37f;
  text-decoration: none;
}

.markdown-body a:hover {
  text-decoration: underline;
}

.markdown-body hr {
  margin: 10px 0;
  border: none;
  border-top: 1px solid #e5e5e5;
}

.markdown-body del {
  color: #8e8e93;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  border-bottom-left-radius: 4px;
}

.typing-indicator span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #c7c7cc;
  animation: typing-bounce 1.4s ease-in-out infinite both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
.typing-indicator span:nth-child(3) { animation-delay: 0s; }

@keyframes typing-bounce {
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
.input-area {
  flex-shrink: 0;
  background: #ffffff;
  border-top: 1px solid #e5e5e5;
  padding: 16px 24px;
}

.input-wrapper {
  max-width: 768px;
  margin: 0 auto;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
  padding: 12px 16px;
  font-size: 15px;
  font-family: inherit;
  color: #343541;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  resize: none;
  outline: none;
  line-height: 1.5;
  max-height: 200px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.chat-input::placeholder {
  color: #8e8e93;
}

.chat-input:hover {
  border-color: #c7c7cc;
}

.chat-input:focus {
  border-color: #10a37f;
  box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.12);
}

.chat-input:disabled {
  background: #f7f7f8;
  cursor: not-allowed;
}

.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  background: #10a37f;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #0d8c6d;
}

.send-btn:disabled {
  background: #e5e5e5;
  color: #c7c7cc;
  cursor: not-allowed;
}

/* 发送按钮加载动画 */
.send-loading {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
