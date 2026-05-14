package com.example.auth;

/**
 * Result of a login attempt. Immutable.
 *
 * <p>Use the static factory methods rather than the constructor directly:</p>
 * <pre>{@code
 *   return LoginResult.success();
 *   return LoginResult.failure("Invalid credentials");
 * }</pre>
 */
public record LoginResult(boolean isSuccess, String errorMessage) {

    public static LoginResult success() {
        return new LoginResult(true, null);
    }

    public static LoginResult failure(String reason) {
        return new LoginResult(false, reason);
    }
}
