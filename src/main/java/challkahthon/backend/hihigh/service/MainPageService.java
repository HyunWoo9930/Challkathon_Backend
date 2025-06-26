package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.dto.response.MainPageResponseDto;
import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.dto.CareerNewsDto;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import challkahthon.backend.hihigh.repository.UserRepository;
import challkahthon.backend.hihigh.utils.InterestParsingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainPageService {

    private final CareerNewsRepository careerNewsRepository;
    private final UserRepository userRepository;
    private final InterestParsingUtils interestParsingUtils;

    /**
     * 사용자가 입력한 관심사 키워드별로 뉴스 분류
     */
    public MainPageResponseDto getPersonalizedMainPageNews(String username) {
        if (username == null) {
            return MainPageResponseDto.builder()
                .name("Guest")
                .message("로그인하시면 관심사별 맞춤 뉴스를 제공해드립니다.")
                .personalizedNews(Collections.emptyList())
                .newsByKeyword(Collections.emptyMap())
                .userKeywords(Collections.emptyList())
                .keywordStats(Collections.emptyList())
                .hasPersonalizedNews(false)
                .build();
        }

        try {
            User user = userRepository.findByLoginId(username)
                    .orElse(null);

            if (user == null || user.getInterests() == null || user.getInterests().trim().isEmpty()) {
                return MainPageResponseDto.builder()
                    .name(username)
                    .message("관심사를 설정하시면 관심사별 맞춤 뉴스를 제공해드립니다.")
                    .personalizedNews(Collections.emptyList())
                    .newsByKeyword(Collections.emptyMap())
                    .userKeywords(Collections.emptyList())
                    .keywordStats(Collections.emptyList())
                    .hasPersonalizedNews(false)
                    .build();
            }

            // 사용자가 입력한 원본 관심사를 콤마로 분리
            List<String> userInputKeywords = parseUserInputKeywords(user.getInterests());
            
            // 맞춤 뉴스 조회 (더 많이 가져와서 키워드별로 분류)
            List<CareerNews> personalizedNews = careerNewsRepository.findPersonalizedNews(user, PageRequest.of(0, 100));

            // 관련성 점수 기준으로 필터링 (0.2 이상으로 낮춤)
            List<CareerNews> relevantNews = personalizedNews.stream()
                .filter(news -> news.getRelevanceScore() != null && news.getRelevanceScore() >= 0.2)
                .sorted((a, b) -> Double.compare(
                    b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0,
                    a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0
                ))
                .collect(Collectors.toList());

            // 사용자 입력 키워드별로 뉴스 분류
            Map<String, List<CareerNewsDto>> newsByKeyword = classifyNewsByUserKeywords(relevantNews, userInputKeywords);
            
            // 키워드별 통계 생성
            List<MainPageResponseDto.KeywordNewsStats> keywordStats = generateKeywordStats(newsByKeyword);

            // 전체 개인화 뉴스 (상위 15개)
            List<CareerNewsDto> personalizedNewsDto = relevantNews.stream()
                    .limit(15)
                    .map(CareerNewsDto::fromEntity)
                    .collect(Collectors.toList());

            // 총 뉴스 개수 계산
            int totalNewsCount = newsByKeyword.values().stream()
                    .mapToInt(List::size)
                    .sum();

            String message = personalizedNewsDto.isEmpty() ? 
                "아직 관심사에 맞는 뉴스가 수집되지 않았습니다. 잠시 후 다시 확인해주세요." :
                String.format("'%s' 관심사별로 총 %d개의 뉴스를 분류했습니다!", 
                    String.join(", ", userInputKeywords), 
                    totalNewsCount);

            return MainPageResponseDto.builder()
                .name(username)
                .message(message)
                // .personalizedNews(personalizedNewsDto)
                .newsByKeyword(newsByKeyword)
                .userKeywords(userInputKeywords)
                .keywordStats(keywordStats)
                .hasPersonalizedNews(!personalizedNewsDto.isEmpty())
                .build();

        } catch (Exception e) {
            log.error("맞춤 메인페이지 뉴스 조회 실패: {}", e.getMessage());
            return MainPageResponseDto.builder()
                .name(username)
                .message("뉴스를 불러오는 중 문제가 발생했습니다.")
                .personalizedNews(Collections.emptyList())
                .newsByKeyword(Collections.emptyMap())
                .userKeywords(Collections.emptyList())
                .keywordStats(Collections.emptyList())
                .hasPersonalizedNews(false)
                .build();
        }
    }

    /**
     * 사용자가 입력한 관심사 문자열을 키워드 리스트로 파싱
     * 예: "디자인, 마케팅, 개발" -> ["디자인", "마케팅", "개발"]
     */
    private List<String> parseUserInputKeywords(String interests) {
        if (interests == null || interests.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(interests.split("[,，]")) // 콤마와 한글 콤마로 분리
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 사용자가 입력한 키워드별로 뉴스 분류
     */
    private Map<String, List<CareerNewsDto>> classifyNewsByUserKeywords(List<CareerNews> newsList, List<String> userKeywords) {
        Map<String, List<CareerNewsDto>> newsByKeyword = new LinkedHashMap<>();
        Set<Long> usedNewsIds = new HashSet<>(); // 중복 방지용
        
        // 각 사용자 입력 키워드별로 관련 뉴스 찾기
        for (String keyword : userKeywords) {
            List<CareerNewsDto> keywordNews = new ArrayList<>();
            
            for (CareerNews news : newsList) {
                // 이미 다른 키워드에 사용된 뉴스는 제외
                if (usedNewsIds.contains(news.getId())) {
                    continue;
                }
                
                if (isNewsRelatedToUserKeyword(news, keyword)) {
                    CareerNewsDto newsDto = CareerNewsDto.fromEntity(news);
                    keywordNews.add(newsDto);
                    usedNewsIds.add(news.getId()); // 중복 방지를 위해 추가
                }
            }
            
            // 관련성 점수순으로 정렬하고 상위 8개만 선택
            keywordNews = keywordNews.stream()
                    .sorted((a, b) -> Double.compare(
                        b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0,
                        a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0
                    ))
                    .limit(8)
                    .collect(Collectors.toList());
            
            if (!keywordNews.isEmpty()) {
                newsByKeyword.put(keyword, keywordNews);
            }
        }
        
        return newsByKeyword;
    }

    /**
     * 뉴스가 사용자가 입력한 특정 키워드와 관련있는지 확인
     */
    private boolean isNewsRelatedToUserKeyword(CareerNews news, String userKeyword) {
        String content = (news.getTitle() + " " + 
                         (news.getTranslatedContent() != null ? news.getTranslatedContent() : "") + " " +
                         (news.getOriginalContent() != null ? news.getOriginalContent() : "")).toLowerCase();
        
        String lowerKeyword = userKeyword.toLowerCase().trim();
        
        // 1. 직접 매칭 (한글/영문 모두)
        if (content.contains(lowerKeyword)) {
            return true;
        }
        
        // 2. 사용자 키워드별 확장 매칭
        return checkUserKeywordVariations(content, lowerKeyword);
    }

    /**
     * 사용자 키워드의 다양한 변형으로 매칭 확인
     */
    private boolean checkUserKeywordVariations(String content, String userKeyword) {
        // 사용자가 입력할 수 있는 키워드들의 영문/한글 변형들
        Map<String, String[]> keywordVariations = new HashMap<>();
        
        // 한글 키워드의 영문 변형
        keywordVariations.put("디자인", new String[]{"design", "designer", "designing", "visual design", "graphic design", "ui", "ux"});
        keywordVariations.put("마케팅", new String[]{"marketing", "market", "promotion", "advertising", "campaign", "digital marketing", "seo"});
        keywordVariations.put("개발", new String[]{"development", "developer", "programming", "coding", "software", "web development", "app development"});
        keywordVariations.put("기획", new String[]{"planning", "plan", "strategy", "product management", "project management", "pm"});
        keywordVariations.put("데이터", new String[]{"data", "analytics", "analysis", "database", "big data", "data science"});
        keywordVariations.put("ai", new String[]{"artificial intelligence", "machine learning", "deep learning", "인공지능", "머신러닝"});
        keywordVariations.put("it", new String[]{"information technology", "tech", "technology", "정보기술"});
        
        // 영문 키워드의 한글 변형 및 확장
        keywordVariations.put("design", new String[]{"디자인", "디자이너", "ui", "ux", "visual", "graphic"});
        keywordVariations.put("marketing", new String[]{"마케팅", "광고", "홍보", "프로모션"});
        keywordVariations.put("development", new String[]{"개발", "개발자", "프로그래밍", "코딩"});
        keywordVariations.put("ui", new String[]{"user interface", "사용자 인터페이스", "인터페이스", "디자인"});
        keywordVariations.put("ux", new String[]{"user experience", "사용자 경험", "경험", "디자인"});
        keywordVariations.put("frontend", new String[]{"프론트엔드", "front-end", "client-side", "ui"});
        keywordVariations.put("backend", new String[]{"백엔드", "back-end", "server-side", "서버"});
        
        String[] variations = keywordVariations.get(userKeyword);
        if (variations != null) {
            for (String variation : variations) {
                if (content.contains(variation.toLowerCase())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 키워드별 통계 생성
     */
    private List<MainPageResponseDto.KeywordNewsStats> generateKeywordStats(Map<String, List<CareerNewsDto>> newsByKeyword) {
        return newsByKeyword.entrySet().stream()
                .map(entry -> {
                    String keyword = entry.getKey();
                    List<CareerNewsDto> newsList = entry.getValue();
                    
                    double avgScore = newsList.stream()
                            .filter(news -> news.getRelevanceScore() != null)
                            .mapToDouble(CareerNewsDto::getRelevanceScore)
                            .average()
                            .orElse(0.0);
                    
                    return MainPageResponseDto.KeywordNewsStats.builder()
                            .keyword(keyword)
                            .newsCount(newsList.size())
                            .averageRelevanceScore(Math.round(avgScore * 100.0) / 100.0)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getNewsCount(), a.getNewsCount())) // 뉴스 개수 순으로 정렬
                .collect(Collectors.toList());
    }

    /**
     * 관심사 키워드 기반 뉴스 검색 (관리자용)
     */
    public List<CareerNewsDto> searchNewsByInterests(String username, int limit) {
        try {
            User user = userRepository.findByLoginId(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

            if (user.getInterests() == null || user.getInterests().trim().isEmpty()) {
                return Collections.emptyList();
            }

            // 관심사 키워드로 뉴스 검색
            List<String> keywords = interestParsingUtils.parseInterestsToKeywords(user.getInterests());
            List<CareerNews> relevantNews = new ArrayList<>();

            for (String keyword : keywords.subList(0, Math.min(keywords.size(), 3))) { // 상위 3개 키워드만
                List<CareerNews> keywordNews = careerNewsRepository.findByUserAndInterestContaining(
                    user, keyword, PageRequest.of(0, limit / 3 + 1));
                relevantNews.addAll(keywordNews);
            }

            // 중복 제거 및 관련성 점수 순 정렬
            return relevantNews.stream()
                    .distinct()
                    .sorted((a, b) -> Double.compare(
                        b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0,
                        a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0
                    ))
                    .limit(limit)
                    .map(CareerNewsDto::fromEntity)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("관심사 기반 뉴스 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 특정 사용자 입력 키워드의 뉴스만 조회
     */
    public MainPageResponseDto getNewsBySpecificKeyword(String username, String keyword, int limit) {
        try {
            User user = userRepository.findByLoginId(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

            // 해당 키워드 관련 뉴스 조회
            List<CareerNews> personalizedNews = careerNewsRepository.findPersonalizedNews(user, PageRequest.of(0, limit * 2));
            
            List<CareerNews> keywordNews = personalizedNews.stream()
                    .filter(news -> isNewsRelatedToUserKeyword(news, keyword))
                    .limit(limit)
                    .collect(Collectors.toList());

            List<CareerNewsDto> keywordNewsDto = keywordNews.stream()
                    .filter(news -> news.getRelevanceScore() != null && news.getRelevanceScore() >= 0.2)
                    .sorted((a, b) -> Double.compare(
                        b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0,
                        a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0
                    ))
                    .map(CareerNewsDto::fromEntity)
                    .collect(Collectors.toList());

            Map<String, List<CareerNewsDto>> newsByKeyword = new HashMap<>();
            newsByKeyword.put(keyword, keywordNewsDto);

            return MainPageResponseDto.builder()
                    .name(username)
                    .message(String.format("'%s' 관심사 관련 뉴스 %d개", keyword, keywordNewsDto.size()))
                    .personalizedNews(keywordNewsDto)
                    .newsByKeyword(newsByKeyword)
                    .userKeywords(Arrays.asList(keyword))
                    .hasPersonalizedNews(!keywordNewsDto.isEmpty())
                    .build();

        } catch (Exception e) {
            log.error("키워드별 뉴스 조회 실패: {}", e.getMessage());
            return MainPageResponseDto.builder()
                    .name(username)
                    .message("뉴스 조회 중 오류가 발생했습니다.")
                    .personalizedNews(Collections.emptyList())
                    .newsByKeyword(Collections.emptyMap())
                    .userKeywords(Collections.emptyList())
                    .hasPersonalizedNews(false)
                    .build();
        }
    }
}
