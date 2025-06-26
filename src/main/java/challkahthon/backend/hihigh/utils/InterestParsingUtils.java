package challkahthon.backend.hihigh.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InterestParsingUtils {
    
    private static final Map<String, List<String>> TECH_STACK_KEYWORDS = Map.of(
        "frontend", Arrays.asList("react", "vue", "angular", "javascript", "typescript", "css", "html", "웹개발", "프론트엔드"),
        "backend", Arrays.asList("spring", "java", "python", "django", "flask", "node.js", "express", "api", "서버개발", "백엔드"),
        "mobile", Arrays.asList("android", "ios", "flutter", "react native", "swift", "kotlin", "모바일개발", "앱개발"),
        "devops", Arrays.asList("docker", "kubernetes", "aws", "azure", "gcp", "jenkins", "ci/cd", "데브옵스", "클라우드"),
        "data", Arrays.asList("data science", "machine learning", "ai", "python", "r", "sql", "bigdata", "데이터분석", "인공지능"),
        "design", Arrays.asList("ui", "ux", "figma", "photoshop", "design", "디자인", "기획")
    );
    
    private static final Map<String, List<String>> INDUSTRY_KEYWORDS = Map.of(
        "fintech", Arrays.asList("fintech", "금융", "핀테크", "blockchain", "cryptocurrency", "payment"),
        "ecommerce", Arrays.asList("e-commerce", "이커머스", "쇼핑", "retail", "온라인쇼핑"),
        "healthcare", Arrays.asList("healthcare", "의료", "헬스케어", "바이오", "medical"),
        "education", Arrays.asList("education", "edtech", "교육", "에듀테크", "learning"),
        "gaming", Arrays.asList("game", "gaming", "게임", "unity", "unreal"),
        "startup", Arrays.asList("startup", "스타트업", "창업", "venture")
    );
    
    private static final Map<String, List<String>> CAREER_LEVEL_KEYWORDS = Map.of(
        "junior", Arrays.asList("신입", "주니어", "junior", "entry", "fresher", "new grad"),
        "senior", Arrays.asList("시니어", "senior", "experienced", "lead", "principal"),
        "management", Arrays.asList("매니저", "manager", "lead", "team lead", "cto", "ceo")
    );
    
    public List<String> parseInterestsToKeywords(String interests) {
        if (interests == null || interests.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<String> keywords = new HashSet<>();
        String normalizedInterests = interests.toLowerCase().trim();
        
        List<String> interestItems = Arrays.stream(normalizedInterests.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        for (String item : interestItems) {
            keywords.add(item);
            addMappedKeywords(keywords, item, TECH_STACK_KEYWORDS);
            addMappedKeywords(keywords, item, INDUSTRY_KEYWORDS);
            addMappedKeywords(keywords, item, CAREER_LEVEL_KEYWORDS);
        }
        
        log.debug("Parsed interests '{}' to keywords: {}", interests, keywords);
        return new ArrayList<>(keywords);
    }
    
    public Set<String> categorizeInterests(String interests) {
        if (interests == null || interests.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<String> categories = new HashSet<>();
        String normalizedInterests = interests.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : TECH_STACK_KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(normalizedInterests::contains)) {
                categories.add(entry.getKey());
            }
        }
        
        for (Map.Entry<String, List<String>> entry : INDUSTRY_KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(normalizedInterests::contains)) {
                categories.add(entry.getKey());
            }
        }
        
        return categories;
    }
    
    public List<String> generateSearchQueries(String interests) {
        List<String> keywords = parseInterestsToKeywords(interests);
        List<String> queries = new ArrayList<>();
        
        for (String keyword : keywords) {
            queries.add(keyword + " developer");
            queries.add(keyword + " career");
            queries.add(keyword + " job");
            queries.add(keyword + " 개발자");
            queries.add(keyword + " 채용");
        }
        
        if (keywords.size() >= 2) {
            for (int i = 0; i < keywords.size() - 1; i++) {
                queries.add(keywords.get(i) + " " + keywords.get(i + 1));
            }
        }
        
        return queries.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }
    
    public double calculateRelevanceScore(String newsContent, String userInterests) {
        if (newsContent == null || userInterests == null) {
            return 0.0;
        }
        
        String normalizedContent = newsContent.toLowerCase();
        List<String> keywords = parseInterestsToKeywords(userInterests);
        
        int matchCount = 0;
        int totalKeywords = keywords.size();
        
        for (String keyword : keywords) {
            if (normalizedContent.contains(keyword.toLowerCase())) {
                matchCount++;
            }
        }
        
        double baseScore = totalKeywords > 0 ? (double) matchCount / totalKeywords : 0.0;
        double weightedScore = baseScore;
        
        if (normalizedContent.length() > 100) {
            String titlePart = normalizedContent.substring(0, Math.min(100, normalizedContent.length()));
            for (String keyword : keywords) {
                if (titlePart.contains(keyword.toLowerCase())) {
                    weightedScore += 0.1;
                }
            }
        }
        
        return Math.min(1.0, weightedScore);
    }
    
    private void addMappedKeywords(Set<String> keywords, String item, Map<String, List<String>> mapping) {
        for (List<String> mappedKeywords : mapping.values()) {
            if (mappedKeywords.contains(item)) {
                keywords.addAll(mappedKeywords);
                break;
            }
        }
    }
}
