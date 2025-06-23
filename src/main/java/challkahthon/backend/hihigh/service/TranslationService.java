package challkahthon.backend.hihigh.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.scheduling.annotation.Async;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TranslationService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${translation.microsoft.api-key}")
    private String microsoftApiKey;
    
    @Value("${translation.microsoft.region:koreacentral}")
    private String microsoftRegion;
    
    // 번역 API 제한사항 - 용량 문제 해결을 위해 축소
    private static final int MAX_TEXT_LENGTH = 2000; // 5000 → 2000으로 축소
    private static final int CHUNK_SIZE = 1500; // 4000 → 1500으로 축소
    
    /**
     * 제목을 한국어로 번역 (동기 - 빠른 응답용)
     */
    public String translateTitle(String title) {
        if (title == null || title.trim().isEmpty() || title.length() > 200) {
            return title;
        }
        
        try {
            return translateWithMicrosoft(title, "en", "ko");
        } catch (Exception e) {
            log.error("Title translation failed: {}", e.getMessage());
            return title;
        }
    }
    
    /**
     * 요약을 한국어로 번역 (동기 - 빠른 응답용)
     */
    public String translateSummary(String summary) {
        if (summary == null || summary.trim().isEmpty() || summary.length() > 500) {
            return summary;
        }
        
        try {
            return translateWithMicrosoft(summary, "en", "ko");
        } catch (Exception e) {
            log.error("Summary translation failed: {}", e.getMessage());
            return summary;
        }
    }
    
    /**
     * 비동기로 전체 본문을 한국어로 번역
     */
    @Async("translationTaskExecutor")
    public CompletableFuture<String> translateContentAsync(String content) {
        if (content == null || content.trim().isEmpty()) {
            return CompletableFuture.completedFuture(content);
        }
        
        try {
            log.info("Starting async translation for content (length: {})", content.length());
            
            // 긴 본문은 더 작은 청크로 분할하여 번역
            String translatedContent = translateWithChunking(content, "en", "ko");
            
            log.info("Async translation completed (length: {})", translatedContent.length());
            return CompletableFuture.completedFuture(translatedContent);
            
        } catch (Exception e) {
            log.error("Async content translation failed: {}", e.getMessage());
            return CompletableFuture.completedFuture(content); // 실패 시 원문 반환
        }
    }
    
    /**
     * 영어를 한국어로 번역 (기존 메서드 - 호환성 유지)
     */
    public String translateEnglishToKorean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        try {
            return translateWithChunking(text, "en", "ko");
        } catch (Exception e) {
            log.error("Translation failed: {}", e.getMessage());
            // 번역 실패 시 원문 반환
            return text;
        }
    }
    
    /**
     * 한국어를 영어로 번역 (긴 텍스트 청크 처리)
     */
    public String translateKoreanToEnglish(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        try {
            return translateWithChunking(text, "ko", "en");
        } catch (Exception e) {
            log.error("Translation failed: {}", e.getMessage());
            return text;
        }
    }
    
    /**
     * 자동 언어 감지 후 한국어로 번역 (긴 텍스트 청크 처리)
     */
    public String translateToKorean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        try {
            return translateWithChunking(text, "", "ko");
        } catch (Exception e) {
            log.error("Translation failed: {}", e.getMessage());
            return text;
        }
    }
    
    /**
     * 긴 텍스트를 청크로 나누어서 번역
     */
    private String translateWithChunking(String text, String source, String target) {
        // 텍스트가 제한 길이보다 짧으면 바로 번역
        if (text.length() <= MAX_TEXT_LENGTH) {
            return translateWithMicrosoft(text, source, target);
        }
        
        log.info("Text length ({}) exceeds limit ({}), splitting into chunks", text.length(), MAX_TEXT_LENGTH);
        
        // 긴 텍스트를 문장이나 문단 단위로 청크 분할
        List<String> chunks = splitTextIntoChunks(text, CHUNK_SIZE);
        
        StringBuilder translatedText = new StringBuilder();
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            log.debug("Translating chunk {}/{} (length: {})", i + 1, chunks.size(), chunk.length());
            
            try {
                String translatedChunk = translateWithMicrosoft(chunk, source, target);
                translatedText.append(translatedChunk);
                
                // 청크 간 약간의 간격 (문단 구분)
                if (i < chunks.size() - 1) {
                    translatedText.append(" ");
                }
                
                // API 요청 간격 (Rate Limiting 방지) - 용량 문제 완화를 위해 증가
                Thread.sleep(300); // 100ms → 300ms
                
            } catch (Exception e) {
                log.error("Failed to translate chunk {}: {}", i + 1, e.getMessage());
                // 실패한 청크는 원문 추가
                translatedText.append(chunk);
                if (i < chunks.size() - 1) {
                    translatedText.append(" ");
                }
            }
        }
        
        log.info("Successfully translated text with {} chunks", chunks.size());
        return translatedText.toString().trim();
    }
    
    /**
     * 텍스트를 의미있는 단위로 청크 분할
     */
    private List<String> splitTextIntoChunks(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        
        // 먼저 문단으로 분할 시도
        String[] paragraphs = text.split("\n\n");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            // 현재 문단을 추가했을 때 크기 초과하는지 확인
            if (currentChunk.length() + paragraph.length() + 2 > maxChunkSize) {
                // 현재 청크가 비어있지 않으면 저장
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                
                // 문단 자체가 너무 크면 문장으로 분할
                if (paragraph.length() > maxChunkSize) {
                    chunks.addAll(splitParagraphIntoSentences(paragraph, maxChunkSize));
                } else {
                    currentChunk.append(paragraph);
                }
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }
        
        // 마지막 청크 추가
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    /**
     * 문단을 문장 단위로 분할
     */
    private List<String> splitParagraphIntoSentences(String paragraph, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        
        // 문장 분할 (영어 기준, 한국어는 .!?로 구분)
        String[] sentences = paragraph.split("(?<=[.!?])\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() + 1 > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                
                // 문장 자체가 너무 크면 강제로 분할
                if (sentence.length() > maxChunkSize) {
                    chunks.addAll(forceSplitText(sentence, maxChunkSize));
                } else {
                    currentChunk.append(sentence);
                }
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    /**
     * 텍스트 강제 분할 (마지막 수단)
     */
    private List<String> forceSplitText(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        
        for (int i = 0; i < text.length(); i += maxChunkSize) {
            int endIndex = Math.min(i + maxChunkSize, text.length());
            
            // 단어 경계에서 분할하려고 시도
            if (endIndex < text.length()) {
                int lastSpaceIndex = text.lastIndexOf(' ', endIndex);
                if (lastSpaceIndex > i) {
                    endIndex = lastSpaceIndex;
                }
            }
            
            chunks.add(text.substring(i, endIndex).trim());
            
            // 단어 경계로 분할한 경우 인덱스 조정
            if (endIndex < text.length() && text.charAt(endIndex) == ' ') {
                i = endIndex;
            } else {
                i = endIndex - 1;
            }
        }
        
        return chunks;
    }
    
    /**
     * Microsoft Translator API를 사용한 번역
     */
    private String translateWithMicrosoft(String text, String source, String target) {
        try {
            // URL 구성 - source가 빈 값이면 자동 감지
            String url = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=" + target;
            if (!source.isEmpty()) {
                url += "&from=" + source;
            }
            
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Ocp-Apim-Subscription-Key", microsoftApiKey);
            headers.add("Ocp-Apim-Subscription-Region", microsoftRegion);
            
            // 요청 바디 설정 (배열 형태)
            Map<String, String> textObj = new HashMap<>();
            textObj.put("text", text);
            Object[] requestBody = new Object[]{textObj};
            
            HttpEntity<Object[]> request = new HttpEntity<>(requestBody, headers);
            
            log.debug("Translating text: '{}' from '{}' to '{}'", text, source.isEmpty() ? "auto" : source, target);
            
            // API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                if (jsonResponse.isArray() && jsonResponse.size() > 0) {
                    JsonNode translations = jsonResponse.get(0).get("translations");
                    if (translations.isArray() && translations.size() > 0) {
                        String translatedText = translations.get(0).get("text").asText();
                        log.debug("Translation successful: '{}'", translatedText);
                        return translatedText;
                    }
                }
            }
            
            throw new RuntimeException("Microsoft Translator API returned unexpected response");
            
        } catch (Exception e) {
            log.error("Microsoft Translator API call failed: {}", e.getMessage());
            throw new RuntimeException("Microsoft translation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 번역 서비스 상태 확인
     */
    public boolean isTranslationServiceAvailable() {
        try {
            // 간단한 테스트 번역으로 서비스 상태 확인
            String testResult = translateWithMicrosoft("Hello", "en", "ko");
            return testResult != null && !testResult.isEmpty() && !testResult.equals("Hello");
        } catch (Exception e) {
            log.warn("Translation service is not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Microsoft Translator 지원 언어 목록 조회
     */
    public Map<String, String> getSupportedLanguages() {
        Map<String, String> languages = new HashMap<>();
        // Microsoft Translator가 지원하는 주요 언어들
        languages.put("en", "English");
        languages.put("ko", "Korean");
        languages.put("ja", "Japanese");
        languages.put("zh", "Chinese (Simplified)");
        languages.put("zh-Hant", "Chinese (Traditional)");
        languages.put("es", "Spanish");
        languages.put("fr", "French");
        languages.put("de", "German");
        languages.put("it", "Italian");
        languages.put("pt", "Portuguese");
        languages.put("ru", "Russian");
        languages.put("ar", "Arabic");
        languages.put("hi", "Hindi");
        languages.put("th", "Thai");
        languages.put("vi", "Vietnamese");
        languages.put("id", "Indonesian");
        languages.put("ms", "Malay");
        languages.put("tl", "Filipino");
        return languages;
    }
    
    /**
     * API 키 유효성 검증
     */
    public boolean isApiKeyValid() {
        if (microsoftApiKey == null || microsoftApiKey.trim().isEmpty()) {
            log.error("Microsoft Translator API key is not configured");
            return false;
        }
        
        try {
            // 간단한 API 호출로 키 유효성 검증
            translateWithMicrosoft("test", "en", "ko");
            return true;
        } catch (Exception e) {
            log.error("Microsoft Translator API key validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 사용량 정보 조회 (로그로만 출력)
     */
    public void logUsageInfo() {
        log.info("Microsoft Translator API - Region: {}", microsoftRegion);
        log.info("Microsoft Translator API - Key configured: {}", !microsoftApiKey.isEmpty());
        log.info("Microsoft Translator API - Service available: {}", isTranslationServiceAvailable());
    }
}