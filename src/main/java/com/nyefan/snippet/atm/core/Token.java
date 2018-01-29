package com.nyefan.snippet.atm.core;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nyefan.snippet.atm.rest.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;


public final class Token {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
    private static final JWSAlgorithm jwsAlgorithm = JWSAlgorithm.RS256;
    private static final JWEAlgorithm jweAlgorithm = JWEAlgorithm.RSA_OAEP_256;
    private static final EncryptionMethod encryptionMethod = EncryptionMethod.A256GCM;

    //TODO: move all these transaction objects into a core.Transaction class
    public enum TransactionType {
        BALANCE("BALANCE"),
        DEPOSIT("DEPOSIT"),
        WITHDRAW("WITHDRAW");

        private String transactionType;
        private final static String transactionTypeClaim = "transactionType";

        TransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        public static TransactionType fromString(String type) {
            return Arrays.stream(TransactionType.values())
                    .filter(i -> i.transactionType.equalsIgnoreCase(type))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }

    public static final class Pin {
        private String pin;
        private final static String pinClaim = "pin";

        public Pin(String pin) {
            this.pin = pin;
        }

        public boolean verify(String pin) {
            return this.pin.equals(pin);
        }
    }

    public static final class Card {
        private String cardNumber;
        private final static String cardNumberClaim = "cardNumber";

        public Card(String cardNumber) {
            this.cardNumber = cardNumber;
        }

        public String getCardNumber() {
            return cardNumber;
        }
    }

    public enum CurrencyUnit {
        USD("USD"),
        GBP("GBP"),
        EUR("EUR");

        private String currencyUnit;

        CurrencyUnit(String currencyUnit) {
            this.currencyUnit = currencyUnit;
        }

        public static CurrencyUnit fromString(String unit) {
            return Arrays.stream(CurrencyUnit.values())
                    .filter(i -> i.currencyUnit.equalsIgnoreCase(unit))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }

    public static final class TransactionAmount {
        private double transactionAmount;
        private CurrencyUnit currencyUnit;
        private static final String transactionAmountClaim = "transactionAmount";
        private static final String currencyUnitClaim = "currencyUnit";

        public TransactionAmount(String transactionAmount, CurrencyUnit currencyUnit) {
            this.transactionAmount = Double.parseDouble(transactionAmount);
            this.currencyUnit = currencyUnit;
        }

        public TransactionAmount(double transactionAmount, CurrencyUnit currencyUnit) {
            this.transactionAmount = transactionAmount;
            this.currencyUnit = currencyUnit;
        }

        public TransactionAmount inCurrencyUnit(CurrencyUnit currencyUnit) {
            if (this.currencyUnit.equals(currencyUnit)) {
                return this; //operation is idempotent in this case; if anything else ever becomes mutable, this must change
            }

            throw new UnsupportedOperationException("This functionality is not yet implemented");
        }

        public double getTransactionAmount() {
            return transactionAmount;
        }

        public CurrencyUnit getCurrencyUnit() {
            return currencyUnit;
        }
    }

    private final TransactionType transactionType;
    private final TransactionAmount transactionAmount;
    private final Pin pin;
    private final Card card;
    private final JWEObject token;


    //TODO: create a TokenBuilder
    //TODO: don't require transactionAmount for balance queries

    public Token(TransactionType transactionType, Pin pin, Card card, TransactionAmount transactionAmount, PublicKey encryptionKey, PrivateKey signingKey) throws JOSEException {
        this.transactionType = transactionType;
        this.transactionAmount = transactionAmount;
        this.pin = pin;
        this.card = card;

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issueTime(Date.from(Instant.now()))
                .notBeforeTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .issuer("atm-guid")
                .claim(TransactionType.transactionTypeClaim, transactionType.transactionType)
                .claim(TransactionAmount.transactionAmountClaim, transactionAmount.transactionAmount)
                //TODO: this is awkward; revisit
                .claim(TransactionAmount.currencyUnitClaim, transactionAmount.currencyUnit.currencyUnit)
                .claim(Pin.pinClaim, pin.pin)
                .claim(Card.cardNumberClaim, card.cardNumber)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(jwsAlgorithm), claimsSet);
        signedJWT.sign(new RSASSASigner(signingKey));

        JWEObject jweObject = new JWEObject(
                new JWEHeader.Builder(jweAlgorithm, encryptionMethod)
                        .contentType("JWT")
                        .build(),
                new Payload(signedJWT)
        );

        jweObject.encrypt(new RSAEncrypter((RSAPublicKey) encryptionKey));

        token = jweObject;
    }

    public Token(JWEObject jweObject, PublicKey verificationKey, PrivateKey decryptionKey) throws JOSEException, ParseException {
        token = jweObject;

        jweObject.decrypt(new RSADecrypter(decryptionKey));
        SignedJWT signedJWT = jweObject.getPayload().toSignedJWT();

        if (signedJWT == null) {
            throw new JOSEException("Payload is not a signed JWT");
        }

        if (!signedJWT.verify(new RSASSAVerifier((RSAPublicKey) verificationKey))) {
            throw new JOSEException("JWT Payload signature unable to be verified");
        }

        //TODO: make one Date from an instant immediate upon receiving the request
        JWTClaimsSet claimSet = signedJWT.getJWTClaimsSet();
        if (claimSet.getIssueTime().before(Date.from(Instant.now())) ||
                claimSet.getNotBeforeTime().before(Date.from(Instant.now())) ||
                !claimSet.getIssuer().equalsIgnoreCase("atm-guid")) { //use Set.contains(String)
            throw new JOSEException("JWT Claims invalid");
        }

        transactionType = TransactionType.fromString(claimSet.getStringClaim(TransactionType.transactionTypeClaim));
        transactionAmount = new TransactionAmount(claimSet.getDoubleClaim(TransactionAmount.transactionAmountClaim),
                CurrencyUnit.fromString(claimSet.getStringClaim(TransactionAmount.currencyUnitClaim)));
        pin = new Pin(claimSet.getStringClaim(Pin.pinClaim));
        card = new Card(claimSet.getStringClaim(Card.cardNumberClaim));
    }

    //This is to prevent users of the Token from being required to understand or be aware of JWEs
    public static Token parse(String tokenString, PublicKey verificationKey, PrivateKey decryptionKey) throws ParseException, JOSEException {
        return new Token(JWEObject.parse(tokenString), verificationKey, decryptionKey);
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public TransactionAmount getTransactionAmount() {
        return transactionAmount;
    }

    //TODO: consider security implications of returning card/pin directly in the long term
    public Card getCard() {
        return card;
    }

    public Pin getPin() {
        return pin;
    }
}
