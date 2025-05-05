package inha.gdgoc.domain.auth.service;

import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    @Value("${spring.mail.username}")
    private String sender;

    @Autowired(required = false)
    private final JavaMailSender mailSender;

    public String sendAuthCode(String toEmail) {
        String code = String.format("%06d", new Random().nextInt(999999));
        String subject = "[GDGoC INHA] 비밀번호 재설정 인증 코드";
        String text = "5분 내에 인증을 완료해주세요!\n인증 코드: " + code;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(sender);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);

        return code;
    }

}
