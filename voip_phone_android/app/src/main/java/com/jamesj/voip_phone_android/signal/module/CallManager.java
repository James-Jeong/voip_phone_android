package com.jamesj.voip_phone_android.signal.module;

import com.jamesj.voip_phone_android.media.sdp.base.Sdp;
import com.jamesj.voip_phone_android.signal.base.CallInfo;
import com.orhanobut.logger.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class CallManager {

    private static CallManager callManager = null;

    private final HashMap<String, CallInfo> callMap = new HashMap<>();
    private final ReentrantLock callMapLock = new ReentrantLock();

    ////////////////////////////////////////////////////////////////////////////////

    public CallManager() {
        // Nothing
    }

    public static CallManager getInstance () {
        if (callManager == null) {
            callManager = new CallManager();
        }
        return callManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public int getCallMapSize() {
        try {
            callMapLock.lock();

            return callMap.size();
        } catch (Exception e) {
            Logger.w("%s Fail to get the call map size.", SipLogFormatter.getCallLogHeader(null,null, null, null), e);
            return 0;
        } finally {
            callMapLock.unlock();
        }
    }

    public CallInfo addCallInfo(String sessionId, String callId, String fromNo, String fromSipIp, int fromSipPort, String toNo, String toSipIp, int toSipPort) {
        if (sessionId == null || callId == null || fromNo == null || fromSipIp == null || fromSipPort <= 0 || toNo == null || toSipIp == null || toSipPort <= 0) {
            Logger.w("%s Fail to add the call info. (sessionId=%s, callId=%s, fromNo=%s, toNo=%s, fromSipIp=%s, fromSipPort=%s, toSipIp=%s, toSipPort=%s)",
                    SipLogFormatter.getCallLogHeader(sessionId, callId, fromNo, toNo), sessionId, callId, fromNo, fromSipIp, fromSipPort, toNo, toSipIp, toSipPort
            );
            return null;
        }

        try {
            callMapLock.lock();

            callMap.putIfAbsent(callId, new CallInfo(sessionId, callId, fromNo, fromSipIp, fromSipPort, toNo, toSipIp, toSipPort));
            return callMap.get(callId);
        } catch (Exception e) {
            Logger.w("%s Fail to add the call info.", SipLogFormatter.getCallLogHeader(sessionId, callId, fromNo, toNo), e);
            return null;
        } finally {
            callMapLock.unlock();
        }
    }

    public CallInfo deleteCallInfo(String callId) {
        if (callId == null) { return null; }

        try {
            callMapLock.lock();

            return callMap.remove(callId);
        } catch (Exception e) {
            Logger.w("%s Fail to delete the call map.", SipLogFormatter.getCallLogHeader(null, callId, null, null), e);
            return null;
        } finally {
            callMapLock.unlock();
        }
    }

    public Map<String, CallInfo> getCloneCallMap( ) {
        HashMap<String, CallInfo> cloneMap;

        try {
            callMapLock.lock();

            cloneMap = (HashMap<String, CallInfo>) callMap.clone();
        } catch (Exception e) {
            Logger.w("%s Fail to clone the call map.", SipLogFormatter.getCallLogHeader(null,null, null, null), e);
            cloneMap = callMap;
        } finally {
            callMapLock.unlock();
        }

        return cloneMap;
    }

    public CallInfo getCallInfo(String callId) {
        if (callId == null) { return null; }

        try {
            callMapLock.lock();

            return callMap.get(callId);
        } catch (Exception e) {
            Logger.w("%s Fail to get the call info.", SipLogFormatter.getCallLogHeader(null, callId, null, null), e);
            return null;
        } finally {
            callMapLock.unlock();
        }
    }

    public void clearCallInfoMap() {
        try {
            callMapLock.lock();

            callMap.clear();
            Logger.d("Success to clear the call map.");
        } catch (Exception e) {
            Logger.w("%s Fail to clear the call map.", SipLogFormatter.getCallLogHeader(null, null, null, null), e);
        } finally {
            callMapLock.unlock();
        }
    }

    public CallInfo findOtherCallInfo(String sessionId, String curCallId) {
        try {
            callMapLock.lock();

            for (Map.Entry<String, CallInfo> entry : callMap.entrySet()) {
                CallInfo callInfo = entry.getValue();
                if (callInfo != null) {
                    if (callInfo.getSessionId().equals(sessionId) && !callInfo.getCallId().equals(curCallId)) {
                        return callInfo;
                    }
                }
            }
        } catch (Exception e) {
            Logger.w("%s Fail to get the other call info.", SipLogFormatter.getCallLogHeader(sessionId, curCallId, null, null), e);
            return null;
        } finally {
            callMapLock.unlock();
        }

        return null;
    }

    public CallInfo findCallInfoByMediaAddress(String ip, int port) {
        try {
            callMapLock.lock();

            for (Map.Entry<String, CallInfo> entry : callMap.entrySet()) {
                CallInfo callInfo = entry.getValue();
                if (callInfo != null) {
                    Sdp sdp = callInfo.getSdp();
                    if (sdp == null) { continue; }
                    if (sdp.getSessionOriginAddress().equals(ip) && sdp.getMediaPort(Sdp.AUDIO) == port) {
                        return callInfo;
                    }
                }
            }
        } catch (Exception e) {
            Logger.w("%s Fail to get the call info by ip and port. (ip=%s, port=%s)", SipLogFormatter.getCallLogHeader(null, null, null, null), ip, port, e);
            return null;
        } finally {
            callMapLock.unlock();
        }

        return null;
    }

    public CallInfo findCallInfoByFromMdn(String mdn) {
        try {
            callMapLock.lock();

            for (Map.Entry<String, CallInfo> entry : callMap.entrySet()) {
                CallInfo callInfo = entry.getValue();
                if (callInfo != null) {
                    if (callInfo.getFromNo().equals(mdn)) {
                        return callInfo;
                    }
                }
            }
        } catch (Exception e) {
            Logger.w("%s Fail to get the call info by from mdn.", SipLogFormatter.getCallLogHeader(null, null, mdn, null), e);
            return null;
        } finally {
            callMapLock.unlock();
        }

        return null;
    }

    public CallInfo findCallInfoByToMdn(String mdn) {
        try {
            callMapLock.lock();

            for (Map.Entry<String, CallInfo> entry : callMap.entrySet()) {
                CallInfo callInfo = entry.getValue();
                if (callInfo != null) {
                    if (callInfo.getToNo().equals(mdn)) {
                        return callInfo;
                    }
                }
            }
        } catch (Exception e) {
            Logger.w("%s Fail to get the call info by to mdn.", SipLogFormatter.getCallLogHeader(null, null, null, mdn), e);
            return null;
        } finally {
            callMapLock.unlock();
        }

        return null;
    }

    public void addSdpIntoCallInfo(String callId, Sdp sdp) {
        if (callId == null || sdp == null) { return; }

        try {
            callMapLock.lock();

            CallInfo callInfo = getCallInfo(callId);
            if (callInfo != null) {
                callInfo.setSdp(sdp);
            }
        } catch (Exception e) {
            Logger.w("%s Fail to set the sdp into the call info. (sdpUnit=%s)", SipLogFormatter.getCallLogHeader(null, callId, null, null), sdp, e);
        } finally {
            callMapLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

}
