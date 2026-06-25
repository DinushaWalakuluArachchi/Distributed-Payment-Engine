package com.paymentengine.apigateway.router;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentRouter {

    private final RestTemplate restTemplate;

    @Value("${services.payment-service}")
    private String paymentServiceUrl;

    @PostMapping("/payments")
    public ResponseEntity<String> initiatePayment(@RequestBody String body, HttpServletRequest request){
        return forward(paymentServiceUrl + "/payments", body, request);

    }
    @GetMapping("/payments/{id}")
    public ResponseEntity<String> getPayment(@PathVariable String id, HttpServletRequest request){
        return forward(paymentServiceUrl + "/payments/" + id, null, request);
    }

    private ResponseEntity<String> forward(String targetUrl, String body, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()){
            String name = headerNames.nextElement();
            headers.set(name, request.getHeader(name));
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpEntity<String> entity = new HttpEntity<>(body,headers);

        log.debug("Forwarding {} to {}", method, targetUrl);

        return restTemplate.exchange(targetUrl, method, entity, String.class);
    }
}
