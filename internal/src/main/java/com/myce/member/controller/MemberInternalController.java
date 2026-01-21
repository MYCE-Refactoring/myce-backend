package com.myce.member.controller;

import com.myce.member.dto.MemberInfoResponse;
import com.myce.member.service.MemberInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/members")
@RequiredArgsConstructor
public class MemberInternalController {

    private final MemberInfoService memberInfoService;

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberInfoResponse> getMemberInfo(@PathVariable Long memberId) {
        MemberInfoResponse response = memberInfoService.getMemberInfo(memberId);

        return ResponseEntity.ok(response);
    }
}
