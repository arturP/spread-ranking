package io.artur.interview.kanga.spread_ranking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Value("${app.security.api-token:ABC123}")
    private String validApiToken;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            
            if (isValidToken(token)) {
                log.debug("Valid bearer token provided for request: {}", request.getRequestURI());
                
                // Create authentication token with API_USER role
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        "api-user", 
                        null, 
                        List.of(new SimpleGrantedAuthority("ROLE_API_USER"))
                    );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("Invalid bearer token provided for request: {}", request.getRequestURI());
            }
        } else {
            log.debug("No bearer token provided for request: {}", request.getRequestURI());
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isValidToken(String token) {
        return validApiToken.equals(token);
    }
}