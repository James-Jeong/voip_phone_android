package com.jamesj.voip_phone_android.signal.module;

import com.jamesj.voip_phone_android.signal.base.RoomInfo;
import com.orhanobut.logger.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class GroupCallManager {
    
    /* GroupCallManager Singleton object */
    private static GroupCallManager groupCallManager = null;

    /* Key: Room-ID (using session-id), value: RoomInfo */
    private final HashMap<String, RoomInfo> roomMap = new HashMap<>();
    /* Room Map Lock */
    private final ReentrantLock roomMapLock = new ReentrantLock();

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public GroupCallManager()
     * @brief GroupCallManager 생성자 함수
     */
    public GroupCallManager() {
        // Nothing
    }

    public static GroupCallManager getInstance () {
        if (groupCallManager == null) {
            groupCallManager = new GroupCallManager();
        }
        return groupCallManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public int getRoomMapSize() {
        try {
            roomMapLock.lock();

            return roomMap.size();
        } catch (Exception e) {
            Logger.w("Fail to get the room map size.");
            return 0;
        } finally {
            roomMapLock.unlock();
        }
    }

    public void addRoomInfo(String roomId, String callId) {
        if (roomId == null || callId == null) {
            Logger.w("Fail to add the room info. (roomId=%s)", roomId);
            return;
        }

        try {
            roomMapLock.lock();

            RoomInfo roomInfo = roomMap.get(roomId);
            if (roomInfo == null) {
                // Room 없으면 새로 생성
                roomInfo = new RoomInfo(roomId);
                roomMap.putIfAbsent(roomId, roomInfo);
            }

            // Room 에 지정한 Call 추가
            if (roomInfo.getRemoteCallList(callId) == null) {
                roomInfo.addCall(callId);
                Logger.d("Success to add the callId in the room. (callId=%s)", callId);
            } else {
                Logger.w("Fail to add the callId in the room. (callId=%s)", callId);
            }

            Logger.d("Success to add the room info. (roomId=%s, callId=%s)", roomId, callId);
        } catch (Exception e) {
            Logger.w("Fail to add the room info. (roomId=%s)", roomId);
        } finally {
            roomMapLock.unlock();
        }
    }

    public RoomInfo deleteRoomInfo(String roomId, String callId) {
        if (roomId == null || callId == null) { return null; }

        try {
            roomMapLock.lock();

            RoomInfo roomInfo = roomMap.get(roomId);
            if (roomInfo == null) {
                return null;
            }

            if (roomInfo.deleteCall(callId) != null) {
                Logger.d("Success to delete the callId in the room. (roomId=%s, callId=%s)", roomId, callId);
            }

            // Room 안에 모든 호가 삭제되면 Room 도 삭제
            if (roomInfo.getCallMapSize() == 0) {
                Logger.d("Success to delete the room info. (roomId=%s, callId=%s)", roomId, callId);
                return roomMap.remove(roomId);
            } else {
                return roomInfo;
            }
        } catch (Exception e) {
            Logger.w("Fail to delete the room info. (roomId=%s, callId=%s)", roomId, callId);
            return null;
        } finally {
            roomMapLock.unlock();
        }
    }

    public Map<String, RoomInfo> getCloneRoomMap( ) {
        HashMap<String, RoomInfo> cloneMap;

        try {
            roomMapLock.lock();

            cloneMap = (HashMap<String, RoomInfo>) roomMap.clone();
        } catch (Exception e) {
            Logger.w("Fail to clone the room map.");
            cloneMap = roomMap;
        } finally {
            roomMapLock.unlock();
        }

        return cloneMap;
    }

    public RoomInfo getRoomInfo(String roomId) {
        if (roomId == null) { return null; }

        try {
            roomMapLock.lock();

            return roomMap.get(roomId);
        } catch (Exception e) {
            Logger.w("Fail to get the room info. (roomId=%s)", roomId);
            return null;
        } finally {
            roomMapLock.unlock();
        }
    }

    public void clearRoomInfoMap() {
        try {
            roomMapLock.lock();

            roomMap.clear();
            Logger.d("Success to clear the room map.");
        } catch (Exception e) {
            Logger.w("Fail to clear the room map.");
        } finally {
            roomMapLock.unlock();
        }
    }


}
