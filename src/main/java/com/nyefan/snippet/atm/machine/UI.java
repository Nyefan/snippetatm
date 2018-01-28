package com.nyefan.snippet.atm.machine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class UI {

    private static Logger LOGGER = LoggerFactory.getLogger(UI.class);

    private static final KeyPair MACHINE_KEY_PAIR;

    static {
        //TODO: retrieve from filesystem
        //TODO: EC key (atms are harder to upgrade than servers, and the signing/encrypting cost curve wrt length of key is flatter for ec than rsa)
        KeyPairGenerator generator = null;

        while (generator == null) { //can't do anything without the key - just keep trying till you get it
            try {
                generator = KeyPairGenerator.getInstance("RSA");
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error(e.getMessage(), e);
            }
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

    public static String requestPin() {
        throw new UnsupportedOperationException("This is not yet implemented");
    }

    public static double queryAccountBalance() {
        throw new UnsupportedOperationException("This is not yet implemented");
    }

    public static void withdrawCash(double amount) {
        throw new UnsupportedOperationException("This is not yet implemented");
    }

    public static void makeDeposit(double amount) {
        throw new UnsupportedOperationException("This is not yet implemented");
    }
}
