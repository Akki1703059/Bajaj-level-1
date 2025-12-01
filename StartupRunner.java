package com.example.bh;

import com.example.bh.dto.GenerateWebhookRequest;
import com.example.bh.dto.GenerateWebhookResponse;
import com.example.bh.dto.SubmitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

    @Value("${app.name}")
    private String name;

    @Value("${app.regNo}")
    private String regNo;

    @Value("${app.email}")
    private String email;

    @Value("${app.finalQuery}")
    private String finalQuery;

    @Value("${bfhl.baseUrl}")
    private String baseUrl;

    @Value("${bfhl.generatePath}")
    private String generatePath;

    @Value("${bfhl.submitPath}")
    private String submitPath;

    private final RestTemplate restTemplate;

    public StartupRunner(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting flow...");
        // 1) Generate Webhook
        String generateUrl = baseUrl + generatePath;
        GenerateWebhookRequest req = new GenerateWebhookRequest(name, regNo, email);
        log.info("POST {}", generateUrl);
        ResponseEntity<GenerateWebhookResponse> genResp = restTemplate.postForEntity(
                generateUrl, req, GenerateWebhookResponse.class);

        if (!genResp.getStatusCode().is2xxSuccessful() || genResp.getBody() == null) {
            log.error("Failed to generate webhook. Status: {}", genResp.getStatusCode());
            return;
        }
        GenerateWebhookResponse body = genResp.getBody();
        log.info("Received webhook: {}", body.getWebhook());
        log.info("Received accessToken (JWT): {}", body.getAccessToken());

        // 2) Decide which SQL question to open based on last two digits of regNo
        Integer lastTwo = extractLastTwoDigits(regNo);
        if (lastTwo == null) {
            log.warn("Could not extract last two digits from regNo='{}'. Please ensure regNo contains digits.", regNo);
        } else {
            boolean isOdd = (lastTwo % 2) == 1;
            String q1 = "https://drive.google.com/file/d/1IeSI6l6KoSQAFfRihIT9tEDICtoz-G/view?usp=sharing";
            String q2 = "https://drive.google.com/file/d/143MR5cLFrlNEuHzzWJ5RHnEWuijuM9X/view?usp=sharing";
            log.info("regNo last two digits = {} => {} => Open {}", lastTwo, (isOdd ? "ODD" : "EVEN"), (isOdd ? q1 : q2));
        }

        // 3) Store your SQL locally (optional requirement)
        Path out = Path.of("final_query.sql");
        Files.writeString(out, finalQuery == null ? "" : finalQuery);
        log.info("Wrote your SQL to {}", out.toAbsolutePath());

        // 4) Submit your SQL to the returned webhook (fallback to testWebhook if webhook was null)
        String submitUrl = (body.getWebhook() != null && !body.getWebhook().isBlank())
                ? body.getWebhook()
                : (baseUrl + submitPath);

        log.info("Submitting final query to: {}", submitUrl);
        boolean submitted = submitWithAuth(submitUrl, body.getAccessToken(), finalQuery);

        if (!submitted) {
            log.error("Submission failed. Please verify the token and try again.");
        } else {
            log.info("Submission done. Check server response above for confirmation.");
        }
    }

    private boolean submitWithAuth(String url, String token, String finalQuery) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token); // as per spec
        Map<String, String> payload = Map.of("finalQuery", finalQuery);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            log.info("Submit response (Authorization: <token>): status={}, body={}", resp.getStatusCode(), resp.getBody());
            return resp.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("401 with raw token. Retrying once with Bearer prefix...");
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Map<String, String>> entity2 = new HttpEntity<>(payload, headers);
            ResponseEntity<String> resp2 = restTemplate.postForEntity(url, entity2, String.class);
            log.info("Submit response (Authorization: Bearer <token>): status={}, body={}", resp2.getStatusCode(), resp2.getBody());
            return resp2.getStatusCode().is2xxSuccessful();
        }
    }

    private Integer extractLastTwoDigits(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(\d{1,})").matcher(text);
        String lastDigits = null;
        while (m.find()) {
            lastDigits = m.group(1);
        }
        if (lastDigits == null || lastDigits.isEmpty()) return null;
        if (lastDigits.length() == 1) return Integer.parseInt(lastDigits);
        String lastTwoStr = lastDigits.substring(lastDigits.length() - 2);
        return Integer.parseInt(lastTwoStr);
    }
}
