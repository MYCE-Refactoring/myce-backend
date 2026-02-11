package com.myce.client.payment.dto;

public class PaymentEndPoints {

    // 결제 검증/저장
    public static final String PAYMENT_VERIFY = "/internal/payment";
    public static final String PAYMENT_VERIFY_VBANK = "/internal/payment/vbank";
    public static final String PAYMENT_WEBHOOK = "/internal/payment/webhook";

    // 결제 조회
    public static final String PAYMENT_BY_TARGET = "/internal/payment/by-target";
    public static final String PAYMENT_BY_TARGETS = "/internal/payment/by-targets";

    // 환불
    public static final String REFUND = "/internal/payment/refund";
    public static final String REFUND_REQUEST = "/internal/payment/refund-request";
    public static final String REFUND_REJECT = "/internal/payment/refund-reject";
    public static final String IMP_UID = "/internal/payment/imp-uid";
    public static final String REFUND_BY_TARGET = "/internal/payment/refunds/by-target";
    public static final String REFUND_SUM = "/internal/payment/refunds/sum";
}
