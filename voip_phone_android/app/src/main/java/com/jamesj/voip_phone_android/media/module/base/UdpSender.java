package com.jamesj.voip_phone_android.media.module.base;

import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.media.MediaManager;
import com.jamesj.voip_phone_android.media.codec.amr.AmrManager;
import com.jamesj.voip_phone_android.media.codec.evs.EvsManager;
import com.jamesj.voip_phone_android.media.codec.pcm.ALawTranscoder;
import com.jamesj.voip_phone_android.media.codec.pcm.ULawTranscoder;
import com.jamesj.voip_phone_android.media.module.AudioRecorder;
import com.jamesj.voip_phone_android.media.netty.module.NettyChannel;
import com.jamesj.voip_phone_android.media.protocol.base.ConcurrentCyclicFIFO;
import com.jamesj.voip_phone_android.media.protocol.jrtp.JRtp;
import com.jamesj.voip_phone_android.media.protocol.rtp.RtpPacket;
import com.jamesj.voip_phone_android.media.sdp.base.Sdp;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.jamesj.voip_phone_android.signal.base.CallInfo;
import com.jamesj.voip_phone_android.signal.module.CallManager;
import com.orhanobut.logger.Logger;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @class public class UdpSender extends TaskUnit
 * @brief UdpSender class
 */
public class UdpSender extends TaskUnit {

    private final int TIME_DELAY;

    /* Netty Channel to send the message */
    private final NettyChannel nettyChannel;
    /* Mike Data Buffer */
    private final ConcurrentCyclicFIFO<byte[]> mikeBuffer;

    private final ConcurrentCyclicFIFO<MediaFrame> sendBuffer = new ConcurrentCyclicFIFO<>();
    private ScheduledThreadPoolExecutor executor;

    /* JRtp Message object */
    private final JRtp jRtp = new JRtp();
    /* RTP Message object */
    private final RtpPacket rtpPacket = new RtpPacket();

    private final AtomicBoolean mute = new AtomicBoolean(false);

    ////////////////////////////////////////////////////////////////////////////////

    private AudioRecorder audioRecorder = null;

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn protected UdpSender(ConcurrentCyclicFIFO<byte[]> mikeBuffer, NettyChannel nettyChannel, int interval)
     * @brief UdpSender 생성자 함수
     * @param nettyChannel Netty Channel
     * @param interval Task interval
     */
    public UdpSender(ConcurrentCyclicFIFO<byte[]> mikeBuffer, NettyChannel nettyChannel, int interval) {
        super(interval);

        this.TIME_DELAY = interval * 8;
        this.nettyChannel = nettyChannel;
        this.mikeBuffer = mikeBuffer;
    }

    public void start() {
        try {
            if (executor == null) {
                executor = new ScheduledThreadPoolExecutor(5);

                SendTask sendTask = new SendTask(
                        20
                        //MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_WB)? 40 : 20
                );
                executor.scheduleAtFixedRate(
                        sendTask,
                        sendTask.getInterval(),
                        sendTask.getInterval(),
                        TimeUnit.MILLISECONDS
                );
                Logger.d("UdpSender SendTask is added.");
            }

            if (audioRecorder == null) {
                audioRecorder = new AudioRecorder(AppInstance.getInstance().getMasterFragmentActivity());
                audioRecorder.startRecording();
            }

            switch (MediaManager.getInstance().getPriorityCodec()) {
                case MediaManager.EVS:
                    EvsManager.getInstance().startUdpSenderTask(sendBuffer);
                    break;
                case (MediaManager.AMR_NB):
                    AmrManager.getInstance().startEncAmrNb();
                    break;
                case (MediaManager.AMR_WB):
                    AmrManager.getInstance().startEncAmrWb();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Logger.w("UdpSender.start.Exception", e);
            e.printStackTrace();
        }
    }

    public void stop() {
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
            audioRecorder = null;
        }

        sendBuffer.clear();
        switch (MediaManager.getInstance().getPriorityCodec()) {
            case MediaManager.EVS:
                EvsManager.getInstance().stopUdpSenderTask();
                break;
            case (MediaManager.AMR_NB):
                AmrManager.getInstance().stopEncAmrNb();
                break;
            case (MediaManager.AMR_WB):
                AmrManager.getInstance().stopEncAmrWb();
                break;
            default:
                break;
        }

        if (executor != null) {
            executor.shutdown();
            executor = null;
            Logger.d("UdpSender SendTask is removed.");
        }
    }

    public boolean isMute() {
        return mute.get();
    }

    public void setMute(boolean mute) {
        this.mute.set(mute);
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public void run()
     * @brief UdpSender 비즈니스 로직을 실행하는 함수
     */
    @Override
    public void run() {
        if (audioRecorder == null) {
            return;
        }

        try {
            byte[] data = audioRecorder.read();
            //byte[] data = mikeBuffer.poll();
            if (data == null || data.length == 0) {
                return;
            }

            if (mute.get()) { return; }

            // 1) Pre-process the audio data.
            // PCM
            /*if (voipClient.getTargetAudioFormat().getEncoding().toString().equals(
                    AudioFormat.Encoding.PCM_SIGNED.toString())) {*/
                // Convert to little endian.
                /*if (voipClient.isTargetBigEndian()) {
                    data = RtpUtil.changeByteOrder(data);
                }*/

                /*RecordManager pcmRecordManager = voipClient.getTargetPcmRecordManager();
                if (pcmRecordManager != null) {
                    pcmRecordManager.addData(data);
                }*/

                // ALAW
                if (MediaManager.getInstance().getPriorityCodec().equals(MediaManager.ALAW)) {
                    data = ALawTranscoder.encode(
                            data
                    );

                    /*RecordManager aLawRecordManager = voipClient.getTargetALawRecordManager();
                    if (aLawRecordManager != null) {
                        aLawRecordManager.addData(data);
                    }*/
                }
                // ULAW
                else if (MediaManager.getInstance().getPriorityCodec().equals(MediaManager.ULAW)) {
                    data = ULawTranscoder.encode(
                            data
                    );

                    /*RecordManager uLawRecordManager = voipClient.getTargetULawRecordManager();
                    if (uLawRecordManager != null) {
                        uLawRecordManager.addData(data);
                    }*/
                }
                // EVS
                else if (MediaManager.getInstance().getPriorityCodec().equals(MediaManager.EVS)) {
                    EvsManager.getInstance().addUdpSenderInputData(data);
                    return;
                }
                // AMR-NB
                else if (MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_NB)) {
                    data = AmrManager.getInstance().encAmrNb(
                            MediaManager.AMR_NB_MAX_MODE_SET,
                            data
                    );

                    /*if (data != null) {
                        RecordManager amrNbRecordManager = voipClient.getTargetAmrNbRecordManager();
                        if (amrNbRecordManager != null) {
                            amrNbRecordManager.addData(data);
                        }
                    }*/
                }
                // AMR-WB
                else if (MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_WB)) {
                    data = AmrManager.getInstance().encAmrWb(
                            MediaManager.AMR_WB_MAX_MODE_SET,
                            data
                    );

                    /*if (data != null) {
                        RecordManager amrWbRecordManager = voipClient.getTargetAmrWbRecordManager();
                        if (amrWbRecordManager != null) {
                            amrWbRecordManager.addData(data);
                        }
                    }*/
                }
            //}

            sendBuffer.offer(
                    new MediaFrame(
                            false,
                            data
                    )
            );
        } catch (Exception e){
            Logger.w("Fail to read the data.", e);
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    private class SendTask extends TaskUnit {

        protected SendTask(int interval) {
            super(interval);
        }

        @Override
        public void run() {
            MediaFrame mediaFrame = sendBuffer.poll();
            if (mediaFrame == null) {
                return;
            }

            boolean isDtmf = mediaFrame.isDtmf();
            byte[] data = mediaFrame.getData();

            // 2) Broadcast the rtp packet.
            Map<String, CallInfo> callInfoMap = CallManager.getInstance().getCloneCallMap();
            if (callInfoMap.isEmpty()) { return; }

            for (Map.Entry<String, CallInfo> entry : callInfoMap.entrySet()) {
                CallInfo callInfo = entry.getValue();
                if (callInfo == null) {
                    continue;
                }

                Sdp remoteSdp = callInfo.getSdp();
                if (remoteSdp == null) {
                    return;
                }

                // 3) Insert the encoded data into a rtp packet.
                int seqNum;
                if (isDtmf) {
                    seqNum = callInfo.getDtmfSeqNum();
                    rtpPacket.setValue(
                            2, 0, 0, 0, 0, 101,
                            seqNum,
                            callInfo.getDtmfTimestamp(),
                            callInfo.getDtmfSsrc(),
                            data,
                            data.length
                    );
                    callInfo.setDtmfSeqNum(seqNum + 1);
                } else {
                    seqNum = callInfo.getAudioSeqNum();
                    rtpPacket.setValue(
                            2, 0, 0, 0, 0, MediaManager.getInstance().getPriorityCodecId(),
                            seqNum,
                            callInfo.getAudioTimestamp(),
                            callInfo.getAudioSsrc(),
                            data,
                            data.length
                    );
                    callInfo.setAudioSeqNum(seqNum + 1);
                }

                // Set final data
                ConfigManager configManager = AppInstance.getInstance().getConfigManager();

                byte[] bufData;
                if (configManager.isUseProxy()) {
                    jRtp.setData(
                            configManager.getNettyServerIp(),
                            nettyChannel.getListenPort(),
                            8000,
                            16,
                            1,
                            (short) 100,
                            rtpPacket.getData()
                    );
                    bufData = jRtp.getData();
                } else {
                    bufData = rtpPacket.getData();
                }

                // 3) Send the rtp packet.
                ByteBuf buf = Unpooled.copiedBuffer(
                        bufData
                );

                if (nettyChannel.sendMessage(
                        remoteSdp.getId(),
                        buf,
                        remoteSdp.getSessionOriginAddress(),
                        remoteSdp.getMediaPort(Sdp.AUDIO))) {
                    /*Logger.d("Send RTP. (callId=%s, src=%s, dst=%s:%s)",
                            remoteSdp.getId(), nettyChannel.getListenPort(),
                            remoteSdp.getSessionOriginAddress(), remoteSdp.getMediaPort(Sdp.AUDIO)
                    );*/
                } else {
                    Logger.w("Fail to send RTP. (callId=%s, src=%s, dst=%s:%s)",
                            remoteSdp.getId(), nettyChannel.getListenPort(),
                            remoteSdp.getSessionOriginAddress(), remoteSdp.getMediaPort(Sdp.AUDIO)
                    );
                }

                if (isDtmf) {
                    callInfo.setDtmfTimestamp(callInfo.getDtmfTimestamp() + TIME_DELAY); // 20ms per 1 rtp packet (sampling-rate: 8000)
                    if (callInfo.getDtmfSeqNum() >= CallInfo.MAX_SEQ_NUM) {
                        callInfo.initDtmfSeqNum();
                    }
                    if (callInfo.getDtmfTimestamp() >= Long.MAX_VALUE - 1) {
                        callInfo.initDtmfTimestamp();
                    }
                } else {
                    callInfo.setAudioTimestamp(callInfo.getAudioTimestamp() + TIME_DELAY); // 20ms per 1 rtp packet (sampling-rate: 8000)
                    if (callInfo.getAudioSeqNum() >= CallInfo.MAX_SEQ_NUM) {
                        callInfo.initAudioSeqNum();
                    }
                    if (callInfo.getAudioTimestamp() >= Long.MAX_VALUE - 1) {
                        callInfo.initAudioTimestamp();
                    }
                }
            }
        }
    }

    public ConcurrentCyclicFIFO<MediaFrame> getSendBuffer() {
        return sendBuffer;
    }
}
