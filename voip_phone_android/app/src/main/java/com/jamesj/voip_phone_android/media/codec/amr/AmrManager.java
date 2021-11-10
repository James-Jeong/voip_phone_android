package com.jamesj.voip_phone_android.media.codec.amr;

import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.media.MediaManager;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.orhanobut.logger.Logger;

import org.scijava.nativelib.NativeLoader;

/**
 * @class public class AmrManager
 * @brief AmrManager class
 */
public class AmrManager {

    private static AmrManager amrManager = null;

    ////////////////////////////////////////////////////////////////////////////////

    public AmrManager() {
        // Nothing
    }

    public static AmrManager getInstance () {
        if (amrManager == null) {
            amrManager = new AmrManager();

        }
        return amrManager;
    }

    public void init() {
        /*String curUserDir = System.getProperty("user.dir");
        if (curUserDir.endsWith("bin")) {
            curUserDir = curUserDir.substring(0, curUserDir.lastIndexOf("bin") - 1);
        }

        if (SystemManager.getInstance().getOs().contains("win")) {
            curUserDir += "\\lib\\libamrjni.dll";
        } else {
            curUserDir += "/lib/libamrjni.so";
        }*/

        try {
            //FrameManager.getInstance().appendTextToFrame(ServiceManager.CLIENT_FRAME_NAME, "Loading... the amr library. (path=" + curUserDir + ")");
            //FrameManager.getInstance().appendTextToFrame("Loading... the amr library.\n");
            NativeLoader.loadLibrary("amrjni");
            //System.load(curUserDir);
            //FrameManager.getInstance().appendTextToFrame("Loaded the amr library.\n");
            //FrameManager.getInstance().appendTextToFrame(ServiceManager.CLIENT_FRAME_NAME, "Loaded the amr library. (path=" + curUserDir + ")");
        } catch (Exception e) {
            String[] audioCodecStrArray = MediaManager.getInstance().getSupportedAudioCodecList();
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            configManager.setPriorityAudioCodec(audioCodecStrArray[0]);
            configManager.setIniValue(ConfigManager.SECTION_MEDIA, ConfigManager.FIELD_PRIORITY_CODEC, audioCodecStrArray[0]);
            //FrameManager.getInstance().selectPriorityCodec(audioCodecStrArray[0]);

            String resultMsg = "Fail to load the amr library.\nPriority codec is changed to [" + audioCodecStrArray[0] + "].\n" + e.getMessage();
            //logger.error("Fail to load the amr library. (path=%s)", curUserDir);
            //FrameManager.getInstance().appendTextToFrame(resultMsg);
            //FrameManager.getInstance().popUpErrorMsg(resultMsg);
            //FrameManager.getInstance().popUpWarnMsgToFrame(resultMsg);
            Logger.d("Priority audio codec option is changed. (before=[%s], after=[%s])", configManager.getPriorityAudioCodec(), audioCodecStrArray[0]);

            return;
        }

        /*FrameManager.getInstance().popUpInfoMsgToFrame(
                //"Success to load the amr library. (path=" + curUserDir + ")"
                "Success to load the amr library."
        );*/
    }

    ////////////////////////////////////////////////////////////////////////////////

    //
    public native byte[] enc_amrnb(int req_mode, byte[] src_data);
    public byte[] encAmrNb(int reqMode, byte[] srcData) {
        return enc_amrnb(reqMode, srcData);
    }

    public native void start_enc_amrnb();
    public void startEncAmrNb() {
        start_enc_amrnb();
    }

    public native void stop_enc_amrnb();
    public void stopEncAmrNb() {
        stop_enc_amrnb();
    }

    public native byte[] dec_amrnb(int dst_data_len, byte[] src_data);
    public byte[] decAmrNb(int dstDataLen, byte[] srcData) {
        return dec_amrnb(dstDataLen, srcData);
    }

    public native void start_dec_amrnb();
    public void startDecAmrNb() {
        start_dec_amrnb();
    }

    public native void stop_dec_amrnb();
    public void stopDecAmrNb() {
        stop_dec_amrnb();
    }
    //

    //
    public native byte[] enc_amrwb(int req_mode, byte[] src_data);
    public byte[] encAmrWb(int reqMode, byte[] srcData) {
        return enc_amrwb(reqMode, srcData);
    }

    public native void start_enc_amrwb();
    public void startEncAmrWb() {
        start_enc_amrwb();
    }

    public native void stop_enc_amrwb();
    public void stopEncAmrWb() {
        stop_enc_amrwb();
    }

    public native byte[] dec_amrwb(int dst_data_len, byte[] src_data);
    public byte[] decAmrWb(int dstDataLen, byte[] srcData) {
        return dec_amrwb(dstDataLen, srcData);
    }

    public native void start_dec_amrwb();
    public void startDecAmrWb() {
        start_dec_amrwb();
    }

    public native void stop_dec_amrwb();
    public void stopDecAmrWb() {
        stop_dec_amrwb();
    }
    //

}
