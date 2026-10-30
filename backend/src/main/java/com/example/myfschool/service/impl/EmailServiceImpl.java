package com.example.myfschool.service.impl;

import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendPasswordResetCode(String recipientEmail, String recipientName, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("MyFSchool - Mã đặt lại mật khẩu");
        message.setText("Xin chào " + recipientName + ",\n\n"
                + "Mã xác thực để đặt lại mật khẩu MyFSchool của bạn là: " + code + "\n"
                + "Mã có hiệu lực trong 10 phút. Không chia sẻ mã này với bất kỳ ai.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.");

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Unable to send password-reset email ({})", exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.EMAIL_DELIVERY_FAILED);
        }
    }
}
