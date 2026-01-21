package com.myce.notification.internal.service;

import com.myce.common.entity.BusinessProfile;
import com.myce.common.entity.type.TargetType;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.repository.BusinessProfileRepository;
import com.myce.expo.entity.Expo;
import com.myce.expo.repository.ExpoRepository;
import com.myce.notification.internal.dto.MailSendContextRequest;
import com.myce.notification.internal.dto.MailSendContextResponse;
import com.myce.reservation.dto.RecipientInfoDto;
import com.myce.reservation.repository.ReserverRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationInternalService {

    private final ReserverRepository reserverRepository;
    private final ExpoRepository expoRepository;
    private final BusinessProfileRepository businessProfileRepository;

    @Transactional
    public MailSendContextResponse mailSendContext(MailSendContextRequest req) {
//        List<RecipientInfoDto> recipientInfos = reserverRepository
//                .findReserversByFilter(req.getExpoId(),
//                        req.getEntranceStatus(), req.getName(), req.getPhone(),
//                        req.getReservationCode(), req.getTicketName())
//                .stream()
//                .map(r -> new RecipientInfoDto(r.getEmail(), r.getName()))
//                .toList();
        List<RecipientInfoDto> recipientInfos =
                reserverRepository.searchByFilter(
                        req.getExpoId(),
                        req.getEntranceStatus(),
                        req.getName(),
                        req.getPhone(),
                        req.getReservationCode(),
                        req.getTicketName()
                );

        String expoName = expoRepository.findById(req.getExpoId())
                .map( Expo::getTitle)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        Optional<BusinessProfile> profile =
                businessProfileRepository.findByTargetIdAndTargetType(req.getExpoId(), TargetType.EXPO);

        return new MailSendContextResponse(
                expoName,
                profile.map(BusinessProfile::getContactPhone).orElse(null),
                profile.map(BusinessProfile::getContactEmail).orElse(null),
                recipientInfos
        );
    }



    }

