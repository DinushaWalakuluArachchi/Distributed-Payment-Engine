package com.paymentengine.apigateway.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class IdempotencyFilter implements Filter {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String KEY_PREFIX = "Idempotency:";

    private final RedissonClient redisson;

    @Value("${idempotency.ttl-seconds:86400}")
    private long ttlSecond;


    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;


        if (!"POST".equalsIgnoreCase(request.getMethod())){
            chain.doFilter(req,res);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        if (idempotencyKey == null || idempotencyKey.isBlank()){
            log.warn("POST request missing Idempotency-Key header");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write( "{\"error\":\"Idempotency-Key header required for POST requests\"}");
            return;
        }

        String redisKey = KEY_PREFIX + idempotencyKey;
        RBucket<String> bucket = redisson.getBucket(redisKey);
        String cachResponse = bucket.get();

        if (cachResponse != null){
            log.info("Idempotency hit for key: {} - replaying cached response", idempotencyKey);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setHeader("X-Idempotency-Replayed","true");
            response.getWriter().write(cachResponse);
            return;
        }

        CachingResponseWrapper wrapperResponse = new CachingResponseWrapper(response);
        chain.doFilter(req, wrapperResponse);

        String responseBody = new String(
                wrapperResponse.getResponseBody(), StandardCharsets.UTF_8
        );

        int statusCode = wrapperResponse.getStatus();

        if (statusCode >= 200 && statusCode < 300){
            bucket.set(responseBody, ttlSecond, TimeUnit.SECONDS);
            log.info("Idempotency Key stored: {} (TTL {}s)", idempotencyKey, ttlSecond);
        }

        response.setStatus(statusCode);
        response.setContentType(wrapperResponse.getContentType());
        wrapperResponse.copyBodyToResponse();

    }
}
