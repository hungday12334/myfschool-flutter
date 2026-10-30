package com.example.myfschool.service.impl;

import com.example.myfschool.client.SpeedSmsClient;
import com.example.myfschool.dto.request.SendSMSRequest;
import com.example.myfschool.dto.response.SendSMSResponse;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.service.SMSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@RequiredArgsConstructor
@Slf4j
@Service
public class SMSServiceImpl implements SMSService {
    private final SpeedSmsClient sendSms;
    @Override
    public SendSMSResponse send(SendSMSRequest sendSMSRequest) {
        String fromPhoneNumber = sendSMSRequest.phoneNumber();
        Random random = new Random();
        int randomNumber = random.nextInt(900000) + 100000;
        String message = "Mã xác thực số điện thoại của bạn tại hệ thống MyFSchool của bạn là: " + randomNumber;
        try{
            return new SendSMSResponse(sendSms.sendSms(fromPhoneNumber, message));
        }catch(Exception e){
            log.error("Speed SMS error message: {}", e.getMessage());
            throw new BusinessException(ErrorCode.UNSUCCESS_SENDING_SMS, "Lỗi trong việc gửi SMS" + e.getMessage());
        }

    }
}
