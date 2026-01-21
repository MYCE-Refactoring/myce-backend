package com.myce.auth.security.provider;

import com.myce.auth.dto.CustomUserDetails;
import com.myce.auth.dto.type.LoginType;
import com.myce.auth.security.CustomAuthenticationToken;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.member.entity.Member;
import com.myce.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAuthenticationProvider implements AuthenticationProvider {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final UserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomAuthenticationToken token = (CustomAuthenticationToken) authentication;

        // Member타입이 아니면 다음 provider로 넘어감
        if (token.getLoginType() != LoginType.MEMBER) {
            return null;
        }

        String loginId = token.getName();
        String password = token.getCredentials().toString();
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.MEMBER_NOT_EXIST));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new BadCredentialsException("Invalid password for member.");
        }

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(loginId);
        return new CustomAuthenticationToken(userDetails, null, userDetails.getAuthorities(), LoginType.MEMBER);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return CustomAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
