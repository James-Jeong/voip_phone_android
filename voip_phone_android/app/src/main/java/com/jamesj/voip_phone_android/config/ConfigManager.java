package com.jamesj.voip_phone_android.config;

import android.content.Context;

import com.jamesj.voip_phone_android.media.MediaManager;
import com.jamesj.voip_phone_android.media.sdp.SdpParser;
import com.jamesj.voip_phone_android.media.sdp.base.Sdp;
import com.orhanobut.logger.Logger;

import org.apache.commons.net.ntp.TimeStamp;
import org.ini4j.Ini;

import java.io.FileInputStream;

public class ConfigManager {

    private String configPath;
    private String contactPath;
    private Ini ini = null;

    // Section String
    public static final String SECTION_COMMON = "COMMON"; // COMMON Section 이름
    public static final String SECTION_SIGNAL = "SIGNAL"; // SIGNAL Section 이름
    public static final String SECTION_MEDIA = "MEDIA"; // MEDIA Section 이름
    public static final String SECTION_RECORD = "RECORD"; // RECORD Section 이름
    private static final String SECTION_SDP = "SDP"; // SDP Section 이름

    // Field String
    public static final String FIELD_UDP_RCV_BUFFER_SIZE = "UDP_RCV_BUFFER_SIZE";
    public static final String FIELD_UDP_SND_BUFFER_SIZE = "UDP_SND_BUFFER_SIZE";
    public static final String FIELD_LONG_CALL_TIME = "LONG_CALL_TIME";
    public static final String FIELD_USE_CLIENT = "USE_CLIENT";
    public static final String FIELD_USE_PROXY = "USE_PROXY";
    public static final String FIELD_PROXY_MODE = "PROXY_MODE";
    public static final String FIELD_CALL_AUTO_ACCEPT = "CALL_AUTO_ACCEPT";

    public static final String FIELD_HOST_NAME = "HOST_NAME";
    public static final String FIELD_FROM_IP = "FROM_IP";
    public static final String FIELD_FROM_PORT = "FROM_PORT";
    public static final String FIELD_TO_IP = "TO_IP";
    public static final String FIELD_TO_PORT = "TO_PORT";
    public static final String FIELD_DEFAULT_REGISTER_EXPIRES = "DEFAULT_REGISTER_EXPIRES";
    public static final String FIELD_CALL_RECV_DURATION = "CALL_RECV_DURATION";

    public static final String FIELD_SEND_WAV = "SEND_WAV";
    public static final String FIELD_LAST_WAV_PATH = "LAST_WAV_PATH";
    public static final String FIELD_DTMF = "DTMF";
    public static final String FIELD_PRIORITY_CODEC = "PRIORITY_CODEC";
    public static final String FIELD_NETTY_SERVER_CONSUMER_COUNT = "NETTY_SERVER_CONSUMER_COUNT";
    public static final String FIELD_NETTY_SERVER_IP = "NETTY_SERVER_IP";
    public static final String FIELD_NETTY_SERVER_PORT = "NETTY_SERVER_PORT";
    public static final String FIELD_SPEAKER_VOLUME = "SPEAKER_VOLUME";
    public static final String FIELD_MIKE_VOLUME = "MIKE_VOLUME";
    public static final String FIELD_RELAY = "RELAY";

    public static final String FIELD_RECORD_PATH = "PATH";
    public static final String FIELD_RAW_FILE = "RAW_FILE";
    public static final String FIELD_ENC_FILE = "ENC_FILE";
    public static final String FIELD_DEC_FILE = "DEC_FILE";

    // COMMON
    private int udpRcvBufferSize = 0; // UDP Recv Buffer 크기
    private int udpSndBufferSize = 0; // UDP Send Buffer 크기
    private long longCallTime = 0; // Long Call 삭제 시간
    private boolean useClient = false; // Client 사용 여부
    private boolean useProxy = false; // Client 가 Proxy 사용 여부
    private boolean isProxyMode = false; // SIP Proxy 여부 (현재 프로세스를 Proxy 로 사용할지 여부)
    private boolean callAutoAccept = false; // 호 자동 수락 여부

    // SIGNAL
    private String hostName = null; // SIP Host 이름
    private String fromIp = null; // SIP Host IP
    private int fromPort = 0; // SIP Host Port
    private String toIp = null; // SIP Remote IP
    private int toPort = 0; // SIP Remote Port
    private int defaultRegisterExpires = 0; // Proxy 인 경우 default 사용자 등록 시간
    private int callRecvDuration = 0; // 호 시작 시 상대방이 전화 받을 때까지 대기하는 시간 > 설정된 시간이 지나면 자동으로 Cancel 보낸다.

    // MEDIA
    private boolean sendWav = false;
    private String lastWavPath = null;
    private boolean dtmf = false;
    private String priorityAudioCodec;
    private int nettyServerConsumerCount = 0; // Netty Server 당 Consumer 개수
    private String nettyServerIp; // Netty Server IP List
    private int nettyServerPort; // Netty Server Port List
    private int speakerVolume; // Speaker Volume
    private int mikeVolume; // Mike Volume
    private boolean relay; // 미디어 릴레이 여부

    // RECORD
    private String recordPath = null; // 녹취 파일 저장 경로
    private boolean rawFile = false; // Raw File 녹취 여부
    private boolean encFile = false; // Encoded File 녹취 여부
    private boolean decFile = false; // Decoded File 녹취 여부

    // SDP
    private final SdpParser sdpParser = new SdpParser();
    private String version;
    private String origin;
    private String session;
    private String time;
    private String connection;
    private String media;
    String[] alawAttributeList;
    String[] ulawAttributeList;
    String[] amrAttributeList;
    String[] amrWbAttributeList;
    String[] evsAttributeList;
    String[] dtmf8000AttributeList;
    String[] dtmf16000AttributeList;
    String[] attributeList;

    private final Context context;

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public AuditConfig()
     * @brief AuditConfig 생성자 함수
     */
    public ConfigManager(Context context) {
        this.context = context;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public boolean load(FileInputStream fileInputStream) {
        if (fileInputStream == null) {
            return false;
        }

        try {
            this.ini = new Ini(fileInputStream);

            loadCommonConfig();
            loadSignalConfig();
            loadMediaConfig();
            loadRecordConfig();
            loadSdpConfig();

            Logger.i("Load config [%s]", fileInputStream);
        } catch (Exception e) {
            Logger.e("ConfigManager.IOException", e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * @fn private void loadCommonConfig()
     * @brief COMMON Section 을 로드하는 함수
     */
    private void loadCommonConfig() {
        this.udpRcvBufferSize = Integer.parseInt(getIniValue(SECTION_COMMON, FIELD_UDP_RCV_BUFFER_SIZE));
        this.udpSndBufferSize = Integer.parseInt(getIniValue(SECTION_COMMON, FIELD_UDP_SND_BUFFER_SIZE));

        this.longCallTime = Long.parseLong(getIniValue(SECTION_COMMON, FIELD_LONG_CALL_TIME));
        if (this.longCallTime < 0 || this.longCallTime >= Long.MAX_VALUE) {
            this.longCallTime = 600000; // 10 분
        }

        this.useClient = Boolean.parseBoolean(getIniValue(SECTION_COMMON, FIELD_USE_CLIENT));
        this.useProxy = Boolean.parseBoolean(getIniValue(SECTION_COMMON, FIELD_USE_PROXY));

        this.isProxyMode = Boolean.parseBoolean(getIniValue(SECTION_COMMON, FIELD_PROXY_MODE));

        // Client process > proxy 옵션 false 처리
        if (this.useClient) {
            this.isProxyMode = false;
        }

        // Proxy process > client 옵션 false 처리
        if (this.isProxyMode) {
            this.useClient = false;
            this.useProxy = false;
        }

        this.callAutoAccept = Boolean.parseBoolean(getIniValue(SECTION_COMMON, FIELD_CALL_AUTO_ACCEPT));

        Logger.d("Load [%s] config...(OK)", SECTION_COMMON);
    }

    /**
     * @fn private void loadSignalConfig()
     * @brief SIGNAL Section 을 로드하는 함수
     */
    private void loadSignalConfig() {
        this.hostName = getIniValue(SECTION_SIGNAL, FIELD_HOST_NAME);

        this.fromIp = getIniValue(SECTION_SIGNAL, FIELD_FROM_IP);
        this.fromPort = Integer.parseInt(getIniValue(SECTION_SIGNAL, FIELD_FROM_PORT));
        if (this.fromPort <= 0 || this.fromPort >= 65536) {
            this.fromPort = 5000;
        }

        this.toIp = getIniValue(SECTION_SIGNAL, FIELD_TO_IP);
        this.toPort = Integer.parseInt(getIniValue(SECTION_SIGNAL, FIELD_TO_PORT));
        if (this.toPort <= 0 || this.toPort >= 65536) {
            Logger.d("! Remote sip port is wrong. Program ends. (remoteSipPort=%s)", this.toPort);
            System.exit(1);
        }

        this.defaultRegisterExpires = Integer.parseInt(getIniValue(SECTION_SIGNAL, FIELD_DEFAULT_REGISTER_EXPIRES));
        if (this.defaultRegisterExpires <= 0 || this.defaultRegisterExpires >= Integer.MAX_VALUE) {
            this.defaultRegisterExpires =  600000; // 10분
        }

        this.callRecvDuration = Integer.parseInt(getIniValue(SECTION_SIGNAL, FIELD_CALL_RECV_DURATION));
        if (this.callRecvDuration <= 0 || this.callRecvDuration >= Integer.MAX_VALUE) {
            this.callRecvDuration =  60000; // 1분
        }

        Logger.d("Load [%s] config...(OK)", SECTION_SIGNAL);
    }

    /**
     * @fn private void loadMediaConfig()
     * @brief MEDIA Section 을 로드하는 함수
     */
    private void loadMediaConfig() {
        this.sendWav = Boolean.parseBoolean(getIniValue(SECTION_MEDIA, FIELD_SEND_WAV));
        this.lastWavPath = getIniValue(SECTION_MEDIA, FIELD_LAST_WAV_PATH);

        this.dtmf = Boolean.parseBoolean(getIniValue(SECTION_MEDIA, FIELD_DTMF));

        this.priorityAudioCodec = getIniValue(SECTION_MEDIA, FIELD_PRIORITY_CODEC);
        MediaManager.getInstance().setPriorityCodec(this.priorityAudioCodec);

        this.nettyServerConsumerCount = Integer.parseInt(getIniValue(SECTION_MEDIA, FIELD_NETTY_SERVER_CONSUMER_COUNT));

        this.nettyServerIp = getIniValue(SECTION_MEDIA, FIELD_NETTY_SERVER_IP);
        this.nettyServerPort = Integer.parseInt(getIniValue(SECTION_MEDIA, FIELD_NETTY_SERVER_PORT));
        if (this.nettyServerPort <= 0 || this.nettyServerPort >= 65536) {
            Logger.d("! Media port is wrong. Program ends. (mediaPort=%s)", this.nettyServerPort);
            System.exit(1);
        }

        this.speakerVolume = Integer.parseInt(getIniValue(SECTION_MEDIA, FIELD_SPEAKER_VOLUME));
        if (this.speakerVolume <= 0 || this.speakerVolume > 100) {
            this.speakerVolume = 100;
        }

        this.mikeVolume = Integer.parseInt(getIniValue(SECTION_MEDIA, FIELD_MIKE_VOLUME));
        if (this.mikeVolume <= 0 || this.mikeVolume > 100) {
            this.mikeVolume = 100;
        }

        this.relay = Boolean.parseBoolean(getIniValue(SECTION_MEDIA, FIELD_RELAY));

        Logger.d("Load [%s] config...(OK)", SECTION_MEDIA);
    }

    /**
     * @fn private void loadRecordConfig()
     * @brief RECORD Section 을 로드하는 함수
     */
    private void loadRecordConfig() {
        this.recordPath = getIniValue(SECTION_RECORD, FIELD_RECORD_PATH);

        this.rawFile = Boolean.parseBoolean(getIniValue(SECTION_RECORD, FIELD_RAW_FILE));
        this.encFile = Boolean.parseBoolean(getIniValue(SECTION_RECORD, FIELD_ENC_FILE));
        this.decFile = Boolean.parseBoolean(getIniValue(SECTION_RECORD, FIELD_DEC_FILE));

        Logger.d("Load [%s] config...(OK)", SECTION_RECORD);
    }

    private void loadSdpConfig() {
        version = getIniValue(SECTION_SDP, "VERSION");
        if (version == null) {
            Logger.e("[SECTION_SDP] VERSION IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }
        version = "v=" + version + "\r\n";

        origin = getIniValue(SECTION_SDP, "ORIGIN");
        if (origin == null) {
            Logger.e("[SECTION_SDP] ORIGIN IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        session = getIniValue(SECTION_SDP, "SESSION");
        if (session == null) {
            Logger.e("[SECTION_SDP] SESSION IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }
        session = "s=" + session + "\r\n";

        time = getIniValue(SECTION_SDP, "TIME");
        if (time == null) {
            Logger.e("[SECTION_SDP] TIME IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }
        time = "t=" + time + "\r\n";

        connection = getIniValue(SECTION_SDP, "CONNECTION");
        if (connection == null) {
            Logger.e("[SECTION_SDP] CONNECTION IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        media = getIniValue(SECTION_SDP, "MEDIA");
        if (media == null) {
            Logger.e("[SECTION_SDP] MEDIA IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        int attrCount = Integer.parseInt(getIniValue(SECTION_SDP, "ATTR_COUNT"));
        if (attrCount <= 0) {
            Logger.e("[SECTION_SDP] ATTR_COUNT IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        alawAttributeList = new String[1];
        String attributeAlaw = getIniValue(SECTION_SDP, String.format("ATTR_ALAW_%d", 0));
        if (attributeAlaw == null) {
            Logger.e("[SECTION_SDP] ATTR_ALAW_%s IS NOT DEFINED IN THE LOCAL SDP.", 0);
            System.exit(1);
        }
        alawAttributeList[0] = attributeAlaw;

        ulawAttributeList = new String[1];
        String attributeUlaw = getIniValue(SECTION_SDP, String.format("ATTR_ULAW_%d", 0));
        if (attributeUlaw == null) {
            Logger.e("[SECTION_SDP] ATTR_ULAW_%s IS NOT DEFINED IN THE LOCAL SDP.", 0);
            System.exit(1);
        }
        ulawAttributeList[0] = attributeUlaw;

        amrAttributeList = new String[2];
        for (int i = 0; i < 2; i++) {
            String attribute = getIniValue(SECTION_SDP, String.format("ATTR_AMR_%d", i));
            if (attribute == null) {
                Logger.e("[SECTION_SDP] ATTR_AMR_%s IS NOT DEFINED IN THE LOCAL SDP.", i);
                System.exit(1);
            }
            amrAttributeList[i] = attribute;
        }

        amrWbAttributeList = new String[2];
        for (int i = 0; i < 2; i++) {
            String attribute = getIniValue(SECTION_SDP, String.format("ATTR_AMRWB_%d", i));
            if (attribute == null) {
                Logger.e("[SECTION_SDP] ATTR_AMRWB_%s IS NOT DEFINED IN THE LOCAL SDP.", i);
                System.exit(1);
            }
            amrWbAttributeList[i] = attribute;
        }

        evsAttributeList = new String[2];
        for (int i = 0; i < 2; i++) {
            String attribute = getIniValue(SECTION_SDP, String.format("ATTR_EVS_%d", i));
            if (attribute == null) {
                Logger.e("[SECTION_SDP] ATTR_EVS_%s IS NOT DEFINED IN THE LOCAL SDP.", i);
                System.exit(1);
            }
            evsAttributeList[i] = attribute;
        }

        dtmf8000AttributeList = new String[2];
        for (int i = 0; i < 2; i++) {
            String attribute = getIniValue(SECTION_SDP, String.format("ATTR_DTMF8000_%d", i));
            if (attribute == null) {
                Logger.e("[SECTION_SDP] ATTR_DTMF8000_%s IS NOT DEFINED IN THE LOCAL SDP.", i);
                System.exit(1);
            }
            dtmf8000AttributeList[i] = attribute;
        }

        dtmf16000AttributeList = new String[2];
        for (int i = 0; i < 2; i++) {
            String attribute = getIniValue(SECTION_SDP, String.format("ATTR_DTMF16000_%d", i));
            if (attribute == null) {
                Logger.e("[SECTION_SDP] ATTR_DTMF16000_%s IS NOT DEFINED IN THE LOCAL SDP.", i);
                System.exit(1);
            }
            dtmf16000AttributeList[i] = attribute;
        }

        attributeList = new String[attrCount];
        for (int i = 0; i < attrCount; i++) {
            String attribute = getIniValue(SECTION_SDP, String.format("ATTR_%d", i));
            if (attribute == null) {
                Logger.e("[SECTION_SDP] ATTR_%s IS NOT DEFINED IN THE LOCAL SDP.", i);
                System.exit(1);
            }
            attributeList[i] = attribute;
        }
    }

    public Sdp loadSdpConfig(String callId) {
        try {
            StringBuilder sdpStr = new StringBuilder();

            // 1) Session
            // 1-1) Version
            sdpStr.append(version);

            // 1-2) Origin
            /*
                - Using NTP Timestamp
                [RFC 4566]
                  <sess-id> is a numeric string such that the tuple of <username>,
                  <sess-id>, <nettype>, <addrtype>, and <unicast-address> forms a
                  globally unique identifier for the session.  The method of
                  <sess-id> allocation is up to the creating tool, but it has been
                  suggested that a Network Time Protocol (NTP) format timestamp be
                  used to ensure uniqueness.
             */
            String originSessionId = String.valueOf(TimeStamp.getCurrentTime().getTime());
            String curOrigin = String.format(this.origin, originSessionId, nettyServerIp);
            curOrigin = "o=" + curOrigin + "\r\n";
            sdpStr.append(curOrigin);

            // 1-3) Session
            sdpStr.append(session);

            // 3) Media
            // 3-1) Connection
            String connection = String.format(this.connection, nettyServerIp);
            connection = "c=" + connection + "\r\n";
            sdpStr.append(connection);

            // 2) Time
            // 2-1) Time
            sdpStr.append(time);

            // 3) Media
            // 3-2) Media
            sdpStr.append("m=");
            String media = String.format(this.media, nettyServerPort, MediaManager.getInstance().getPriorityCodecId());
            if (dtmf) {
                media += " " + MediaManager.getInstance().getPriorityDtmfCodecId();
            }
            sdpStr.append(media);
            sdpStr.append("\r\n");

            // 3-3) Attribute
            String[] curCodecAttributeList;
            /*if (priorityAudioCodec.equals(MediaManager.ALAW)) {
                curCodecAttributeList = alawAttributeList;
            } else if (priorityAudioCodec.equals(MediaManager.ULAW)) {
                curCodecAttributeList = ulawAttributeList;
            } else*/
                if (priorityAudioCodec.equals(MediaManager.AMR_NB)) {
                curCodecAttributeList = amrAttributeList;
            } else if (priorityAudioCodec.equals(MediaManager.AMR_WB)) {
                curCodecAttributeList = amrWbAttributeList;
            } else if (priorityAudioCodec.equals(MediaManager.EVS)) {
                curCodecAttributeList = evsAttributeList;
            } else {
                return null;
            }

            for (String attribute : curCodecAttributeList) {
                sdpStr.append("a=");
                sdpStr.append(String.format(attribute, MediaManager.getInstance().getPriorityCodecId()));
                sdpStr.append("\r\n");
            }

            if (dtmf) {
                if (priorityAudioCodec.equals(MediaManager.AMR_WB)) {
                    for (String attribute : dtmf16000AttributeList) {
                        sdpStr.append("a=");
                        sdpStr.append(String.format(attribute, MediaManager.getInstance().getPriorityDtmfCodecId()));
                        sdpStr.append("\r\n");
                    }
                } else {
                    for (String attribute : dtmf8000AttributeList) {
                        sdpStr.append("a=");
                        sdpStr.append(String.format(attribute, MediaManager.getInstance().getPriorityDtmfCodecId()));
                        sdpStr.append("\r\n");
                    }
                }
            }

            for (String attribute : attributeList) {
                sdpStr.append("a=");
                sdpStr.append(attribute);
                sdpStr.append("\r\n");
            }

            Sdp amfSdp = null;

            try {
                amfSdp = sdpParser.parseSdp(callId, sdpStr.toString());
                Logger.d("(%s) Local SDP=\n%s", callId, amfSdp.getData(false));
            } catch (Exception e) {
                Logger.e("(%s) Fail to parse the local sdp. (%s)", callId, sdpStr, e);
                System.exit(1);
            }

            return amfSdp;
        } catch (Exception e) {
            Logger.w("Fail to load the local sdp.", e);
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn private String getIniValue(String section, String key)
     * @brief INI 파일에서 지정한 section 과 key 에 해당하는 value 를 가져오는 함수
     * @param section Section
     * @param key Key
     * @return 성공 시 value, 실패 시 null 반환
     */
    private String getIniValue(String section, String key) {
        String value = ini.get(section,key);
        //String value = ini.getProperty(key);
        if (value == null) {
            Logger.w("[ %s ] \" %s \" is null.", section, key);
            System.exit(1);
            return null;
        }

        value = value.trim();
        Logger.d("\tGet Config [%s] > [%s] : [%s]", section, key, value);
        return value;
    }

    /**
     * @fn public void setIniValue(String section, String key, String value)
     * @brief INI 파일에 새로운 value 를 저장하는 함수
     * @param section Section
     * @param key Key
     * @param value Value
     */
    public void setIniValue(String section, String key, String value) {
        try {
            /*String[] files = context.fileList();
            for (String file : files) {
                Logger.d("files: %s", file);
            }*/

            ini.put(section, key, value);
            ini.store(context.openFileOutput("user_conf.ini", Context.MODE_PRIVATE));

            String newValue = ini.get(section, key);
            //String newValue = (String) ini.get(key);
            Logger.d("\tSet Config [%s] > [%s] : [%s]", section, key, newValue);
        } catch (Exception e) {
            Logger.w("Fail to set the config. (section=%s, field=%s, value=%s) (%s)", section, key, value, e);
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public int getUdpRcvBufferSize() {
        return udpRcvBufferSize;
    }

    public void setUdpRcvBufferSize(int udpRcvBufferSize) {
        this.udpRcvBufferSize = udpRcvBufferSize;
    }

    public int getUdpSndBufferSize() {
        return udpSndBufferSize;
    }

    public void setUdpSndBufferSize(int udpSndBufferSize) {
        this.udpSndBufferSize = udpSndBufferSize;
    }

    public long getLongCallTime() {
        return longCallTime;
    }

    public void setLongCallTime(long longCallTime) {
        this.longCallTime = longCallTime;
    }

    public boolean isUseClient() {
        return useClient;
    }

    public void setUseClient(boolean useClient) {
        this.useClient = useClient;
    }

    public boolean isCallAutoAccept() {
        return callAutoAccept;
    }

    public void setCallAutoAccept(boolean callAutoAccept) {
        this.callAutoAccept = callAutoAccept;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getFromIp() {
        return fromIp;
    }

    public void setFromIp(String fromIp) {
        this.fromIp = fromIp;
    }

    public int getFromPort() {
        return fromPort;
    }

    public void setFromPort(int fromPort) {
        this.fromPort = fromPort;
    }

    public boolean isProxyMode() {
        return isProxyMode;
    }

    public void setProxyMode(boolean proxyMode) {
        isProxyMode = proxyMode;
    }

    public String getToIp() {
        return toIp;
    }

    public void setToIp(String toIp) {
        this.toIp = toIp;
    }

    public int getToPort() {
        return toPort;
    }

    public void setToPort(int toPort) {
        this.toPort = toPort;
    }

    public int getDefaultRegisterExpires() {
        return defaultRegisterExpires;
    }

    public void setDefaultRegisterExpires(int defaultRegisterExpires) {
        this.defaultRegisterExpires = defaultRegisterExpires;
    }

    public int getCallRecvDuration() {
        return callRecvDuration;
    }

    public void setCallRecvDuration(int callRecvDuration) {
        this.callRecvDuration = callRecvDuration;
    }

    public String getPriorityAudioCodec() {
        return priorityAudioCodec;
    }

    public void setPriorityAudioCodec(String priorityAudioCodec) {
        this.priorityAudioCodec = priorityAudioCodec;
    }

    public int getNettyServerConsumerCount() {
        return nettyServerConsumerCount;
    }

    public void setNettyServerConsumerCount(int nettyServerConsumerCount) {
        this.nettyServerConsumerCount = nettyServerConsumerCount;
    }

    public String getNettyServerIp() {
        return nettyServerIp;
    }

    public void setNettyServerIp(String nettyServerIp) {
        this.nettyServerIp = nettyServerIp;
    }

    public int getNettyServerPort() {
        return nettyServerPort;
    }

    public void setNettyServerPort(int nettyServerPort) {
        this.nettyServerPort = nettyServerPort;
    }

    public String getRecordPath() {
        return recordPath;
    }

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    public boolean isRawFile() {
        return rawFile;
    }

    public void setRawFile(boolean rawFile) {
        this.rawFile = rawFile;
    }

    public boolean isEncFile() {
        return encFile;
    }

    public void setEncFile(boolean encFile) {
        this.encFile = encFile;
    }

    public boolean isDecFile() {
        return decFile;
    }

    public void setDecFile(boolean decFile) {
        this.decFile = decFile;
    }

    public int getSpeakerVolume() {
        return speakerVolume;
    }

    public void setSpeakerVolume(int speakerVolume) {
        this.speakerVolume = speakerVolume;
    }

    public int getMikeVolume() {
        return mikeVolume;
    }

    public void setMikeVolume(int mikeVolume) {
        this.mikeVolume = mikeVolume;
    }

    public boolean isRelay() {
        return relay;
    }

    public void setRelay(boolean relay) {
        this.relay = relay;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public boolean isDtmf() {
        return dtmf;
    }

    public void setDtmf(boolean dtmf) {
        this.dtmf = dtmf;
    }

    public boolean isSendWav() {
        return sendWav;
    }

    public void setSendWav(boolean sendWav) {
        this.sendWav = sendWav;
    }

    public String getLastWavPath() {
        return lastWavPath;
    }

    public void setLastWavPath(String lastWavPath) {
        this.lastWavPath = lastWavPath;
    }

    public String getContactPath() {
        return contactPath;
    }

    public void setContactPath(String contactPath) {
        this.contactPath = contactPath;
    }
}
