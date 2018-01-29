package com.nyefan.snippet.atm.rest;

import com.nyefan.snippet.atm.core.Token;

import java.util.Random;

public class TestClient extends Client {

    private Random random = new Random();
    private double accountBalance = random.nextDouble() * 10000;
    private String pin = "asdf123";

    //TODO: String token; Token.parse(token, Token.getMachinePublicKey, CLOUD_KEY_PAIR.getPrivate())
    public double getAccountBalance(Token token) {
        if (!token.getTransactionType().equals(Token.TransactionType.BALANCE)
                || !token.getPin().verify(pin)) {
            throw new IllegalArgumentException("http 403 (maybe 400 or 422) encountered when querying account balance");
        }
        return accountBalance;
    }

    public double withdrawCash(Token token) {
        if (!token.getTransactionType().equals(Token.TransactionType.WITHDRAW)
                || !token.getPin().verify(pin)) {
            throw new IllegalArgumentException("http 403 (maybe 400 or 422) encountered when withdrawing funds");
        }

        //this is a little weird
        //TODO: rethink TransactionAmount/Token api
        double accountDelta = token.getTransactionAmount().inCurrencyUnit(Token.CurrencyUnit.USD).getTransactionAmount();
        if (accountDelta > accountBalance) {
            throw new IllegalArgumentException("http 403 - insufficient funds");
        }
        accountBalance -= accountDelta;
        return accountBalance;
    }

    public double depositCash(Token token) {
        if (!token.getTransactionType().equals(Token.TransactionType.DEPOSIT)
                || !token.getPin().verify(pin)) {
            throw new IllegalArgumentException("http 403 (maybe 400 or 422) encountered when depositing funds");
        }
        //this is a little weird
        //TODO: rethink TransactionAmount/Token api
        double accountDelta = token.getTransactionAmount().inCurrencyUnit(Token.CurrencyUnit.USD).getTransactionAmount();
        accountBalance += accountDelta;
        return accountBalance;
    }

    //Under the current logic, this should never be called
    public double revertWithdrawal(Token token) {
        throw new UnsupportedOperationException("This functionality is not yet implemented");
    }
}
