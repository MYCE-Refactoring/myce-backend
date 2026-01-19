package com.myce.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myce.auth.dto.CustomUserDetails;
import com.myce.auth.dto.type.LoginType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String INTERNAL_AUTH_VALUE;
    private final String GATEWAY_AUTH_VALUE;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        log.debug("[JwtAuthenticationFilter] Input uri={}, method={}", uri, method);

        String authValue = request.getHeader(InternalHeaderKey.INTERNAL_AUTH);
        if (authValue == null ||
                (!authValue.equals(INTERNAL_AUTH_VALUE)
                        && !authValue.equals(GATEWAY_AUTH_VALUE))) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }


        String role = request.getHeader(InternalHeaderKey.INTERNAL_ROLE);
        String loginTypeStr = request.getHeader(InternalHeaderKey.INTERNAL_LOGIN_TYPE);
        String memberIdStr = request.getHeader(InternalHeaderKey.INTERNAL_MEMBER_ID);

        if (role == null || loginTypeStr == null || memberIdStr == null) {
            log.info("Not exist user info. role={}, loginType={}, memberId={}", role, loginTypeStr, memberIdStr);
            filterChain.doFilter(request, response);
            return;
        }

        // 사용자 정보 확인
        LoginType loginType = LoginType.fromString(loginTypeStr);
        Long memberId = Long.valueOf(memberIdStr);

        log.info("[AuthenticationFilter] JWT find userDetails by memberId: {}, loginType: {}",
                memberIdStr, loginTypeStr);
        CustomUserDetails userDetails;
        try {
            userDetails = CustomUserDetails.builder()
                    .loginType(loginType)
                    .memberId(memberId)
                    .role(role)
                    .build();
        } catch (UsernameNotFoundException e) {
            log.error("[AuthenticationFilter] failed to find user details by memberId: {}", memberIdStr, e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        log.debug("[AuthenticationFilter] Success to find userDetail: {}", userDetails);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 다음 필터로
        filterChain.doFilter(request, response);
    }


    private void setErrorResponse(HttpServletResponse response, String code) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, String> body = Map.of("code", code);
        log.info("[JwtAuthenticationFilter] Set error response: {}", body);
        new ObjectMapper().writeValue(response.getWriter(), body);
    }
}
