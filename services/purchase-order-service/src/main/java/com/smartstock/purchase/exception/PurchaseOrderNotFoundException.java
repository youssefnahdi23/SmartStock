package com.smartstock.purchase.exception;

import org.springframework.http.HttpStatus;

public class PurchaseOrderNotFoundException extends BusinessException {

    public PurchaseOrderNotFoundException(String poId) {
        super("PURCHASE_ORDER_NOT_FOUND",
                "Purchase order not found: " + poId,
                HttpStatus.NOT_FOUND);
    }
}
