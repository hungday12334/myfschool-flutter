package com.example.myfschool.controller;

import com.example.myfschool.dto.request.SendSMSRequest;
import com.example.myfschool.dto.response.ApiResponse;
import com.example.myfschool.dto.response.SendSMSResponse;
import com.example.myfschool.service.SMSService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
public class SMSController {
    private final SMSService smsService;
    @PostMapping("send")
    public ResponseEntity<ApiResponse<SendSMSResponse>> send(@RequestBody SendSMSRequest sendSMSRequest){
        return ResponseEntity.ok(ApiResponse.success("Gửi mail thành công", smsService.send(sendSMSRequest)));
    }
}
