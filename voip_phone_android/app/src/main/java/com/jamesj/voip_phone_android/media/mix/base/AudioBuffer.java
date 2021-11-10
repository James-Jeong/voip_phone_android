package com.jamesj.voip_phone_android.media.mix.base;

import com.jamesj.voip_phone_android.media.protocol.base.ConcurrentCyclicFIFO;
import com.orhanobut.logger.Logger;

/**
 * @class public class AudioBuffer
 * @brief AudioBuffer class
 */
public class AudioBuffer {

    /* Concurrent Cyclic FIFO Buffer */
    private final ConcurrentCyclicFIFO<AudioFrame> buffer = new ConcurrentCyclicFIFO<>();
    /* AudioBuffer key > Call-ID */
    private final String id;

    ////////////////////////////////////////////////////////////////////////////////

    public AudioBuffer(String id) {
        this.id = id;

        Logger.d("AudioBuffer is created. (id=%s)", id);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getId() {
        return id;
    }

    public AudioFrame evolve() {
        return buffer.poll();
    }

    public void resetBuffer() {
        buffer.clear();
    }

    public void offer(AudioFrame audioFrame) {
        buffer.offer(audioFrame);
    }

}
