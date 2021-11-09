package com.jamesj.voip_phone_android.signal.module;

import com.jamesj.voip_phone_android.signal.base.RegiInfo;
import com.orhanobut.logger.Logger;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class RegiManager {

    private static RegiManager regiManager = null;

    private final HashMap<String, RegiInfo> regiMap = new HashMap<>();
    private final ReentrantLock regiMapLock = new ReentrantLock();

    ////////////////////////////////////////////////////////////////////////////////

    public RegiManager() {
        // Nothing
    }

    public static RegiManager getInstance () {
        if (regiManager == null) {
            regiManager = new RegiManager();
        }
        return regiManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void scheduleRegi(String mdn, int expires) {
        if (getRegi(mdn) == null) {
            Logger.w("Fail to schedule the register delete handler. Not found the register info. (mdn=%s)", mdn);
            return;
        }

        //String handlerName = RegiDeleteHandler.class.getSimpleName() + "_" + mdn;
        //TaskManager.getInstance().addTask(handlerName, new RegiDeleteHandler(handlerName, mdn, expires));
    }

    ////////////////////////////////////////////////////////////////////////////////

    public int getRegiMapSize() {
        try {
            regiMapLock.lock();

            return regiMap.size();
        } catch (Exception e) {
            Logger.w("RegiManager.getRegiMapSize.Exception", e);
            return 0;
        } finally {
            regiMapLock.unlock();
        }
    }

    public void addRegi(String mdn, String ip, int port, int expires) {
        if (mdn == null || ip == null || port <= 0 || expires <= 0) {
            Logger.w("Fail to add a new register info. Argument error is occurred. (mdn=%s, ip=%s, port=%s, expires=%s)",
                    mdn, ip, port, expires
            );
            return;
        }

        try {
            regiMapLock.lock();

            regiMap.putIfAbsent(
                    mdn,
                    new RegiInfo(
                            mdn,
                            ip,
                            port,
                            expires
                    )
            );
        } catch (Exception e) {
            Logger.w("RegiManager.addRegi.Exception", e);
        } finally {
            regiMapLock.unlock();
        }
    }

    public RegiInfo getRegi(String mdn) {
        if (mdn == null) { return null; }

        try {
            regiMapLock.lock();

            return regiMap.get(mdn);
        } catch (Exception e) {
            Logger.w("RegiManager.getRegi.Exception", e);
            return null;
        } finally {
            regiMapLock.unlock();
        }
    }

    public void deleteRegi(String mdn) {
        if (mdn == null) { return; }

        try {
            regiMapLock.lock();

            regiMap.remove(mdn);
        } catch (Exception e) {
            Logger.w("RegiManager.deleteRegi.Exception", e);
        } finally {
            regiMapLock.unlock();
        }
    }

}
