package com.taskmanager.security;

/** Spring Security authority names granted to each kind of caller. */
public final class SecurityRoles {

	public static final String ADMIN = "ADMIN";

	public static final String USER = "USER";

	public static final String SERVICE = "SERVICE";

	private SecurityRoles() {
	}

}
