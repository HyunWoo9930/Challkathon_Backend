package challkahthon.backend.hihigh.domain.dto.response;

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
}