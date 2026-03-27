/**
 * 会话管理工具类
 * 管理会话ID和元数据,sessionId由后端生成
 */

class SessionManager {
  constructor() {
    this.sessionId = null
  }

  /**
   * 获取会话ID
   * @returns {string|null} 会话ID,如果不存在返回null
   */
  getSessionId() {
    return this.sessionId
  }

  /**
   * 设置会话ID（用于后端返回的sessionId）
   * @param {string} id 会话ID
   */
  setSessionId(id) {
    this.sessionId = id
    this.saveSessionId()
  }

  /**
   * 保存会话ID到本地存储
   */
  saveSessionId() {
    try {
      localStorage.setItem('translate_session_id', this.sessionId)
    } catch (error) {
      console.error('保存会话ID失败:', error)
    }
  }

  /**
   * 加载会话ID
   * @returns {string|null} 会话ID
   */
  loadSessionId() {
    try {
      const savedSessionId = localStorage.getItem('translate_session_id')
      if (savedSessionId) {
        this.sessionId = savedSessionId
        return this.sessionId
      }
    } catch (error) {
      console.error('加载会话ID失败:', error)
    }
    return null
  }

  /**
   * 保存会话元数据
   * @param {Object} meta 元数据
   */
  saveSessionMeta(meta) {
    try {
      const sessionMeta = {
        sessionId: this.sessionId,
        ...meta,
        updatedAt: Date.now()
      }
      localStorage.setItem('translate_session_meta', JSON.stringify(sessionMeta))
    } catch (error) {
      console.error('保存会话元数据失败:', error)
    }
  }

  /**
   * 加载会话元数据
   */
  loadSessionMeta() {
    try {
      const saved = localStorage.getItem('translate_session_meta')
      if (saved) {
        const meta = JSON.parse(saved)
        // 检查会话ID是否匹配
        if (meta.sessionId === this.sessionId) {
          return meta
        }
      }
    } catch (error) {
      console.error('加载会话元数据失败:', error)
    }
    return null
  }

  /**
   * 清空会话数据
   */
  clearSession() {
    try {
      localStorage.removeItem('translate_session_id')
      localStorage.removeItem('translate_session_meta')
      this.sessionId = null
    } catch (error) {
      console.error('清空会话数据失败:', error)
    }
  }
}

export default new SessionManager()