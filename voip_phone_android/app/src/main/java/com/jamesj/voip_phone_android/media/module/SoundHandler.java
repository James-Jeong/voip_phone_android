package com.jamesj.voip_phone_android.media.module;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.jamesj.voip_phone_android.media.mix.base.AudioBuffer;
import com.jamesj.voip_phone_android.media.module.base.UdpReceiver;
import com.jamesj.voip_phone_android.media.module.base.UdpSender;
import com.jamesj.voip_phone_android.media.netty.NettyChannelManager;
import com.jamesj.voip_phone_android.media.netty.module.NettyChannel;
import com.jamesj.voip_phone_android.media.protocol.base.ConcurrentCyclicFIFO;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SoundHandler {

    /* Mike Data Buffer */
    private final ConcurrentCyclicFIFO<byte[]> mikeBuffer = new ConcurrentCyclicFIFO<>();
    /* Speaker Data Buffer */
    private final AudioBuffer speakerBuffer = new AudioBuffer("SpeakerBuffer");

    private final ScheduledThreadPoolExecutor mikeExecutor = new ScheduledThreadPoolExecutor(2);
    private final ScheduledThreadPoolExecutor speakerExecutor = new ScheduledThreadPoolExecutor(2);

    private ScheduledFuture<?> mikeScheduledFuture = null;
    private ScheduledFuture<?> speakerScheduledFuture = null;

    private UdpSender udpSender = null;
    private UdpReceiver udpReceiver = null;

    ////////////////////////////////////////////////////////////////////////////////

    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int SAMPLE_RATE = 8000;
    public static final int CHANNEL_COUNT = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_COUNT, AUDIO_FORMAT);

    ////////////////////////////////////////////////////////////////////////////////

    private static SoundHandler soundHandler;

    ////////////////////////////////////////////////////////////////////////////////

    public SoundHandler() {
        // nothing
    }

    public static SoundHandler getInstance() {
        if (soundHandler == null) {
            soundHandler = new SoundHandler();
        }

        return soundHandler;
    }

    public void start() {
        startMike();
        startSpeaker();
    }

    public void stop() {
        stopSpeaker();
        stopMike();
    }

    ////////////////////////////////////////////////////////////////////////////////

    //
    // MIKE
    public void startMike() {
        try {
            NettyChannel nettyChannel = NettyChannelManager.getInstance().getClientChannel();
            if (nettyChannel != null) {
                if (udpSender == null) {
                    udpSender = new UdpSender(
                            mikeBuffer,
                            nettyChannel,
                            1
                    );
                }
                udpSender.start();

                if (mikeScheduledFuture == null) {
                    mikeScheduledFuture = mikeExecutor.scheduleAtFixedRate(
                            udpSender,
                            1000,
                            udpSender.getInterval(),
                            TimeUnit.MILLISECONDS
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMike() {
        if (udpSender != null) {
            udpSender.stop();
            udpSender = null;
        }

        if (mikeScheduledFuture != null) {
            mikeScheduledFuture.cancel(true);
            mikeScheduledFuture = null;
        }

        mikeBuffer.clear();
    }
    //

    //
    // SPEAKER
    public void startSpeaker() {
        try {
            if (speakerScheduledFuture == null) {
                udpReceiver = new UdpReceiver(
                        speakerBuffer,
                        1
                );

                speakerScheduledFuture = speakerExecutor.scheduleAtFixedRate(
                        udpReceiver,
                        1000,
                        udpReceiver.getInterval(),
                        TimeUnit.MILLISECONDS
                );
                udpReceiver.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopSpeaker() {
        if (udpReceiver != null) {
            udpReceiver.stop();
            udpReceiver = null;
        }

        if (speakerScheduledFuture != null) {
            speakerScheduledFuture.cancel(true);
            speakerScheduledFuture = null;
        }

        speakerBuffer.resetBuffer();
    }
    //

    ////////////////////////////////////////////////////////////////////////////////

    public void muteMikeOn() {
        if (udpSender != null) {
            udpSender.setMute(true);
        }
    }

    public void muteMikeOff() {
        if (udpSender != null) {
            udpSender.setMute(false);
        }
    }

    public void muteSpeakerOn() {
        if (udpReceiver != null) {
            udpReceiver.setMute(true);
        }
    }

    public void muteSpeakerOff() {
        if (udpReceiver != null) {
            udpReceiver.setMute(false);
        }
    }

}
