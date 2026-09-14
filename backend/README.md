# AI Novel Translate 项目介绍

## 一、项目概述

本项目是一个基于 **Spring AI + 阿里云通义千问（DashScope）** 的 AI 小说翻译工具后端服务，旨在为英文小说/文本提供智能翻译，并在翻译基础上延伸出英语学习辅助能力（生词预测、意群划分、词性还原、单词例句推送墨墨背单词等）。

- **项目名称**：AI Novel Translate
- **GroupId / ArtifactId**：`com.hd` / `ai-translate`
- **版本**：1.0.0
- **技术栈**：Java 21 + Spring Boot 3.2.0 + Spring AI 1.0.0

## 二、技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 / 框架 | Java 21、Spring Boot 3.2.0 |
| AI | Spring AI、Spring AI Alibaba（DashScope 通义千问）、LangChain4J |
| 数据库 | MySQL（Spring Data JPA） |
| 缓存 / 会话 | Redis（Redisson）、Spring AI Redis Memory |
| 对象存储 | MinIO |
| 配置中心 | Nacos（可选，用于 Prompt 配置） |
| 文件导出 | Apache POI（Word）、iText 8（PDF） |
| 其他 | Lombok、Jackson、JSON Schema Generator、Spring Boot Mail |

## 三、核心功能模块

### 1. 翻译模块（`translate`）
核心翻译能力，围绕「会话（Session）」展开。

- **文件上传**：上传 PDF 或文本文件，自动解析章节（Chapter），并将源文件持久化到 MinIO。
- **章节翻译**：按段落切分后分批调用大模型翻译，支持失败兜底。
- **纯文本翻译**：无需会话，直接翻译用户传入的文本。
- **词性还原（lemmatize）**：结合大模型将单词还原为原型（如 `running -> run`）。
- **会话管理**：基于 Redis 存储会话元数据与翻译结果，支持重启、清空、查询。

**会话隔离方案**（Redis + Redisson）：
- 每个会话生成唯一 `sessionId`（UUID）。
- Redis Key 设计：
  - `translate:meta:{sessionId}`：会话状态元数据
  - `translate:data:{sessionId}`：翻译结果（分段 List）
- TTL：7 天（可刷新），刷新页面可恢复、超时自动清空。

### 2. 墨墨背单词推送模块（`maiMemo`）
将翻译过程中学到的单词推送到「墨墨背单词」云词本。

- 设置 / 查询墨墨 API Token
- 创建云词本、追加单词（自动去重）
- 为单词添加例句及中文翻译
- 从翻译会话中为指定单词匹配原句例句

### 3. 意群划分模块（`phraseChunk`）
对英文文本（翻译结果或纯文本）进行意群划分，辅助英语阅读。

### 4. 生词预测模块（`vocabPrediction`）
对英文文本进行生词预测，筛选出值得学习的词汇。

### 5. AI 对话模块（`chat`）
基于 Spring AI ChatClient 的多轮对话，支持对话记忆（Redis 持久化），可挂载英语学习 Skill 与工具集。

### 6. AI 工具集（`tools`）
将功能封装为 AI Tool，供大模型在对话中主动调用：

| 工具 | 说明 |
| --- | --- |
| `PDFGenerationTool` | 生成 PDF 导出 |
| `WORDGenerationTool` | 生成 Word 导出 |
| `TranslationTool` | 翻译 |
| `PhraseChunkTool` | 意群划分 |
| `MaiMemoTool` | 墨墨单词推送 |
| `VocabPredictionTool` | 生词预测 |
| `FileReadTool` | 文件读取 |

所有工具通过 `ToolRegistration` 统一注册，并附加日志回调（`LoggingToolCallback`）。

### 7. 文件存储模块（`storage` / `file`）
- 基于 MinIO 的对象存储，提供上传、下载、生成访问 URL。
- 上传的源文件按 `upload/{sessionId}/{fileName}` 分类存储。

### 8. 通用模块（`common`）
- 用户标识（`UserContext`）、用户拦截器（`UserInterceptor`）、Web MVC 配置。

## 四、目录结构

```
src/main/java/com/hd/ai/
├── translate/          # 核心翻译（controller/service/app/dto/config）
├── chat/               # AI 对话（controller/app/memory/dto）
├── maiMemo/            # 墨墨背单词推送（controller/service/repository/entity/dto）
├── phraseChunk/        # 意群划分（controller/service/app/dto）
├── vocabPrediction/    # 生词预测（controller/service/app/dto）
├── tools/              # AI 工具集
├── file/               # 文件上传
├── storage/            # MinIO 对象存储
├── skill/              # 英语学习 Skill
├── common/             # 通用（实体、拦截器）
└── AiTranslateApplication.java   # 启动类

src/main/resources/
├── application.yml         # 主配置
├── application-local.yml   # 本地配置
├── application-prod.yml    # 生产配置
├── nacos-prompt/           # Nacos Prompt 配置
├── fonts/                  # PDF 导出字体（.ttf/.otf）
└── logback-spring.xml      # 日志配置
```

## 五、核心配置说明

- **服务端口**：`8124`
- **Nacos**：`120.53.234.133:8848`，用于远程 Prompt 配置（`ai-prompts2.yaml`）
- **MySQL**：`jdbc:mysql://localhost:3306/ai_translate`
- **DashScope**：通义千问 API（`DASHSCOPE_API_KEY`）
- **MinIO**：`http://127.0.0.1:9000`，Bucket `translate-ai`
- **墨墨 API**：`https://open.maimemo.com/open/api/v1`

## 六、主要接口一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/upload` | 上传文件解析章节并创建会话 |
| POST | `/api/translate` | 翻译指定章节 |
| POST | `/api/translate/text` | 翻译纯文本 |
| POST | `/api/word/lemmatize` | 词性还原 |
| POST | `/api/word/examples` | 获取单词例句 |
| GET | `/api/session/{sessionId}` | 获取会话信息 |
| POST | `/api/session/restart` | 重启会话 |
| POST | `/api/session/clear` | 清空会话 |
| GET | `/api/user/info` | 获取当前用户标识 |
| POST | `/api/export/pdf` | 导出 PDF |
| POST | `/api/phrase-chunk` | 意群划分（翻译结果） |
| POST | `/api/phrase-chunk/text` | 意群划分（纯文本） |
| POST | `/api/vocab-prediction` | 生词预测 |
| POST | `/api/maiMemo/set-token` | 设置墨墨 Token |
| POST | `/api/maiMemo/create-notepad` | 创建云词本 |
| POST | `/api/maiMemo/add-words` | 追加单词 |
| POST | `/api/maiMemo/add-sentence` | 添加例句 |
| POST | `/api/maiMemo/push` | 推送单词和例句 |
| POST | `/api/chat/create` | 创建对话会话 |
| POST | `/api/chat/do-chat` | AI 对话 |
| GET | `/api/chat/history` | 查询对话历史 |

## 七、启动方式

```bash
# 需先启动 MySQL、Redis、MinIO（或使用 docker-compose.yml）

# 本地启动（使用 application-local.yml 配置）
./backed-startup.sh
# 或
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

项目也提供了 `Dockerfile` 与 `docker-compose.yml`，支持容器化部署。
