package com.myce.expo.controller.admin;

import com.myce.expo.dto.TicketQuantityRequest;
import com.myce.expo.service.info.ExpoTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
public class TicketController {
  private final ExpoTicketService expoTicketService;

  @PatchMapping("/quantity")
  public ResponseEntity<Void> updateRemainingQuantity(
      @RequestBody @Valid TicketQuantityRequest request
  ){
    expoTicketService.updateRemainingQuantity(request.getTicketId(), request.getQuantity());
    return ResponseEntity.ok().build();
  }
}
