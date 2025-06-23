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
    private String summary;
    private LocalDateTime publishedDate;
    private LocalDateTime updatedAt;
    
    // Full content is only included in detailed view
    private String content;
    
    // AI 분석 결과 필드들
    private Boolean isAiAnalyzed;
    private Boolean isRelevant;
    private Boolean categoryMatch;
    private Double relevanceScore;
    private String suggestedCategory;
    private String analysisReason;
    
    /**
     * Convert entity to DTO (summary view without full content)
     */
    public static CareerNewsDto fromEntity(CareerNews news) {
        return CareerNewsDto.builder()
                .id(news.getId())
                .title(news.getTitle())
                .thumbnailUrl(news.getThumbnailUrl())
                .source(news.getSource())
                .sourceUrl(news.getSourceUrl())
                .category(news.getCategory())
                .keywords(news.getKeywords())
                .summary(news.getSummary())
                .publishedDate(news.getPublishedDate())
                .updatedAt(news.getUpdatedAt())
                .isAiAnalyzed(news.getIsAiAnalyzed())
                .isRelevant(news.getIsRelevant())
                .categoryMatch(news.getCategoryMatch())
                .relevanceScore(news.getRelevanceScore())
                .suggestedCategory(news.getSuggestedCategory())
                .analysisReason(news.getAnalysisReason())
                .build();
    }
    
    /**
     * Convert entity to DTO (detailed view with full content)
     */
    public static CareerNewsDto fromEntityWithContent(CareerNews news) {
        CareerNewsDto dto = fromEntity(news);
        // Use translated content if available, otherwise use original content
        String fullContent = (news.getTranslatedContent() != null && !news.getTranslatedContent().trim().isEmpty()) 
                ? news.getTranslatedContent() : news.getOriginalContent();
        dto.setContent(fullContent);
        return dto;
    }
    
    /**
     * Convert entity to DTO (AI analysis focused view)
     */
    public static CareerNewsDto fromEntityWithAIAnalysis(CareerNews news) {
        return CareerNewsDto.builder()
                .id(news.getId())
                .title(news.getTitle())
                .category(news.getCategory())
                .keywords(news.getKeywords())
                .isAiAnalyzed(news.getIsAiAnalyzed())
                .isRelevant(news.getIsRelevant())
                .categoryMatch(news.getCategoryMatch())
                .relevanceScore(news.getRelevanceScore())
                .suggestedCategory(news.getSuggestedCategory())
                .analysisReason(news.getAnalysisReason())
                .publishedDate(news.getPublishedDate())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
}