package com.myce.payment.service.refund;

import com.myce.payment.dto.PaymentImpUidForRefundRequest;
import com.myce.payment.dto.PaymentRefundRequest;
import com.myce.payment.dto.AdRefundRequest;
import com.myce.payment.dto.RefundInternalResponse;

import java.util.Map;

public interface PaymentRefundService {
    RefundInternalResponse refundPayment(PaymentRefundRequest request);

    String getImpUidForRefund(PaymentImpUidForRefundRequest request);
    
    RefundInternalResponse processAdRefund(AdRefundRequest request);
}
