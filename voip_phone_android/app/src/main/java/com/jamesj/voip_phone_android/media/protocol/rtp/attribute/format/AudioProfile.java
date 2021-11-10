package com.jamesj.voip_phone_android.media.protocol.rtp.attribute.format;

import com.jamesj.voip_phone_android.media.MediaManager;

/**
 * @class public class AudioProfile
 * @brief AudioProfile class
 */
public class AudioProfile {

    private static final String PCMA = MediaManager.ALAW;
    private static final String PCMU = MediaManager.ULAW;

    public static String getCodecNameFromID(int id) {
        switch (id) {
            case 8:
                return PCMA;
            case 0:
                return PCMU;
            case 110:
                return MediaManager.EVS;
            case 111:
                return MediaManager.AMR_NB;
            case 112:
                return MediaManager.AMR_WB;
            default:
                return null;
        }
    }

}
