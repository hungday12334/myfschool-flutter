package com.example.myfschool.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class SpeedSmsClient implements SmsClient {

    private final RestClient restClient;

    public SpeedSmsClient(
            @Value("${speedsms.base-url}") String baseUrl,
            @Value("${speedsms.access-token}") String accessToken
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers ->
                        headers.setBasicAuth(accessToken, "x"))
                .build();
    }

    @Override
    public String sendSms(String phoneNumber, String content) {

        Map<String, Object> request = Map.of(
                "to", List.of(phoneNumber),
                "content", content,
                "type", 2,
                "brandname", ""
        );

        JsonNode response = restClient.post()
                .uri("/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        if (response == null ||
                !"success".equals(response.path("status").asText()) ||
                !"00".equals(response.path("code").asText())) {

            throw new IllegalStateException(
                    "SpeedSMS rejected request: " + response
            );
        }

        return response.path("data")
                .path("tranId")
                .asText();
    }

    public JsonNode getUserInfo() {

        return restClient.get()
                .uri("/user/info")
                .retrieve()
                .body(JsonNode.class);
    }
}
