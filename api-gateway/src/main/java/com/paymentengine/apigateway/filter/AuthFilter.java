package com.paymentengine.apigateway.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AuthFilter implements Filter {
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String VALID_KEYS_SET = "gateway:valid-api-keys";

    private final RedissonClient redisson;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()){
            log.warn("Request Rejected - missing X-API-Key header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing X-API-Key header\"}");
            return;
        }
        RSet<String> validKeys = redisson.getSet(VALID_KEYS_SET);
        if (!validKeys.contains(apiKey)){
            log.warn("Request rejected - invalid API key: {}" , apiKey);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid API key\"}");
            return;
        }
        log.debug("Auth passed for key: {}", apiKey);
        chain.doFilter(req,response);
    }
}
