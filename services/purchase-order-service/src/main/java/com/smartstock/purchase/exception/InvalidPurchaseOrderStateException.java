package com.smartstock.purchase.exception;

import org.springframework.http.HttpStatus;

public class InvalidPurchaseOrderStateException extends BusinessException {

    public InvalidPurchaseOrderStateException(String action, String currentStatus) {
        super("INVALID_PO_STATE",
                "Cannot perform '" + action + "' on purchase order with status: " + currentStatus,
                HttpStatus.CONFLICT);
    }
}
