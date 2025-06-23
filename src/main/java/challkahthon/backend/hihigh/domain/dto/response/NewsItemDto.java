package challkahthon.backend.hihigh.domain.dto.response;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsItemDto {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private String source;
    
    public static NewsItemDto fromEntity(CareerNews news) {
        return NewsItemDto.builder()
                .id(news.getId())
                .title(news.getTitle())
                .thumbnailUrl(news.getThumbnailUrl())
                .source(news.getSource())
                .build();
    }
}