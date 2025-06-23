package challkahthon.backend.hihigh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAnalysisService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * AI를 사용해서 기사가 특정 카테고리에 적합한지 분석하고 키워드를 추출합니다.
     */
    public AIAnalysisResult analyzeArticle(String title, String content, String targetCategory) {
        try {
            log.info("AI 분석 시작 - 카테고리: {}, 제목: {}", targetCategory, 
                    title.substring(0, Math.min(50, title.length())));
            
            String analysisPrompt = createAnalysisPrompt(title, content, targetCategory);
            
            String aiResponse = chatClient.prompt()
                    .user(analysisPrompt)
                    .call()
                    .content();
            
            log.debug("AI 응답: {}", aiResponse);
            
            return parseAIResponse(aiResponse, targetCategory);
            
        } catch (Exception e) {
            log.error("AI 분석 중 오류 발생: {}", e.getMessage(), e);
            // 오류 발생 시 기본값 반환
            return AIAnalysisResult.builder()
                    .isRelevant(true) // 기본적으로 관련있다고 가정
                    .categoryMatch(false)
                    .relevanceScore(0.5)
                    .keywords(List.of())
                    .suggestedCategory(targetCategory)
                    .reason("AI 분석 오류로 인한 기본값")
                    .build();
        }
    }
    
    /**
     * 기사 내용만으로 카테고리를 자동 분류합니다.
     */
    public String autoClassifyCategory(String title, String content) {
        try {
            log.info("카테고리 자동 분류 시작 - 제목: {}", 
                    title.substring(0, Math.min(50, title.length())));
            
            String classificationPrompt = createClassificationPrompt(title, content);
            
            String aiResponse = chatClient.prompt()
                    .user(classificationPrompt)
                    .call()
                    .content()
                    .trim();
            
            log.debug("카테고리 분류 결과: {}", aiResponse);
            
            // 유효한 카테고리인지 확인
            List<String> validCategories = List.of("frontend", "backend", "design", "planning", "devops", "general");
            String category = aiResponse.toLowerCase();
            
            if (validCategories.contains(category)) {
                return category;
            } else {
                log.warn("알 수 없는 카테고리 반환: {}, general로 설정", aiResponse);
                return "general";
            }
            
        } catch (Exception e) {
            log.error("카테고리 자동 분류 중 오류 발생: {}", e.getMessage(), e);
            return "general"; // 오류 시 기본 카테고리
        }
    }
    
    /**
     * 키워드만 추출합니다.
     */
    public List<String> extractKeywords(String title, String content) {
        try {
            log.info("키워드 추출 시작");
            
            String keywordPrompt = createKeywordPrompt(title, content);
            
            String aiResponse = chatClient.prompt()
                    .user(keywordPrompt)
                    .call()
                    .content();
            
            log.debug("키워드 추출 결과: {}", aiResponse);
            
            return parseKeywords(aiResponse);
            
        } catch (Exception e) {
            log.error("키워드 추출 중 오류 발생: {}", e.getMessage(), e);
            return List.of(); // 오류 시 빈 리스트
        }
    }
    
    private String createAnalysisPrompt(String title, String content, String targetCategory) {
        return String.format("""
            다음 기사를 분석해서 JSON 형태로 응답해주세요.
            
            **분석할 기사:**
            제목: %s
            내용: %s
            
            **타겟 카테고리: %s**
            
            다음 기준으로 분석해주세요:
            
            1. **카테고리 분류 기준:**
            - frontend: JavaScript, React, Vue, Angular, HTML, CSS, UI 개발, 웹 프론트엔드 관련
            - backend: 서버 개발, API, 데이터베이스, Spring, Node.js, Python, Java 서버 관련
            - design: UI/UX 디자인, 그래픽 디자인, 사용자 경험, 디자인 도구, 프로토타이핑
            - planning: 프로덕트 매니저, 기획, 요구사항 분석, 로드맵, 프로젝트 관리
            - devops: 클라우드, 인프라, CI/CD, 배포, 모니터링, Docker, Kubernetes
            - general: 위 카테고리에 속하지 않는 일반적인 개발/커리어 관련
            
            2. **응답 형식 (반드시 유효한 JSON):**
            {
                "isRelevant": true/false,
                "categoryMatch": true/false,
                "relevanceScore": 0.0-1.0,
                "suggestedCategory": "가장 적합한 카테고리",
                "keywords": ["키워드1", "키워드2", "키워드3", "키워드4", "키워드5"],
                "reason": "판단 근거"
            }
            
            **중요:** 반드시 유효한 JSON만 응답하고, 추가 설명은 하지 마세요.
            """, title, content.substring(0, Math.min(1500, content.length())), targetCategory);
    }
    
    private String createClassificationPrompt(String title, String content) {
        return String.format("""
            다음 기사의 내용을 분석해서 가장 적합한 카테고리 하나만 응답해주세요.
            
            **기사:**
            제목: %s
            내용: %s
            
            **카테고리 옵션:**
            - frontend: JavaScript, React, Vue, Angular, HTML, CSS, UI 개발 관련
            - backend: 서버 개발, API, 데이터베이스, 백엔드 기술 관련  
            - design: UI/UX 디자인, 그래픽 디자인, 사용자 경험 관련
            - planning: 프로덕트 매니저, 기획, 요구사항 분석, 프로젝트 관리 관련
            - devops: 클라우드, 인프라, CI/CD, 배포, 운영 관련
            - general: 위에 해당하지 않는 일반적인 개발/커리어 관련
            
            **응답:** 카테고리명 하나만 소문자로 응답하세요 (예: frontend)
            """, title, content.substring(0, Math.min(1000, content.length())));
    }
    
    private String createKeywordPrompt(String title, String content) {
        return String.format("""
            다음 기사에서 개발/커리어와 관련된 중요한 키워드 5개를 추출해주세요.
            
            **기사:**
            제목: %s
            내용: %s
            
            **키워드 추출 기준:**
            - 기술 스택, 프로그래밍 언어, 프레임워크
            - 개발 방법론, 도구, 플랫폼
            - 직무, 스킬, 커리어 관련 용어
            - 트렌드, 업계 용어
            
            **응답 형식:** 
            키워드1, 키워드2, 키워드3, 키워드4, 키워드5
            
            (쉼표로 구분된 키워드만 응답하고, 다른 설명은 하지 마세요)
            """, title, content.substring(0, Math.min(1000, content.length())));
    }
    
    private AIAnalysisResult parseAIResponse(String aiResponse, String targetCategory) {
        try {
            // JSON 파싱 시도
            JsonNode jsonNode = objectMapper.readTree(aiResponse);
            
            return AIAnalysisResult.builder()
                    .isRelevant(jsonNode.get("isRelevant").asBoolean(true))
                    .categoryMatch(jsonNode.get("categoryMatch").asBoolean(false))
                    .relevanceScore(jsonNode.get("relevanceScore").asDouble(0.5))
                    .suggestedCategory(jsonNode.get("suggestedCategory").asText(targetCategory))
                    .keywords(parseJsonKeywords(jsonNode.get("keywords")))
                    .reason(jsonNode.get("reason").asText("AI 분석 완료"))
                    .build();
                    
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패, 텍스트 파싱 시도: {}", e.getMessage());
            return parseTextResponse(aiResponse, targetCategory);
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
    
    private AIAnalysisResult parseTextResponse(String response, String targetCategory) {
        // JSON 파싱 실패 시 텍스트에서 정보 추출
        String lowerResponse = response.toLowerCase();
        
        boolean isRelevant = !lowerResponse.contains("not relevant") && 
                           !lowerResponse.contains("false");
        
        boolean categoryMatch = lowerResponse.contains("match") || 
                              lowerResponse.contains("appropriate");
        
        return AIAnalysisResult.builder()
                .isRelevant(isRelevant)
                .categoryMatch(categoryMatch)
                .relevanceScore(0.6) // 기본값
                .suggestedCategory(targetCategory)
                .keywords(extractKeywordsFromText(response))
                .reason("텍스트 파싱 결과")
                .build();
    }
    
    private List<String> parseKeywords(String response) {
        List<String> keywords = new ArrayList<>();
        
        // 쉼표로 구분된 키워드 파싱
        String[] parts = response.split(",");
        for (String part : parts) {
            String keyword = part.trim().replaceAll("[\"'\\[\\]]", "");
            if (!keyword.isEmpty() && keyword.length() > 1) {
                keywords.add(keyword);
            }
        }
        
        // 최대 5개로 제한
        return keywords.subList(0, Math.min(keywords.size(), 5));
    }
    
    private List<String> extractKeywordsFromText(String text) {
        // 간단한 키워드 추출 로직
        List<String> keywords = new ArrayList<>();
        String[] commonTechKeywords = {
            "javascript", "react", "vue", "angular", "python", "java", 
            "spring", "node.js", "database", "api", "frontend", "backend",
            "ui", "ux", "design", "devops", "cloud", "docker"
        };
        
        String lowerText = text.toLowerCase();
        for (String keyword : commonTechKeywords) {
            if (lowerText.contains(keyword)) {
                keywords.add(keyword);
                if (keywords.size() >= 5) break;
            }
        }
        
        return keywords;
    }
    
    /**
     * AI 분석 결과를 담는 클래스
     */
    public static class AIAnalysisResult {
        private boolean isRelevant;          // 개발/커리어 관련 기사인지
        private boolean categoryMatch;        // 타겟 카테고리와 일치하는지
        private double relevanceScore;        // 관련도 점수 (0.0 ~ 1.0)
        private String suggestedCategory;     // AI가 제안하는 카테고리
        private List<String> keywords;        // 추출된 키워드들
        private String reason;               // 판단 근거
        
        // Builder 패턴
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
            
            public AIAnalysisResultBuilder isRelevant(boolean isRelevant) {
                this.isRelevant = isRelevant;
                return this;
            }
            
            public AIAnalysisResultBuilder categoryMatch(boolean categoryMatch) {
                this.categoryMatch = categoryMatch;
                return this;
            }
            
            public AIAnalysisResultBuilder relevanceScore(double relevanceScore) {
                this.relevanceScore = relevanceScore;
                return this;
            }
            
            public AIAnalysisResultBuilder suggestedCategory(String suggestedCategory) {
                this.suggestedCategory = suggestedCategory;
                return this;
            }
            
            public AIAnalysisResultBuilder keywords(List<String> keywords) {
                this.keywords = keywords;
                return this;
            }
            
            public AIAnalysisResultBuilder reason(String reason) {
                this.reason = reason;
                return this;
            }
            
            public AIAnalysisResult build() {
                AIAnalysisResult result = new AIAnalysisResult();
                result.isRelevant = this.isRelevant;
                result.categoryMatch = this.categoryMatch;
                result.relevanceScore = this.relevanceScore;
                result.suggestedCategory = this.suggestedCategory;
                result.keywords = this.keywords != null ? this.keywords : new ArrayList<>();
                result.reason = this.reason;
                return result;
            }
        }
        
        // Getters
        public boolean isRelevant() { return isRelevant; }
        public boolean isCategoryMatch() { return categoryMatch; }
        public double getRelevanceScore() { return relevanceScore; }
        public String getSuggestedCategory() { return suggestedCategory; }
        public List<String> getKeywords() { return keywords; }
        public String getReason() { return reason; }
        
        @Override
        public String toString() {
            return String.format("AIAnalysisResult{relevant=%s, categoryMatch=%s, score=%.2f, category='%s', keywords=%s}", 
                    isRelevant, categoryMatch, relevanceScore, suggestedCategory, keywords);
        }
    }
}
