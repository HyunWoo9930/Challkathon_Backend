package challkahthon.backend.hihigh.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import challkahthon.backend.hihigh.domain.dto.request.ChatRequestDto;
import challkahthon.backend.hihigh.domain.dto.response.ChatDto;
import challkahthon.backend.hihigh.domain.dto.response.ChatMessageDto;
import challkahthon.backend.hihigh.domain.entity.Chat;
import challkahthon.backend.hihigh.domain.entity.ChatMessage;
import challkahthon.backend.hihigh.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "채팅", description = "GPT 채팅 관련 API")
public class ChatController {

	private final ChatService chatService;

	@Operation(
		summary = "내 채팅 목록 조회",
		description = "현재 사용자의 모든 채팅 대화 목록을 조회합니다."
	)
	@GetMapping
	public ResponseEntity<?> getAllChats(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		List<Chat> chats = chatService.getAllChats(username);
		List<ChatDto> chatDtos = chats.stream()
			.map(ChatDto::fromEntity)
			.collect(Collectors.toList());
		return ResponseEntity.ok(chatDtos);
	}

	@Operation(
		summary = "채팅 조회",
		description = "특정 채팅의 상세 정보와 메시지를 조회합니다."
	)
	@GetMapping("/{chatId}")
	public ResponseEntity<?> getChatById(@PathVariable Long chatId, Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		Chat chat = chatService.getChatById(username);
		List<ChatMessage> messages = chatService.getChatMessages(username);
		return ResponseEntity.ok(ChatDto.fromEntityWithMessages(chat, messages));
	}

	@Operation(
		summary = "메시지 전송",
		description = "채팅에 메시지를 전송하고 GPT의 응답을 받습니다."
	)
	@PostMapping("/{chatId}/messages")
	public ResponseEntity<?> sendMessage(
		@PathVariable Long chatId,
		@RequestBody ChatRequestDto request,
		Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.badRequest().body("인증되지 않은 사용자입니다.");
		}

		String username = authentication.getName();
		ChatMessage response = chatService.sendMessage(request.getMessage(), username);
		return ResponseEntity.ok(ChatMessageDto.fromEntity(response));
	}
}
