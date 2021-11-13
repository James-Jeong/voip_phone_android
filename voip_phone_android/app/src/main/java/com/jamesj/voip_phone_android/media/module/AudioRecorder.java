package com.jamesj.voip_phone_android.media.module;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioRecord;

import androidx.core.app.ActivityCompat;

import java.util.concurrent.locks.ReentrantLock;

public class AudioRecorder {

    ////////////////////////////////////////////////////////////////////////////////

    private AudioRecord audioRecord;
    private final ReentrantLock audioRecordLock = new ReentrantLock();

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
                SoundHandler.BUFFER_SIZE
        );
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void startRecording() {
        try {
            audioRecordLock.lock();

            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                return;
            }

            audioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            audioRecordLock.unlock();
        }
    }

    public void stopRecording() {
        try {
            audioRecordLock.lock();

            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                return;
            }

            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            audioRecordLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public byte[] read() {
        try {
            audioRecordLock.lock();

            if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                return null;
            }

            byte[] data = new byte[SoundHandler.BUFFER_SIZE];
            int retValue = audioRecord.read(data, 0, SoundHandler.BUFFER_SIZE);
            if (retValue > 0) {
                return data;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            audioRecordLock.unlock();
        }
    }

}
