package challkahthon.backend.hihigh.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerNews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 1000)
    private String title;
    
    @Column(length = 2000)
    private String thumbnailUrl;
    
    private String source;
    
    @Column(length = 2000)
    private String sourceUrl;
    
    private String category;
    
    private String keywords;
    
    // AI 분석 결과 필드들
    private Boolean isAiAnalyzed;
    
    private Boolean isRelevant;
    
    private Boolean categoryMatch;
    
    private Double relevanceScore;
    
    private String suggestedCategory;
    
    private String analysisReason;
    
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String originalContent;
    
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String translatedContent;
    
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String summary;
    
    private String language;
    
    private LocalDateTime publishedDate;
    
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}