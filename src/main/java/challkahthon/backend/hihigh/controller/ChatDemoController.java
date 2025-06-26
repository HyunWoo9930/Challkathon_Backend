package challkahthon.backend.hihigh.controller;

import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.service.ChatContextService;
import challkahthon.backend.hihigh.service.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 개인화된 채팅 기능 데모 및 테스트용 컨트롤러
 */
@RestController
@RequestMapping("/api/chat/demo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "채팅 데모", description = "개인화된 채팅 기능 데모 및 테스트 API")
public class ChatDemoController {

	private final ChatContextService chatContextService;
	private final CustomUserDetailsService userDetailsService;

	@Operation(
		summary = "개인화된 시스템 프롬프트 미리보기",
		description = "현재 사용자 정보를 바탕으로 생성될 개인화된 시스템 프롬프트를 미리 확인합니다."
	)
	@GetMapping("/system-prompt-preview")
	public ResponseEntity<?> getSystemPromptPreview(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User user = userDetailsService.findByLoginId(username);

		if (user == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		String personalizedPrompt = chatContextService.generatePersonalizedSystemPrompt(user);

		return ResponseEntity.ok().body(new SystemPromptPreviewResponse(
			user.getName(),
			user.getInterests(),
			user.getGoals(),
			user.getDesiredOccupation(),
			personalizedPrompt
		));
	}

	@Operation(
		summary = "AI 페르소나 미리보기",
		description = "현재 사용자에게 맞춤화된 AI 페르소나 설정을 미리 확인합니다."
	)
	@GetMapping("/ai-persona-preview")
	public ResponseEntity<?> getAIPersonaPreview(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		User user = userDetailsService.findByLoginId(username);

		if (user == null) {
			return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");
		}

		String aiPersona = chatContextService.generateAIPersona(user);

		return ResponseEntity.ok().body(new AIPersonaPreviewResponse(
			user.getBirthYear(),
			user.getDesiredOccupation(),
			aiPersona
		));
	}

	@Operation(
		summary = "채팅 개인화 설명",
		description = "채팅이 어떻게 개인화되는지에 대한 상세 설명을 제공합니다."
	)
	@GetMapping("/personalization-info")
	public ResponseEntity<?> getPersonalizationInfo() {
		PersonalizationInfoResponse info = new PersonalizationInfoResponse();
		return ResponseEntity.ok(info);
	}

	// Response DTOs
	public static class SystemPromptPreviewResponse {
		public String userName;
		public String userInterests;
		public String userGoals;
		public String userDesiredOccupation;
		public String generatedPrompt;

		public SystemPromptPreviewResponse(String userName, String userInterests,
			String userGoals, String userDesiredOccupation,
			String generatedPrompt) {
			this.userName = userName;
			this.userInterests = userInterests;
			this.userGoals = userGoals;
			this.userDesiredOccupation = userDesiredOccupation;
			this.generatedPrompt = generatedPrompt;
		}
	}

	public static class AIPersonaPreviewResponse {
		public String userBirthYear;
		public String userDesiredOccupation;
		public String generatedPersona;

		public AIPersonaPreviewResponse(String userBirthYear, String userDesiredOccupation,
			String generatedPersona) {
			this.userBirthYear = userBirthYear;
			this.userDesiredOccupation = userDesiredOccupation;
			this.generatedPersona = generatedPersona;
		}
	}

	public static class PersonalizationInfoResponse {
		public String title = "HiHigh 개인화된 채팅 기능";
		public String description = "사용자의 프로필 정보와 채팅 이력을 바탕으로 맞춤형 상담을 제공합니다.";

		public String[] features = {
			"🎯 사용자 프로필 기반 개인화 (이름, 나이, 관심사, 목표, 희망직종)",
			"💬 이전 대화 맥락 유지 및 연속성 있는 상담",
			"🤖 사용자 연령대/직종에 맞는 AI 페르소나 적용",
			"📊 대화 패턴 분석으로 더 깊이 있는 조언 제공",
			"🎨 개인 맞춤형 톤과 어투로 상담 진행",
			"📈 지속적인 커리어 성장을 위한 단계별 가이드"
		};

		public String[] howItWorks = {
			"1. 사용자 프로필 정보 수집 (관심사, 목표, 희망직종 등)",
			"2. 이전 채팅 기록 분석 및 컨텍스트 생성",
			"3. 개인화된 AI 시스템 프롬프트 구성",
			"4. 사용자별 맞춤 페르소나 적용",
			"5. 연속성 있는 대화로 심층적 상담 제공",
			"6. 지속적인 학습으로 상담 품질 향상"
		};

		public String[] benefits = {
			"✅ 처음 채팅하는 느낌이 아닌 친숙한 상담 경험",
			"✅ 개인 상황에 맞는 구체적이고 실행 가능한 조언",
			"✅ 이전 상담 내용을 기억하는 연속적인 멘토링",
			"✅ 나이와 직종에 적합한 커뮤니케이션 스타일",
			"✅ 개인 성장 단계에 따른 맞춤형 커리어 가이드",
			"✅ 지속적인 동기부여와 격려를 통한 목표 달성 지원"
		};
	}
}
