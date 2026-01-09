package com.myce.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myce.auth.dto.CustomUserDetails;
import com.myce.auth.dto.type.LoginType;
import com.myce.auth.security.config.SecurityEndpoints;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String INVALID_TOKEN_CODE = "INVALID_TOKEN";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();


    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        log.debug("[JwtAuthenticationFilter] Input uri={}, method={}", uri, method);
        if (isPermitAll(method, uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String role = request.getHeader(InternalHeaderKey.INTERNAL_ROLE);
        String loginTypeStr = request.getHeader(InternalHeaderKey.INTERNAL_LOGIN_TYPE);
        String memberIdStr = request.getHeader(InternalHeaderKey.INTERNAL_MEMBER_ID);

        if (role == null || loginTypeStr == null || memberIdStr == null) {
            log.info("Not exist user info. role={}, loginType={}, memberId={}", role, loginTypeStr, memberIdStr);
            setErrorResponse(response, INVALID_TOKEN_CODE);
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

    private boolean isPermitAll(String method, String path) {
        if(isExist(SecurityEndpoints.ETC_PERMIT_ALL, path)) return true;

        if(HttpMethod.GET.name().equals(method)) {
            return isExist(SecurityEndpoints.GET_PERMIT_ALL, path);
        }
        if(HttpMethod.POST.name().equals(method)) {
            return isExist(SecurityEndpoints.POST_PERMIT_ALL, path);
        }
        if(HttpMethod.PATCH.name().equals(method)) {
            return isExist(SecurityEndpoints.PATCH_PERMIT_ALL, path);
        }
        if(HttpMethod.DELETE.name().equals(method)) {
            return isExist(SecurityEndpoints.DELETE_PERMIT_ALL, path);
        }

        return false;
    }

    private boolean isExist(String[] patterns, String path) {
        return Arrays.stream(patterns).anyMatch(p -> pathMatcher.match(p, path));
    }
}
