package com.myce.notification.internal.controller;

import com.myce.common.permission.ExpoAdminAccessValidate;
import com.myce.notification.internal.dto.EnsureRequest;
import com.myce.notification.internal.dto.MailSendContextRequest;
import com.myce.notification.internal.dto.MailSendContextResponse;
import com.myce.notification.internal.service.NotificationInternalService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/notifications/ensure")
@RequiredArgsConstructor
@Slf4j
public class NotificationInternalController {

    private final ExpoAdminAccessValidate expoAdminAccessValidate;
    private final NotificationInternalService notificationInternalService;

    @PostMapping("/viewable")
    public ResponseEntity<Void> ensureViewable(@RequestBody EnsureRequest req) {
        log.info("[ensureViewable] expoId={}, memberId={}, loginType={}, permission={}",
                req.getExpoId(), req.getMemberId(), req.getLoginType(), req.getPermission());
        expoAdminAccessValidate.ensureViewable(
                req.getExpoId(), req.getMemberId(), req.getLoginType(), req.getPermission()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/editable")
    public ResponseEntity<Void> ensureEditable(@RequestBody EnsureRequest req) {
        expoAdminAccessValidate.ensureEditable(
                req.getExpoId(), req.getMemberId(), req.getLoginType(), req.getPermission()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mail-context")
    public ResponseEntity<MailSendContextResponse> ensureMailContext(@RequestBody MailSendContextRequest req) {
        MailSendContextResponse response = notificationInternalService.mailSendContext(req);
        return ResponseEntity.ok(response);
    }
}
