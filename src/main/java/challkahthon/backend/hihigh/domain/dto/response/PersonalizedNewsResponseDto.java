package challkahthon.backend.hihigh.domain.dto.response;

import challkahthon.backend.hihigh.dto.CareerNewsDto;
import challkahthon.backend.hihigh.service.CareerNewsService.PersonalizedNewsStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedNewsResponseDto {
    private List<CareerNewsDto> personalizedNews;
    private List<CareerNewsDto> generalNews;
    private PersonalizedNewsStats stats;
    private String message;
    private boolean hasMorePersonalizedNews;
    private boolean hasMoreGeneralNews;
    
    public static PersonalizedNewsResponseDto of(
            List<CareerNewsDto> personalizedNews, 
            List<CareerNewsDto> generalNews,
            PersonalizedNewsStats stats) {
        return PersonalizedNewsResponseDto.builder()
                .personalizedNews(personalizedNews)
                .generalNews(generalNews)
                .stats(stats)
                .hasMorePersonalizedNews(personalizedNews.size() >= 20)
                .hasMoreGeneralNews(generalNews.size() >= 20)
                .message("맞춤 뉴스가 성공적으로 조회되었습니다.")
                .build();
    }
}
