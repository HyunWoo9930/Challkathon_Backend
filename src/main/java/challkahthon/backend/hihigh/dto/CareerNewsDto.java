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
    
    // 개인화 관련 필드
    private String targetUsername;
    private String userInterests;
    private Boolean isPersonalized;
    
    // AI 분석 결과
    private Boolean isAiAnalyzed;
    private Boolean isRelevant;
    private Double relevanceScore;
    private String suggestedCategory;
    private String analysisReason;
    
    // 콘텐츠
    private String translatedContent;
    private String summary;
    private String language;
    
    // 날짜
    private LocalDateTime publishedDate;
    private LocalDateTime createdAt;

    /**
     * Entity를 DTO로 변환 (콘텐츠 제외)
     */
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
                .targetUsername(entity.getTargetUser() != null ? entity.getTargetUser().getLoginId() : null)
                .userInterests(entity.getUserInterests())
                .isPersonalized(entity.getTargetUser() != null)
                .isAiAnalyzed(entity.getIsAiAnalyzed())
                .isRelevant(entity.getIsRelevant())
                .relevanceScore(entity.getRelevanceScore())
                .suggestedCategory(entity.getSuggestedCategory())
                .analysisReason(entity.getAnalysisReason())
                .translatedContent(entity.getTranslatedContent())
                .summary(entity.getSummary())
                .language(entity.getLanguage())
                .publishedDate(entity.getPublishedDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Entity를 DTO로 변환 (전체 콘텐츠 포함)
     */
    public static CareerNewsDto fromEntityWithContent(CareerNews entity) {
        CareerNewsDto dto = fromEntity(entity);
        if (dto != null && entity.getOriginalContent() != null) {
            // 원본 콘텐츠는 summary 필드에 저장 (너무 길 수 있으므로 제한)
            String content = entity.getOriginalContent();
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            dto.setSummary(content);
        }
        return dto;
    }
}
