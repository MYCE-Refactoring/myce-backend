package com.myce.expo.controller;

import com.myce.expo.dto.AdminCodeInfo;
import com.myce.expo.dto.ExpoInfoListResponse;
import com.myce.expo.dto.ExpoInfoResponse;
import com.myce.expo.service.ExpoInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/expos")
@RequiredArgsConstructor
public class ExpoInfoController {

    private final ExpoInfoService expoInfoService;

    @GetMapping("/recent")
    public ResponseEntity<ExpoInfoListResponse> getRecentExpoInfo(@RequestParam("count")int count) {
        ExpoInfoListResponse response = expoInfoService.getRecentExpoInfos(count);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{expo-id}")
    public ResponseEntity<ExpoInfoResponse> getExpoInfo(@PathVariable("expo-id")Long expoId) {
        ExpoInfoResponse response = expoInfoService.getExpoInfo(expoId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/{admin-id}")
    public ResponseEntity<AdminCodeInfo> getAdminCodeInfo(@PathVariable("admin-id")Long adminId) {
        AdminCodeInfo response = expoInfoService.getAdminCodeInfo(adminId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/access/check")
    public ResponseEntity<Void> checkExpoAccess(
            @RequestParam Long expoId,
            @RequestParam Long adminId) {

        boolean isAccessible = expoInfoService.isAdminAccessToExpo(expoId, adminId);

        if (isAccessible) return ResponseEntity.ok().build();
        else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping("/owner/check")
    public ResponseEntity<Void> checkExpoOwner(
            @RequestParam Long expoId,
            @RequestParam Long memberId) {

        boolean isOwner = expoInfoService.isMemberOwnerToExpo(expoId, memberId);

        if (isOwner) return ResponseEntity.ok().build();
        else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
