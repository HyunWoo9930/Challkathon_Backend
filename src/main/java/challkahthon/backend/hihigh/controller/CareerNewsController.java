package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.dto.CareerNewsDto;
import challkahthon.backend.hihigh.service.CareerNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/career-news")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "커리어 뉴스", description = "커리어 뉴스 조회 API")
public class CareerNewsController {

	private final CareerNewsService careerNewsService;

	@Operation(
		summary = "카테고리별 최신 뉴스 조회",
		description = "지정된 카테고리의 최신 뉴스를 조회합니다. 카테고리가 지정되지 않으면 모든 카테고리의 최신 뉴스를 반환합니다."
	)
	@GetMapping("/latest")
	public ResponseEntity<List<CareerNewsDto>> getLatestNews(
		@Parameter(description = "뉴스 카테고리 (frontend, backend, design, planning, devops)")
		@RequestParam(required = false) String category) {
		List<CareerNews> newsList = careerNewsService.getLatestNewsByCategory(category);
		List<CareerNewsDto> newsListDto = newsList.stream()
			.map(CareerNewsDto::fromEntity)
			.collect(Collectors.toList());
		return ResponseEntity.ok(newsListDto);
	}

	@Operation(
		summary = "뉴스 상세 조회",
		description = "지정된 ID의 뉴스 상세 정보를 조회합니다."
	)
	@GetMapping("/{id}")
	public ResponseEntity<?> getNewsById(
		@Parameter(description = "뉴스 ID")
		@PathVariable Long id) {
		CareerNews news = careerNewsService.getNewsById(id);
		if (news == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(CareerNewsDto.fromEntityWithContent(news));
	}
}
