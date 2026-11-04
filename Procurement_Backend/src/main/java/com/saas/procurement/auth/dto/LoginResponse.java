package com.saas.procurement.auth.dto;

import com.saas.procurement.user.dto.UserResponse;

/**
 * @param accessToken JWT to send as "Authorization: Bearer &lt;accessToken&gt;"
 * @param tokenType   always "Bearer"
 * @param expiresIn   seconds until the token expires
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
