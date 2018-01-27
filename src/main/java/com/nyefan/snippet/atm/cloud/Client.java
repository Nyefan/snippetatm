package com.nyefan.snippet.atm.cloud;

import com.nyefan.snippet.atm.machine.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public abstract class Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
    private static final KeyPair KEY_PAIR;

    static {
        //TODO: retrieve from filesystem
        //TODO: EC key (atms are harder to upgrade, and the signing/encrypting cost curve wrt length of key is flatter for ec than rsa)
        KeyPairGenerator generator = null;

        while (generator == null) { //can't do anything without the key - just keep trying till you get it
            try {
                generator = KeyPairGenerator.getInstance("RSA");
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        generator.initialize(2048);
        KEY_PAIR = generator.generateKeyPair();
    }

    public static PublicKey getPublicKey() {
        return KEY_PAIR.getPublic();
    }

    public abstract double getAccountBalance(Token token);

    public abstract double withdrawCash(Token token);

    public abstract double depositCash(Token token);

}
