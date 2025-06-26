package challkahthon.backend.hihigh.domain.entity;

import jakarta.persistence.*;
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
    
    // 사용자별 맞춤 뉴스를 위한 필드 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User targetUser;
    
    private String userInterests; // 이 뉴스가 매칭된 사용자 관심사
    
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
    
    // 전체 사용자 대상 뉴스인지 확인하는 메서드
    public boolean isGlobalNews() {
        return targetUser == null;
    }
}