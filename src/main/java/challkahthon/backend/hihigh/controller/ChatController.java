package challkahthon.backend.hihigh.controller;

import java.util.List;

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
		summary = "채팅 조회",
		description = "현재 사용자의 채팅 정보와 메시지를 조회합니다."
	)
	@GetMapping
	public ResponseEntity<?> getChat(Authentication authentication) {
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
	@PostMapping("/messages")
	public ResponseEntity<?> sendMessage(
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
