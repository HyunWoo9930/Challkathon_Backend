package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.entity.ChatMessage;
import challkahthon.backend.hihigh.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 컨텍스트 관리 서비스
 * 사용자 정보와 채팅 이력을 기반으로 개인화된 컨텍스트를 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatContextService {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
	private static final int MAX_CONTEXT_MESSAGES = 8; // 컨텍스트에 포함할 최대 메시지 수
	private static final int MAX_MESSAGE_LENGTH = 150; // 컨텍스트용 메시지 최대 길이

	/**
	 * 사용자 정보를 기반으로 개인화된 시스템 프롬프트 생성
	 */
	public String generatePersonalizedSystemPrompt(User user) {
		StringBuilder prompt = new StringBuilder();

		prompt.append("당신은 'HiHigh' 커리어 상담 전문 AI 어시스턴트입니다.\n");
		prompt.append("항상 한국어로 친근하면서도 전문적인 톤으로 답변하세요.\n\n");

		// 사용자 프로필 정보
		prompt.append("📋 상담 대상자 프로필:\n");
		prompt.append("• 이름: ").append(user.getName() != null ? user.getName() : "미등록").append("\n");

		// 나이 계산
		if (user.getBirthYear() != null) {
			try {
				int birthYear = Integer.parseInt(user.getBirthYear());
				int currentYear = java.time.LocalDateTime.now().getYear();
				int age = currentYear - birthYear + 1;
				prompt.append("• 연령대: ").append(age).append("세\n");
			} catch (NumberFormatException e) {
				log.warn("Invalid birth year format: {}", user.getBirthYear());
			}
		}

		if (user.getGender() != null) {
			prompt.append("• 성별: ").append(user.getGender().toString()).append("\n");
		}

		// 핵심 정보들
		if (hasValue(user.getInterests())) {
			prompt.append("• 관심 분야: ").append(user.getInterests()).append("\n");
		}

		if (hasValue(user.getGoals())) {
			prompt.append("• 목표: ").append(user.getGoals()).append("\n");
		}

		if (hasValue(user.getDesiredOccupation())) {
			prompt.append("• 희망 직종: ").append(user.getDesiredOccupation()).append("\n");
		}

		// 상담 가이드라인
		prompt.append("\n🎯 상담 가이드라인:\n");
		prompt.append("1. 위 프로필 정보를 바탕으로 개인 맞춤형 조언 제공\n");
		prompt.append("2. 구체적이고 실행 가능한 커리어 가이드 제시\n");
		prompt.append("3. 업계 트렌드와 실무 경험을 반영한 전문적 답변\n");
		prompt.append("4. 격려와 동기부여를 포함한 따뜻한 커뮤니케이션\n");
		prompt.append("5. 단계별 실행 계획 및 구체적 방법론 제안\n");
		prompt.append("6. 이전 대화 내용을 기억하고 연속성 있는 상담 진행\n\n");

		return prompt.toString();
	}

	/**
	 * 채팅 히스토리를 기반으로 컨텍스트 메시지 생성
	 */
	public String generateChatContext(List<ChatMessage> messages) {
		if (messages == null || messages.isEmpty()) {
			return "";
		}

		// 최근 메시지만 선택 (현재 메시지 제외)
		List<ChatMessage> contextMessages = messages.stream()
			.limit(Math.min(MAX_CONTEXT_MESSAGES, messages.size()))
			.collect(Collectors.toList());

		if (contextMessages.isEmpty()) {
			return "";
		}

		StringBuilder context = new StringBuilder();
		context.append("💬 최근 대화 맥락:\n");

		for (ChatMessage message : contextMessages) {
			String role = message.getRole().equals("user") ? "👤 사용자" : "🤖 AI";
			String content = truncateMessage(message.getContent());
			String timestamp = message.getTimestamp().format(DATE_FORMATTER);

			context.append(String.format("[%s] %s: %s\n", timestamp, role, content));
		}

		context.append("\n위 대화 맥락을 고려하여 자연스럽고 연속성 있는 답변을 제공하세요.\n\n");

		return context.toString();
	}

	/**
	 * 사용자의 상담 패턴 분석
	 */
	public String analyzeUserPattern(List<ChatMessage> allMessages, User user) {
		if (allMessages == null || allMessages.size() < 4) {
			return "";
		}

		// 사용자 메시지만 필터링
		List<ChatMessage> userMessages = allMessages.stream()
			.filter(msg -> "user".equals(msg.getRole()))
			.collect(Collectors.toList());

		if (userMessages.size() < 2) {
			return "";
		}

		StringBuilder pattern = new StringBuilder();
		pattern.append("🔍 상담 패턴 분석:\n");

		// 주요 관심 키워드 추출 (간단한 버전)
		String[] commonCareerKeywords = {
			"취업", "이직", "면접", "자소서", "포트폴리오", "스킬", "경력", "성장",
			"개발", "디자인", "기획", "마케팅", "영업", "회사", "업무", "프로젝트"
		};

		int totalMentions = 0;
		for (String keyword : commonCareerKeywords) {
			long mentions = userMessages.stream()
				.mapToLong(msg -> countKeywordMentions(msg.getContent(), keyword))
				.sum();
			if (mentions > 0) {
				totalMentions++;
			}
		}

		if (totalMentions > 0) {
			pattern.append("• 이전 ").append(userMessages.size()).append("회 상담에서 ")
				.append(totalMentions).append("개 커리어 관련 주제 논의\n");
		}

		// 최근 상담 빈도
		if (userMessages.size() >= 3) {
			pattern.append("• 지속적인 커리어 고민 상담 중 (").append(userMessages.size()).append("회차)\n");
		}

		pattern.append("• 위 패턴을 고려하여 더 깊이 있는 맞춤 조언 제공 필요\n\n");

		return pattern.toString();
	}

	/**
	 * 메시지 길이 제한
	 */
	private String truncateMessage(String message) {
		if (message == null)
			return "";
		if (message.length() <= MAX_MESSAGE_LENGTH)
			return message;
		return message.substring(0, MAX_MESSAGE_LENGTH) + "...";
	}

	/**
	 * 문자열 값 체크
	 */
	private boolean hasValue(String value) {
		return value != null && !value.trim().isEmpty();
	}

	/**
	 * 키워드 언급 횟수 카운트
	 */
	private long countKeywordMentions(String content, String keyword) {
		if (content == null || keyword == null)
			return 0;
		return content.toLowerCase().split(keyword.toLowerCase(), -1).length - 1;
	}

	/**
	 * 개인화된 AI 페르소나 생성
	 */
	public String generateAIPersona(User user) {
		StringBuilder persona = new StringBuilder();

		persona.append("🎭 AI 어시스턴트 페르소나:\n");

		// 사용자 나이대에 따른 톤 조정
		if (user.getBirthYear() != null) {
			try {
				int age = java.time.LocalDateTime.now().getYear() - Integer.parseInt(user.getBirthYear()) + 1;
				if (age <= 25) {
					persona.append("• 친근하고 격려하는 멘토 역할, 실무 경험 공유\n");
				} else if (age <= 35) {
					persona.append("• 동료 같은 전문가 톤, 구체적 커리어 전략 제시\n");
				} else {
					persona.append("• 경험 있는 시니어 조언자 톤, 폭넓은 관점 제공\n");
				}
			} catch (NumberFormatException e) {
				persona.append("• 균형 잡힌 전문가 톤 유지\n");
			}
		}

		// 희망 직종에 따른 전문성 강조
		if (hasValue(user.getDesiredOccupation())) {
			persona.append("• ").append(user.getDesiredOccupation())
				.append(" 분야 전문 지식 보유한 상담사 역할\n");
		}

		persona.append("• 항상 긍정적이고 실행 가능한 솔루션 제공\n");
		persona.append("• 사용자의 강점을 발견하고 발전 방향 제시\n\n");

		return persona.toString();
	}
}
