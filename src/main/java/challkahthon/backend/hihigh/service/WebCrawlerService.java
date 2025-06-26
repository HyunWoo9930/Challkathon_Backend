package challkahthon.backend.hihigh.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebCrawlerService {

	private final CareerNewsRepository careerNewsRepository;
	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${news.api.key:YOUR_NEWS_API_KEY}")
	private String newsApiKey;

	@Value("${gnews.api.key:YOUR_GNEWS_API_KEY}")
	private String gNewsApiKey;

	private static final Map<String, String> RSS_SOURCES = new HashMap<>();

	static {
		RSS_SOURCES.put("Dev.to", "https://dev.to/feed");
		RSS_SOURCES.put("Medium Tech", "https://medium.com/feed/topic/technology");
		RSS_SOURCES.put("CSS-Tricks", "https://css-tricks.com/feed");
		RSS_SOURCES.put("Smashing Magazine", "https://www.smashingmagazine.com/feed");
		RSS_SOURCES.put("A List Apart", "https://alistapart.com/main/feed");
	}

	public List<CareerNews> crawlCareerNews() {
		return crawlAllSources();
	}

	public List<CareerNews> crawlFromNewsAPI() {
		List<CareerNews> newsList = new ArrayList<>();

		try {
			String query = "software developer OR programming OR javascript OR react";
			String url = String.format(
				"https://newsapi.org/v2/everything?q=%s&language=en&sortBy=publishedAt&pageSize=5&apiKey=%s",
				query, newsApiKey
			);

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			JsonNode root = objectMapper.readTree(response.getBody());
			JsonNode articles = root.get("articles");

			if (articles != null && articles.isArray()) {
				int count = 0;
				for (JsonNode article : articles) {
					if (count >= 3)
						break;

					CareerNews news = parseNewsAPIArticle(article);
					if (news != null) {
						newsList.add(news);
						count++;
					}
				}
			}

		} catch (Exception e) {
			log.error("Error crawling from NewsAPI: {}", e.getMessage());
		}

		return newsList;
	}

	public List<CareerNews> crawlFromGNewsAPI() {
		List<CareerNews> newsList = new ArrayList<>();

		try {
			String query = "software developer programming";
			String url = String.format(
				"https://gnews.io/api/v4/search?q=%s&lang=en&country=us&max=3&apikey=%s",
				query, gNewsApiKey
			);

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			JsonNode root = objectMapper.readTree(response.getBody());
			JsonNode articles = root.get("articles");

			if (articles != null && articles.isArray()) {
				for (JsonNode article : articles) {
					CareerNews news = parseGNewsArticle(article);
					if (news != null) {
						newsList.add(news);
					}
				}
			}

		} catch (Exception e) {
			log.error("Error crawling from GNews API: {}", e.getMessage());
		}

		return newsList;
	}

	public List<CareerNews> crawlFromRSSFeeds() {
		List<CareerNews> newsList = new ArrayList<>();

		for (Map.Entry<String, String> source : RSS_SOURCES.entrySet()) {
			try {
				Document doc = Jsoup.connect(source.getValue())
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
					.timeout(10000)
					.get();

				Elements items = doc.select("item");
				if (items.isEmpty()) {
					items = doc.select("entry");
				}

				int count = 0;
				for (Element item : items) {
					if (count >= 2)
						break;

					CareerNews news = parseRSSItem(item, source.getKey());
					if (news != null && isCareerRelated(news.getTitle())) {
						newsList.add(news);
						count++;
					}
				}

			} catch (Exception e) {
				log.error("Error crawling RSS from {}: {}", source.getKey(), e.getMessage());
			}
		}

		return newsList;
	}

	public List<CareerNews> crawlAllSources() {
		List<CareerNews> allNews = new ArrayList<>();

		if (!"YOUR_NEWS_API_KEY".equals(newsApiKey)) {
			allNews.addAll(crawlFromNewsAPI());
		}

		if (!"YOUR_GNEWS_API_KEY".equals(gNewsApiKey)) {
			allNews.addAll(crawlFromGNewsAPI());
		}

		allNews.addAll(crawlFromRSSFeeds());

		return removeDuplicates(allNews);
	}

	private CareerNews parseNewsAPIArticle(JsonNode article) {
		try {
			String title = article.get("title").asText();
			String content = article.get("description") != null ? article.get("description").asText() : "";
			String url = article.get("url").asText();
			String source = article.get("source").get("name").asText();
			String publishedAt = article.get("publishedAt").asText();

			String thumbnailUrl = "";
			if (article.has("urlToImage") && !article.get("urlToImage").isNull()) {
				thumbnailUrl = article.get("urlToImage").asText();
			}

			LocalDateTime publishedDate = LocalDateTime.parse(
				publishedAt.replace("Z", ""),
				DateTimeFormatter.ISO_LOCAL_DATE_TIME
			);

			return CareerNews.builder()
				.title(title)
				.thumbnailUrl(thumbnailUrl)
				.originalContent(content)
				.sourceUrl(url)
				.source(source)
				.category("general")
				.language("en")
				.publishedDate(publishedDate)
				.createdAt(LocalDateTime.now())
				.build();

		} catch (Exception e) {
			log.error("Error parsing NewsAPI article: {}", e.getMessage());
			return null;
		}
	}

	private CareerNews parseGNewsArticle(JsonNode article) {
		try {
			String title = article.get("title").asText();
			String content = article.get("description") != null ? article.get("description").asText() : "";
			String url = article.get("url").asText();
			String source = article.get("source").get("name").asText();
			String publishedAt = article.get("publishedAt").asText();

			String thumbnailUrl = "";
			if (article.has("image") && !article.get("image").isNull()) {
				thumbnailUrl = article.get("image").asText();
			}

			LocalDateTime publishedDate = LocalDateTime.parse(
				publishedAt.replace("Z", ""),
				DateTimeFormatter.ISO_LOCAL_DATE_TIME
			);

			return CareerNews.builder()
				.title(title)
				.thumbnailUrl(thumbnailUrl)
				.originalContent(content)
				.sourceUrl(url)
				.source(source)
				.category("general")
				.language("en")
				.publishedDate(publishedDate)
				.createdAt(LocalDateTime.now())
				.build();

		} catch (Exception e) {
			log.error("Error parsing GNews article: {}", e.getMessage());
			return null;
		}
	}

	private CareerNews parseRSSItem(Element item, String sourceName) {
		try {
			Element titleElement = item.select("title").first();
			if (titleElement == null) {
				return null;
			}
			String title = titleElement.text();

			String url = "";
			Element linkElement = item.select("link").first();
			if (linkElement != null) {
				url = linkElement.text();
				if (url.isEmpty()) {
					url = linkElement.attr("href");
				}
			}

			if (title.isEmpty() || url.isEmpty()) {
				return null;
			}

			String content = "";
			Element contentElement = item.select("description").first();
			if (contentElement == null) {
				contentElement = item.select("content").first();
			}
			if (contentElement != null) {
				content = contentElement.text();
			}

			String thumbnailUrl = extractThumbnailFromRSS(item);

			return CareerNews.builder()
				.title(title)
				.thumbnailUrl(thumbnailUrl)
				.originalContent(content)
				.sourceUrl(url)
				.source(sourceName)
				.category("general")
				.language("en")
				.publishedDate(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.build();

		} catch (Exception e) {
			log.error("Error parsing RSS item from {}: {}", sourceName, e.getMessage());
			return null;
		}
	}

	private String extractThumbnailFromRSS(Element item) {
		Element mediaElement = item.select("media\\:thumbnail, enclosure[type^=image], image").first();
		if (mediaElement != null) {
			String thumbnailUrl = mediaElement.attr("url");
			if (thumbnailUrl.isEmpty()) {
				thumbnailUrl = mediaElement.attr("href");
			}
			return thumbnailUrl;
		}

		Element descElement = item.select("description").first();
		if (descElement != null) {
			String descContent = descElement.text();
			try {
				Document doc = Jsoup.parse(descContent);
				Element img = doc.select("img").first();
				if (img != null) {
					return img.attr("src");
				}
			} catch (Exception e) {
				// ignore
			}
		}

		return "";
	}

	private boolean isCareerRelated(String text) {
		String lowerText = text.toLowerCase();
		String[] keywords = {
			"career", "job", "employment", "hiring", "developer", "programmer",
			"coding", "programming", "frontend", "backend", "design", "engineer",
			"javascript", "python", "java", "react", "vue", "angular", "css",
			"html", "database", "api", "cloud", "devops", "ui", "ux"
		};

		for (String keyword : keywords) {
			if (lowerText.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	private List<CareerNews> removeDuplicates(List<CareerNews> newsList) {
		Map<String, CareerNews> uniqueNews = new HashMap<>();

		for (CareerNews news : newsList) {
			if (!uniqueNews.containsKey(news.getSourceUrl())) {
				uniqueNews.put(news.getSourceUrl(), news);
			}
		}

		return new ArrayList<>(uniqueNews.values());
	}

	public int saveCareerNews(List<CareerNews> newsList) {
		if (newsList.isEmpty()) {
			return 0;
		}

		List<CareerNews> savedNews = careerNewsRepository.saveAll(newsList);
		return savedNews.size();
	}

	public int crawlAndSaveCareerNews() {
		List<CareerNews> newsList = crawlCareerNews();
		return saveCareerNews(newsList);
	}

	public int crawlAndSaveAllSources() {
		List<CareerNews> newsList = crawlAllSources();

		List<CareerNews> newsToSave = new ArrayList<>();
		for (CareerNews news : newsList) {
			if (!careerNewsRepository.existsBySourceUrlAndTargetUserIsNull(news.getSourceUrl())) {
				newsToSave.add(news);
			}
		}

		if (!newsToSave.isEmpty()) {
			careerNewsRepository.saveAll(newsToSave);
		}

		return newsToSave.size();
	}
}
