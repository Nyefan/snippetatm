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
    private static final KeyPair MACHINE_KEY_PAIR;
    private static final JWSAlgorithm jwsAlgorithm = JWSAlgorithm.RS256;

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
        MACHINE_KEY_PAIR = generator.generateKeyPair();
    }

    public static PublicKey getCloudPublicKey() {
        return MACHINE_KEY_PAIR.getPublic();
    }

    public enum TransactionType {
        BALANCE("BALANCE"),
        DEPOSIT("DEPOSIT"),
        WITHDRAW("WITHDRAW");

        private String type;
        private final static String claim = "transactionType";

        TransactionType(String type) {
            this.type = type;
        }

        public TransactionType parse(String type) {
            return Arrays.stream(TransactionType.values())
                    .filter(i -> i.type.equalsIgnoreCase(type))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }

    public final class Pin {

        private String pin;
        private final static String pinClaim = "pin";

        public Pin(String pin) {
            this.pin = pin;
        }
    }

    public final class Card {
        private String cardNumber;
        private final static String cardNumberClaim = "cardNumber";

        public Card(String cardNumber) {
            this.cardNumber = cardNumber;
        }
    }

    private final TransactionType transactionType;
    private final Pin pin;
    private final Card card;
    private final JWEObject token;

    public Token(TransactionType transactionType, Pin pin, Card card, PublicKey encryptionKey, PrivateKey signingKey) throws JOSEException {
        this.transactionType = transactionType;
        this.pin = pin;
        this.card = card;

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issueTime(Date.from(Instant.now()))
                .notBeforeTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .issuer("atm-guid")
                //TODO: this is awkward; revisit
                .claim(TransactionType.claim, transactionType.type)
                .claim(Pin.pinClaim, pin.pin)
                .claim(Card.cardNumberClaim, card.cardNumber)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(jwsAlgorithm), claimsSet);
        signedJWT.sign(new RSASSASigner(signingKey));

        JWEObject jweObject = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
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

        transactionType = TransactionType.valueOf(claimSet.getStringClaim(TransactionType.claim));
        pin = new Pin(claimSet.getStringClaim(Pin.pinClaim));
        card = new Card(claimSet.getStringClaim(Card.cardNumberClaim));
    }

    public static Token parse(String tokenString, PublicKey verificationKey, PrivateKey decryptionKey) throws ParseException, JOSEException {
        return new Token(JWEObject.parse(tokenString), verificationKey, decryptionKey);
    }
}
