package com.myce.system.repository.querydsl;

import com.myce.system.entity.AdFeeSetting;
import com.myce.system.entity.QAdFeeSetting;
import com.myce.system.entity.QAdPosition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class AdFeeSettingRepositoryImpl implements AdFeeSettingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AdFeeSetting> search(Long positionId, String name, Pageable pageable) {

        QAdFeeSetting adFee = QAdFeeSetting.adFeeSetting;
        QAdPosition adPosition = QAdPosition.adPosition;

        List<AdFeeSetting> content = queryFactory
                .selectFrom(adFee)
                .join(adFee.adPosition, adPosition).fetchJoin()
                .where(
                        positionEq(positionId),
                        nameContains(name)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(adFee.count())
                .from(adFee)
                .where(
                        positionEq(positionId),
                        nameContains(name)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression positionEq(Long positionId) {
        return positionId == null ? null
                : QAdFeeSetting.adFeeSetting.adPosition.id.eq(positionId);
    }

    private BooleanExpression nameContains(String name) {
        return (name == null || name.isBlank()) ? null
                : QAdFeeSetting.adFeeSetting.name.contains(name);
    }
}
