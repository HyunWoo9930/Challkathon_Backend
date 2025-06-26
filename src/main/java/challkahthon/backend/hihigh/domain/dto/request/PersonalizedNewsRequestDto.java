package challkahthon.backend.hihigh.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedNewsRequestDto {
    private String category;
    private String interest;
    private Integer size;
    private Double minRelevanceScore;
    private Boolean highRelevanceOnly;
    
    // 기본값 설정
    public Integer getSize() {
        return size != null ? size : 20;
    }
    
    public Double getMinRelevanceScore() {
        return minRelevanceScore != null ? minRelevanceScore : 0.3;
    }
    
    public Boolean getHighRelevanceOnly() {
        return highRelevanceOnly != null ? highRelevanceOnly : false;
    }
}
