package com.jamesj.voip_phone_android.media.netty.handler;

import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.media.dtmf.base.DtmfUnit;
import com.jamesj.voip_phone_android.media.mix.base.AudioFrame;
import com.jamesj.voip_phone_android.media.module.SoundHandler;
import com.jamesj.voip_phone_android.media.protocol.rtp.RtpPacket;
import com.jamesj.voip_phone_android.media.protocol.rtp.module.RtpQosHandler;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.orhanobut.logger.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

/**
 * @class public class ClientChannelHandler extends SimpleChannelInboundHandler<DatagramPacket>
 */
public class ClientChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final ConfigManager configManager = AppInstance.getInstance().getConfigManager();

    private final RtpQosHandler rtpQosHandler = new RtpQosHandler();

    ////////////////////////////////////////////////////////////////////////////////

    public ClientChannelHandler() {
        Logger.d("ClientChannelHandler is created.");
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @param ctx ChannelHandlerContext {@link ChannelHandlerContext}
     * @param msg UDP 패킷 데이터
     * @fn protected void channelRead0 (ChannelHandlerContext ctx, DatagramPacket msg)
     * @brief UDP 패킷을 Media Server 으로부터 수신하는 함수
     */
    @Override
    protected void channelRead0 (ChannelHandlerContext ctx, DatagramPacket msg) {
        ByteBuf buf = msg.content();
        if (buf == null) {
            return;
        }

        try {
            if (buf.readableBytes() > 0) {
                handleRtpPacket(buf);
            }
        } catch (Exception e) {
            Logger.w("Fail to handle UDP Packet.", e);
        }
    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        //Logger.w("ServerHandler.exceptionCaught", cause);
        //ctx.close();
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public void handleRtpPacket(ByteBuf buf)
     * @brief RTP Packet 을 처리하는 함수
     * @param buf ByteBuf 객체
     */
    private void handleRtpPacket(ByteBuf buf) {
        try {
            if (configManager.isUseClient()) {
                // 1) Read a packet data.
                int readBytes = buf.readableBytes();
                byte[] data = new byte[readBytes];
                buf.getBytes(0, data);
                if (buf.readableBytes() <= 0) {
                    return;
                }
                //

                // 2) Check QoS.
                RtpPacket rtpPacket = new RtpPacket(data, data.length);
                /*if (!rtpQosHandler.checkSeqNum(rtpPacket.getSeqNum())) {
                    //Logger.d("Wrong RTP Packet is detected. Discarded. (jRtp=%s, rtpPacket=%s)", jRtp, rtpPacket);
                    Logger.w("Wrong RTP Packet is detected. Discarded. (rtpPacket=%s)", rtpPacket);
                    return;
                }*/
                byte[] payload = rtpPacket.getPayload();
                //

                // 3) Write the audio data to the speaker
                if (payload.length > 0) {
                    // TODO
                    AudioFrame audioFrame = new AudioFrame(rtpPacket.getPayloadType() == DtmfUnit.DTMF_TYPE);
                    audioFrame.setData(payload, true);
                    SoundHandler.getInstance().getSpeakerBuffer().offer(audioFrame);
                }
                //
            }
        } catch (Exception e) {
            Logger.w("ClientChannelHandler.handleRtpPacket.Exception", e);
        }
    }

}
