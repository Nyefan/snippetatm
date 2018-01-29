package com.nyefan.snippet.atm.machine;

import com.nimbusds.jose.JOSEException;
import com.nyefan.snippet.atm.core.Token;
import com.nyefan.snippet.atm.rest.Client;
import com.nyefan.snippet.atm.rest.TestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Scanner;

//TODO: separate into UI and UIService
public class UI {

    private static final Logger LOGGER = LoggerFactory.getLogger(UI.class);
    private static final KeyPair MACHINE_KEY_PAIR;
    //TODO: read from filesystem
    private static final Token.CurrencyUnit currencyUnit = Token.CurrencyUnit.USD;
    //TODO: for easier testing/mocking, initialize pin and starting account balance in this constructor
    private static final Client CLIENT = new TestClient();
    private static final boolean customerFriendly = false;

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
                Thread.sleep(1000); //don't burn up the cpu when we're testing compilation and debugging with the repl before this is implemented
                //main event loop
                Scanner in = new Scanner(System.in);
                String cardNumber = detectCard();

                System.out.format("Your card has been detected with number %s\n", cardNumber);
                System.out.println("Please select an operation:\n1:\tGet Account Balance\n2:\tWithdraw Cash\n3:\tDeposit Cash");

                int selectedOption = in.nextInt();

                //
                switch (selectedOption) {
                    case 1: //balance query
                        String balancePin = requestPin(in, System.out);
                        queryAccountBalance(cardNumber, balancePin);
                        break;
                    case 2: //withdrawal
                        System.out.format("Please enter amount in %s:", currencyUnit.name());
                        double withdrawalAmount = in.nextDouble();
                        String withdrawalPin = requestPin(in, System.out);
                        double remainingBalance = withdrawCash(withdrawalAmount, cardNumber, withdrawalPin);
                        try {
                            dispenseCashWithdrawal(withdrawalAmount);
                        } catch (HardwareException e) {
                            LOGGER.error("Couldn't dispense cash - reverting withdrawal");
                            revertWithdrawal(withdrawalAmount, cardNumber, withdrawalPin);
                            break;
                        }
                        System.out.format("Your remaining balance is %f.2 %s\n", remainingBalance, currencyUnit.name());
                        break;
                    case 3: //deposit
                        System.out.format("Please place cash in tray (enter amount in %s):", currencyUnit.name());
                        double depositAmount;
                        try {
                            depositAmount = detectCashDeposit(in);
                        } catch (HardwareException e) {
                            LOGGER.error("Couldn't accept cash - reverting deposit");
                            break;
                        }
                        String depositPin = requestPin(in, System.out);
                        double newBalance;
                        try {
                            newBalance = depositCash(depositAmount, cardNumber, depositPin);
                        } catch (Exception e) {
                            LOGGER.error("Couldn't verify deposit - please contact your bank");
                            if (customerFriendly) {
                                LOGGER.info("Provisionally refunding deposit amount of {} onto card {}", String.format("%.2f", depositAmount), cardNumber);
                                dispenseCashWithdrawal(depositAmount);
                            }
                            break;
                        }
                        System.out.format("Your new balance i %f.2 %s", newBalance, currencyUnit.name());
                        break;
                    default:
                        System.out.println("That is not a valid option; returning to selection screen");
                        break;
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private static String requestPin(Scanner in, PrintStream out) {
        out.print("Please enter your pin:");
        return in.nextLine();
    }

    private static double queryAccountBalance(String cardNumber, String pin) throws JOSEException {
        Token token = new Token(
                Token.TransactionType.BALANCE,
                new Token.Pin(pin),
                new Token.Card(cardNumber),
                new Token.TransactionAmount(0d, currencyUnit), //this is required but has no effect; will be fixed in later iteration
                TestClient.getCloudPublicKey(),
                MACHINE_KEY_PAIR.getPrivate());
        return CLIENT.getAccountBalance(token);
    }

    private static double withdrawCash(double amount, String cardNumber, String pin) throws JOSEException {
        Token token = new Token(
                Token.TransactionType.WITHDRAW,
                new Token.Pin(pin),
                new Token.Card(cardNumber),
                new Token.TransactionAmount(amount, currencyUnit),
                TestClient.getCloudPublicKey(),
                MACHINE_KEY_PAIR.getPrivate());
        return CLIENT.withdrawCash(token);
    }

    private static double depositCash(double amount, String cardNumber, String pin) throws JOSEException {
        Token token = new Token(
                Token.TransactionType.DEPOSIT,
                new Token.Pin(pin),
                new Token.Card(cardNumber),
                new Token.TransactionAmount(amount, currencyUnit),
                TestClient.getCloudPublicKey(),
                MACHINE_KEY_PAIR.getPrivate());
        return CLIENT.depositCash(token);
    }

    private static void revertWithdrawal(double amount, String cardNumber, String pin) throws JOSEException {
        Token token = new Token(
                Token.TransactionType.WITHDRAW,
                new Token.Pin(pin),
                new Token.Card(cardNumber),
                new Token.TransactionAmount(amount, currencyUnit),
                TestClient.getCloudPublicKey(),
                MACHINE_KEY_PAIR.getPrivate());
        CLIENT.revertWithdrawal(token);
    }

    private static String detectCard() {
        //TODO: Hardware interface
        return "1234 5678 9012 3456";
    }

    private static double detectCashDeposit(Scanner in) throws HardwareException {
        //TODO: Hardware interface
        try {
            return in.nextDouble();
        } catch (Exception e) {
            throw new HardwareException(e);
        }
    }

    private static void dispenseCashWithdrawal(double amount) throws HardwareException {
        //TODO: Hardware interface
        return;
    }

    public static class HardwareException extends Exception {
        HardwareException(Throwable cause) {
            super(cause);
        }
    }
}
