package challkahthon.backend.hihigh.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import challkahthon.backend.hihigh.domain.dto.response.MainPageResponseDto;
import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.dto.CareerNewsDto;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import challkahthon.backend.hihigh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainPageService {

	private final CareerNewsRepository careerNewsRepository;
	private final UserRepository userRepository;

	public MainPageResponseDto getPersonalizedMainPageNews(String username) {
		if (username == null) {
			return MainPageResponseDto.builder()
				.name("Guest")
				.message("로그인하시면 관심사별 맞춤 뉴스를 제공해드립니다.")
				.personalizedNews(Collections.emptyList())
				.newsByKeyword(Collections.emptyMap())
				.userKeywords(Collections.emptyList())
				.keywordStats(Collections.emptyList())
				.hasPersonalizedNews(false)
				.build();
		}

		try {
			User user = userRepository.findByLoginId(username)
				.orElse(null);

			if (user == null || user.getInterests() == null || user.getInterests().trim().isEmpty()) {
				return MainPageResponseDto.builder()
					.name(username)
					.message("관심사를 설정하시면 관심사별 맞춤 뉴스를 제공해드립니다.")
					.personalizedNews(Collections.emptyList())
					.newsByKeyword(Collections.emptyMap())
					.userKeywords(Collections.emptyList())
					.keywordStats(Collections.emptyList())
					.hasPersonalizedNews(false)
					.build();
			}

			List<String> userInputKeywords = parseUserInputKeywords(user.getInterests());

			List<CareerNews> allNews = careerNewsRepository.findPersonalizedNews(PageRequest.of(0, 50));

			Map<String, List<CareerNewsDto>> newsByKeyword = classifyNewsByUserKeywords(userInputKeywords);

			List<MainPageResponseDto.KeywordNewsStats> keywordStats = generateKeywordStats(newsByKeyword);

			List<CareerNewsDto> personalizedNewsDto = allNews.stream()
				.limit(15)
				.map(CareerNewsDto::fromEntity)
				.collect(Collectors.toList());

			int totalNewsCount = newsByKeyword.values().stream()
				.mapToInt(List::size)
				.sum();

			String message = personalizedNewsDto.isEmpty() ?
				"아직 관심사에 맞는 뉴스가 수집되지 않았습니다. 잠시 후 다시 확인해주세요." :
				String.format("'%s' 관심사별로 총 %d개의 뉴스를 분류했습니다!",
					String.join(", ", userInputKeywords),
					totalNewsCount);

			return MainPageResponseDto.builder()
				.name(username)
				.message(message)
				.newsByKeyword(newsByKeyword)
				.userKeywords(userInputKeywords)
				.keywordStats(keywordStats)
				.hasPersonalizedNews(!personalizedNewsDto.isEmpty())
				.build();

		} catch (Exception e) {
			log.error("맞춤 메인페이지 뉴스 조회 실패: {}", e.getMessage());
			return MainPageResponseDto.builder()
				.name(username)
				.message("뉴스를 불러오는 중 문제가 발생했습니다.")
				.personalizedNews(Collections.emptyList())
				.newsByKeyword(Collections.emptyMap())
				.userKeywords(Collections.emptyList())
				.keywordStats(Collections.emptyList())
				.hasPersonalizedNews(false)
				.build();
		}
	}

	private List<String> parseUserInputKeywords(String interests) {
		if (interests == null || interests.trim().isEmpty()) {
			return Collections.emptyList();
		}

		return Arrays.stream(interests.split("[,，]"))
			.map(String::trim)
			.filter(keyword -> !keyword.isEmpty())
			.collect(Collectors.toList());
	}

	private Map<String, List<CareerNewsDto>> classifyNewsByUserKeywords(List<String> userKeywords) {
		Map<String, List<CareerNewsDto>> newsByKeyword = new LinkedHashMap<>();
		Set<Long> usedNewsIds = new HashSet<>();

		for (String keyword : userKeywords) {
			List<CareerNewsDto> keywordNews = new ArrayList<>();

			List<CareerNews> newsList = careerNewsRepository.findByUserInterests(keyword, PageRequest.of(0, 50));

			for (CareerNews news : newsList) {
				if (usedNewsIds.contains(news.getId())) {
					continue;
				}

				if (isNewsRelatedToUserKeyword(news, keyword)) {
					CareerNewsDto newsDto = CareerNewsDto.fromEntity(news);
					keywordNews.add(newsDto);
					usedNewsIds.add(news.getId());
				}
			}

			keywordNews = keywordNews.stream()
				.limit(8)
				.collect(Collectors.toList());

			if (!keywordNews.isEmpty()) {
				newsByKeyword.put(keyword, keywordNews);
			}
		}

		return newsByKeyword;
	}

	private boolean isNewsRelatedToUserKeyword(CareerNews news, String userKeyword) {
		String content = (news.getTitle() + " " +
			(news.getOriginalContent() != null ? news.getOriginalContent() : "")).toLowerCase();

		String lowerKeyword = userKeyword.toLowerCase().trim();

		if (content.contains(lowerKeyword)) {
			return true;
		}

		return checkUserKeywordVariations(content, lowerKeyword);
	}

	private boolean checkUserKeywordVariations(String content, String userKeyword) {
		Map<String, String[]> keywordVariations = new HashMap<>();

		keywordVariations.put("디자인", new String[] {"design", "designer", "designing", "ui", "ux"});
		keywordVariations.put("마케팅", new String[] {"marketing", "market", "promotion", "advertising"});
		keywordVariations.put("개발", new String[] {"development", "developer", "programming", "coding", "software"});
		keywordVariations.put("기획", new String[] {"planning", "plan", "strategy", "management", "pm"});
		keywordVariations.put("데이터", new String[] {"data", "analytics", "analysis", "database"});

		keywordVariations.put("design", new String[] {"디자인", "디자이너", "ui", "ux", "visual"});
		keywordVariations.put("marketing", new String[] {"마케팅", "광고", "홍보"});
		keywordVariations.put("development", new String[] {"개발", "개발자", "프로그래밍", "코딩"});
		keywordVariations.put("frontend", new String[] {"프론트엔드", "front-end", "ui"});
		keywordVariations.put("backend", new String[] {"백엔드", "back-end", "서버"});

		String[] variations = keywordVariations.get(userKeyword);
		if (variations != null) {
			for (String variation : variations) {
				if (content.contains(variation.toLowerCase())) {
					return true;
				}
			}
		}

		return false;
	}

	private List<MainPageResponseDto.KeywordNewsStats> generateKeywordStats(
		Map<String, List<CareerNewsDto>> newsByKeyword) {
		return newsByKeyword.entrySet().stream()
			.map(entry -> {
				String keyword = entry.getKey();
				List<CareerNewsDto> newsList = entry.getValue();

				return MainPageResponseDto.KeywordNewsStats.builder()
					.keyword(keyword)
					.newsCount(newsList.size())
					.averageRelevanceScore(0.0)
					.build();
			})
			.sorted((a, b) -> Integer.compare(b.getNewsCount(), a.getNewsCount()))
			.collect(Collectors.toList());
	}

	public List<CareerNewsDto> searchNewsByInterests(String username, int limit) {
		try {
			User user = userRepository.findByLoginId(username)
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

			if (user.getInterests() == null || user.getInterests().trim().isEmpty()) {
				return Collections.emptyList();
			}

			List<String> keywords = parseUserInputKeywords(user.getInterests());
			List<CareerNews> relevantNews = new ArrayList<>();

			for (String keyword : keywords.subList(0, Math.min(keywords.size(), 3))) {
				List<CareerNews> keywordNews = careerNewsRepository.findByInterestContaining(
					keyword, PageRequest.of(0, limit / 3 + 1));
				relevantNews.addAll(keywordNews);
			}

			return relevantNews.stream()
				.distinct()
				.limit(limit)
				.map(CareerNewsDto::fromEntity)
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("관심사 기반 뉴스 검색 실패: {}", e.getMessage());
			return Collections.emptyList();
		}
	}
}
