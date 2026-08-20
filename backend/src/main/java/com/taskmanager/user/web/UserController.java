package com.taskmanager.user.web;

import com.taskmanager.user.UserAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Self-service operations of the authenticated user. */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

	private final UserAccountService userAccountService;

	public UserController(UserAccountService userAccountService) {
		this.userAccountService = userAccountService;
	}

	@PostMapping("/password")
	public ResponseEntity<Void> changePassword(@RequestBody PasswordChangeRequest request, Authentication authentication) {
		userAccountService.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());
		return ResponseEntity.noContent().build();
	}

}
