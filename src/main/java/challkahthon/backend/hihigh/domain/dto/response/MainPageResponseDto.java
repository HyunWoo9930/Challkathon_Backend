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
    private Map<String, List<NewsItemDto>> newsByCategory;
    
    // 사용자 맞춤 뉴스 관련 필드 추가
    private List<CareerNewsDto> personalizedNews;
    private Boolean hasPersonalizedNews;
    private String personalizedNewsMessage;
    
    // 기존 생성자와의 호환성을 위한 편의 메서드
    public static MainPageResponseDto of(String name, Map<String, List<NewsItemDto>> newsByCategory) {
        return MainPageResponseDto.builder()
                .name(name)
                .newsByCategory(newsByCategory)
                .hasPersonalizedNews(false)
                .build();
    }
    
    public static MainPageResponseDto withPersonalized(
            String name, 
            Map<String, List<NewsItemDto>> newsByCategory,
            List<CareerNewsDto> personalizedNews) {
        return MainPageResponseDto.builder()
                .name(name)
                .newsByCategory(newsByCategory)
                .personalizedNews(personalizedNews)
                .hasPersonalizedNews(personalizedNews != null && !personalizedNews.isEmpty())
                .personalizedNewsMessage(personalizedNews != null && !personalizedNews.isEmpty() 
                    ? "당신의 관심사에 맞는 뉴스를 찾았습니다!" 
                    : "관심사를 설정하시면 맞춤 뉴스를 제공해드립니다.")
                .build();
    }
}
