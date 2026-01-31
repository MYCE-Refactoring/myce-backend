package com.myce.expo.controller.admin;

import com.myce.expo.dto.TicketQuantityRequest;
import com.myce.expo.repository.TicketRepository;
import com.myce.expo.service.info.ExpoTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
public class TicketController {
  private final ExpoTicketService expoTicketService;
  private final TicketRepository ticketRepository;

  @PatchMapping("/quantity")
  public ResponseEntity<Void> updateRemainingQuantity(
      @RequestBody @Valid TicketQuantityRequest request
  ){
    expoTicketService.updateRemainingQuantity(request.getTicketId(), request.getQuantity());
    return ResponseEntity.ok().build();
  }

}
