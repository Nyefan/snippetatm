package com.nyefan.snippet.atm.machine;

import com.nimbusds.jose.JOSEException;
import com.nyefan.snippet.atm.core.Token;
import com.nyefan.snippet.atm.rest.Client;
import com.nyefan.snippet.atm.rest.TestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

//TODO: separate into UI and UIService
public class UI {

    private static final Logger LOGGER = LoggerFactory.getLogger(UI.class);
    private static final KeyPair MACHINE_KEY_PAIR;
    //TODO: read from filesystem
    private static final Token.CurrencyUnit currencyUnit = Token.CurrencyUnit.USD;
    //TODO: for easier testing/mocking, initialize pin and starting account balance in this constructor
    private static final Client CLIENT = new TestClient();

    static {
        //TODO: retrieve from filesystem
        //TODO: EC key (atms are harder to upgrade than servers, and the signing/encrypting cost curve wrt length of key is flatter for ec than rsa)
        KeyPairGenerator generator = null;

        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(1); //no point in continuing if you can't get the key
        }

        generator.initialize(2048);
        MACHINE_KEY_PAIR = generator.generateKeyPair();
    }

    //TODO: make this a dao function
    public static PublicKey getMachinePublicKey() {
        return MACHINE_KEY_PAIR.getPublic();
    }

    public static void main(String... args) {
        while (true) {
            try {
                Thread.sleep(200); //don't burn up the cpu when we're testing compilation and debugging with the repl before this is implemented
                //main event loop
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public static double queryAccountBalance(String cardNumber, String pin) throws JOSEException {
        Token token = new Token(
                Token.TransactionType.BALANCE,
                new Token.Pin(pin),
                new Token.Card(cardNumber),
                new Token.TransactionAmount(0d, currencyUnit), //this is required but has no effect; will be fixed in later iteration
                TestClient.getCloudPublicKey(),
                MACHINE_KEY_PAIR.getPrivate());
        return CLIENT.getAccountBalance(token);
    }

    public static double withdrawCash(double amount, String cardNumber, String pin) throws JOSEException {
        Token token = new Token(
                Token.TransactionType.WITHDRAW,
                new Token.Pin(pin),
                new Token.Card(cardNumber),
                new Token.TransactionAmount(amount, currencyUnit),
                TestClient.getCloudPublicKey(),
                MACHINE_KEY_PAIR.getPrivate());
        return CLIENT.withdrawCash(token);
    }

    public static double depositCash(double amount, String cardNumber, String pin) throws JOSEException {
        Token token = new Token(
                Token.TransactionType.DEPOSIT,
                new Token.Pin(pin),
                new Token.Card(cardNumber),
                new Token.TransactionAmount(amount, currencyUnit),
                TestClient.getCloudPublicKey(),
                MACHINE_KEY_PAIR.getPrivate());
        return CLIENT.depositCash(token);
    }
}
