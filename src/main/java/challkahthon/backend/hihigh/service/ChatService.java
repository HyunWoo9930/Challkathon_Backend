package challkahthon.backend.hihigh.service;

import challkahthon.backend.hihigh.domain.entity.Chat;
import challkahthon.backend.hihigh.domain.entity.ChatMessage;
import challkahthon.backend.hihigh.domain.entity.User;
import challkahthon.backend.hihigh.dto.Message;
import challkahthon.backend.hihigh.repository.ChatMessageRepository;
import challkahthon.backend.hihigh.repository.ChatRepository;
import challkahthon.backend.hihigh.repository.UserRepository;
import challkahthon.backend.hihigh.utils.ChatGPTUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

	private final ChatRepository chatRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserRepository userRepository;
	private final ChatGPTUtils chatGPTUtils;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final challkahthon.backend.hihigh.config.ChatGPTConfig chatGPTConfig;
	private final ChatContextService chatContextService;

	@Value("${chatgpt.model}")
	private String model;

	@Value("${chatgpt.url}")
	private String apiURL;

	/**
	 * Create a new chat conversation
	 * @param username The username of the user creating the chat
	 * @return The created chat
	 */
	@Transactional
	public Chat createChat(String username) {
		// Find the user by username (loginId)
		User user = userRepository.findByName(username)
			.orElseThrow(() -> new RuntimeException("User not found with username: " + username));

		// Check if the user already has a chat
		Chat existingChat = chatRepository.findByUser(user);
		if (existingChat != null) {
			return existingChat;
		}
		// Create a new chat for the user
		Chat chat = Chat.builder()
			.user(user)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
		return chatRepository.save(chat);
	}

	/**
	 * Get all chats for a user
	 * @param username The username of the user
	 * @return List of all chats for the user
	 */
	@Transactional
	public List<Chat> getAllChats(String username) {
		// Find the user by username (loginId)
		User user = userRepository.findByName(username)
			.orElseThrow(() -> new RuntimeException("User not found with username: " + username));

		// Check if the user has a chat
		Chat existingChat = chatRepository.findByUser(user);
		if (existingChat == null) {
			// Create a new chat for the user if they don't have one
			createChat(username);
		}

		// Find all chats for the user
		return chatRepository.findByUserOrderByUpdatedAtDesc(user);
	}

	@Transactional(readOnly = true)
	public Chat getChatById(String username) {
		// Find the user by username (loginId)
		User user = userRepository.findByLoginId(username)
			.orElseThrow(() -> new RuntimeException("User not found with username: " + username));

		Chat chat = chatRepository.findByUser(user);

		// Check if the chat belongs to the user
		if (chat.getUser() == null || !chat.getUser().getId().equals(user.getId())) {
			throw new RuntimeException("Access denied: Chat does not belong to the user");
		}

		return chat;
	}

	/**
	 * Generate personalized system message based on user information
	 * @param user The user entity
	 * @return Personalized system message
	 */
	private String generatePersonalizedSystemMessage(User user) {
		StringBuilder systemMessage = new StringBuilder();

		systemMessage.append("당신은 HiHigh 커리어 상담 전문 AI 어시스턴트입니다. ");
		systemMessage.append("항상 한국어로 친근하고 전문적인 톤으로 답변해주세요.\n\n");

		// 사용자 기본 정보
		systemMessage.append("=== 상담 중인 사용자 정보 ===\n");
		systemMessage.append("이름: ").append(user.getName() != null ? user.getName() : "미등록").append("\n");

		if (user.getBirthYear() != null) {
			int currentYear = LocalDateTime.now().getYear();
			int age = currentYear - Integer.parseInt(user.getBirthYear()) + 1;
			systemMessage.append("나이: 약 ").append(age).append("세\n");
		}

		if (user.getGender() != null) {
			systemMessage.append("성별: ").append(user.getGender()).append("\n");
		}

		// 사용자의 관심사
		if (user.getInterests() != null && !user.getInterests().trim().isEmpty()) {
			systemMessage.append("관심사: ").append(user.getInterests()).append("\n");
		}

		// 사용자의 목표
		if (user.getGoals() != null && !user.getGoals().trim().isEmpty()) {
			systemMessage.append("목표: ").append(user.getGoals()).append("\n");
		}

		// 사용자의 희망직종
		if (user.getDesiredOccupation() != null && !user.getDesiredOccupation().trim().isEmpty()) {
			systemMessage.append("희망직종: ").append(user.getDesiredOccupation()).append("\n");
		}

		systemMessage.append("\n=== 상담 가이드라인 ===\n");
		systemMessage.append("1. 위 사용자 정보를 바탕으로 맞춤형 조언을 제공하세요.\n");
		systemMessage.append("2. 커리어, 취업, 자기계발에 관한 질문에 전문적으로 답변하세요.\n");
		systemMessage.append("3. 구체적이고 실행 가능한 조언을 제공하세요.\n");
		systemMessage.append("4. 사용자의 상황과 목표에 맞는 개인화된 답변을 하세요.\n");
		systemMessage.append("5. 격려와 동기부여도 함께 제공하세요.\n");
		systemMessage.append("6. 필요시 관련 업계 트렌드나 구체적인 방법론을 제시하세요.\n\n");

		// 이전 대화 맥락 안내
		systemMessage.append("=== 대화 맥락 ===\n");
		systemMessage.append("이전 대화 내용을 참고하여 연속성 있는 상담을 제공하세요. ");
		systemMessage.append("사용자가 이전에 언급한 내용이나 고민을 기억하고 이를 바탕으로 답변하세요.\n");

		return systemMessage.toString();
	}

	/**
	 * Send a message to a chat and get a response from ChatGPT
	 * @param userMessage The message from the user
	 * @param username The username of the user
	 * @return The response from ChatGPT
	 */
	@Transactional
	public ChatMessage sendMessage(String userMessage, String username) {
		// Find the user by username (loginId)
		User user = userRepository.findByLoginId(username)
			.orElseThrow(() -> new RuntimeException("User not found with username: " + username));

		Chat chat = chatRepository.findByUser(user);

		// Save user message
		ChatMessage userChatMessage = ChatMessage.builder()
			.chat(chat)
			.role("user")
			.content(userMessage)
			.timestamp(LocalDateTime.now())
			.build();
		chatMessageRepository.save(userChatMessage);

		// Get all messages for this chat
		List<ChatMessage> allMessages = chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());

		// 현재 메시지를 제외한 이전 메시지들 (최근 20개만)
		List<ChatMessage> previousMessages = allMessages.stream()
			.filter(msg -> !msg.getId().equals(userChatMessage.getId()))
			.skip(Math.max(0, allMessages.size() - 21)) // 현재 메시지 포함해서 21개, 이전 메시지 20개
			.collect(Collectors.toList());

		// Convert to format expected by ChatGPT API
		List<Map<String, String>> messages = new ArrayList<>();

		// 1. 개인화된 시스템 프롬프트 생성
		String personalizedSystemPrompt = chatContextService.generatePersonalizedSystemPrompt(user);
		messages.add(Map.of("role", "system", "content", personalizedSystemPrompt));

		// 2. AI 페르소나 설정
		String aiPersona = chatContextService.generateAIPersona(user);
		messages.add(Map.of("role", "system", "content", aiPersona));

		// 3. 채팅 컨텍스트 추가 (이전 대화가 있는 경우)
		if (!previousMessages.isEmpty()) {
			String chatContext = chatContextService.generateChatContext(previousMessages);
			if (!chatContext.isEmpty()) {
				messages.add(Map.of("role", "system", "content", chatContext));
			}

			// 4. 사용자 패턴 분석 (충분한 대화 이력이 있는 경우)
			if (previousMessages.size() >= 4) {
				String userPattern = chatContextService.analyzeUserPattern(allMessages, user);
				if (!userPattern.isEmpty()) {
					messages.add(Map.of("role", "system", "content", userPattern));
				}
			}

			// 5. 최근 대화 메시지들을 실제 대화 형태로 추가 (최근 6개만)
			int recentLimit = Math.min(6, previousMessages.size());
			List<ChatMessage> recentMessages = previousMessages.subList(
				Math.max(0, previousMessages.size() - recentLimit),
				previousMessages.size()
			);

			for (ChatMessage msg : recentMessages) {
				messages.add(Map.of(
					"role", msg.getRole(),
					"content", msg.getContent()
				));
			}
		}

		// 6. 현재 사용자 메시지 추가
		messages.add(Map.of("role", "user", "content", userMessage));

		try {
			// Prepare request body with optimized parameters
			Map<String, Object> requestBodyMap = new HashMap<>();
			requestBodyMap.put("model", model);
			requestBodyMap.put("messages", messages);
			requestBodyMap.put("max_tokens", 2000); // 더 자세한 답변을 위해 토큰 증가
			requestBodyMap.put("temperature", 0.8); // 약간 더 창의적인 답변
			requestBodyMap.put("top_p", 0.9);
			requestBodyMap.put("frequency_penalty", 0.1); // 반복 줄이기
			requestBodyMap.put("presence_penalty", 0.1); // 새로운 주제 유도

			// Convert to JSON
			String requestBody = objectMapper.writeValueAsString(requestBodyMap);
			HttpEntity<String> entity = new HttpEntity<>(requestBody, chatGPTConfig.httpHeaders());

			log.info("Sending personalized request to ChatGPT for user: {} with {} context messages",
				username, messages.size());

			// Call API
			String response = restTemplate.postForObject(apiURL, entity, String.class);

			// Parse response
			Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
			List<Map<String, Object>> choices = (List<Map<String, Object>>)responseMap.get("choices");
			Map<String, Object> choice = choices.get(0);
			Map<String, Object> messageMap = (Map<String, Object>)choice.get("message");
			String content = (String)messageMap.get("content");

			// Save assistant message
			ChatMessage assistantMessage = ChatMessage.builder()
				.chat(chat)
				.role("assistant")
				.content(content)
				.timestamp(LocalDateTime.now())
				.build();
			chatMessageRepository.save(assistantMessage);

			// Update chat's updated time
			chat.setUpdatedAt(LocalDateTime.now());
			chatRepository.save(chat);

			log.info("Successfully received personalized response from ChatGPT for user: {}", username);

			return assistantMessage;
		} catch (Exception e) {
			log.error("Error calling ChatGPT API with personalized context: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to get response from ChatGPT", e);
		}
	}

	/**
	 * Get all messages for a chat
	 * @param username The username of the user
	 * @return List of all messages for the chat
	 */
	@Transactional(readOnly = true)
	public List<ChatMessage> getChatMessages(String username) {

		// Find the user by username (loginId)
		User user = userRepository.findByLoginId(username)
			.orElseThrow(() -> new RuntimeException("User not found with username: " + username));

		Chat chat = chatRepository.findByUser(user);

		return chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());
	}

	/**
	 * Get chat summary for user context
	 * @param username The username of the user
	 * @return Summary of recent chat interactions
	 */
	@Transactional(readOnly = true)
	public String getChatSummary(String username) {
		User user = userRepository.findByLoginId(username)
			.orElseThrow(() -> new RuntimeException("User not found with username: " + username));

		Chat chat = chatRepository.findByUser(user);
		List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());

		if (messages.isEmpty()) {
			return "이전 대화 기록이 없습니다.";
		}

		return chatContextService.generateChatContext(messages);
	}

	/**
	 * Get user's conversation statistics
	 * @param username The username of the user
	 * @return Conversation statistics
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> getUserChatStats(String username) {
		User user = userRepository.findByLoginId(username)
			.orElseThrow(() -> new RuntimeException("User not found with username: " + username));

		Chat chat = chatRepository.findByUser(user);
		List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByTimestamp(chat.getId());

		Map<String, Object> stats = new HashMap<>();
		stats.put("totalMessages", messages.size());
		stats.put("userMessages", messages.stream().filter(m -> "user".equals(m.getRole())).count());
		stats.put("assistantMessages", messages.stream().filter(m -> "assistant".equals(m.getRole())).count());

		if (!messages.isEmpty()) {
			stats.put("firstMessageDate", messages.get(0).getTimestamp());
			stats.put("lastMessageDate", messages.get(messages.size() - 1).getTimestamp());
		}

		return stats;
	}
}