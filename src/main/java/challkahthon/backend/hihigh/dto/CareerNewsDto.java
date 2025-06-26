package challkahthon.backend.hihigh.dto;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerNewsDto {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private String source;
    private String sourceUrl;
    private String category;
    private String keywords;

    private String targetUsername;
    private String userInterests;
    private Boolean isPersonalized;

    private String content;
    private String language;

    private LocalDateTime publishedDate;
    private LocalDateTime createdAt;

    public static CareerNewsDto fromEntity(CareerNews entity) {
        if (entity == null) {
            return null;
        }

        return CareerNewsDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .thumbnailUrl(entity.getThumbnailUrl())
                .source(entity.getSource())
                .sourceUrl(entity.getSourceUrl())
                .category(entity.getCategory())
                .keywords(entity.getKeywords())
                .targetUsername(null)
                .userInterests(entity.getUserInterests())
                .isPersonalized(entity.getUserInterests() != null && !entity.getUserInterests().trim().isEmpty())
                .language(entity.getLanguage())
                .publishedDate(entity.getPublishedDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static CareerNewsDto fromEntityWithContent(CareerNews entity) {
        CareerNewsDto dto = fromEntity(entity);
        if (dto != null && entity.getOriginalContent() != null) {
            String content = entity.getOriginalContent();
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            dto.setContent(content);
        }
        return dto;
    }
}
