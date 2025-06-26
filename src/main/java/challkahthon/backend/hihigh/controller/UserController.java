package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.dto.InterestsUpdateDto;
import challkahthon.backend.hihigh.dto.UserResponseDto;
import challkahthon.backend.hihigh.dto.UserUpdateDto;
import challkahthon.backend.hihigh.repository.UserRepository;
import challkahthon.backend.hihigh.service.CareerNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "사용자", description = "사용자 정보 관리 API")
public class UserController {

    private final UserRepository userRepository;
    private final CareerNewsService careerNewsService;

    @Operation(
        summary = "사용자 정보 조회",
        description = "현재 로그인한 사용자의 정보를 조회합니다."
    )
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
        }

        String username = authentication.getName();
        try {
            User user = userRepository.findByLoginId(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            UserResponseDto response = UserResponseDto.fromEntity(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "사용자 정보 업데이트",
        description = "사용자의 기본 정보를 업데이트합니다."
    )
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(
            @RequestBody UserUpdateDto updateDto,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
        }

        String username = authentication.getName();
        try {
            User user = userRepository.findByLoginId(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 기본 정보 업데이트
            if (updateDto.getName() != null) {
                user.setName(updateDto.getName());
            }
            if (updateDto.getGender() != null) {
                user.setGender(updateDto.getGender());
            }
            if (updateDto.getBirthYear() != null) {
                user.setBirthYear(updateDto.getBirthYear());
            }

            userRepository.save(user);

            UserResponseDto response = UserResponseDto.fromEntity(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("사용자 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "관심사 업데이트",
        description = "사용자의 관심사를 업데이트하고 맞춤 뉴스 크롤링을 시작합니다."
    )
    @PutMapping("/interests")
    public ResponseEntity<?> updateInterests(
            @RequestBody InterestsUpdateDto updateDto,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
        }

        String username = authentication.getName();
        try {
            User user = userRepository.findByLoginId(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 관심사 업데이트
            user.setInterests(updateDto.getInterests());
            userRepository.save(user);

            // 맞춤 뉴스 크롤링 트리거
            careerNewsService.triggerPersonalizedCrawling(username);

            return ResponseEntity.ok("관심사가 업데이트되었습니다. 맞춤 뉴스 수집이 시작되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("관심사 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "목표 업데이트",
        description = "사용자의 목표를 업데이트합니다."
    )
    @PutMapping("/goals")
    public ResponseEntity<?> updateGoals(
            @RequestBody challkahthon.backend.hihigh.dto.GoalsUpdateDto updateDto,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
        }

        String username = authentication.getName();
        try {
            User user = userRepository.findByLoginId(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            user.setGoals(updateDto.getGoals());
            userRepository.save(user);

            return ResponseEntity.ok("목표가 업데이트되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("목표 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "희망직종 업데이트",
        description = "사용자의 희망직종을 업데이트하고 관련 뉴스 크롤링을 시작합니다."
    )
    @PutMapping("/desired-occupation")
    public ResponseEntity<?> updateDesiredOccupation(
            @RequestBody challkahthon.backend.hihigh.dto.DesiredOccupationUpdateDto updateDto,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
        }

        String username = authentication.getName();
        try {
            User user = userRepository.findByLoginId(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            user.setDesiredOccupation(updateDto.getDesiredOccupation());
            userRepository.save(user);

            // 희망직종 변경 시에도 맞춤 뉴스 크롤링 트리거
            careerNewsService.triggerPersonalizedCrawling(username);

            return ResponseEntity.ok("희망직종이 업데이트되었습니다. 관련 뉴스 수집이 시작되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("희망직종 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
        summary = "사용자 맞춤 설정 일괄 업데이트",
        description = "관심사, 목표, 희망직종을 한 번에 업데이트합니다."
    )
    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(
            @RequestBody UserPreferencesUpdateDto updateDto,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
        }

        String username = authentication.getName();
        try {
            User user = userRepository.findByLoginId(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            boolean shouldTriggerCrawling = false;

            // 관심사 업데이트
            if (updateDto.getInterests() != null) {
                user.setInterests(updateDto.getInterests());
                shouldTriggerCrawling = true;
            }

            // 목표 업데이트
            if (updateDto.getGoals() != null) {
                user.setGoals(updateDto.getGoals());
            }

            // 희망직종 업데이트
            if (updateDto.getDesiredOccupation() != null) {
                user.setDesiredOccupation(updateDto.getDesiredOccupation());
                shouldTriggerCrawling = true;
            }

            userRepository.save(user);

            // 관심사나 희망직종이 변경된 경우 맞춤 뉴스 크롤링 트리거
            if (shouldTriggerCrawling) {
                careerNewsService.triggerPersonalizedCrawling(username);
                return ResponseEntity.ok("설정이 업데이트되었습니다. 맞춤 뉴스 수집이 시작되었습니다.");
            } else {
                return ResponseEntity.ok("설정이 업데이트되었습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("설정 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 일괄 업데이트용 DTO
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserPreferencesUpdateDto {
        private String interests;
        private String goals;
        private String desiredOccupation;
    }
}
