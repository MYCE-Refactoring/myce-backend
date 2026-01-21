package com.myce.member.service;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.member.entity.Member;
import com.myce.member.dto.MemberInfoResponse;
import com.myce.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberInfoService {

    private final MemberRepository memberRepository;

    public MemberInfoResponse getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->new CustomException(CustomErrorCode.MEMBER_NOT_EXIST));

        return new MemberInfoResponse(
            member.getName(),
            member.getRole().name(),
            member.getMemberGrade().getDescription()
        );
    }
}
