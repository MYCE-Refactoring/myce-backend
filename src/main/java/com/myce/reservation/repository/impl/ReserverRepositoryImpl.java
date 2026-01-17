package com.myce.reservation.repository.impl;

import com.myce.expo.entity.QTicket;
import com.myce.notification.internal.dto.RecipientInfoDto;
import com.myce.qrcode.entity.QQrCode;
import com.myce.qrcode.entity.code.QrCodeStatus;
import com.myce.reservation.entity.QReservation;
import com.myce.reservation.entity.QReserver;
import com.myce.reservation.entity.code.ReservationStatus;
import com.myce.system.entity.AdFeeSetting;
import com.myce.system.entity.QAdFeeSetting;
import com.myce.system.entity.QAdPosition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class ReserverRepositoryImpl implements ReserverRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RecipientInfoDto> searchByFilter(Long expoId, String entranceStatus, String name,
                                                String phone, String reservationCode, String ticketName) {

        QReserver rv = QReserver.reserver;
        QReservation r = QReservation.reservation;
        QTicket t = QTicket.ticket;
        QQrCode qc = QQrCode.qrCode;

        BooleanBuilder where = new BooleanBuilder();

        where.and(r.expo.id.eq(expoId));
        where.and(r.status.eq(ReservationStatus.CONFIRMED));

        where.and(reserverNameContains(rv, name));
        where.and(phoneContains(rv, phone));
        where.and(reservationCodeContains(r, reservationCode));
        where.and(ticketNameEq(t, ticketName));
        where.and(entranceStatusCondition(entranceStatus, qc));

        return queryFactory
                .select(
                        com.querydsl.core.types.Projections.constructor(
                                RecipientInfoDto.class,
                                rv.email,
                                rv.name
                        )
                )
                .from(rv)
                .join(rv.reservation, r)
                .join(r.ticket, t)
                .leftJoin(qc).on(qc.reserver.eq(rv))
                .where(where)
                .distinct()
                .fetch();
    }

    private BooleanExpression reserverNameContains(QReserver rv, String name) {
        return (name == null || name.isBlank())
                ? null
                : rv.name.containsIgnoreCase(name);
    }

    private BooleanExpression phoneContains(QReserver rv, String phone) {
        return (phone == null || phone.isBlank())
                ? null
                : rv.phone.contains(phone);
    }

    private BooleanExpression reservationCodeContains(QReservation r, String code) {
        return (code == null || code.isBlank())
                ? null
                : r.reservationCode.contains(code);
    }

    private BooleanExpression ticketNameEq(QTicket t, String ticketName) {
        return (ticketName == null || ticketName.isBlank())
                ? null
                : t.name.eq(ticketName);
    }

    private BooleanExpression entranceStatusCondition(String entranceStatus, QQrCode qc) {
        if (entranceStatus == null) return null;

        return switch (entranceStatus) {
            case "입장 완료" -> qc.status.eq(QrCodeStatus.USED);
            case "티켓 만료" -> qc.status.eq(QrCodeStatus.EXPIRED);
            case "발급 대기" -> qc.id.isNull();
            case "입장 전" -> qc.status.in(
                    QrCodeStatus.APPROVED,
                    QrCodeStatus.ACTIVE
            );
            default -> null;
        };
    }


}
