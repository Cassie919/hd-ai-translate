/**
 * API 封装工具类
 * 提供后端接口的统一调用方法
 */
import axios from 'axios'

/**
 * 创建新会话，从后端获取 chatId。
 * 对话窗口打开时或新增对话窗口时调用。
 * @returns {Promise<string>} 后端生成的 chatId
 */
export async function createChat() {
  const res = await axios.post('/api/chat/create')
  return res.data?.chatId
}

/**
 * 发起对话，携带 chatId 保持多轮记忆。
 * @param {string} message 用户消息
 * @param {string} chatId 会话 ID
 * @returns {Promise<string>} AI 回复内容
 */
export async function doChat(message, chatId) {
  const res = await axios.post('/api/chat/do-chat', null, {
    params: { message, chatId }
  })
  return res.data
}

/**
 * 查询会话历史记录
 * @param {string} chatId 会话 ID
 * @returns {Promise<{chatId: string, messages: Array<{role: string, content: string}>}>}
 */
export async function getChatHistory(chatId) {
  const res = await axios.get('/api/chat/history', {
    params: { chatId }
  })
  return res.data
}

/**
 * 查询所有会话 ID 列表
 * @returns {Promise<string[]>} 会话 ID 列表
 */
export async function listChatIds() {
  const res = await axios.get('/api/chat/list')
  return res.data
}

/**
 * 获取指定章节的原始内容（后端从源文件重新解析）
 * @param {string} sessionId 会话 ID
 * @param {string} chapterTitle 章节标题
 * @returns {Promise<{chapterTitle: string, content: string}>}
 */
export async function getChapterContent(sessionId, chapterTitle) {
  const res = await axios.get('/api/chapter/content', {
    params: { sessionId, chapterTitle }
  })
  return res.data
}

/**
 * 翻译指定章节
 * @param {string} sessionId 会话 ID
 * @param {string} chapterTitle 章节标题
 * @returns {Promise<Array<{index: number, en: string, cn: string}>>} 逐段翻译结果
 */
export async function translateChapter(sessionId, chapterTitle) {
  const res = await axios.post('/api/translate', { sessionId, chapterTitle })
  return res.data
}

/**
 * 对翻译结果进行意群划分
 * @param {Array<{index: number, en: string, cn: string}>} items 翻译结果条目
 * @returns {Promise<{items: Array<{index: number, en: string, cn: string, chunks: string[]}>}>}
 */
export async function phraseChunk(items) {
  const res = await axios.post('/api/phrase-chunk', { items })
  return res.data
}

/**
 * 获取单词例句：从翻译会话的逐句译文里，为指定单词匹配英文原句作为例句、中文作为译文
 * @param {string} sessionId 会话 ID
 * @param {string[]} words 目标单词列表
 * @returns {Promise<{matched: Array<{word: string, sentence: string, translation: string}>, notFound: string[]}>}
 */
export async function getWordExamples(sessionId, words) {
  const res = await axios.post('/api/word/examples', { sessionId, words })
  return res.data
}

/**
 * 词性还原：将单词列表还原为原型，输出顺序与输入一致
 * @param {string[]} words 待还原的单词列表
 * @returns {Promise<string[]>} 原型列表（与输入顺序一一对应）
 */
export async function lemmatizeWords(words) {
  const res = await axios.post('/api/word/lemmatize', { words })
  return Array.isArray(res.data?.lemmas) ? res.data.lemmas : []
}

/**
 * 设置墨墨 API Token（后端会先探活校验，无效则不保存）
 * @param {string} token 墨墨 API Token
 * @returns {Promise<{success: boolean, message: string, notepadId?: string}>}
 */
export async function setMaiMemoToken(token) {
  const res = await axios.post('/api/maiMemo/set-token', { token })
  return res.data
}

/**
 * 查询当前用户是否已配置墨墨 Token
 * @returns {Promise<{success: boolean, hasToken: boolean}>}
 */
export async function getMaiMemoTokenStatus() {
  const res = await axios.get('/api/maiMemo/token-status')
  return res.data
}

/**
 * 仅推送单词：向已有云词本追加单词（后端自动去重）
 * @param {string[]} words 要追加的单词列表
 * @returns {Promise<{success: boolean, uniqueWords: string[], duplicateWords: string[]}>}
 */
export async function addMaiMemoWords(words) {
  const res = await axios.post('/api/maiMemo/add-words', { words })
  return res.data
}

/**
 * 推送单词和例句：将单词列表加到生词本，然后逐个添加例句
 * @param {Array<{word: string, sentence: string, translation: string}>} items 单词列表，每个元素包含 word/sentence/translation
 * @returns {Promise<{success: boolean, addWordsResult: object, sentenceSummary: object, sentenceResults: object[]}>}
 */
export async function pushMaiMemo(items) {
  const res = await axios.post('/api/maiMemo/push', { items })
  return res.data
}
