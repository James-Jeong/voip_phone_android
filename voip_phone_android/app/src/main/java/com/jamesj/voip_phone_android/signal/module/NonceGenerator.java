package com.jamesj.voip_phone_android.signal.module;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class NonceGenerator {

    //private static final SecureRandom secureRandom;

    ////////////////////////////////////////////////////////////////////////////////

    private NonceGenerator() {
        // Nothing
    }

    ////////////////////////////////////////////////////////////////////////////////

    /*static {
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");

            final byte[] ar = new byte[64];
            secureRandom.nextBytes(ar);

            Arrays.fill(ar, (byte) 0);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }*/

    ////////////////////////////////////////////////////////////////////////////////

    public static String createRandomNonce() {
        /*final byte[] ar = new byte[48];
        secureRandom.nextBytes(ar);*/

        byte[] array = new byte[7]; // length is bounded by 7
        new Random().nextBytes(array);
        String nonce = new String(array, StandardCharsets.UTF_8);

        //Arrays.fill(ar, (byte) 0);
        return nonce;
    }

}
