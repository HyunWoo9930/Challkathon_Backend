package challkahthon.backend.hihigh.domain.dto.response;

import challkahthon.backend.hihigh.dto.CareerNewsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainPageResponseDto {
    private String name;
    private String message;
    
    // 사용자 맞춤 뉴스 (전체)
    private List<CareerNewsDto> personalizedNews;
    
    // 키워드별로 분류된 뉴스
    private Map<String, List<CareerNewsDto>> newsByKeyword;
    
    // 사용자 키워드 목록
    private List<String> userKeywords;
    
    // 맞춤 뉴스 존재 여부
    private Boolean hasPersonalizedNews;
    
    // 키워드별 뉴스 통계
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordNewsStats {
        private String keyword;
        private int newsCount;
        private double averageRelevanceScore;
    }
    
    // 키워드별 통계 정보
    private List<KeywordNewsStats> keywordStats;
}
