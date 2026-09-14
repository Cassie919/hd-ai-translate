<template>
  <div class="landing">
    <div class="landing-bg"></div>
    <div class="landing-content">
      <h1 class="landing-title">HD AI Translate</h1>
      <p class="landing-subtitle">选择一个功能开始使用</p>

      <div class="entry-cards">
        <!-- 翻译入口暂时隐藏，需要时恢复以下卡片即可 -->
        <!-- <router-link to="/translate" class="entry-card">
          <div class="entry-icon">📖</div>
          <div class="entry-name">翻译</div>
          <div class="entry-desc">上传小说文件，按章节进行 AI 翻译</div>
          <div class="entry-arrow">→</div>
        </router-link> -->

        <router-link to="/chat" class="entry-card">
          <div class="entry-icon">💬</div>
          <div class="entry-name">Chat 对话</div>
          <div class="entry-desc">与 AI 进行多轮对话交流</div>
          <div class="entry-arrow">→</div>
        </router-link>

        <router-link to="/workflow" class="entry-card">
          <div class="entry-icon">🧭</div>
          <div class="entry-name">Workflow</div>
          <div class="entry-desc">编排工作流，自动化完成翻译任务</div>
          <div class="entry-arrow">→</div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.landing {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  overflow: hidden;
}

.landing-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(64, 158, 255, 0.12), transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(103, 194, 58, 0.12), transparent 40%);
}

.landing-content {
  position: relative;
  text-align: center;
  padding: 40px 24px;
  max-width: 1080px;
  width: 100%;
}

.landing-title {
  margin: 0;
  font-size: 40px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 1px;
}

.landing-subtitle {
  margin: 12px 0 48px;
  font-size: 16px;
  color: #909399;
}

.entry-cards {
  display: flex;
  gap: 24px;
  justify-content: center;
  flex-wrap: wrap;
}

.entry-card {
  position: relative;
  width: 280px;
  padding: 32px 24px 56px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  text-decoration: none;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.entry-card:hover {
  transform: translateY(-4px);
  border-color: #409eff;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.15);
}

.entry-icon {
  font-size: 40px;
  margin-bottom: 16px;
}

.entry-name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.entry-desc {
  font-size: 14px;
  color: #909399;
  line-height: 1.5;
}

.entry-arrow {
  position: absolute;
  right: 20px;
  bottom: 16px;
  font-size: 18px;
  color: #c0c4cc;
  transition: all 0.25s ease;
}

.entry-card:hover .entry-arrow {
  color: #409eff;
  transform: translateX(4px);
}
</style>
