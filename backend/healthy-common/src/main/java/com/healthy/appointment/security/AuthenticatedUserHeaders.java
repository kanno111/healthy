package com.healthy.appointment.security;

/** Internal identity headers written by the gateway after authentication. */
public final class AuthenticatedUserHeaders {
    public static final String USER_ID = "X-Auth-User-Id";
    public static final String ROLE = "X-Auth-Role";

    private AuthenticatedUserHeaders() {
    }
}
