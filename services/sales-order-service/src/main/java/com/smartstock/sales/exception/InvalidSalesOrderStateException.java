package com.smartstock.sales.exception;

public class InvalidSalesOrderStateException extends BusinessException {
    public InvalidSalesOrderStateException(String action, String currentStatus) {
        super("SO_INVALID_STATE",
                "Cannot perform action '" + action + "' on sales order in status '" + currentStatus + "'");
    }
}
