package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.entity.CareerNews;
import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.dto.CareerNewsDto;
import challkahthon.backend.hihigh.repository.CareerNewsRepository;
import challkahthon.backend.hihigh.repository.UserRepository;
import challkahthon.backend.hihigh.service.CareerNewsService;
import challkahthon.backend.hihigh.service.PersonalizedCrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "관리자", description = "관리자 전용 API")
public class AdminController {

    private final CareerNewsRepository careerNewsRepository;
    private final UserRepository userRepository;
    private final CareerNewsService careerNewsService;
    private final PersonalizedCrawlerService personalizedCrawlerService;

    @Operation(
        summary = "전체 뉴스 조회 (관리자)",
        description = "전체 뉴스를 조회합니다. 일반 뉴스와 맞춤 뉴스를 모두 포함합니다."
    )
    @GetMapping("/news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            List<CareerNews> newsList = careerNewsRepository.findAll(
                PageRequest.of(page, size)).getContent();
            
            List<CareerNewsDto> newsListDto = newsList.stream()
                .map(CareerNewsDto::fromEntity)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(newsListDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("뉴스 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "사용자별 맞춤 뉴스 조회 (관리자)",
        description = "특정 사용자의 맞춤 뉴스를 조회합니다."
    )
    @GetMapping("/users/{username}/personalized-news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserPersonalizedNews(
            @PathVariable String username,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
            
            List<CareerNews> newsList = careerNewsRepository.findByTargetUserOrderByCreatedAtDesc(
                user, PageRequest.of(0, size));
            
            List<CareerNewsDto> newsListDto = newsList.stream()
                .map(CareerNewsDto::fromEntity)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(newsListDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("사용자 맞춤 뉴스 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "전체 사용자 맞춤 크롤링 실행 (관리자)",
        description = "모든 사용자를 대상으로 맞춤 뉴스 크롤링을 실행합니다."
    )
    @PostMapping("/crawl-all-personalized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerGlobalPersonalizedCrawling() {
        try {
            careerNewsService.triggerGlobalPersonalizedCrawling();
            return ResponseEntity.ok("전체 사용자 맞춤 뉴스 크롤링이 시작되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("전체 크롤링 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "시스템 통계 조회 (관리자)",
        description = "전체 시스템의 뉴스 및 사용자 통계를 조회합니다."
    )
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 전체 뉴스 통계
            long totalNews = careerNewsRepository.count();
            long globalNews = careerNewsRepository.countByTargetUserIsNull();
            long personalizedNews = totalNews - globalNews;
            
            // 사용자 통계
            long totalUsers = userRepository.count();
            long usersWithInterests = userRepository.findAll().stream()
                .filter(user -> user.getInterests() != null && !user.getInterests().trim().isEmpty())
                .count();
            
            // 최근 일주일 뉴스 통계
            LocalDateTime lastWeek = LocalDateTime.now().minusWeeks(1);
            long recentNews = careerNewsRepository.findAll().stream()
                .filter(news -> news.getCreatedAt().isAfter(lastWeek))
                .count();
            
            // AI 분석 통계
            long analyzedNews = careerNewsRepository.findAll().stream()
                .filter(news -> news.getIsAiAnalyzed() != null && news.getIsAiAnalyzed())
                .count();
            
            long relevantNews = careerNewsRepository.findAll().stream()
                .filter(news -> news.getIsRelevant() != null && news.getIsRelevant())
                .count();
            
            stats.put("totalNews", totalNews);
            stats.put("globalNews", globalNews);
            stats.put("personalizedNews", personalizedNews);
            stats.put("totalUsers", totalUsers);
            stats.put("usersWithInterests", usersWithInterests);
            stats.put("recentNews", recentNews);
            stats.put("analyzedNews", analyzedNews);
            stats.put("relevantNews", relevantNews);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("통계 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "사용자 목록 및 관심사 조회 (관리자)",
        description = "모든 사용자의 목록과 설정된 관심사를 조회합니다."
    )
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            List<Map<String, Object>> userInfoList = users.stream()
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("loginId", user.getLoginId());
                    userInfo.put("name", user.getName());
                    userInfo.put("interests", user.getInterests());
                    userInfo.put("goals", user.getGoals());
                    userInfo.put("desiredOccupation", user.getDesiredOccupation());
                    userInfo.put("hasInterests", user.getInterests() != null && !user.getInterests().trim().isEmpty());
                    
                    // 해당 사용자의 맞춤 뉴스 개수
                    long personalizedNewsCount = careerNewsRepository.countByTargetUser(user);
                    userInfo.put("personalizedNewsCount", personalizedNewsCount);
                    
                    return userInfo;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(userInfoList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("사용자 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "특정 사용자 맞춤 크롤링 실행 (관리자)",
        description = "특정 사용자를 대상으로 맞춤 뉴스 크롤링을 실행합니다."
    )
    @PostMapping("/users/{username}/crawl")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerUserPersonalizedCrawling(@PathVariable String username) {
        try {
            User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
            
            personalizedCrawlerService.crawlPersonalizedNewsForUser(user);
            
            return ResponseEntity.ok(String.format("사용자 %s의 맞춤 뉴스 크롤링이 시작되었습니다.", username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("사용자별 크롤링 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "뉴스 삭제 (관리자)",
        description = "특정 뉴스를 삭제합니다."
    )
    @DeleteMapping("/news/{newsId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteNews(@PathVariable Long newsId) {
        try {
            if (!careerNewsRepository.existsById(newsId)) {
                return ResponseEntity.notFound().build();
            }
            
            careerNewsRepository.deleteById(newsId);
            return ResponseEntity.ok("뉴스가 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("뉴스 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "사용자별 맞춤 뉴스 모두 삭제 (관리자)",
        description = "특정 사용자의 모든 맞춤 뉴스를 삭제합니다."
    )
    @DeleteMapping("/users/{username}/personalized-news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUserPersonalizedNews(@PathVariable String username) {
        try {
            User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
            
            List<CareerNews> userNews = careerNewsRepository.findByTargetUser(user);
            careerNewsRepository.deleteAll(userNews);
            
            return ResponseEntity.ok(String.format("사용자 %s의 맞춤 뉴스 %d개가 삭제되었습니다.", 
                username, userNews.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("사용자 맞춤 뉴스 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
