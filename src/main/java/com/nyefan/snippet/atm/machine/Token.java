package com.nyefan.snippet.atm.machine;

import com.nimbusds.jwt.EncryptedJWT;

public final class Token {

    private enum TransactionType {
        PIN("PIN"),
        BALANCE("BALANCE"),
        DEPOSIT("DEPOSIT"),
        WITHDRAW("WITHDRAW");

        private String type;

        TransactionType(String type) {
            this.type = type;
        }
    }

    public Token(TransactionType transactionType) {

    }
}
