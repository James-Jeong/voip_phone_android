package com.jamesj.voip_phone_android.media.module;

import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.media.dtmf.base.DtmfUnit;
import com.jamesj.voip_phone_android.media.mix.AudioMixManager;
import com.jamesj.voip_phone_android.media.module.base.TaskUnit;
import com.jamesj.voip_phone_android.media.netty.NettyChannelManager;
import com.jamesj.voip_phone_android.media.netty.module.NettyChannel;
import com.jamesj.voip_phone_android.media.protocol.jrtp.JRtp;
import com.jamesj.voip_phone_android.media.protocol.rtp.RtpPacket;
import com.jamesj.voip_phone_android.media.protocol.rtp.attribute.format.base.RtpAudioFormat;
import com.jamesj.voip_phone_android.media.protocol.rtp.base.RtpFormat;
import com.jamesj.voip_phone_android.media.protocol.rtp.base.RtpFrame;
import com.jamesj.voip_phone_android.media.protocol.rtp.jitter.JitterBuffer;
import com.jamesj.voip_phone_android.media.sdp.base.Sdp;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.jamesj.voip_phone_android.signal.base.CallInfo;
import com.jamesj.voip_phone_android.signal.base.RoomInfo;
import com.jamesj.voip_phone_android.signal.module.CallManager;
import com.jamesj.voip_phone_android.signal.module.GroupCallManager;
import com.orhanobut.logger.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;


/**
 * @class public class ServerRtpHandler extends TaskUnit
 * @brief ServerRtpHandler class
 */
public class ServerRtpHandler extends TaskUnit {

    private final String key;
    private final JitterBuffer jitterBuffer;

    ////////////////////////////////////////////////////////////////////////////////

    public ServerRtpHandler(String key, JitterBuffer jitterBuffer, int interval) {
        super(interval);

        this.key = key;
        this.jitterBuffer = jitterBuffer;

        Logger.d("(%s) ServerRtpHandler is created.", key);
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void run() {
        try {
            // 1) Read data
            RtpFrame rtpFrame = jitterBuffer.read();
            if (rtpFrame == null) {
                return;
            }

            byte[] rtpData = rtpFrame.getData();
            if (rtpData == null) {
                return;
            }

            RtpFormat rtpFormat = rtpFrame.getRtpFormat();
            if (rtpFormat == null) {
                return;
            }

            String ip = rtpFormat.getIp();
            int port = rtpFormat.getPort();
            short gain = (short) rtpFormat.getGain();
            RtpAudioFormat audioFormat = (RtpAudioFormat) rtpFormat.getFormat();
            int samplingRate = audioFormat.getSampleRate();
            int sampleSize = audioFormat.getSampleSize();
            int channelSize = audioFormat.getChannels();
            //

            // 2) Find CallInfo
            CallInfo callInfo = CallManager.getInstance().findCallInfoByMediaAddress(ip, port);
            if (callInfo == null) {
                //Logger.w("(%s) Fail to find the call info. (ip=%s, port=%s)", key, ip, port);
                return;
            }
            String callId = callInfo.getCallId();
            //

            // 3) Check Call Type & Send Rtp data
            // 3-1) Group call
            if (callInfo.getIsRoomEntered()) {
                RoomInfo roomInfo = GroupCallManager.getInstance().getRoomInfo(callInfo.getSessionId());
                if (roomInfo == null) {
                    //Logger.w("(%s) Fail to find the room info. (roomId=%s)", key, callInfo.getSessionId());
                    return;
                }

                for (String curCallId : roomInfo.cloneRemoteCallList(callId)) {
                    CallInfo remoteCallInfo = CallManager.getInstance().getCallInfo(curCallId);
                    if (remoteCallInfo == null) {
                        continue;
                    }

                    perform(
                            callId,
                            ip, port,
                            remoteCallInfo,
                            samplingRate,
                            sampleSize,
                            channelSize,
                            gain,
                            rtpData
                    );
                }
            }
            // 3-2) Relay call
            else {
                CallInfo remoteCallInfo = callInfo.getRemoteCallInfo();
                if (remoteCallInfo == null) {
                    //Logger.w("(%s) Fail to find the remote call info. (ip=%s, port=%s)", key, ip, port);
                    return;
                }

                perform(
                        callId,
                        ip, port,
                        remoteCallInfo,
                        samplingRate,
                        sampleSize,
                        channelSize,
                        gain,
                        rtpData
                );
            }
            //

            // 4) Mix Audio
            RtpPacket rtpPacket = new RtpPacket(rtpData, rtpData.length);
            if (rtpPacket.getPayloadType() != DtmfUnit.DTMF_TYPE) {
                AudioMixManager.getInstance().perform(
                        callInfo.getSessionId(),
                        callInfo.getCallId(),
                        samplingRate,
                        sampleSize,
                        channelSize,
                        gain,
                        rtpPacket.getPayload()
                );
            }
            //
        } catch (Exception e) {
            Logger.w("(%s) Fail to process the rtp data.", key, e);
        }
    }

    private void perform (
            String callId,
            String ip, int port,
            CallInfo remoteCallInfo,
            int samplingRate, int sampleSize, int channelSize, short gain, byte[] rtpData) {
        // 1) Get Remote sdp unit
        Sdp remoteSdp = remoteCallInfo.getSdp();
        if (remoteSdp == null) {
            Logger.w("(%s) Fail to find the remote sdp unit. (ip=%s, port=%s)", key, ip, port);
            return;
        }
        //

        // 2) Get Proxy channel
        NettyChannel nettyChannel = NettyChannelManager.getInstance().getProxyChannel(callId);
        if (nettyChannel == null) {
            Logger.d("(%s) Fail to find the netty channel. (ip=%s, port=%s)", key, ip, port);
            return;
        }
        //

        // 3) Make Rtp data
        ByteBuf byteBuf;
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        if (configManager.isRelay()) {
            byteBuf = Unpooled.copiedBuffer(rtpData);
        } else {
            JRtp jRtp = new JRtp();
            jRtp.setData(
                    configManager.getNettyServerIp(),
                    nettyChannel.getListenPort(),
                    samplingRate,
                    sampleSize,
                    channelSize,
                    gain,
                    rtpData
            );

            byteBuf = Unpooled.copiedBuffer(jRtp.getData());
        }
        //

        // 4) Send JRtp data
        if (nettyChannel.sendMessage(remoteSdp.getId(), byteBuf, remoteSdp.getSessionOriginAddress(), remoteSdp.getMediaPort(Sdp.AUDIO))) {
            Logger.d("(%s) Relay RTP. (curCallId=%s, remoteCallId=%s, src=%s:%s, dst=%s:%s)", key, callId, remoteSdp.getId(), ip, port, remoteSdp.getSessionOriginAddress(), remoteSdp.getMediaPort(Sdp.AUDIO));
        }
        //
    }

}
