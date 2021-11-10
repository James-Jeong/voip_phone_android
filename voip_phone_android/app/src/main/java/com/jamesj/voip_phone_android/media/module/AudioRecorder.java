package com.jamesj.voip_phone_android.media.module;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

import java.util.concurrent.locks.ReentrantLock;

public class AudioRecorder {

    ////////////////////////////////////////////////////////////////////////////////

    private final int audioSource = MediaRecorder.AudioSource.MIC;
    private final int sampleRate = 8000;
    private final int channelCount = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelCount, audioFormat);

    ////////////////////////////////////////////////////////////////////////////////

    private AudioRecord audioRecord;
    private final ReentrantLock audioRecordLock = new ReentrantLock();

    ////////////////////////////////////////////////////////////////////////////////

    public AudioRecorder(Activity activity) {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
        }

        audioRecord = new AudioRecord(
                audioSource,
                sampleRate,
                channelCount,
                audioFormat,
                bufferSize
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

            byte[] data = new byte[bufferSize];
            int retValue = audioRecord.read(data, 0, bufferSize);
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
