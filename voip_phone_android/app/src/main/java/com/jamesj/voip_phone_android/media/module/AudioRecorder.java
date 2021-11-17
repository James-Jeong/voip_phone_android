package com.jamesj.voip_phone_android.media.module;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioRecord;

import androidx.core.app.ActivityCompat;

public class AudioRecorder {

    ////////////////////////////////////////////////////////////////////////////////

    private AudioRecord audioRecord;

    ////////////////////////////////////////////////////////////////////////////////

    public AudioRecorder(Activity activity) {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
        }

        audioRecord = new AudioRecord(
                SoundHandler.AUDIO_SOURCE,
                SoundHandler.SAMPLE_RATE,
                SoundHandler.CHANNEL_COUNT,
                SoundHandler.AUDIO_FORMAT,
                320
        );
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void startRecording() {
        if (audioRecord == null) {
            return;
        }

        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            return;
        }

        audioRecord.startRecording();
    }

    public void stopRecording() {
        if (audioRecord == null) {
            return;
        }

        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
            return;
        }

        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public byte[] read() {
        if (audioRecord == null) {
            return null;
        }

        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            return null;
        }

        byte[] data = new byte[320];
        int retValue = audioRecord.read(data, 0, 320);
        if (retValue > 0) {
            return data;
        } else {
            return null;
        }
    }

}
