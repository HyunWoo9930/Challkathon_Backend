package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.utils.ChatGPTUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAnalysisService {

    private final ChatGPTUtils chatGPTUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIAnalysisResult analyzeArticle(String title, String content, String targetCategory) {
        try {
            log.info("AI 분석 시작 - 카테고리: {}, 제목: {}", targetCategory,
                title.substring(0, Math.min(50, title.length())));

            String systemPrompt = "너는 카테고리와 키워드를 분류하는 전문가야. 아래 기사에 대한 분석을 JSON으로 응답해.";
            String userPrompt = createAnalysisPrompt(title, content, targetCategory);

            String aiResponse = chatGPTUtils.callChatGPT(systemPrompt, userPrompt);
            log.debug("AI 응답: {}", aiResponse);

            return parseAIResponse(aiResponse, targetCategory);

        } catch (Exception e) {
            log.error("AI 분석 중 오류 발생: {}", e.getMessage(), e);
            return AIAnalysisResult.builder()
                .isRelevant(true)
                .categoryMatch(false)
                .relevanceScore(0.5)
                .keywords(List.of())
                .suggestedCategory(targetCategory)
                .reason("AI 분석 오류로 인한 기본값")
                .build();
        }
    }

    public String autoClassifyCategory(String title, String content) {
        try {
            log.info("카테고리 자동 분류 시작 - 제목: {}", title);

            String systemPrompt = "너는 기사를 읽고 카테고리를 정확히 분류하는 전문가야.";
            String userPrompt = createClassificationPrompt(title, content);

            String aiResponse = chatGPTUtils.callChatGPT(systemPrompt, userPrompt).trim().toLowerCase();
            log.debug("AI 응답 카테고리: {}", aiResponse);

            List<String> validCategories = List.of("frontend", "backend", "design", "planning", "devops", "general");
            return validCategories.contains(aiResponse) ? aiResponse : "general";

        } catch (Exception e) {
            log.error("카테고리 자동 분류 오류: {}", e.getMessage(), e);
            return "general";
        }
    }

    public List<String> extractKeywords(String title, String content) {
        try {
            log.info("키워드 추출 시작");

            String systemPrompt = "너는 기사에서 중요한 키워드를 정확히 뽑아내는 AI야.";
            String userPrompt = createKeywordPrompt(title, content);

            String aiResponse = chatGPTUtils.callChatGPT(systemPrompt, userPrompt);
            log.debug("키워드 응답: {}", aiResponse);

            return parseKeywords(aiResponse);

        } catch (Exception e) {
            log.error("키워드 추출 오류: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private String createAnalysisPrompt(String title, String content, String targetCategory) {
        return String.format("""
            다음 기사를 분석해서 JSON 형태로 응답해주세요.

            **제목:** %s
            **내용:** %s
            **타겟 카테고리:** %s

            [카테고리 기준]
            - frontend: JavaScript, React, Vue, HTML, CSS
            - backend: 서버, Spring, Node.js, Python, DB
            - design: UI/UX, 그래픽, 디자인 도구
            - planning: 기획, PM, 요구사항 분석
            - devops: 클라우드, Docker, 배포
            - general: 기타 개발/커리어

            [응답 JSON 형식]
            {
              "isRelevant": true/false,
              "categoryMatch": true/false,
              "relevanceScore": 0.0~1.0,
              "suggestedCategory": "카테고리명",
              "keywords": ["키워드1", ..., "키워드5"],
              "reason": "판단 근거"
            }

            유효한 JSON만 응답하고, 설명은 생략하세요.
            """, title, content.substring(0, Math.min(1500, content.length())), targetCategory);
    }

    private String createClassificationPrompt(String title, String content) {
        return String.format("""
            아래 기사에 가장 적합한 카테고리 하나만 소문자로 응답해주세요.

            제목: %s
            내용: %s

            [카테고리 옵션]
            - frontend
            - backend
            - design
            - planning
            - devops
            - general
            """, title, content.substring(0, Math.min(1000, content.length())));
    }

    private String createKeywordPrompt(String title, String content) {
        return String.format("""
            다음 기사에서 중요 키워드 5개를 쉼표로 구분해서 응답해주세요.

            제목: %s
            내용: %s

            예: React, Spring Boot, PM, Docker, 클라우드
            """, title, content.substring(0, Math.min(1000, content.length())));
    }

    private AIAnalysisResult parseAIResponse(String aiResponse, String targetCategory) {
        try {
            JsonNode node = objectMapper.readTree(aiResponse);

            return AIAnalysisResult.builder()
                .isRelevant(node.get("isRelevant").asBoolean(true))
                .categoryMatch(node.get("categoryMatch").asBoolean(false))
                .relevanceScore(node.get("relevanceScore").asDouble(0.5))
                .suggestedCategory(node.get("suggestedCategory").asText(targetCategory))
                .keywords(parseJsonKeywords(node.get("keywords")))
                .reason(node.get("reason").asText("AI 응답 분석 완료"))
                .build();
        } catch (Exception e) {
            log.warn("JSON 파싱 실패, fallback 실행: {}", e.getMessage());
            return AIAnalysisResult.builder()
                .isRelevant(true)
                .categoryMatch(false)
                .relevanceScore(0.5)
                .suggestedCategory(targetCategory)
                .keywords(extractKeywordsFromText(aiResponse))
                .reason("JSON 파싱 실패, fallback 적용")
                .build();
        }
    }

    private List<String> parseJsonKeywords(JsonNode keywordsNode) {
        List<String> keywords = new ArrayList<>();
        if (keywordsNode != null && keywordsNode.isArray()) {
            for (JsonNode keyword : keywordsNode) {
                keywords.add(keyword.asText());
            }
        }
        return keywords;
    }

    private List<String> parseKeywords(String response) {
        String[] raw = response.split(",");
        List<String> keywords = new ArrayList<>();
        for (String r : raw) {
            String k = r.trim().replaceAll("[\"'\\[\\]]", "");
            if (!k.isEmpty()) keywords.add(k);
        }
        return keywords.subList(0, Math.min(keywords.size(), 5));
    }

    private List<String> extractKeywordsFromText(String text) {
        String[] known = {"react", "vue", "spring", "docker", "api", "pm", "devops", "frontend", "backend"};
        List<String> result = new ArrayList<>();
        String lower = text.toLowerCase();
        for (String k : known) {
            if (lower.contains(k)) {
                result.add(k);
                if (result.size() >= 5) break;
            }
        }
        return result;
    }

    public static class AIAnalysisResult {
        private boolean isRelevant;
        private boolean categoryMatch;
        private double relevanceScore;
        private String suggestedCategory;
        private List<String> keywords;
        private String reason;

        public static AIAnalysisResultBuilder builder() {
            return new AIAnalysisResultBuilder();
        }

        public static class AIAnalysisResultBuilder {
            private boolean isRelevant;
            private boolean categoryMatch;
            private double relevanceScore;
            private String suggestedCategory;
            private List<String> keywords;
            private String reason;

            public AIAnalysisResultBuilder isRelevant(boolean val) { this.isRelevant = val; return this; }
            public AIAnalysisResultBuilder categoryMatch(boolean val) { this.categoryMatch = val; return this; }
            public AIAnalysisResultBuilder relevanceScore(double val) { this.relevanceScore = val; return this; }
            public AIAnalysisResultBuilder suggestedCategory(String val) { this.suggestedCategory = val; return this; }
            public AIAnalysisResultBuilder keywords(List<String> val) { this.keywords = val; return this; }
            public AIAnalysisResultBuilder reason(String val) { this.reason = val; return this; }

            public AIAnalysisResult build() {
                AIAnalysisResult result = new AIAnalysisResult();
                result.isRelevant = this.isRelevant;
                result.categoryMatch = this.categoryMatch;
                result.relevanceScore = this.relevanceScore;
                result.suggestedCategory = this.suggestedCategory;
                result.keywords = this.keywords != null ? this.keywords : List.of();
                result.reason = this.reason;
                return result;
            }
        }

        public boolean isRelevant() { return isRelevant; }
        public boolean isCategoryMatch() { return categoryMatch; }
        public double getRelevanceScore() { return relevanceScore; }
        public String getSuggestedCategory() { return suggestedCategory; }
        public List<String> getKeywords() { return keywords; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("AIAnalysisResult{relevant=%s, match=%s, score=%.2f, category='%s', keywords=%s}",
                isRelevant, categoryMatch, relevanceScore, suggestedCategory, keywords);
        }
    }
}
