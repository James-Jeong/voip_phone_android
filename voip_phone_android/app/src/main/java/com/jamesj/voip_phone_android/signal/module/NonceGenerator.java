package com.jamesj.voip_phone_android.signal.module;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

public class NonceGenerator {

    private static final SecureRandom secureRandom;
    private static final Random random = new Random();

    ////////////////////////////////////////////////////////////////////////////////

    private NonceGenerator() {
        // Nothing
    }

    ////////////////////////////////////////////////////////////////////////////////

    static {
        //try {
            //secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
            secureRandom = new SecureRandom();

            final byte[] ar = new byte[64];
            secureRandom.nextBytes(ar);

            Arrays.fill(ar, (byte) 0);
        //} catch (GeneralSecurityException e) {
            //throw new IllegalStateException(e);
        //}
    }

    ////////////////////////////////////////////////////////////////////////////////

    public static String createRandomNonce() {
        final byte[] ar = new byte[48];

        secureRandom.nextBytes(ar);

        final String nonce = new String(
                java.util.Base64.getUrlEncoder().withoutPadding().encode(ar),
                StandardCharsets.UTF_8
        );

        Arrays.fill(ar, (byte) 0);
        return nonce;
        /*byte[] array = new byte[7];
        random.nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);*/
    }

}
