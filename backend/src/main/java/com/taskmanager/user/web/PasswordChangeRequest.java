package com.taskmanager.user.web;

/**
 * Payload accepted by {@code POST /api/users/me/password}. Deliberately
 * carries no Bean Validation annotations: every rule ({@code currentPassword}
 * must match, {@code newPassword} length and difference from the current
 * one) needs the stored password hash to evaluate, so it is checked in
 * {@link com.taskmanager.user.UserAccountService}, the single source of
 * truth for this validation.
 */
public record PasswordChangeRequest(String currentPassword, String newPassword) {
}
