package com.jamesj.voip_phone_android.media.codec.evs;

import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.media.MediaManager;
import com.jamesj.voip_phone_android.media.codec.evs.base.EvsTaskUnit;
import com.jamesj.voip_phone_android.media.module.base.MediaFrame;
import com.jamesj.voip_phone_android.media.module.base.TaskUnit;
import com.jamesj.voip_phone_android.media.protocol.base.ConcurrentCyclicFIFO;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.orhanobut.logger.Logger;

import org.scijava.nativelib.NativeLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @class public class EvsManager
 * @brief EvsManager class
 */
public class EvsManager {

    private final int EVS_ENC_DATA_SIZE = 324;
    private final int EVS_DEC_DATA_SIZE = 320;

    private static EvsManager evsManager = null;

    private EvsTaskUnit udpSenderTaskUnit = null;
    private EvsTaskUnit udpReceiverTaskUnit = null;
    private EvsTaskUnit audioMixerTaskUnit = null;

    private final String[] evsEncArgv = new String[]{
            "EVS_enc.exe",
            "8000",
            "8",
            "none",
            "none"
    };

    private final String[] evsDecArgv = new String[]{
            "EVS_dec.exe",
            "8",
            "none",
            "none"
    };

    ////////////////////////////////////////////////////////////////////////////////

    public EvsManager() {
        // Nothing
    }

    public static EvsManager getInstance () {
        if (evsManager == null) {
            evsManager = new EvsManager();

        }
        return evsManager;
    }

    public void init() {
        /*String curUserDir = System.getProperty("user.dir");
        if (curUserDir.endsWith("bin")) {
            curUserDir = curUserDir.substring(0, curUserDir.lastIndexOf("bin") - 1);
        }

        if (SystemManager.getInstance().getOs().contains("win")) {
            curUserDir += "\\lib\\libevsjni.dll";
        } else {
            curUserDir += "/lib/libevsjni.so";
        }*/

        try {
            //FrameManager.getInstance().appendTextToFrame(ServiceManager.CLIENT_FRAME_NAME, "Loading... the evs library. (path=" + curUserDir + ")");
            //FrameManager.getInstance().appendTextToFrame("Loading... the evs library.\n");
            NativeLoader.loadLibrary("evsjni");
            //System.load(curUserDir);
            //FrameManager.getInstance().appendTextToFrame("Loaded the evs library.\n");
            //FrameManager.getInstance().appendTextToFrame(ServiceManager.CLIENT_FRAME_NAME, "Loaded the evs library. (path=" + curUserDir + ")");
        } catch (Exception e) {
            String[] audioCodecStrArray = MediaManager.getInstance().getSupportedAudioCodecList();
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            configManager.setPriorityAudioCodec(audioCodecStrArray[0]);
            configManager.setIniValue(ConfigManager.SECTION_MEDIA, ConfigManager.FIELD_PRIORITY_CODEC, audioCodecStrArray[0]);
            //FrameManager.getInstance().selectPriorityCodec(audioCodecStrArray[0]);

            String resultMsg = "Fail to load the evs library.\nPriority codec is changed to [" + audioCodecStrArray[0] + "].\n" + e.getMessage();
            //logger.error("Fail to load the amr library. (path=%s)", curUserDir);
            //FrameManager.getInstance().appendTextToFrame(resultMsg);
            //FrameManager.getInstance().popUpErrorMsg(resultMsg);
            //FrameManager.getInstance().popUpWarnMsgToFrame(resultMsg);
            Logger.d("Priority audio codec option is changed. (before=[%s], after=[%s])", configManager.getPriorityAudioCodec(), audioCodecStrArray[0]);

            return;
        }

        /*FrameManager.getInstance().popUpInfoMsgToFrame(
                //"Success to load the evs library. (path=" + curUserDir + ")"
                "Success to load the evs library."
        );*/
    }

    ////////////////////////////////////////////////////////////////////////////////
    // JNI

    public native byte[] enc_evs(String[] args, byte[] src_data);
    public byte[] encEvs(String[] args, byte[] srcData) {
        return enc_evs(args, srcData);
    }

    public native byte[] dec_evs(String[] args, int dst_data_len, byte[] src_data);
    public byte[] decEvs(String[] args, int dstDataLen, byte[] src_data) {
        return dec_evs(args, dstDataLen, src_data);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void startUdpSenderTask(ConcurrentCyclicFIFO<MediaFrame> outputBuffer) {
        if (udpSenderTaskUnit == null) {
            udpSenderTaskUnit = new EvsTaskUnit(10);
            udpSenderTaskUnit.setOutputMediaFrameBuffer(outputBuffer);

            ScheduledThreadPoolExecutor udpSenderTaskExecutor = udpSenderTaskUnit.getTaskExecutor();
            if (udpSenderTaskExecutor == null) {
                udpSenderTaskExecutor = new ScheduledThreadPoolExecutor(5);

                try {
                    UdpSenderTask udpSenderTask = new UdpSenderTask(
                            20,
                            udpSenderTaskUnit
                    );

                    udpSenderTaskExecutor.scheduleAtFixedRate(
                            udpSenderTask,
                            udpSenderTask.getInterval(),
                            udpSenderTask.getInterval(),
                            TimeUnit.MILLISECONDS
                    );
                } catch (Exception e) {
                    Logger.w("EvsManager.startUdpSenderTask.Exception", e);
                }

                Logger.d("EvsManager UdpSenderTask is added.");
            }
        }
    }

    public void stopUdpSenderTask() {
        if (udpSenderTaskUnit != null) {
            udpSenderTaskUnit.clearInputBuffer();

            ScheduledThreadPoolExecutor udpSenderTaskExecutor = udpSenderTaskUnit.getTaskExecutor();
            if (udpSenderTaskExecutor != null) {
                udpSenderTaskUnit.setOutputMediaFrameBuffer(null);
                udpSenderTaskExecutor.shutdown();
                Logger.d("EvsManager UdpSenderTask is removed.");
            }

            udpSenderTaskUnit = null;
        }
    }

    public void addUdpSenderInputData(byte[] data) {
        if (udpSenderTaskUnit == null) {
            return;
        }

        if (data.length != EVS_DEC_DATA_SIZE) {
            Logger.w("UdpSender input data length is not [%s]. (length=%s)", EVS_DEC_DATA_SIZE, data.length);
            return;
        }

        udpSenderTaskUnit.getInputBuffer().offer(data);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void startUdpReceiverTask(ConcurrentCyclicFIFO<MediaFrame> outputBuffer) {
        if (udpReceiverTaskUnit == null) {
            udpReceiverTaskUnit = new EvsTaskUnit(10);
            udpReceiverTaskUnit.setOutputMediaFrameBuffer(outputBuffer);

            ScheduledThreadPoolExecutor udpReceiverTaskExecutor = udpReceiverTaskUnit.getTaskExecutor();
            if (udpReceiverTaskExecutor == null) {
                udpReceiverTaskExecutor = new ScheduledThreadPoolExecutor(5);

                try {
                    UdpReceiverTask udpReceiverTask = new UdpReceiverTask(
                            20,
                            udpReceiverTaskUnit
                    );

                    udpReceiverTaskExecutor.scheduleAtFixedRate(
                            udpReceiverTask,
                            udpReceiverTask.getInterval(),
                            udpReceiverTask.getInterval(),
                            TimeUnit.MILLISECONDS
                    );
                } catch (Exception e) {
                    Logger.w("EvsManager.startUdpReceiverTask.Exception", e);
                }

                Logger.d("EvsManager UdpReceiverTask is added.");
            }
        }
    }

    public void stopUdpReceiverTask() {
        if (udpReceiverTaskUnit != null) {
            udpReceiverTaskUnit.clearInputBuffer();

            ScheduledThreadPoolExecutor udpReceiverTaskExecutor = udpReceiverTaskUnit.getTaskExecutor();
            if (udpReceiverTaskExecutor != null) {
                udpReceiverTaskUnit.setOutputMediaFrameBuffer(null);
                udpReceiverTaskExecutor.shutdown();
                Logger.d("EvsManager UdpReceiverTask is removed.");
            }

            udpReceiverTaskUnit = null;
        }
    }

    public void addUdpReceiverInputData(byte[] data) {
        if (udpReceiverTaskUnit == null) {
            return;
        }

        /*if (data.length != udpReceiverTaskUnit.getDataSize()) {
            Logger.w("UdpReceiver input data length is not [%s]. (length=%s)", udpReceiverTaskUnit.getDataSize(), data.length);
            return;
        }*/

        //Logger.d("addUdpReceiverInputData: data.length=%s", data.length);
        udpReceiverTaskUnit.getInputBuffer().offer(data);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void startAudioMixerTask(String[] evsDecArgv, ConcurrentCyclicFIFO<byte[]> outputBuffer) {
        if (evsDecArgv == null || outputBuffer == null) {
            return;
        }

        if (audioMixerTaskUnit == null) {
            audioMixerTaskUnit = new EvsTaskUnit(10);
            audioMixerTaskUnit.setOutputByteBuffer(outputBuffer);

            ScheduledThreadPoolExecutor audioMixerTaskExecutor = audioMixerTaskUnit.getTaskExecutor();
            if (audioMixerTaskExecutor == null) {
                audioMixerTaskExecutor = new ScheduledThreadPoolExecutor(5);

                try {
                    AudioMixerTask audioMixerTask = new AudioMixerTask(
                            20,
                            audioMixerTaskUnit,
                            evsDecArgv
                    );

                    audioMixerTaskExecutor.scheduleAtFixedRate(
                            audioMixerTask,
                            audioMixerTask.getInterval(),
                            audioMixerTask.getInterval(),
                            TimeUnit.MILLISECONDS
                    );
                } catch (Exception e) {
                    Logger.w("EvsManager.startAudioMixerTask.Exception", e);
                }

                Logger.d("EvsManager AudioMixerTask is added.");
            }
        }
    }

    public void stopAudioMixerTask() {
        if (audioMixerTaskUnit != null) {
            audioMixerTaskUnit.clearInputBuffer();

            ScheduledThreadPoolExecutor audioMixerTaskExecutor = audioMixerTaskUnit.getTaskExecutor();
            if (audioMixerTaskExecutor != null) {
                audioMixerTaskUnit.setOutputByteBuffer(null);
                audioMixerTaskExecutor.shutdown();
                Logger.d("EvsManager AudioMixerTask is removed.");
            }

            audioMixerTaskUnit = null;
        }
    }

    public void addAudioMixerInputData(byte[] data) {
        if (audioMixerTaskUnit == null) {
            return;
        }

        /*if (data.length != audioMixerTaskUnit.getDataSize()) {
            Logger.w("AudioMixer input data length is not [%s]. (length=%s)", audioMixerTaskUnit.getDataSize(), data.length);
            return;
        }*/

        //Logger.d("addAudioMixerInputData: data.length=%s", data.length);
        audioMixerTaskUnit.getInputBuffer().offer(data);
    }

    ////////////////////////////////////////////////////////////////////////////////

    private class UdpSenderTask extends TaskUnit {

        final EvsTaskUnit udpSenderTaskUnit;
        int curDataCount = 0;
        int totalDataLength = 0;

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        /////////////////////////////////////////////

        protected UdpSenderTask(int interval, EvsTaskUnit udpSenderTaskUnit) {
            super(interval);
            this.udpSenderTaskUnit = udpSenderTaskUnit;
            Logger.d("udpSenderTaskUnit: %s", udpSenderTaskUnit);
        }

        @Override
        public void run() {
            if (udpSenderTaskUnit == null) {
                Logger.w("udpSenderTaskUnit is null.");
                return;
            }

            try {
                byte[] curData = udpSenderTaskUnit.getInputBuffer().poll();
                if (curData == null || curData.length == 0) {
                    //Logger.w("UdpSenderTask > curData is null.");
                    return;
                }

                // 10 개씩 모아서 encode
                byteArrayOutputStream.write(curData);
                //System.arraycopy(curData, 0, totalData, totalDataLength, udpSenderTaskUnit.getDataSize());
                totalDataLength += EVS_DEC_DATA_SIZE;

                curDataCount++;
                if (curDataCount < udpSenderTaskUnit.getMergeCount()) {
                    //Logger.d("(%s) udpSenderTaskUnit > curDataSize=%s (input)", curDataCount, curData.length);
                    return;
                }

                byte[] totalData = byteArrayOutputStream.toByteArray(); // 3200 ([320 * 10])
                byte[] newData = EvsManager.getInstance().encEvs( // 3240 ([320 * 10] + [4 * 10])
                        evsEncArgv,
                        totalData
                );

                if (newData != null) {
                    // record
                    /*RecordManager evsRecordManager = VoipClient.getInstance().getTargetEvsRecordManager();
                    if (evsRecordManager != null) {
                        evsRecordManager.addData(newData);
                        //evsRecordManager.writeFileStream(currentByteBuffer);
                    }*/

                    // send
                    totalDataLength = 0;
                    if (udpSenderTaskUnit.getOutputMediaFrameBuffer() != null) {
                        for (int i = 0; i < curDataCount; i++) {
                            curData = new byte[EVS_ENC_DATA_SIZE];
                            System.arraycopy(newData, totalDataLength, curData, 0, EVS_ENC_DATA_SIZE);
                            udpSenderTaskUnit.getOutputMediaFrameBuffer().offer(
                                    new MediaFrame(
                                            false,
                                            curData
                                    )
                            );
                            totalDataLength += EVS_ENC_DATA_SIZE;
                        }
                    }
                }

                // clear
                Arrays.fill(totalData, (byte) 0);
                totalDataLength = 0;
                curDataCount = 0;
                byteArrayOutputStream.reset();
            } catch (Exception e) {
                Logger.w("UdpSenderTask.run.Exception", e);
            } finally {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    Logger.w("UdpSenderTask.run.IOException", e);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    private class UdpReceiverTask extends TaskUnit {

        final EvsTaskUnit udpReceiverTaskUnit;
        int curDataCount = 0;
        int totalDataLength = 0;

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        /////////////////////////////////////////////

        protected UdpReceiverTask(int interval, EvsTaskUnit udpReceiverTaskUnit) {
            super(interval);
            this.udpReceiverTaskUnit = udpReceiverTaskUnit;
            Logger.d("udpReceiverTaskUnit: %s", udpReceiverTaskUnit);
        }

        @Override
        public void run() {
            if (udpReceiverTaskUnit == null) {
                Logger.w("udpReceiverTaskUnit is null.");
                return;
            }

            try {

                byte[] curData = udpReceiverTaskUnit.getInputBuffer().poll();
                if (curData == null || curData.length == 0) {
                    //Logger.w("UdpReceiverTask > curData is null.");
                    return;
                }

                // record
                /*RecordManager evsRecordManager = VoipClient.getInstance().getSourceEvsRecordManager();
                if (evsRecordManager != null) {
                    evsRecordManager.addData(curData);
                    //evsRecordManager.writeFileStream(data);
                }*/

                // 10 개씩 모아서 decode
                byteArrayOutputStream.write(curData);
                //System.arraycopy(curData, 0, totalData, totalDataLength, udpReceiverTaskUnit.getDataSize());
                totalDataLength += EVS_ENC_DATA_SIZE;
                //Logger.d("udpReceiverTaskUnit > totalDataLength: %s", totalDataLength);

                curDataCount++;
                if (curDataCount < udpReceiverTaskUnit.getMergeCount()) {
                    //Logger.d("(%s) udpReceiverTaskUnit > curDataSize=%s (input)", curDataCount, curData.length);
                    return;
                }

                byte[] totalData = byteArrayOutputStream.toByteArray(); // 3240
                byte[] newData = EvsManager.getInstance().decEvs( // 3200
                        evsDecArgv,
                        EVS_ENC_DATA_SIZE * udpReceiverTaskUnit.getMergeCount(),
                        totalData
                );

                if (newData != null) {
                    //Logger.d("udpReceiverTaskUnit > newDataSize=%s", newData.length);

                    // send
                    totalDataLength = 0;
                    if (udpReceiverTaskUnit.getOutputMediaFrameBuffer() != null) {
                        for (int i = 0; i < curDataCount; i++) {
                            curData = new byte[EVS_DEC_DATA_SIZE];
                            System.arraycopy(newData, totalDataLength, curData, 0, EVS_DEC_DATA_SIZE);
                            udpReceiverTaskUnit.getOutputMediaFrameBuffer().offer(
                                    new MediaFrame(
                                            false,
                                            curData
                                    )
                            );
                            totalDataLength += EVS_DEC_DATA_SIZE;
                            //Logger.d("udpReceiverTaskUnit > curDataSize=%s (output)", curData.length);
                        }
                    }
                }

                // clear
                Arrays.fill(totalData, (byte) 0);
                totalDataLength = 0;
                curDataCount = 0;
                byteArrayOutputStream.reset();
            } catch (Exception e) {
                Logger.w("UdpReceiverTask.run.Exception", e);
            } finally {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    Logger.w("UdpReceiverTask.run.IOException", e);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    private class AudioMixerTask extends TaskUnit {

        final String[] evsDecArgv;
        final EvsTaskUnit audioMixerTaskUnit;
        int curDataCount = 0;
        int totalDataLength = 0;

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        /////////////////////////////////////////////

        protected AudioMixerTask(int interval, EvsTaskUnit audioMixerTaskUnit, String[] evsDecArgv) {
            super(interval);
            this.evsDecArgv = evsDecArgv;
            this.audioMixerTaskUnit = audioMixerTaskUnit;
            Logger.d("audioMixerTaskUnit: %s", audioMixerTaskUnit);
        }

        @Override
        public void run() {
            if (audioMixerTaskUnit == null) {
                Logger.w("audioMixerTaskUnit is null.");
                return;
            }

            try {
                byte[] curData = audioMixerTaskUnit.getInputBuffer().poll();
                if (curData == null || curData.length == 0) {
                    //Logger.w("AudioMixerTask > curData is null.");
                    return;
                }

                // 10 개씩 모아서 decode
                byteArrayOutputStream.write(curData);
                //System.arraycopy(curData, 0, totalData, totalDataLength, audioMixerTaskUnit.getDataSize());
                totalDataLength += EVS_ENC_DATA_SIZE;

                curDataCount++;
                if (curDataCount < audioMixerTaskUnit.getMergeCount()) {
                    //Logger.d("(%s) audioMixerTaskUnit > curDataSize=%s (input)", curDataCount, curData.length);
                    return;
                }

                byte[] totalData = byteArrayOutputStream.toByteArray(); // 3240
                byte[] newData = EvsManager.getInstance().decEvs( // 3200
                        evsDecArgv,
                        EVS_ENC_DATA_SIZE * audioMixerTaskUnit.getMergeCount(),
                        totalData
                );

                if (newData != null) {
                    //Logger.d("audioMixerTaskUnit > newDataSize=%s", newData.length);

                    // send
                    totalDataLength = 0;
                    if (audioMixerTaskUnit.getOutputByteBuffer() != null) {
                        for (int i = 0; i < curDataCount; i++) {
                            curData = new byte[EVS_DEC_DATA_SIZE];
                            System.arraycopy(newData, totalDataLength, curData, 0, EVS_DEC_DATA_SIZE);
                            audioMixerTaskUnit.getOutputByteBuffer().offer(curData);
                            totalDataLength += EVS_DEC_DATA_SIZE;
                            //Logger.d("audioMixerTaskUnit > curDataSize=%s (output)", curData.length);
                        }
                    }
                }

                // clear
                Arrays.fill(totalData, (byte) 0);
                totalDataLength = 0;
                curDataCount = 0;
                byteArrayOutputStream.reset();
            } catch (Exception e) {
                Logger.w("AudioMixerTask.run.Exception", e);
            } finally {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    Logger.w("AudioMixerTask.run.IOException", e);
                }
            }
        }
    }

}
