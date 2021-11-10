package com.jamesj.voip_phone_android.media.module.base;


import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.media.MediaManager;
import com.jamesj.voip_phone_android.media.protocol.base.ConcurrentCyclicFIFO;
import com.jamesj.voip_phone_android.service.AppInstance;

/**
 * @class public class PcmGenerator extends TaskUnit
 * @brief PcmGenerator class
 */
public class PcmGenerator extends TaskUnit {

    private final int BUFFER_LENGTH; // 8k bitstream > 160, 48k (8k * 6 = 48k) bitstream > 960 (160 * 6)

    /* Audio Data input stream (using fd) */
//    private final AudioInputStream stream;
//    private AudioInputStream wavStream = null;

    /* Mike Data Buffer */
    private final ConcurrentCyclicFIFO<byte[]> mikeBuffer;

    private boolean isSendWav;

    ////////////////////////////////////////////////////////////////////////////////

    //public PcmGenerator(ConcurrentCyclicFIFO<byte[]> mikeBuffer, AudioInputStream stream, int interval) {
    public PcmGenerator(ConcurrentCyclicFIFO<byte[]> mikeBuffer, int interval) {
        super(interval);

        //this.stream = stream;
        this.mikeBuffer = mikeBuffer;

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        this.isSendWav = configManager.isSendWav();

        /*if (isSendWav) {
            WavFile wavFile = VoipClient.getInstance().getWavFile();
            if (wavFile == null) {
                BUFFER_LENGTH = MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_WB) ? 640 : 320;
                isSendWav = false;
            } else {
                if (wavFile.getSampleRate() == 16000) {
                    BUFFER_LENGTH = 640;
                } else {
                    BUFFER_LENGTH = 320;
                }

                try {
                    wavStream = wavFile.loadWavFileToAudioInputStream();
                } catch (Exception e) {
                    logger.warn("PcmGenerator.Exception", e);
                }
            }
        } else {*/
            BUFFER_LENGTH = MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_WB) ? 640 : 320;
        //}

        /*if (wavStream == null) {
            isSendWav = false;
        }*/
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void run() {
        byte[] data = new byte[BUFFER_LENGTH];

        /*try {
            if (isSendWav) {
                if (wavStream.read(data) != -1) {
                    mikeBuffer.offer(data); // RIFF type > little-endian
                }
            } else {
                if (stream.read(data) != -1) {
                    // Convert to little endian.
                    if (VoipClient.getInstance().isTargetBigEndian()) {
                        data = RtpUtil.changeByteOrder(data);
                    }
                    mikeBuffer.offer(data); // little-endian
                }
            }
        }
        catch (Exception e) {
            logger.warn("PcmGenerator.run.Exception", e);
        }*/
    }

}
