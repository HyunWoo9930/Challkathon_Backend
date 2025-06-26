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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User targetUser;
    
    private String userInterests;
    
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String originalContent;
    
    private String language;
    
    private LocalDateTime publishedDate;
    
    private LocalDateTime createdAt;
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public boolean isGlobalNews() {
        return targetUser == null;
    }
}
