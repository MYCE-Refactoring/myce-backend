package com.myce.advertisement.service;

import com.myce.advertisement.entity.Advertisement;

public interface AdStatusService {

    void verifyApprove(Advertisement ad);
    void verifyCancel(Advertisement ad);
    void verifyReject(Advertisement ad);
    void verifyPublish(Advertisement ad);
    void verifyDenyCancel(Advertisement ad);
    void verifyComplete(Advertisement ad);
    void verifyCancelByStatus(Advertisement ad);
    void verifyRequestRefundByStatus(Advertisement ad);
    void verifyCancelPendingApproval(Advertisement ad);
    void verifyCancelPendingPayment(Advertisement ad);
    void verifyRequestRefund(Advertisement ad);
    void verifyRequestPartialRefund(Advertisement ad);

}
