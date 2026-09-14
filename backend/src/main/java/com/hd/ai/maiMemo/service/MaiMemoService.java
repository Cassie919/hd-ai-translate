package com.hd.ai.maiMemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hd.ai.common.entity.User;
import com.hd.ai.maiMemo.config.MaiMemoConfig;
import com.hd.ai.maiMemo.entity.UserMomoConfig;
import com.hd.ai.maiMemo.repository.UserMomoConfigRepository;
import com.hd.ai.maiMemo.repository.UserRepository;
import com.hd.ai.translate.dto.TranslateItem;
import com.hd.ai.translate.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MaiMemoService {

    private final MaiMemoConfig config;
    private final UserRepository userRepository;
    private final UserMomoConfigRepository momoConfigRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final SessionService sessionService;

    private static final String TITLE = "Novel Translate";
    private static final String BRIEF = "小说翻译 录入词汇";
    private static final List<String> TAGS = List.of("词典");
    private static final String ORIGIN = "novel";

    public MaiMemoService(MaiMemoConfig config, UserRepository userRepository,
                          UserMomoConfigRepository momoConfigRepository, ObjectMapper objectMapper,
                          RestClient.Builder restClientBuilder, SessionService sessionService) {
        this.config = config;
        this.userRepository = userRepository;
        this.momoConfigRepository = momoConfigRepository;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(config.getBaseUrl())
                .build();
        this.sessionService = sessionService;
    }

    // ============================================================
    // 0. 设置 Token
    // ============================================================
    public Map<String, Object> setToken(String userId, String token) {
        String trimmedToken = token == null ? "" : token.trim();
        // 保存前先探活校验，避免无效 Token 落库后推送时才暴露问题
        validateToken(trimmedToken);

        // 查找或创建用户
        User user = userRepository.findByUserKey(userId)
                .orElseGet(() -> userRepository.save(
                        User.builder().userKey(userId).build()
                ));
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        // 查找或创建配置
        UserMomoConfig momoConfig = momoConfigRepository.findByUserId(user.getId())
                .orElseGet(() -> UserMomoConfig.builder().userId(user.getId()).build());
        momoConfig.setMomoToken(trimmedToken);
        momoConfigRepository.save(momoConfig);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Token 设置成功");
        if (momoConfig.getNotepadId() != null) {
            result.put("notepadId", momoConfig.getNotepadId());
        }
        return result;
    }

    /**
     * 校验 Token 是否有效。
     * 墨墨开放 API 无专用校验接口，这里调用「查询云词本列表」(GET /notepads?limit=1)
     * 这一只读、无副作用的接口探活：2xx 视为有效，401/403 视为无效。
     *
     * @param token 待校验的墨墨 API Token
     * @return 校验通过返回 true
     * @throws RuntimeException Token 为空、无效或校验请求异常时抛出
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Token 不能为空");
        }
        String trimmed = token.trim();
        try {
            restClient.get()
                    .uri("/notepads?limit=1")
                    .header("Authorization", "Bearer " + trimmed)
                    .header("Accept", "application/json")
                    .exchange((req, res) -> {
                        HttpStatusCode status = res.getStatusCode();
                        if (status.is2xxSuccessful()) {
                            return Boolean.TRUE;
                        }
                        String errorBody = new String(res.getBody().readAllBytes());
                        log.error("validateToken 校验失败: status={}, body={}", status.value(), errorBody);
                        if (status.value() == 401 || status.value() == 403) {
                            throw new RuntimeException("Token 无效或已过期，请检查后重试");
                        }
                        throw new RuntimeException("Token 校验失败: HTTP " + status.value() + " - " + errorBody);
                    });
            return true;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("validateToken 异常: {}", e.getMessage(), e);
            throw new RuntimeException("Token 校验失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 0.1 查询 Token 配置状态
    // ============================================================
    public Map<String, Object> getTokenStatus(String userId) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> userOpt = userRepository.findByUserKey(userId);
        if (userOpt.isEmpty()) {
            result.put("success", true);
            result.put("hasToken", false);
            return result;
        }
        boolean hasToken = momoConfigRepository.findByUserId(userOpt.get().getId())
                .map(cfg -> cfg.getMomoToken() != null && !cfg.getMomoToken().isEmpty())
                .orElse(false);
        result.put("success", true);
        result.put("hasToken", hasToken);
        return result;
    }

    // ============================================================
    // 1. 创建云词本
    // ============================================================
    public Map<String, Object> createNotepad(String userId, List<String> words) throws JsonProcessingException {
        log.info("createNotepad 开始, userId={}, wordsCount={}", userId, words.size());
        UserMomoConfig momoConfig = getMomoConfig(userId);
        log.debug("获取用户配置成功, userId={}, hasExistingNotepad={}", userId, momoConfig.getNotepadId() != null);
        String token = momoConfig.getMomoToken();
        String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 构建 Markdown 内容
        StringBuilder content = new StringBuilder("# ").append(todayDate).append("\n");
        for (String word : words) {
            content.append(word).append("\n");
        }

        // 构建请求体
        Map<String, Object> notepadBody = new HashMap<>();
        notepadBody.put("status", "PUBLISHED");
        notepadBody.put("content", content.toString());
        notepadBody.put("title", TITLE);
        notepadBody.put("brief", BRIEF);
        notepadBody.put("tags", TAGS);

        Map<String, Object> requestBody = Map.of("notepad", notepadBody);
        try {
            log.info("createNotepad 请求体: {}", objectMapper.writeValueAsString(requestBody));
        } catch (JsonProcessingException e) {
            log.warn("createNotepad 请求体序列化失败", e);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/notepads")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange((req, res) -> {
                    HttpStatusCode status = res.getStatusCode();
                    if (status.is4xxClientError() || status.is5xxServerError()) {
                        String errorBody = new String(res.getBody().readAllBytes());
                        log.error("createNotepad API 返回错误: status={}, body={}", status.value(), errorBody);
                        throw new RuntimeException("创建云词本失败: HTTP " + status.value() + " - " + errorBody);
                    }
                    try {
                        return objectMapper.readValue(res.getBody(), Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException("创建云词本响应解析失败", e);
                    }
                });

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            log.error("createNotepad API 返回失败, response={}", response);
            throw new RuntimeException("创建云词本失败: " + response);
        }

        // 提取 notepadId
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> notepad = (Map<String, Object>) data.get("notepad");
        String notepadId = (String) notepad.get("id");
        log.info("createNotepad API 调用成功, notepadId={}", notepadId);

        // 保存 notepadId
        momoConfig.setNotepadId(notepadId);
        momoConfigRepository.save(momoConfig);
        log.info("notepadId 已保存, userId={}, notepadId={}", userId, notepadId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("notepadId", notepadId);
        result.put("addedWords", words);
        log.info("createNotepad 完成, userId={}, notepadId={}, addedWords={}", userId, notepadId, words);
        return result;
    }

    // ============================================================
    // 2. 向云词本追加单词
    // ============================================================
    public Map<String, Object> addWords(String userId, List<String> words) throws JsonProcessingException {
        log.info("addWords 请求: userId={}, words数量={}, words={}", userId, words.size(), words);
        UserMomoConfig momoConfig = getMomoConfig(userId);
        String token = momoConfig.getMomoToken();
        String notepadId = momoConfig.getNotepadId();

        if (notepadId == null || notepadId.isEmpty()) {
            // 自动创建云词本
            log.info("用户 {} 尚未创建云词本，自动创建中... words: {}", userId, words);
            Map<String, Object> createResult = createNotepad(userId, words);
            log.info("自动创建云词本成功, notepadId: {}", createResult.get("notepadId"));
            // 新创建的云词本，所有单词均为新增
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("uniqueWords", words);
            result.put("duplicateWords", List.of());
            return result;
        }

        // ① GET 获取当前云词本内容
        @SuppressWarnings("unchecked")
        Map<String, Object> getResp = restClient.get()
                .uri("/notepads/{id}", notepadId)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .retrieve()
                .body(Map.class);

        if (getResp == null || !Boolean.TRUE.equals(getResp.get("success"))) {
            throw new RuntimeException("获取云词本内容失败: " + getResp);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) getResp.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> notepadData = (Map<String, Object>) data.get("notepad");
        String currentContent = (String) notepadData.get("content");

        // 解析当前内容，找到今天日期的标题并提取已有单词
        String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dateHeading = "# " + todayDate;

        String newContent = mergeWordsIntoContent(currentContent, dateHeading, words);

        // ② POST 更新云词本
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("status", notepadData.getOrDefault("status", "PUBLISHED"));
        updateBody.put("content", newContent);
        updateBody.put("title", notepadData.getOrDefault("title", TITLE));
        updateBody.put("brief", notepadData.getOrDefault("brief", BRIEF));

        @SuppressWarnings("unchecked")
        List<Object> existingTags = (List<Object>) notepadData.get("tags");
        updateBody.put("tags", existingTags != null ? existingTags : TAGS);

        Map<String, Object> requestBody = Map.of("notepad", updateBody);

        @SuppressWarnings("unchecked")
        Map<String, Object> postResp = restClient.post()
                .uri("/notepads/{id}", notepadId)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange((req, res) -> {
                    HttpStatusCode status = res.getStatusCode();
                    if (status.is4xxClientError() || status.is5xxServerError()) {
                        String errorBody = new String(res.getBody().readAllBytes());
                        log.error("更新云词本 API 返回错误: status={}, body={}", status.value(), errorBody);
                        throw new RuntimeException("更新云词本失败: HTTP " + status.value() + " - " + errorBody);
                    }
                    try {
                        return objectMapper.readValue(res.getBody(), Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException("更新云词本响应解析失败", e);
                    }
                });

        if (postResp == null || !Boolean.TRUE.equals(postResp.get("success"))) {
            throw new RuntimeException("更新云词本失败: " + postResp);
        }

        // 返回去重结果
        Set<String> existingWordSet = extractWordsUnderHeading(currentContent, dateHeading);
        List<String> uniqueWords = new ArrayList<>();
        List<String> duplicateWords = new ArrayList<>();
        for (String word : words) {
            if (existingWordSet.contains(word)) {
                duplicateWords.add(word);
            } else {
                uniqueWords.add(word);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("uniqueWords", uniqueWords);
        result.put("duplicateWords", duplicateWords);
        log.info("addWords 完成: userId={}, notepadId={}, 新增={}, 重复={}", userId, notepadId, uniqueWords.size(), duplicateWords.size());
        return result;
    }

    // ============================================================
    // 3. 给单词添加例句
    // ============================================================
    public Map<String, Object> addSentence(String userId, String word, String sentence, String translation) {
        UserMomoConfig momoConfig = getMomoConfig(userId);
        String token = momoConfig.getMomoToken();

        // ① GET 查单词，获取单词 ID
        @SuppressWarnings("unchecked")
        Map<String, Object> searchResp = restClient.get()
                .uri("/vocabulary?spelling={word}", word)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .retrieve()
                .body(Map.class);

        if (searchResp == null || !Boolean.TRUE.equals(searchResp.get("success"))) {
            throw new RuntimeException("未找到单词: " + word);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) searchResp.get("data");
        if (data == null) {
            throw new RuntimeException("未找到单词: " + word);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> voc = (Map<String, Object>) data.get("voc");
        if (voc == null) {
            throw new RuntimeException("未找到单词: " + word);
        }

        Object wordId = voc.get("id");

        // ② POST 创建例句
        Map<String, Object> phraseBody = new HashMap<>();
        phraseBody.put("voc_id", wordId);
        phraseBody.put("phrase", sentence);
        phraseBody.put("interpretation", translation);
        phraseBody.put("tags", TAGS);
        phraseBody.put("origin", ORIGIN);

        Map<String, Object> requestBody = Map.of("phrase", phraseBody);

        @SuppressWarnings("unchecked")
        Map<String, Object> createResp = restClient.post()
                .uri("/phrases")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange((req, res) -> {
                    HttpStatusCode status = res.getStatusCode();
                    if (status.is4xxClientError() || status.is5xxServerError()) {
                        String errorBody = new String(res.getBody().readAllBytes());
                        log.error("添加例句 API 返回错误: status={}, body={}", status.value(), errorBody);
                        throw new RuntimeException("添加例句失败: HTTP " + status.value() + " - " + errorBody);
                    }
                    try {
                        return objectMapper.readValue(res.getBody(), Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException("添加例句响应解析失败", e);
                    }
                });

        if (createResp == null || !Boolean.TRUE.equals(createResp.get("success"))) {
            throw new RuntimeException("添加例句失败: " + createResp);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "例句已添加到单词 " + word);
        return result;
    }

    // ============================================================
    // 4. 推送墨墨（一键：加到生词本 + 添加例句）
    // ============================================================
    public Map<String, Object> pushToMaiMemo(String userId, List<Map<String, String>> items) {
        log.info("pushToMaiMemo 开始: userId={}, items数量={}", userId, items.size());

        // 提取去重后的单词列表
        List<String> words = items.stream()
                .map(item -> item.get("word"))
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        log.info("pushToMaiMemo 去重后单词: userId={}, 单词数={}, words={}", userId, words.size(), words);

        // 步骤一：加到生词本
        Map<String, Object> addWordsResult;
        try {
            addWordsResult = addWords(userId, words);
            log.info("pushToMaiMemo 步骤一完成(加到生词本): userId={}, uniqueWords={}, duplicateWords={}",
                    userId,
                    addWordsResult.get("uniqueWords"),
                    addWordsResult.get("duplicateWords"));
        } catch (Exception e) {
            log.error("pushToMaiMemo 步骤一失败(加到生词本): userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("加到生词本失败: " + e.getMessage(), e);
        }

        // 步骤二：逐个添加例句
        log.info("pushToMaiMemo 步骤二开始(添加例句): userId={}, 例句数={}", userId, items.size());
        List<Map<String, Object>> sentenceResults = new ArrayList<>();
        int sentenceSuccessCount = 0;
        int sentenceFailCount = 0;

        for (Map<String, String> item : items) {
            String word = item.get("word");
            String sentence = item.get("sentence");
            String translation = item.get("translation");
            Map<String, Object> oneResult = new HashMap<>();
            oneResult.put("word", word);
            try {
                addSentence(userId, word, sentence, translation);
                oneResult.put("success", true);
                oneResult.put("message", "例句添加成功");
                sentenceSuccessCount++;
                log.info("pushToMaiMemo 例句添加成功: userId={}, word={}, sentence={}", userId, word, sentence);
            } catch (Exception e) {
                oneResult.put("success", false);
                oneResult.put("message", e.getMessage());
                sentenceFailCount++;
                log.warn("pushToMaiMemo 例句添加失败: userId={}, word={}, sentence={}, error={}", userId, word, sentence, e.getMessage());
            }
            sentenceResults.add(oneResult);
        }

        log.info("pushToMaiMemo 完成: userId={}, 加词结果={}, 例句total={}, success={}, fail={}",
                userId, addWordsResult, items.size(), sentenceSuccessCount, sentenceFailCount);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("addWordsResult", addWordsResult);
        result.put("sentenceSummary", Map.of(
                "total", items.size(),
                "success", sentenceSuccessCount,
                "fail", sentenceFailCount
        ));
        result.put("sentenceResults", sentenceResults);
        return result;
    }

    // ============================================================
    // 5. 从翻译会话按单词取原文例句+译文，组装后推送墨墨
    // ============================================================

    /**
     * 从翻译会话的逐句译文里，为每个目标单词匹配包含它的英文原句作为例句、对应中文作为译文，
     * 组装成可直接用于 pushToMaiMemo 的 items。未匹配到的单词单独返回。
     *
     * @param sessionId 翻译会话ID（Redis 中存有逐句 en/cn）
     * @param words     目标单词列表（已去重）
     * @return 包含 matched（可推送项）与 notFound（未匹配到例句/译文的单词）的 Map
     */
    public Map<String, Object> buildWordItemsFromSession(String sessionId, List<String> words) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> matched = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        if (sessionId == null || sessionId.isBlank()) {
            notFound.addAll(words);
            result.put("matched", matched);
            result.put("notFound", notFound);
            return result;
        }

        var sessionData = sessionService.getSessionData(sessionId);
        List<TranslateItem> items = sessionData != null ? sessionData.getTranslateItems() : null;
        if (items == null || items.isEmpty()) {
            notFound.addAll(words);
            result.put("matched", matched);
            result.put("notFound", notFound);
            return result;
        }

        for (String rawWord : words) {
            String word = rawWord.trim();
            if (word.isEmpty()) {
                continue;
            }
            String lower = word.toLowerCase();
            // 在英文原句里匹配（单词边界，忽略大小写）
            TranslateItem hit = items.stream()
                    .filter(it -> it.getEn() != null
                            && it.getCn() != null
                            && it.getEn().toLowerCase().matches(".*\\b" + Pattern.quote(lower) + "\\b.*"))
                    .findFirst()
                    .orElse(null);
            if (hit != null) {
                Map<String, String> item = new HashMap<>();
                item.put("word", word);
                item.put("sentence", hit.getEn().trim());
                item.put("translation", hit.getCn().trim());
                matched.add(item);
            } else {
                notFound.add(word);
            }
        }

        log.info("buildWordItemsFromSession: sessionId={}, 匹配={}, 未匹配={}", sessionId, matched.size(), notFound.size());
        result.put("matched", matched);
        result.put("notFound", notFound);
        return result;
    }

    /**
     * 按单词名 + 翻译会话，自动取原文例句与译文并推送到墨墨。
     * 适用于用户指令："给 abandon、ability 添加例句推送墨墨"。
     */
    public Map<String, Object> pushSelectedWords(String userId, String sessionId, String wordsText) {
        List<String> words = parseWords(wordsText);
        if (words.isEmpty()) {
            throw new RuntimeException("未解析到任何有效的单词");
        }
        Map<String, Object> built = buildWordItemsFromSession(sessionId, words);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> matched = (List<Map<String, String>>) built.get("matched");
        @SuppressWarnings("unchecked")
        List<String> notFound = (List<String>) built.get("notFound");

        if (matched.isEmpty()) {
            throw new RuntimeException("在翻译会话中未找到这些单词的原文例句: " + notFound);
        }

        Map<String, Object> pushResult = pushToMaiMemo(userId, matched);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("pushedWords", matched.stream().map(m -> m.get("word")).collect(Collectors.toList()));
        result.put("notFoundWords", notFound);
        result.put("pushDetail", pushResult);
        return result;
    }

    /**
     * 解析逗号/换行分隔的单词文本为去重后的单词列表。
     */
    private List<String> parseWords(String wordsText) {
        List<String> words = new ArrayList<>();
        if (wordsText == null || wordsText.isBlank()) {
            return words;
        }
        for (String raw : wordsText.split("[,\\n\\r]")) {
            String w = raw.trim();
            if (!w.isEmpty() && !words.contains(w)) {
                words.add(w);
            }
        }
        return words;
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 根据 userId 获取用户配置，校验 token 是否已设置
     */
    private UserMomoConfig getMomoConfig(String userId) {
        User user = userRepository.findByUserKey(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在，请先设置 Token: " + userId));
        UserMomoConfig momoConfig = momoConfigRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("用户未设置 Token，请先调用 setToken"));
        if (momoConfig.getMomoToken() == null || momoConfig.getMomoToken().isEmpty()) {
            throw new RuntimeException("用户未设置 Token，请先调用 setToken");
        }
        return momoConfig;
    }

    /**
     * 将新单词合并到现有 Markdown 内容中（去重并插入到日期标题下方）
     */
    private String mergeWordsIntoContent(String content, String dateHeading, List<String> newWords) {
        String[] lines = content.split("\n", -1);
        Set<String> existingWords = extractWordsUnderHeading(content, dateHeading);

        // 过滤掉已存在的单词
        List<String> uniqueWords = new ArrayList<>();
        for (String word : newWords) {
            if (!existingWords.contains(word)) {
                uniqueWords.add(word);
            }
        }

        if (uniqueWords.isEmpty()) {
            return content; // 没有新单词
        }

        // 查找日期标题行
        int headingIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals(dateHeading)) {
                headingIndex = i;
                break;
            }
        }

        StringBuilder result = new StringBuilder();

        if (headingIndex >= 0) {
            // 找到标题：在标题行后插入新单词
            for (int i = 0; i <= headingIndex; i++) {
                result.append(lines[i]).append("\n");
            }
            for (String word : uniqueWords) {
                result.append(word).append("\n");
            }
            for (int i = headingIndex + 1; i < lines.length; i++) {
                result.append(lines[i]);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }
        } else {
            // 没找到标题：在末尾追加标题和新单词
            result.append(content);
            if (!content.endsWith("\n")) {
                result.append("\n");
            }
            result.append(dateHeading).append("\n");
            for (String word : uniqueWords) {
                result.append(word).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 从 Markdown 内容中提取指定日期标题下的所有单词
     */
    private Set<String> extractWordsUnderHeading(String content, String dateHeading) {
        Set<String> words = new HashSet<>();
        String[] lines = content.split("\n");

        boolean underTargetHeading = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                if (trimmed.equals(dateHeading)) {
                    underTargetHeading = true;
                } else {
                    underTargetHeading = false;
                }
                continue;
            }
            if (underTargetHeading && !trimmed.isEmpty()) {
                words.add(trimmed);
            }
        }
        return words;
    }
}
