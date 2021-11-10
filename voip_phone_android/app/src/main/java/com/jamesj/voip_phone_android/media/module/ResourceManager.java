package com.jamesj.voip_phone_android.media.module;

import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.orhanobut.logger.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Resource Manager
 */
public class ResourceManager {

    private static ResourceManager resourceManager = null;
    private final ConcurrentLinkedQueue<Integer> channelQueues;

    private int localUdpPortMin = 0;
    private int localUdpPortMax = 0;
    private final int portGap = 2;

    ////////////////////////////////////////////////////////////////////////////////

    public ResourceManager( ) {
        channelQueues = new ConcurrentLinkedQueue<>();
    }

    public static ResourceManager getInstance ( ) {
        if (resourceManager == null) {
            resourceManager = new ResourceManager();
        }

        return resourceManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void initResource() {
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        localUdpPortMin = configManager.getNettyServerPort();
        localUdpPortMax = localUdpPortMin + 2000;

        for (int idx = localUdpPortMin; idx <= localUdpPortMax; idx += portGap) {
            try {
                channelQueues.add(idx);
            } catch (Exception e) {
                Logger.e("Exception to RTP port resource in Queue", e);
                return;
            }
        }

        Logger.i("Ready to RTP port resource in Queue. (port range: %s - %s, gap=%s)",
                localUdpPortMin, localUdpPortMax, portGap
        );
    }

    public void releaseResource () {
        channelQueues.clear();
        Logger.i("Release RTP port resource in Queue. (port range: %s - %s, gap=%s)",
                localUdpPortMin, localUdpPortMax, portGap
        );
    }

    public int takePort () {
        if (channelQueues.isEmpty()) {
            Logger.w("RTP port resource in Queue is empty.");
            return -1;
        }

        int port = -1;
        try {
            Integer value = channelQueues.poll();
            if (value != null) {
                port = value;
            }
        } catch (Exception e) {
            Logger.w("Exception to get RTP port resource in Queue.", e);
        }

        Logger.d("Success to get RTP port(=%s) resource in Queue.", port);
        return port;
    }

    public void restorePort (int port) {
        if (!channelQueues.contains(port)) {
            try {
                channelQueues.offer(port);
            } catch (Exception e) {
                Logger.w("Exception to restore RTP port(=%s) resource in Queue.", port, e);
            }
        }
    }

    public void removePort (int port) {
        try {
            channelQueues.remove(port);
        } catch (Exception e) {
            Logger.w("Exception to remove to RTP port(=%s) resource in Queue.", port, e);
        }
    }

}
