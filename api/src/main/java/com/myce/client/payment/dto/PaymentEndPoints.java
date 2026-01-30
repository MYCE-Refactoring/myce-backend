package com.myce.client.payment.dto;

public class PaymentEndPoints {

    // 결제 검증/저장
    public static final String PAYMENT_VERIFY = "/payment";
    public static final String PAYMENT_VERIFY_VBANK = "/payment/vbank";
    public static final String PAYMENT_WEBHOOK = "/payment/webhook";

    // 결제 조회
    public static final String PAYMENT_BY_TARGET = "/payment/by-target";
    public static final String PAYMENT_BY_TARGETS = "/payment/by-targets";

    // 환불
    public static final String REFUND = "/payment/refund";
    public static final String REFUND_REQUEST = "/payment/refund-request";
    public static final String REFUND_REJECT = "/payment/refund-reject";
    public static final String IMP_UID = "/payment/imp-uid";
    public static final String REFUND_BY_TARGET = "/payment/refunds/by-target";
    public static final String REFUND_SUM = "/payment/refunds/sum";
}
