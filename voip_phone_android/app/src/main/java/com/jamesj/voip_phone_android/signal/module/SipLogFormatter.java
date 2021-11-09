package com.jamesj.voip_phone_android.signal.module;

public class SipLogFormatter {

    /* (Call-ID) (From mdn) (To mdn) */
    private static final String CALL_LOG_HEADER = "[CALL] (%s) (%s) (%s) (%s)";

    public static String getCallLogHeader(String sessionId, String callId, String fromNo, String toNo) {
        return String.format(CALL_LOG_HEADER, sessionId, callId, fromNo, toNo);
    }

}
