package com.jamesj.voip_phone_android.signal.module;

import com.jamesj.voip_phone_android.media.module.TaskManager;
import com.jamesj.voip_phone_android.media.module.base.TaskUnit;
import com.jamesj.voip_phone_android.signal.base.CallInfo;


/**
 * @author jamesj
 * @class public class CallCancelHandler extends TaskUnit
 * @brief CallCancelHandler
 */
public class CallCancelHandler extends TaskUnit {

    private final String callId;
    private final String toHostName;
    private final String toIp;
    private final int toPort;

    private final SipManager sipManager;

    ////////////////////////////////////////////////////////////////////////////////

    public CallCancelHandler(SipManager sipManager, String callId, String toHostName, String toIp, int toPort, int interval) {
        super(interval);

        this.sipManager = sipManager;
        this.callId = callId;
        this.toHostName = toHostName;
        this.toIp = toIp;
        this.toPort = toPort;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void run () {
        CallInfo callInfo = CallManager.getInstance().getCallInfo(callId);
        if (callInfo == null) {
            return;
        }

        if (callInfo.getIsCallStarted() && !callInfo.getIsInviteAccepted()) {
            //FrameManager.getInstance().processByeToFrame(ServiceManager.CLIENT_FRAME_NAME);

            sipManager.sendCancel(callId, toHostName, toIp, toPort);

            //VoipClient voipClient = VoipClient.getInstance();
            //logger.debug("Cancel to [{}]", voipClient.getRemoteHostName());
        }

        TaskManager.getInstance().removeTask(CallCancelHandler.class.getSimpleName() + callId);
    }

}
