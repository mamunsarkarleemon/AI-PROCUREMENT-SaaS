package com.saas.procurement.user;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.common.error.ApiException;
import com.saas.procurement.common.error.ErrorCode;
import com.saas.procurement.company.CompanyMember;
import com.saas.procurement.company.CompanyMemberRepository;
import com.saas.procurement.company.MemberStatus;
import com.saas.procurement.user.dto.MembershipResponse;
import com.saas.procurement.user.dto.UserResponse;

@Service
public class UserProfileService {

	private final UserRepository userRepository;
	private final CompanyMemberRepository memberRepository;

	public UserProfileService(UserRepository userRepository, CompanyMemberRepository memberRepository) {
		this.userRepository = userRepository;
		this.memberRepository = memberRepository;
	}

	/**
	 * Loads the profile of the authenticated user. The id always comes from the verified JWT
	 * subject, never from a request parameter.
	 */
	@Transactional(readOnly = true)
	public UserResponse getProfile(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
		if (user.getStatus() == UserStatus.DISABLED) {
			throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
		}
		return toResponse(user, activeMemberships(userId));
	}

	@Transactional(readOnly = true)
	public List<CompanyMember> activeMemberships(UUID userId) {
		return memberRepository.findByUserIdAndStatus(userId, MemberStatus.ACTIVE);
	}

	public UserResponse toResponse(User user, List<CompanyMember> memberships) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getPlatformRole(),
				user.getStatus(),
				user.getEmailVerifiedAt() != null,
				user.getCreatedAt(),
				memberships.stream().map(MembershipResponse::from).toList());
	}

}
