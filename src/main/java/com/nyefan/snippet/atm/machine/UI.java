package com.nyefan.snippet.atm.machine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UI {

    private static Logger LOGGER = LoggerFactory.getLogger(UI.class);


    public static void main(String... args) {
        while (true) {
            try {
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

    public static void makeDeposit(double amout) {
        throw new UnsupportedOperationException("This is not yet implemented");
    }
}
