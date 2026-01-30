package com.myce.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    @Value("${external.auth.value}")
    private String internalAuthValue;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.debug("[InternalAuthFilter] Receive request. method={}, uri={}", method, uri);

        String authValue = request.getHeader(InternalHeaderKey.INTERNAL_AUTH);
        if (authValue == null || !authValue.equals(internalAuthValue)) {
            log.debug("[InternalAuthFilter] Not exist auth value. method={}, uri={}, value={}", method, uri, authValue);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
