package com.myce.auth.service.impl;

import com.myce.auth.dto.VerificationEmailRequest;
import com.myce.auth.dto.VerifyEmailCodeRequest;
import com.myce.auth.dto.type.VerificationType;
import com.myce.auth.entity.EmailVerificationInfo;
import com.myce.auth.repsitory.EmailVerificationRepository;
import com.myce.auth.service.AuthVerificationService;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.util.RandomCodeGenerateUtil;
import com.myce.notification.component.MailSendComponent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthVerificationServiceImpl implements AuthVerificationService {

    private static final int RANDOM_CODE_LENGTH = 6;
    private static final int LIMIT_TIME = 10;

    private final RandomCodeGenerateUtil randomCodeGenerateUtil;
    private final EmailVerificationRepository emailVerificationRepository;
    private final MailSendComponent mailSendComponent;

    @Override
    public void sendVerificationMail(VerificationEmailRequest request) {
        String email = request.getEmail();
        String code = randomCodeGenerateUtil.generateRandomCode(RANDOM_CODE_LENGTH);
        VerificationType verificationType = request.getVerificationType();


//        MessageTemplate messageTemplate = messageTemplateService
//                .getMessageForVerification(verificationType.getDescription(), code, String.valueOf(LIMIT_TIME));
//
//        mailSendComponent.sendMail(
//                email,
//                messageTemplate.getSubject(),
//                messageTemplate.getContent()
//                );
        mailSendComponent.sendVerificationMail(email, verificationType.getDescription(), code,String.valueOf(LIMIT_TIME));

        log.debug("[EmailVerification-{}] Successfully sent verification email to {}", verificationType.name(), email);
        EmailVerificationInfo emailVerification = new EmailVerificationInfo(email, code, LocalDateTime.now());
        emailVerificationRepository.save(emailVerification, verificationType.name(), LIMIT_TIME);
    }

    @Override
    public void verifyCode(VerifyEmailCodeRequest request) {
        String typeName = request.getVerificationType().name();
        String email = request.getEmail();
        EmailVerificationInfo verificationInfo = emailVerificationRepository.findByEmail(typeName, email);
        log.debug("[EmailVerification-{}] Verifying code {}", typeName, email);
        if (verificationInfo == null) throw new CustomException(CustomErrorCode.EXPIRED_VERIFICATION_TIME);

        if(!verificationInfo.getCode().equals(request.getCode()))
            throw new CustomException(CustomErrorCode.INVALID_VERIFICATION_CODE);
    }
}
