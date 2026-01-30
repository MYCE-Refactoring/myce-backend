package com.myce.reservation.service;

import com.myce.reservation.dto.GuestReservationRequest;

public interface ReservationGuestService {
  void updateGuestId(GuestReservationRequest request);
}
