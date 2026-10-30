package com.example.myfschool.service;

import com.example.myfschool.dto.request.SendSMSRequest;
import com.example.myfschool.dto.response.SendSMSResponse;

public interface SMSService {
    public SendSMSResponse send(SendSMSRequest sendSMSRequest);
}
