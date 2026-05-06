package com.bankflow.account.exception;

public class DuplicateAccountException extends RuntimeException {

    private final String field;
    private final String value;

    public DuplicateAccountException(String field, String value) {
        super(String.format(
                "Account already exists with %s: %s", field, value
        ));
        this.field = field;
        this.value = value;
    }

    public String getField() { return field; }
    public String getValue() { return value; }
}