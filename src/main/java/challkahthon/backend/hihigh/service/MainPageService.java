package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.dto.response.MainPageResponseDto;
import challkahthon.backend.hihigh.domain.dto.response.NewsItemDto;
import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainPageService {

    private final CareerNewsRepository careerNewsRepository;

    /**
     * Get news items for the main page, categorized by category
     * @return MainPageResponseDto containing news items by category
     */
    public MainPageResponseDto getMainPageNews() {
        // Get all available categories
        List<String> categories = List.of("frontend", "backend", "design", "planning", "devops");
        
        // Create a map to store news items by category
        Map<String, List<NewsItemDto>> newsByCategory = new HashMap<>();
        
        // For each category, get the latest news items
        for (String category : categories) {
            List<CareerNews> newsList = careerNewsRepository.findTop10ByCategoryOrderByPublishedDateDesc(category);
            List<NewsItemDto> newsItemDtos = newsList.stream()
                    .map(NewsItemDto::fromEntity)
                    .collect(Collectors.toList());
            newsByCategory.put(category, newsItemDtos);
        }
        
        // Create and return the response DTO
        return MainPageResponseDto.builder()
                .newsByCategory(newsByCategory)
                .build();
    }
}