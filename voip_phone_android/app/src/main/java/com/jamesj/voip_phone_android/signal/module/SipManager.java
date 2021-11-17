package com.jamesj.voip_phone_android.signal.module;

import android.javax.sip.ClientTransaction;
import android.javax.sip.Dialog;
import android.javax.sip.DialogState;
import android.javax.sip.DialogTerminatedEvent;
import android.javax.sip.IOExceptionEvent;
import android.javax.sip.ListeningPoint;
import android.javax.sip.PeerUnavailableException;
import android.javax.sip.RequestEvent;
import android.javax.sip.ResponseEvent;
import android.javax.sip.ServerTransaction;
import android.javax.sip.SipFactory;
import android.javax.sip.SipListener;
import android.javax.sip.SipProvider;
import android.javax.sip.SipStack;
import android.javax.sip.TimeoutEvent;
import android.javax.sip.Transaction;
import android.javax.sip.TransactionState;
import android.javax.sip.TransactionTerminatedEvent;
import android.javax.sip.address.Address;
import android.javax.sip.address.AddressFactory;
import android.javax.sip.address.SipURI;
import android.javax.sip.address.URI;
import android.javax.sip.header.AllowHeader;
import android.javax.sip.header.AuthorizationHeader;
import android.javax.sip.header.CSeqHeader;
import android.javax.sip.header.CallIdHeader;
import android.javax.sip.header.ContactHeader;
import android.javax.sip.header.ContentTypeHeader;
import android.javax.sip.header.ExpiresHeader;
import android.javax.sip.header.FromHeader;
import android.javax.sip.header.Header;
import android.javax.sip.header.HeaderFactory;
import android.javax.sip.header.MaxForwardsHeader;
import android.javax.sip.header.ReasonHeader;
import android.javax.sip.header.SupportedHeader;
import android.javax.sip.header.ToHeader;
import android.javax.sip.header.UserAgentHeader;
import android.javax.sip.header.ViaHeader;
import android.javax.sip.header.WWWAuthenticateHeader;
import android.javax.sip.message.MessageFactory;
import android.javax.sip.message.Request;
import android.javax.sip.message.Response;

import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.media.MediaManager;
import com.jamesj.voip_phone_android.media.mix.AudioMixManager;
import com.jamesj.voip_phone_android.media.module.SoundHandler;
import com.jamesj.voip_phone_android.media.module.TaskManager;
import com.jamesj.voip_phone_android.media.netty.NettyChannelManager;
import com.jamesj.voip_phone_android.media.netty.module.NettyChannel;
import com.jamesj.voip_phone_android.media.sdp.SdpParser;
import com.jamesj.voip_phone_android.media.sdp.base.Sdp;
import com.jamesj.voip_phone_android.media.sdp.base.attribute.RtpAttribute;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.jamesj.voip_phone_android.signal.base.CallInfo;
import com.jamesj.voip_phone_android.signal.base.RegiInfo;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class SipManager implements SipListener {

    ////////////////////////////////////////////////////////////////////////////////
    // COMMON
    private final Random random = new Random();

    ////////////////////////////////////////////////////////////////////////////////
    // SIP Stack Variables
    private static final String AUTHENTICATION_ALGORITHM = "MD5";
    private static final String SIP_TRANSPORT_TYPE = "udp";

    private AddressFactory addressFactory = null;
    private HeaderFactory headerFactory = null;
    private MessageFactory messageFactory = null;
    private SipStack sipStack = null;
    private SipProvider sipProvider = null;

    private Address hostAddress = null;
    private Address contactAddress = null;
    private ContactHeader contactHeader = null;
    private String hostIp;
    private int hostPort;
    private String hostName;
    private int defaultRegisterExpires;

    ////////////////////////////////////////////////////////////////////////////////
    // SIP Object Variables
    private String toIp;
    private int toPort;

    ////////////////////////////////////////////////////////////////////////////////
    // SIP Register Variables
    private final String md5PassWd = "1234";
    private String userNonce = null;
    private String sessionId;

    ////////////////////////////////////////////////////////////////////////////////

    public SipManager() {
        Logger.addLogAdapter(new AndroidLogAdapter());
    }

    ////////////////////////////////////////////////////////////////////////////////
    public void start () {
        try {
            sipStack.start();
        } catch (Exception e) {
            Logger.w("Fail to start the sip stack.");
        }
    }

    public void stop () {
        try {
            //CallManager.getInstance().clearCallInfoMap();
            sipStack.stop();
        } catch (Exception e) {
            Logger.w("Fail to stop the sip stack.");
        }
    }

    public boolean init() {
        SipFactory sipFactory = SipFactory.getInstance();

        try {
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            hostName = configManager.getHostName();
            hostIp = configManager.getFromIp();
            hostPort = configManager.getFromPort();

            defaultRegisterExpires = configManager.getDefaultRegisterExpires();
            toIp = configManager.getToIp();
            toPort = configManager.getToPort();

            //Date curTime = new Date();
            //SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHH");
            //String curTimeStr = timeFormat.format(curTime);

            Properties properties = new Properties();
            properties.setProperty("android.javax.sip.STACK_NAME", hostName);
            properties.setProperty("android.gov.nist.javax.sip.TRACE_LEVEL", "LOG4J");
            properties.setProperty("tavax.sip.LOG4J_LOGGER_NAME", "SIPStackLogger");

            //properties.setProperty("android.gov.nist.javax.sip.DEBUG_LOG", hostName + "_debug.log");
            //properties.setProperty("android.gov.nist.javax.sip.SERVER_LOG", hostName + "_server.log");
            //properties.setProperty("tavax.sip.TRACE_LEVEL", "LOG4J");

            if (sipStack != null) {
                sipStack.stop();
            }

            sipFactory.setPathName("android.gov.nist");
            sipStack = sipFactory.createSipStack(properties);
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
            Logger.e("%s PeerUnavailableException %s (%s) (%s)", SipLogFormatter.getCallLogHeader(null, null, null, null), e, e.getCause(), e.getStackTrace());
            return false;
        }

        try {
            if (addressFactory == null) { addressFactory = sipFactory.createAddressFactory(); }
            if (headerFactory == null) { headerFactory = sipFactory.createHeaderFactory(); }
            if (messageFactory == null) { messageFactory = sipFactory.createMessageFactory(); }

            ListeningPoint listeningPoint = sipStack.createListeningPoint(hostIp, hostPort, "udp");
            SipManager listener = this;

            sipProvider = sipStack.createSipProvider(listeningPoint);
            sipProvider.addSipListener(listener);
            sipProvider.setAutomaticDialogSupportEnabled(false);

            SipURI sipUri = addressFactory.createSipURI(hostName, hostIp);
            sipUri.setHost(hostIp);
            sipUri.setPort(hostPort);
            sipUri.setLrParam();
            hostAddress = addressFactory.createAddress(hostName, sipUri);

            hostIp = listeningPoint.getIPAddress();
            hostPort = listeningPoint.getPort();
            contactAddress = addressFactory.createAddress("<sip:" + hostName + "@" + hostIp + ":" + hostPort + ">");
            contactHeader = headerFactory.createContactHeader(contactAddress);

            Logger.d("sipStack: [%s] [%s] [%s] [%s]",
                    sipStack.getStackName(),
                    listeningPoint.getIPAddress(),
                    listeningPoint.getPort(),
                    listeningPoint.getTransport()
            );
        } catch (Exception e) {
            Logger.e("SignalManager.Exception [%s]", e);
            return false;
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public String parseSipIp(Header header)
     * @brief From 또는 To header 에서 SIP IP 를 문자열로 파싱하는 함수
     * @param header From or To header
     * @return 성공 시 SIP IP String, 실패 시 null 반환
     */
    public String parseSipIp(Header header) {
        if (header == null) {
            return null;
        }

        URI uri;
        if (header.getName().equals(FromHeader.NAME)) {
            FromHeader fromHeader = (FromHeader) header;
            uri = fromHeader.getAddress().getURI();
        } else if (header.getName().equals(ToHeader.NAME)) {
            ToHeader toHeader = (ToHeader) header;
            uri = toHeader.getAddress().getURI();
        } else {
            return null;
        }

        if (uri == null) {
            return null;
        }

        String uriScheme = uri.toString();
        String address = uriScheme.substring(uriScheme.indexOf("@") + 1);
        return address.substring(0, address.indexOf(":"));
    }

    /**
     * @fn public int parseSipPort(Header header)
     * @brief From 또는 To header 에서 SIP Port 를 정수로 파싱하는 함수
     * @param header From or To header
     * @return 성공 시 SIP Port Integer, 실패 시 null 반환
     */
    public int parseSipPort(Header header) {
        if (header == null) {
            return -1;
        }

        URI uri;
        if (header.getName().equals(FromHeader.NAME)) {
            FromHeader fromHeader = (FromHeader) header;
            uri = fromHeader.getAddress().getURI();
        } else if (header.getName().equals(ToHeader.NAME)) {
            ToHeader toHeader = (ToHeader) header;
            uri = toHeader.getAddress().getURI();
        } else {
            return -1;
        }

        if (uri == null) {
            return -1;
        }

        String uriScheme = uri.toString();
        String address = uriScheme.substring(uriScheme.indexOf("@") + 1);
        return Integer.parseInt(address.substring(address.indexOf(":") + 1, address.indexOf(";")));
    }

    //////////////////////////////////////////////////////////////////////

    @Override
    public void processRequest(RequestEvent requestEvent) {
        if (sipProvider == null) { return; }

        Request request = requestEvent.getRequest();
        if (request == null) {
            return;
        }

        try {
            if (Request.REGISTER.equals(request.getMethod())) {
                processRegister(requestEvent);
            } else if (Request.INVITE.equals(request.getMethod())) {
                processInvite(requestEvent);
            } else if (Request.BYE.equals(request.getMethod())) {
                processBye(requestEvent);
            } else if (Request.CANCEL.equals(request.getMethod())) {
                processCancel(requestEvent);
            } else if (Request.MESSAGE.equals(request.getMethod())) {
                processMessage(requestEvent);
            } else if (Request.ACK.equals(request.getMethod())) {
                processAck(requestEvent);
            } else {
                Logger.w("Undefined Request is detected.");
            }
        } catch (Exception e) {
            Logger.w("Fail to process the request.");
        }
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        if (response == null) {
            return;
        }

        CSeqHeader cSeqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        String requestMethodName = cSeqHeader.getMethod();

        Logger.d("Recv Response: %s", response);

        CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        if (callIdHeader == null) {
            Logger.w("Fail to find the from header in the response. (%s)", response);
            return;
        }

        String callId = callIdHeader.getCallId();

        CallInfo callInfo = null;
        if (!requestMethodName.equals(Request.REGISTER)) {
            callInfo = CallManager.getInstance().getCallInfo(callId);
            if (callInfo == null) {
                return;
            }
        }

        switch (response.getStatusCode()) {
            case Response.TRYING:
            case Response.RINGING:
                break;
            case Response.OK:
                processOk(responseEvent);
                break;
            case Response.UNAUTHORIZED:
                Logger.d("Recv 401 Unauthorized. Authentication will be processed.");
                if (requestMethodName.equals(Request.REGISTER)) {
                    sendRegister(true, response);
                } else {
                    sendRegister(true, null);
                    callInfo.setIsInviteUnauthorized(true);
                }
                break;
            case Response.FORBIDDEN:
                Logger.d("Recv 403 Forbidden.");
                if (requestMethodName.equals(Request.INVITE)) {
                    Logger.d("Call is not started.");
                    AppInstance.getInstance().getMasterFragmentActivity().getPhoneFragment().processBye();

                    TaskManager.getInstance().removeTask(CallCancelHandler.class.getSimpleName() + callId);
                    callInfo.setIsCallStarted(false);

                    CallManager.getInstance().deleteCallInfo(callId);
                } else {
                    Logger.d("Fail to register.");
                }
                break;
            case Response.REQUEST_TERMINATED:
                Logger.d("Recv 487 Request Terminated. Call is not started.");
                if (requestMethodName.equals(Request.INVITE)) {
                    AppInstance.getInstance().getMasterFragmentActivity().getPhoneFragment().processBye();

                    TaskManager.getInstance().removeTask(CallCancelHandler.class.getSimpleName() + callId);
                    callInfo.setIsCallStarted(false);
                    sendAck(responseEvent);

                    CallManager.getInstance().deleteCallInfo(callId);
                }
                break;
            default:
                Logger.w("Undefined response is detected. (responseCode=%s)", response.getStatusCode());
                break;
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction()) {
            transaction = timeoutEvent.getServerTransaction();
        } else {
            transaction = timeoutEvent.getClientTransaction();
        }

        if (transaction != null) {
            Logger.w("Transaction is timeout. (transactionState=%s, dialog=%s)", transaction.getState(), transaction.getDialog());
        }
    }

    @Override
    public void processIOException(IOExceptionEvent ioExceptionEvent) {
        Logger.w("SignalManager.IOExceptionEvent (host=%s)", ioExceptionEvent.getHost());
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        if (transactionTerminatedEvent.isServerTransaction()) {
            ServerTransaction serverTransaction = transactionTerminatedEvent.getServerTransaction();
            if (serverTransaction != null && serverTransaction.getState() == TransactionState.TERMINATED) {
                Logger.d("ServerTransaction is terminated. (branchId=%s, state=%s)", serverTransaction.getBranchId(), serverTransaction.getState());
            }
        } else {
            ClientTransaction clientTransaction = transactionTerminatedEvent.getClientTransaction();
            if (clientTransaction != null && clientTransaction.getState() == TransactionState.TERMINATED) {
                Logger.d("ClientTransaction is terminated. (branchId=%s, state=%s)", clientTransaction.getBranchId(), clientTransaction.getState());
            }
        }
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        Dialog dialog = dialogTerminatedEvent.getDialog();
        if (dialog != null && dialog.getState() == DialogState.TERMINATED) {
            Logger.d("Dialog is terminated. (dialogId=%s, state=%s, callId=%s)", dialog.getDialogId(), dialog.getState(), dialog.getCallId());
        }
    }


    //////////////////////////////////////////////////////////////////////
    // REGISTER

    /**
     * @fn public void sendRegister ()
     * @brief REGISTER Method 를 보내는 함수
     * 클라이언트만 사용하는 함수
     */
    public void sendRegister (boolean isRecv401, Response response401) {
        try {
            //String proxyHostName = VoipClient.getInstance().getProxyHostName();
            String proxyHostName = "proxy";

            // Create SIP URI
            SipURI sipUri = addressFactory.createSipURI(proxyHostName, toIp);
            sipUri.setHost(toIp);
            sipUri.setPort(toPort);
            sipUri.setLrParam();

            // Add Route Header
            Address addressTo = addressFactory.createAddress(proxyHostName, sipUri);
            // Create the request URI for the SIP message
            URI requestURI = addressTo.getURI();

            // Create the SIP message headers
            // Via
            List<ViaHeader> viaHeaders = new ArrayList<>();
            ViaHeader viaHeader = this.headerFactory.createViaHeader(hostIp, hostPort, SIP_TRANSPORT_TYPE, null);
            viaHeaders.add(viaHeader);

            // Max-Forwards
            MaxForwardsHeader maxForwardsHeader = this.headerFactory.createMaxForwardsHeader(70);

            // Call -Id
            CallIdHeader callIdHeader = this.sipProvider.getNewCallId();

            // CSeq
            CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1L, Request.REGISTER);

            // From
            FromHeader fromHeader = this.headerFactory.createFromHeader(hostAddress, String.valueOf(random.nextInt(10000)));

            // To
            ToHeader toHeader = this.headerFactory.createToHeader(contactAddress, null);

            // Allow
            String allowList = Request.INVITE + ","
                    + Request.ACK + ","
                    + Request.CANCEL + ","
                    + Request.BYE + ","
                    + Request.MESSAGE;
            AllowHeader allowHeader = this.headerFactory.createAllowHeader(allowList);

            // Supported
            String supportedList = "path,gruu,outbound";
            SupportedHeader supportedHeader = this.headerFactory.createSupportedHeader(supportedList);

            // Expires
            ExpiresHeader expiresHeader = this.headerFactory.createExpiresHeader(defaultRegisterExpires);

            // Create the REGISTER request
            Request request = this.messageFactory.createRequest (
                    requestURI,
                    Request.REGISTER,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwardsHeader
            );

            request.addHeader(allowHeader);
            request.addHeader(supportedHeader);
            request.addHeader(expiresHeader);
            request.addHeader(contactHeader);

            // 401 응답 수신하면 인증 과정 수행
            if (isRecv401 && response401 != null) {
                AuthorizationHeader authorizationHeader = this.headerFactory.createAuthorizationHeader(hostName);

                WWWAuthenticateHeader wwwAuthenticateHeader = (WWWAuthenticateHeader) response401.getHeader(WWWAuthenticateHeader.NAME);

                String userName = fromHeader.getAddress().getDisplayName();
                authorizationHeader.setUsername(userName);
                authorizationHeader.setRealm(wwwAuthenticateHeader.getRealm());
                authorizationHeader.setNonce(wwwAuthenticateHeader.getNonce());
                authorizationHeader.setURI(requestURI);
                authorizationHeader.setAlgorithm(wwwAuthenticateHeader.getAlgorithm());

                // MD5 Hashing
                MessageDigest messageDigest = MessageDigest.getInstance(AUTHENTICATION_ALGORITHM);

                messageDigest.update(userName.getBytes(StandardCharsets.UTF_8));
                messageDigest.update(wwwAuthenticateHeader.getRealm().getBytes(StandardCharsets.UTF_8));
                messageDigest.update(md5PassWd.getBytes(StandardCharsets.UTF_8));
                byte[] a1 = messageDigest.digest();
                messageDigest.reset();

                String uri = requestURI.getScheme();
                messageDigest.update(Request.REGISTER.getBytes(StandardCharsets.UTF_8));
                messageDigest.update(uri.getBytes(StandardCharsets.UTF_8));
                byte[] a2 = messageDigest.digest();
                messageDigest.reset();

                messageDigest.update(a1);
                messageDigest.update(a2);
                userNonce = new String(messageDigest.digest());

                authorizationHeader.setResponse(userNonce);
                request.addHeader(authorizationHeader);
            }

            ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(request);
            new Thread(() -> {
                try {
                    clientTransaction.sendRequest();
                    Logger.d("REGISTER Request sent. (request=%s)", request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        catch ( Exception e ) {
            Logger.w("REGISTER Request sent failed.", e);
        }
    }

    /**
     * @fn public void processRegister(RequestEvent requestEvent)
     * @brief REGISTER 요청을 처리하는 함수
     * 프록시만 사용하는 함수
     * @param requestEvent 요청 이벤트
     */
    public void processRegister(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        if (request != null) {
            Logger.d("Recv REGISTER: %s", request);
        } else {
            return;
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        boolean isUseClient = configManager.isUseClient();
        if (isUseClient) {
            Logger.d("This program is client. Fail to process the REGISTER request.");
            return;
        }

        try {
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
            if (callIdHeader == null) { return; }

            FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
            String fromNo = fromHeader.getAddress().getDisplayName();

            RegiInfo regiInfo = RegiManager.getInstance().getRegi(fromNo);

            ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
            int expires = expiresHeader.getExpires();

            MessageDigest messageDigest = MessageDigest.getInstance(AUTHENTICATION_ALGORITHM);
            Response response;

            if (regiInfo != null) {
                AuthorizationHeader authorizationHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
                if (authorizationHeader == null) {
                    response = messageFactory.createResponse(200, request);
                } else {
                    String responseNonce = authorizationHeader.getResponse();
                    if (responseNonce.equals(userNonce)) {
                        response = messageFactory.createResponse(200, request);
                    } else {
                        // Nonce 가 일치하지 않으면 인증 실패 > 403 Forbidden 으로 응답
                        response = messageFactory.createResponse(403, request);
                        sipProvider.sendResponse(response);
                        Logger.w("Fail to authenticate. Send 403 Forbidden for the register. (recvNonce=%s, userNonce=%s)", responseNonce, userNonce);
                        return;
                    }
                }

                RegiManager.getInstance().scheduleRegi(fromNo, expires);

                Logger.d("Send 200 OK for REGISTER (FromNo=%s): %s", fromNo, response);
                AppInstance.getInstance().getMasterFragmentActivity().getPhoneFragment().processBye();
                Logger.d("Success to register. (fromNo=%s)", fromNo);
            } else {
                String fromSipIp = parseSipIp(fromHeader);
                int fromSipPort = parseSipPort(fromHeader);

                RegiManager.getInstance().addRegi(fromNo, fromSipIp, fromSipPort, expires);

                response = messageFactory.createResponse(401, request);

                WWWAuthenticateHeader wwwAuthenticateHeader = this.headerFactory.createWWWAuthenticateHeader(hostName);
                wwwAuthenticateHeader.setAlgorithm(AUTHENTICATION_ALGORITHM);
                wwwAuthenticateHeader.setRealm(hostName);
                wwwAuthenticateHeader.setNonce(NonceGenerator.createRandomNonce());
                response.addHeader(wwwAuthenticateHeader);

                // MD5 Hashing
                messageDigest.update(fromNo.getBytes(StandardCharsets.UTF_8));
                messageDigest.update(hostName.getBytes(StandardCharsets.UTF_8));
                messageDigest.update(md5PassWd.getBytes(StandardCharsets.UTF_8));
                byte[] a1 = messageDigest.digest();
                messageDigest.reset();

                String requestUriScheme = request.getRequestURI().getScheme();
                messageDigest.update(Request.REGISTER.getBytes(StandardCharsets.UTF_8));
                messageDigest.update(requestUriScheme.getBytes(StandardCharsets.UTF_8));
                byte[] a2 = messageDigest.digest();
                messageDigest.reset();

                messageDigest.update(a1);
                messageDigest.update(a2);
                userNonce = new String(messageDigest.digest());
                Logger.d("Send 401 UNAUTHORIZED for REGISTER (fromNo=%s): %s", fromNo, response);
            }

            sipProvider.sendResponse(response);
        } catch (Exception e) {
            Logger.w("Fail to send the response for the REGISTER request.", e);
        }
    }

    /////////////////////////////////////////////////////////////////////
    // INVITE

    /**
     * @fn public String sendInvite (String sessionId, String fromHostName, String toHostName, String toIp, int toPort)
     * @brief INVITE 요청을 보내는 함수
     * @param sessionId Session Id (일회성)
     * @param fromHostName From Host name
     * @param toHostName To Host name
     * @param toIp Host sip ip
     * @param toPort Host sip port
     * @return Call-ID
     */
    public String sendInvite (String sessionId, String fromHostName, String toHostName, String toIp, int toPort) {
        if (sessionId == null || toHostName == null || toIp == null || toPort <= 0) {
            Logger.w("Fail to send the invite request. (sessionId=%s, toHostName=%s, toIP=%s, toPort=%s)", sessionId, toHostName, toIp, toPort);
            return null;
        }

        try {
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();

            // Create SIP URI
            SipURI toSipUri = addressFactory.createSipURI(toHostName, toIp);
            toSipUri.setHost(toIp);
            toSipUri.setPort(toPort);
            toSipUri.setLrParam();

            // Add Route Header
            Address toAddress = addressFactory.createAddress(toHostName, toSipUri);
            // Create the request URI for the SIP message
            URI requestURI = toAddress.getURI();

            // Create the SIP message headers
            // The " Via" headers
            List<ViaHeader> viaHeaders = new ArrayList<>();
            ViaHeader viaHeader = this.headerFactory.createViaHeader (hostIp , hostPort, SIP_TRANSPORT_TYPE, null);
            viaHeaders.add(viaHeader);

            // The "Max - Forwards " header
            MaxForwardsHeader maxForwardsHeader = this.headerFactory.createMaxForwardsHeader(70);

            // The "Call -Id" header
            CallIdHeader callIdHeader = this.sipProvider.getNewCallId();
            String callId = callIdHeader.getCallId();

            // The " CSeq " header
            CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1L,Request.INVITE);

            // The " From " header
            Address fromAddress = hostAddress;
            if (configManager.isProxyMode()) {
                SipURI sipUri = addressFactory.createSipURI(fromHostName, hostIp);
                sipUri.setHost(hostIp);
                sipUri.setPort(hostPort);
                sipUri.setLrParam();
                fromAddress = addressFactory.createAddress(fromHostName, sipUri);
            }
            FromHeader fromHeader = this.headerFactory.createFromHeader(fromAddress, String.valueOf(random.nextInt(10000)));

            // The "To" header
            ToHeader toHeader = this.headerFactory.createToHeader(toAddress, null);

            CallInfo callInfo = CallManager.getInstance().addCallInfo(
                    sessionId,
                    callIdHeader.getCallId(),
                    fromHeader.getAddress().getDisplayName(),
                    hostIp,
                    hostPort,
                    toHeader.getAddress().getDisplayName(),
                    toIp,
                    toPort
            );

            if (configManager.isProxyMode()) {
                AudioMixManager.getInstance().addAudioMixer(
                        callInfo.getSessionId(),
                        callId,
                        configManager.getRecordPath() + File.separator + "V_" + callInfo.getSessionId() + "_mix.wav",
                        //Integer.parseInt(MediaManager.getInstance().getPriorityCodecSamplingRate()),
                        MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_WB)? 16000 : 8000,
                        16,
                        (short) 1
                );
            }

            // Create the REGISTER request
            Request request = this.messageFactory.createRequest (
                    requestURI,
                    Request.INVITE,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwardsHeader
            );

            request.addHeader(contactHeader);

            ArrayList<String> userAgentList = new ArrayList<>();
            userAgentList.add(getRandomStr(6));
            UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgentList);
            request.addHeader(userAgentHeader);

            // SDP
            int listenPort;
            NettyChannel nettyChannel;
            if (configManager.isUseClient()) {
                NettyChannelManager.getInstance().start();
                nettyChannel = NettyChannelManager.getInstance().getClientChannel();
                listenPort = nettyChannel.getListenPort();
            } else {
                if (!configManager.isRelay()) {
                    if (NettyChannelManager.getInstance().addProxyChannel(callId)) {
                        nettyChannel = NettyChannelManager.getInstance().getProxyChannel(callId);
                        listenPort = nettyChannel.getListenPort();
                    } else {
                        Logger.w("Fail to send invite. (callId=%s)", callId);
                        return null;
                    }
                } else {
                    listenPort = configManager.getNettyServerPort();
                }
            }

            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
            Sdp localSdp = configManager.loadSdpConfig("LOCAL");
            if (localSdp == null) {
                return null;
            }

            localSdp.setMediaPort(Sdp.AUDIO, listenPort);
            byte[] contents = localSdp.getData(false).getBytes();
            request.setContent(contents, contentTypeHeader);

            // Create new client transaction & send the invite request
            ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(request);
            new Thread(() -> {
                try {
                    clientTransaction.sendRequest();
                    Logger.d("INVITE Request sent. (request=%s)", request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Set dialog option
            Dialog dialog = clientTransaction.getDialog();
            if (dialog == null) {
                dialog = sipProvider.getNewDialog(clientTransaction);
            }
            dialog.terminateOnBye(true);

            // Set Call flags
            callInfo.setCallIdHeader(callIdHeader);
            callInfo.setIsCallCanceled(false);
            callInfo.setIsCallStarted(true);

            // Schedule call cancel handler
            String callCancelHandlerId = CallCancelHandler.class.getSimpleName() + callId;
            TaskManager.getInstance().addTask(
                    callCancelHandlerId,
                    new CallCancelHandler(
                            this,
                            callId,
                            toHostName,
                            toIp,
                            toPort,
                            configManager.getCallRecvDuration()
                    )
            );
            callInfo.setCallCancelHandlerId(callCancelHandlerId);

            return callId;
        } catch ( Exception e ) {
            Logger.w("INVITE Request sent failed.", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @fn public void processInvite(RequestEvent requestEvent)
     * @brief INVITE 요청을 수신하여 처리하는 함수
     * @param requestEvent 요청 이벤트
     */
    public void processInvite(RequestEvent requestEvent) {
        try {
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            boolean isUseClient = configManager.isUseClient();
            boolean isProxyMode = configManager.isProxyMode();

            // Get request
            Request request = requestEvent.getRequest();
            if (request == null) { return; }

            // Call-Id
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
            if (callIdHeader == null) { return; }

            // Via
            ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);

            // From
            FromHeader inviteFromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
            String fromNo = inviteFromHeader.getAddress().getDisplayName();

            // To
            ToHeader inviteToHeader = (ToHeader) request.getHeader(ToHeader.NAME);
            String toNo = inviteToHeader.getAddress().getDisplayName();

            String callId = callIdHeader.getCallId();
            Logger.d("Recv INVITE: %s (callId=%s)", request, callId);

            // 프록시 입장에서 Ingoing invite request 먼저 수신
            // Register 등록 여부 검사 (Server 인 경우만 실행)
            if (isProxyMode) {
                RegiInfo regiInfo = RegiManager.getInstance().getRegi(fromNo);

                // 미등록 > 401 Unauthorized 로 호 거절
                if (regiInfo == null) {
                    Response response = messageFactory.createResponse(401, request);
                    sipProvider.sendResponse(response);

                    Logger.w("Unauthorized user is detected. Fail to process the invite request. (callId=%s)", callId);
                    return;
                }

                // Remote host 등록 여부 검사
                if (!toNo.equals(configManager.getHostName())) {
                    if (toNo.equals(fromNo)
                            || RegiManager.getInstance().getRegi(toNo) == null) {
                        Response response = messageFactory.createResponse(403, request);
                        sipProvider.sendResponse(response);

                        Logger.w("403 Forbidden. Fail to process the invite request. (toNo=%s, callId=%s)", toNo, callId);
                        return;
                    }
                }
            }
            //

            if (CallManager.getInstance().getCallMapSize() == 0) {
                sessionId = NonceGenerator.createRandomNonce();
            }

            String sipIp = parseSipIp(inviteFromHeader);
            int sipPort = parseSipPort(inviteFromHeader);

            CallInfo callInfo = CallManager.getInstance().addCallInfo(
                    sessionId,
                    callId,
                    fromNo,
                    hostIp,
                    hostPort,
                    toNo,
                    sipIp,
                    sipPort
            );

            if (isProxyMode) {
                AudioMixManager.getInstance().addAudioMixer(
                        callInfo.getSessionId(),
                        callId,
                        configManager.getRecordPath() + File.separator + "V_" + callInfo.getSessionId() + "_mix.wav",
                        //Integer.parseInt(MediaManager.getInstance().getPriorityCodecSamplingRate()),
                        MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_WB)? 16000 : 8000,
                        16,
                        (short) 1
                );
            }

            callInfo.setFirstViaHeader(
                    headerFactory.createViaHeader(
                            viaHeader.getHost(),
                            viaHeader.getPort(),
                            viaHeader.getTransport(),
                            viaHeader.getBranch()
                    )
            );
            callInfo.setCallIdHeader(callIdHeader);
            callInfo.setInviteRequest(request);

            // Get Server transaction
            ServerTransaction serverTransaction = requestEvent.getServerTransaction();
            if (serverTransaction == null) {
                serverTransaction = sipProvider.getNewServerTransaction(request);
            }
            callInfo.setInviteServerTransaction(serverTransaction);

            // Dialog setting > terminate on bye
            Dialog dialog = serverTransaction.getDialog();
            if (dialog == null) {
                dialog = sipProvider.getNewDialog(serverTransaction);
            }
            dialog.terminateOnBye(true);

            // Get sdp
            byte[] rawSdpData = request.getRawContent();
            if (rawSdpData != null) {
                SdpParser sdpParser = new SdpParser();
                Sdp sdp = sdpParser.parseSdp(callId, new String(rawSdpData));
                CallManager.getInstance().addSdpIntoCallInfo(callId, sdp);
            }

            // 1) Send 100 Trying
            Response tryingResponse = messageFactory.createResponse(Response.TRYING, request);
            serverTransaction.sendResponse(tryingResponse);
            Logger.d("Send 100 Trying for INVITE: %s", tryingResponse);

            // 2) Send 180 Ringing
            Response ringingResponse = messageFactory.createResponse(Response.RINGING, request);
            ringingResponse.addHeader(contactHeader);

            ToHeader ringingToHeader = (ToHeader) ringingResponse.getHeader(ToHeader.NAME);
            String toTag = Integer.toString(random.nextInt(10000));
            ringingToHeader.setTag(toTag); // Application is supposed to set.

            serverTransaction.sendResponse(ringingResponse);
            Logger.d("Send 180 Ringing for INVITE: %s", ringingResponse);

            // Proxy mode 이고, Proxy 로 직접 호 연결 시도가 감지되면, Group Call 을 시작한다. > Session Room 생성
            if (isProxyMode) {
                if (toNo.equals(configManager.getHostName())) {
                    Logger.d("Group Call(%s) has started by [%s]", callInfo.getSessionId(), fromNo);

                    // 1) Group Call
                    // Proxy 에서 Ingoing invite 에 대한 200 OK 바로 전송
                    if (sendInviteOk(callId)) {
                        GroupCallManager.getInstance().addRoomInfo(
                                callInfo.getSessionId(),
                                callId
                        );
                    }
                } else {
                    Logger.d("Relay Call(%s) has started by [%s]", callInfo.getSessionId(), fromNo);

                    // 2) Relay Call
                    // 프록시 입장에서 Ingoing invite 에 대한 100, 180 응답 전송 후, Outgoing invite 전송
                    // Send the outgoing invite request to remote host
                    RegiInfo remoteRegiInfo = RegiManager.getInstance().getRegi(toNo);
                    if (remoteRegiInfo == null) {
                        // Remote Call Info 가 없으면 ingoing peer 로 cancel 전송
                        Logger.w("Fail to find the remote peer info. Send the cancel request to ingoing peer. (callId=%s)", callId);
                        sendCancel(
                                callId,
                                fromNo,
                                sipIp,
                                sipPort
                        );
                        return;
                    }

                    sendInvite(
                            callInfo.getSessionId(),
                            fromNo,
                            remoteRegiInfo.getFromNo(),
                            remoteRegiInfo.getIp(),
                            remoteRegiInfo.getPort()
                    );
                }
            }

            if (isUseClient) {
                callInfo.setIsCallRecv(true);
                AppInstance.getInstance().getMasterFragmentActivity().getPhoneFragment().processInvite(callId, fromNo);
            }
        } catch (Exception e) {
            Logger.w("Fail to process INVITE.", e);
            e.printStackTrace();
        }
    }

    /**
     * @fn public boolean sendInviteOk()
     * @brief Call 유입 시 승인하기 위해 200 OK 를 보내는 함수
     * @return 성공 시 true, 실패 시 false 반환
     */
    public boolean sendInviteOk(String callId) {
        try {
            CallInfo callInfo = CallManager.getInstance().getCallInfo(callId);
            if (callInfo == null) {
                Logger.w("(%s) Fail to send the invite 200 OK. Not found the callInfo.", callId);
                return false;
            }

            if (callInfo.getInviteRequest() == null || callInfo.getInviteServerTransaction() == null) {
                Logger.w("(%s) Fail to send 200 OK for INVITE. Not found the invite server transaction.", callId);
                return false;
            }

            if (callInfo.getIsCallCanceled()) {
                Logger.w("(%s) Call is canceled. Fail to send the invite 200 ok.", callId);
                return false;
            }

            int listenPort;
            NettyChannel nettyChannel;
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            if (configManager.isProxyMode()) {
                if (!configManager.isRelay()) {
                    if (NettyChannelManager.getInstance().addProxyChannel(callId)) {
                        nettyChannel = NettyChannelManager.getInstance().getProxyChannel(callId);
                        listenPort = nettyChannel.getListenPort();
                    } else {
                        Logger.w("(%s) Fail to send invite 200 ok.", callId);
                        sendCancel(
                                callId,
                                callInfo.getFromNo(),
                                callInfo.getFromSipIp(),
                                callInfo.getFromSipPort()
                        );
                        return false;
                    }
                } else {
                    listenPort = configManager.getNettyServerPort();
                }
            } else {
                NettyChannelManager.getInstance().start();
                nettyChannel = NettyChannelManager.getInstance().getClientChannel();
                listenPort = nettyChannel.getListenPort();
            }

            // Send 200 OK
            Response okResponse = messageFactory.createResponse(Response.OK, callInfo.getInviteRequest());
            okResponse.addHeader(contactHeader);

            byte[] rawSdpData = callInfo.getInviteRequest().getRawContent();
            if (rawSdpData != null) {
                SdpParser sdpParser = new SdpParser();
                Sdp remoteSdp = sdpParser.parseSdp(callId, new String(rawSdpData));
                //Sdp localSdp = SignalManager.getInstance().getLocalSdp();
                Sdp localSdp = configManager.loadSdpConfig("LOCAL");
                localSdp.setMediaPort(Sdp.AUDIO, listenPort);

                if (remoteSdp != null) {
                    if (remoteSdp.intersect(Sdp.AUDIO, localSdp)) {
                        List<RtpAttribute> otherSdpCodecList = remoteSdp.getMediaDescriptionFactory().getIntersectedCodecList(Sdp.AUDIO);
                        String remoteCodec = otherSdpCodecList.get(0).getRtpMapAttributeFactory().getCodecName();
                        String localCodec = MediaManager.getInstance().getPriorityCodec();
                        Logger.d("(%s) RemoteCodec: %s, LocalCodec: %s", callId, remoteCodec, localCodec);

                        if (!localCodec.equals(remoteCodec)) {
                            Logger.d("(%s) Send CANCEL to remote call.", callId);
                            AppInstance.getInstance().getMasterFragmentActivity().getPhoneFragment().processBye();
                            sendCancel(callId, callInfo.getToNo(), callInfo.getToSipIp(), callInfo.getToSipPort());
                            return false;
                        }
                    }
                }

                byte[] contents = localSdp.getData(false).getBytes();
                ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
                okResponse.setContent(contents, contentTypeHeader);

                callInfo.getInviteServerTransaction().sendResponse(okResponse);
                Logger.d("(%s) Send 200 OK for INVITE: %s", callId, okResponse);
                callInfo.setIsInviteAccepted(true);
            } else {
                // NO SDP
                Logger.d("(%s) NO SDP is detected. Fail to send 200 ok for the invite request.", callId);
                return false;
            }

            if (configManager.isUseClient()) {
                SoundHandler.getInstance().start();
            }
        } catch (Exception e) {
            Logger.w("(%s) Fail to send 200 OK for the invite request.", callId, e);
            e.printStackTrace();
        }

        return true;
    }

    /////////////////////////////////////////////////////////////////////
    // ACK

    /**
     * @fn public void sendAck(ResponseEvent responseEvent)
     * @brief ACK 요청을 보내는 함수
     * @param responseEvent 응답 이벤트
     */
    public void sendAck(ResponseEvent responseEvent) {
        CSeqHeader cSeqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
        if (cSeqHeader != null && cSeqHeader.getMethod().equals(Request.INVITE)) {
            try {
                Request request = responseEvent.getDialog().createAck(cSeqHeader.getSeqNumber());
                responseEvent.getDialog().sendAck(request);
                Logger.d("Send ACK for INVITE: %s", request);
            } catch (Exception e) {
                Logger.w("Fail to send the ACK request for the INVITE 200 OK.", e);
            }
        }
    }

    /**
     * @fn public void processAck(RequestEvent requestEvent)
     * @brief ACK 요청을 처리하는 함수
     * @param requestEvent 요청 이벤트
     */
    public void processAck(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        Logger.d("Recv ACK: %s", request);
    }

    /////////////////////////////////////////////////////////////////////
    // CANCEL

    /**
     * @fn public void sendCancel(String callId, String toHostName, String toIp, int toPort)
     * @brief CANCEL 요청을 보내는 함수
     * @param callId Call-Id
     */
    public void sendCancel(String callId, String toHostName, String toIp, int toPort) {
        CallInfo callInfo = CallManager.getInstance().getCallInfo(callId);
        if (callInfo == null) {
            return;
        }

        if (callInfo.getIsInviteAccepted()) {
            Logger.w("Call is already accepted. Fail to send the cancel request. (callId=%s)", callId);
            return;
        }

        try {
            // Create SIP URI
            SipURI sipUri = addressFactory.createSipURI(toHostName, toIp);
            sipUri.setHost(toIp);
            sipUri.setPort(toPort);
            sipUri.setLrParam();

            // Add Route Header
            Address addressTo = addressFactory.createAddress(toHostName, sipUri);
            // Create the request URI for the SIP message
            URI requestURI = addressTo.getURI();

            // Create the SIP message headers
            // The " Via" headers
            List<ViaHeader> viaHeaders = new ArrayList<>();
            ViaHeader viaHeader = this.headerFactory.createViaHeader (hostIp , hostPort, SIP_TRANSPORT_TYPE, null);
            viaHeaders.add(viaHeader);

            // The "Max - Forwards " header
            MaxForwardsHeader maxForwardsHeader = this.headerFactory.createMaxForwardsHeader(70);

            // The "Call -Id" header
            CallIdHeader callIdHeader = callInfo.getCallIdHeader();
            if (callIdHeader == null) { return; }

            // The " CSeq " header
            CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1L, Request.CANCEL);

            // The " From " header
            FromHeader fromHeader = this.headerFactory.createFromHeader(hostAddress, String.valueOf(random.nextInt(10000)));

            // The "To" header
            ToHeader toHeader = this.headerFactory.createToHeader(addressTo, String.valueOf(random.nextInt(10000)));

            // The "Reason" header
            ReasonHeader reasonHeader = this.headerFactory.createReasonHeader("SIP", Response.DECLINE, "Decline");

            // Create the BYE request
            Request request = this.messageFactory.createRequest (
                    requestURI,
                    Request.CANCEL,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwardsHeader
            );

            request.addHeader(contactHeader);
            request.addHeader(reasonHeader);

            CallManager.getInstance().deleteCallInfo(callId);
            new Thread(() -> {
                try {
                    sipProvider.sendRequest(request);
                    Logger.d("CANCEL Request sent. (request=%s)", request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            if (configManager.isProxyMode()) {
                NettyChannelManager.getInstance().deleteProxyChannel(callId);
                AudioMixManager.getInstance().removeAudioMixer(
                        callInfo.getSessionId(),
                        callId
                );
                callInfo.setRemoteCallInfo(null);

                if (callInfo.getIsRoomEntered()) {
                    GroupCallManager.getInstance().deleteRoomInfo(callInfo.getSessionId(), callId);
                }
            }

            callInfo.setCallIdHeader(null);
            callInfo.setIsInviteAccepted(false);
            callInfo.setIsCallStarted(false);
            callInfo.setIsCallRecv(false);
            callInfo.setIsCallCanceled(true);
        } catch (Exception e) {
            Logger.w("CANCEL Request sent failed.", e);
        }
    }

    /**
     * @fn public void processCancel(RequestEvent requestEvent)
     * @brief CANCEL 요청을 처리하는 함수
     * @param requestEvent 요청 이벤트
     */
    public void processCancel (RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        if (callIdHeader == null) { return; }

        String callId = callIdHeader.getCallId();
        CallInfo callInfo = CallManager.getInstance().getCallInfo(callId);
        if (callInfo == null) {
            return;
        }

        //FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
        //String fromNo = fromHeader.getAddress().getDisplayName();

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        boolean isUseClient = configManager.isUseClient();
        boolean isProxyMode = configManager.isProxyMode();

        try {
            if (CallManager.getInstance().deleteCallInfo(callId) != null) {
                Logger.d("Recv CANCEL: %s", request);

                if (isProxyMode) {
                    CallInfo remoteCallInfo = CallManager.getInstance().findOtherCallInfo(callInfo.getSessionId(), callId);
                    if (remoteCallInfo != null) {
                        String toHostName = callInfo.getFromNo().equals(configManager.getHostName()) ? remoteCallInfo.getFromNo() : callInfo.getToNo();
                        sendCancel(
                                remoteCallInfo.getCallId(),
                                toHostName,
                                remoteCallInfo.getToSipIp(),
                                remoteCallInfo.getToSipPort()
                        );
                        Logger.d("Send CANCEL to remote call. (%s)", remoteCallInfo.getCallId());
                    } else {
                        Logger.w("Fail to send the cancel request to remote call. (%s)", callId);
                    }
                }

                callInfo.setIsInviteAccepted(false);
                callInfo.setIsCallStarted(false);
                callInfo.setIsCallCanceled(true);
                callInfo.setIsCallRecv(false);

                Response response = messageFactory.createResponse(200, request);
                sipProvider.sendResponse(response);
                Logger.d("Send 200 OK for CANCEL: %s", response);

                if (isUseClient) {
                    AppInstance.getInstance().getMasterFragmentActivity().getPhoneFragment().processBye();
                }

                if (configManager.isProxyMode()) {
                    NettyChannelManager.getInstance().deleteProxyChannel(callId);
                    AudioMixManager.getInstance().removeAudioMixer(
                            callInfo.getSessionId(),
                            callId
                    );
                    callInfo.setRemoteCallInfo(null);

                    if (callInfo.getIsRoomEntered()) {
                        GroupCallManager.getInstance().deleteRoomInfo(callInfo.getSessionId(), callId);
                    }
                }

                send487(callId);
            }
        } catch (Exception e) {
            Logger.w("Fail to send the 200 OK response for the CANCEL request. (callId=%s)", callId, e);
        }
    }

    /////////////////////////////////////////////////////////////////////
    // BYE

    /**
     * @fn public void sendBye(String callId, String toHostName, String toIp, int toPort)
     * @brief BYE 요청을 보내는 함수
     * @param callId Call-Id
     */
    public void sendBye(String callId, String toHostName, String toIp, int toPort) {
        CallInfo callInfo = CallManager.getInstance().getCallInfo(callId);
        if (callInfo == null) {
            return;
        }

        if (!callInfo.getIsInviteAccepted()) {
            Logger.w("Call is not accepted. Fail to send the bye request. (callId=%s)", callId);
            return;
        }

        try {
            // Create SIP URI
            SipURI sipUri = addressFactory.createSipURI(toHostName, toIp);
            sipUri.setHost(toIp);
            sipUri.setPort(toPort);
            sipUri.setLrParam();

            // Add Route Header
            Address addressTo = addressFactory.createAddress(toHostName, sipUri);
            // Create the request URI for the SIP message
            URI requestURI = addressTo.getURI();

            // Create the SIP message headers
            // The " Via" headers
            List<ViaHeader> viaHeaders = new ArrayList<>();
            ViaHeader viaHeader = this.headerFactory.createViaHeader (hostIp , hostPort, SIP_TRANSPORT_TYPE, null);
            viaHeaders.add(viaHeader);

            // The "Max - Forwards " header
            MaxForwardsHeader maxForwardsHeader = this.headerFactory.createMaxForwardsHeader(70);

            // The "Call -Id" header
            CallIdHeader callIdHeader = callInfo.getCallIdHeader();
            if (callIdHeader == null) { return; }

            // The " CSeq " header
            CSeqHeader cSeqHeader = this.headerFactory.createCSeqHeader(1L, Request.BYE);

            // The " From " header
            FromHeader fromHeader = this.headerFactory.createFromHeader(hostAddress, String.valueOf(random.nextInt(10000)));

            // The "To" header
            ToHeader toHeader = this.headerFactory.createToHeader(addressTo, String.valueOf(random.nextInt(10000)));

            // Create the BYE request
            Request request = this.messageFactory.createRequest (
                    requestURI,
                    Request.BYE,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwardsHeader
            );

            CallManager.getInstance().deleteCallInfo(callId);

            request.addHeader(contactHeader);

            ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(request);
            new Thread(() -> {
                try {
                    clientTransaction.sendRequest();
                    Logger.d("BYE Request sent. (request=%s)", request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            if (configManager.isProxyMode()) {
                NettyChannelManager.getInstance().deleteProxyChannel(callId);
                AudioMixManager.getInstance().removeAudioMixer(
                        callInfo.getSessionId(),
                        callId
                );
                callInfo.setRemoteCallInfo(null);

                if (callInfo.getIsRoomEntered()) {
                    GroupCallManager.getInstance().deleteRoomInfo(callInfo.getSessionId(), callId);
                }
            } else {
                NettyChannelManager.getInstance().stop();
                SoundHandler.getInstance().stop();
            }

            callInfo.setCallIdHeader(null);
            callInfo.setIsInviteAccepted(false);
            callInfo.setIsCallStarted(false);
            callInfo.setIsCallRecv(false);
            Logger.d("BYE Request sent. (request=%s)", request);
        } catch (Exception e) {
            Logger.w("BYE Request sent failed.", e);
        }
    }

    /**
     * @fn public void processBye(RequestEvent requestEvent)
     * @brief BYE 요청을 처리하는 함수
     * @param requestEvent 요청 이벤트
     */
    public void processBye (RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        if (callIdHeader == null) { return; }

        String callId = callIdHeader.getCallId();
        CallInfo callInfo = CallManager.getInstance().getCallInfo(callId);
        if (callInfo == null) {
            return;
        }

        //FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
        //String fromNo = fromHeader.getAddress().getDisplayName();

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        boolean isUseClient = configManager.isUseClient();
        boolean isProxyMode = configManager.isProxyMode();

        CallInfo remoteCallInfo = callInfo.getRemoteCallInfo();

        try {
            if (CallManager.getInstance().deleteCallInfo(callId) != null) {
                Logger.d("Recv BYE: %s", request);

                if (isProxyMode) {
                    NettyChannelManager.getInstance().deleteProxyChannel(callId);
                    AudioMixManager.getInstance().removeAudioMixer(
                            callInfo.getSessionId(),
                            callId
                    );
                    callInfo.setRemoteCallInfo(null);

                    if (callInfo.getIsRoomEntered()) {
                        GroupCallManager.getInstance().deleteRoomInfo(callInfo.getSessionId(), callId);
                    } else {
                        if (remoteCallInfo != null){
                            String toHostName = callInfo.getFromNo().equals(configManager.getHostName()) ? remoteCallInfo.getFromNo() : callInfo.getToNo();
                            sendBye(remoteCallInfo.getCallId(), toHostName, remoteCallInfo.getToSipIp(), remoteCallInfo.getToSipPort());
                            Logger.d("Send BYE to remote call. (%s)", remoteCallInfo.getCallId());
                        } else{
                            Logger.w("Fail to send the bye request to remote call. (%s)", callId);
                        }
                    }
                } else {
                    NettyChannelManager.getInstance().stop();
                    SoundHandler.getInstance().stop();
                }

                callInfo.setIsInviteAccepted(false);
                callInfo.setIsCallStarted(false);
                callInfo.setIsCallRecv(false);

                new Thread(() -> {
                    try {
                        Response response = messageFactory.createResponse(200, request);
                        sipProvider.sendResponse(response);
                        Logger.d("Send 200 OK for BYE: %s", response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                if (isUseClient) {
                    AppInstance.getInstance().getMasterFragmentActivity().getPhoneFragment().processBye();
                }
            }
        } catch (Exception e) {
            Logger.w("Fail to send the 200 OK response for the BYE request. (callId=%s)", callId, e);
            e.printStackTrace();
        }
    }

    /////////////////////////////////////////////////////////////////////

    public void send487(String callId) {
        CallInfo callInfo = CallManager.getInstance().getCallInfo(callId);
        if (callInfo == null) {
            return;
        }

        if (callInfo.getInviteRequest() == null || callInfo.getInviteServerTransaction() == null) {
            Logger.w("Fail to send 487 Request Terminated for INVITE.");
            return;
        }

        try {
            // Send 487 Request Terminated
            Response okResponse = messageFactory.createResponse(Response.REQUEST_TERMINATED, callInfo.getInviteRequest());
            callInfo.getInviteServerTransaction().sendResponse(okResponse);
            Logger.d("Send 487 Request Terminated for INVITE: %s", okResponse);
        } catch (Exception e) {
            Logger.w("Fail to send 487 Request Terminated for the invite request.", e);
        }
    }

    /////////////////////////////////////////////////////////////////////
    // MESSAGE

    /**
     * @fn public void processMessage(RequestEvent requestEvent)
     * @brief MESSAGE 요청을 처리하는 함수
     * @param requestEvent 요청 이벤트
     */
    public void processMessage(RequestEvent requestEvent) {
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();
        Request request = requestEvent.getRequest();

        CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        if (callIdHeader == null) { return; }

        String callId = callIdHeader.getCallId();
        CallInfo callInfo = CallManager.getInstance().getCallInfo(callId);
        if (callInfo == null) {
            return;
        }

        Logger.d("Recv Message: %s", request);

        try {
            Response response = messageFactory.createResponse(200, request);
            serverTransaction.sendResponse(response);
            Logger.d("Send 200 OK for MESSAGE: %s", response);

            //ContentDispositionHeader contentDispositionHeader = (ContentDispositionHeader) request.getHeader(ContentDispositionHeader.NAME);
        } catch (Exception e) {
            Logger.w("Fail to send the 200 OK response for the MESSAGE request.", e);
        }
    }

    /////////////////////////////////////////////////////////////////////
    // 200 OK

    /**
     * @fn public void processOk (ResponseEvent responseEvent)
     * @brief 200 OK 응답을 처리하는 함수
     * @param responseEvent 응답 이벤트
     */
    public void processOk (ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();

        CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        if (callIdHeader == null) { return; }

        CSeqHeader cSeqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        if (cSeqHeader == null) { return; }

        String methodName = cSeqHeader.getMethod();
        String callId = callIdHeader.getCallId();

        CallInfo callInfo = null;

        // REGISTER 아닌 경우에만 CallInfo 처리
        if (!methodName.equals(Request.REGISTER)) {
            callInfo = CallManager.getInstance().getCallInfo(callId);
            if (callInfo == null) {
                return;
            }
        }

        try {
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            boolean isUseClient = configManager.isUseClient();

            // INVITE 200 OK 처리
            if (methodName.equals(Request.INVITE)) {
                if (callInfo.getIsCallCanceled()) {
                    Logger.w("(%s) Call is canceled. Fail to process the invite 200 ok.", callId);
                    return;
                }

                callInfo.setIsInviteAccepted(true);
                callInfo.setIsInviteUnauthorized(false);

                byte[] rawSdpData = response.getRawContent();
                if (rawSdpData != null) {
                    try {
                        SdpParser sdpParser = new SdpParser();
                        Sdp remoteSdp = sdpParser.parseSdp(callId, new String(rawSdpData));

                        // 우선 순위 코덱 일치 확인
                        if (remoteSdp != null) {
                            Sdp localSdp = configManager.loadSdpConfig("LOCAL");
                            if (remoteSdp.intersect(Sdp.AUDIO, localSdp)) {
                                List<RtpAttribute> otherSdpCodecList = remoteSdp.getMediaDescriptionFactory().getIntersectedCodecList(Sdp.AUDIO);
                                String remoteCodec = otherSdpCodecList.get(0).getRtpMapAttributeFactory().getCodecName();
                                String localCodec = MediaManager.getInstance().getPriorityCodec();
                                Logger.d("(%s) RemoteCodec: %s, LocalCodec: %s", callId, remoteCodec, localCodec);

                                if (localCodec.equals(remoteCodec)) {
                                    CallManager.getInstance().addSdpIntoCallInfo(callId, remoteSdp);
                                } else {
                                    sendAck(responseEvent);
                                    sendBye(callId, callInfo.getToNo(), callInfo.getToSipIp(), callInfo.getToSipPort());
                                    Logger.d("(%s) Send BYE to remote call.", callId);

                                    AppInstance.getInstance().getMasterFragmentActivity().getPhoneFragment().processBye();
                                    return;
                                }
                            }
                        }

                        if (configManager.isProxyMode()) {
                            if (!callInfo.getIsRoomEntered()) {
                                // Set remote call info > 프록시만 설정 : from mdn callInfo 에 to mdn callInfo 를 remote 로 설정
                                CallInfo remoteCallInfo = CallManager.getInstance().findOtherCallInfo(callInfo.getSessionId(), callId);
                                if (remoteCallInfo != null) {
                                    if (sendInviteOk(remoteCallInfo.getCallId())) {
                                        callInfo.setRemoteCallInfo(remoteCallInfo);
                                        remoteCallInfo.setRemoteCallInfo(callInfo);
                                        Logger.w("(%s) Success to set the remote peer and send the invite 200 ok response. (remoteCallId=%s)", callId, remoteCallInfo.getCallId());
                                    }
                                } else {
                                    Logger.w("(%s) Fail to set the remote peer and send the invite 200 ok response.", callId);
                                }
                            }
                        } else {
                            SoundHandler.getInstance().start();
                        }

                        // INVITE 200 OK 인 경우 ACK 전송
                        sendAck(responseEvent);
                    } catch (Exception e) {
                        Logger.w("(%s) Fail to process the invite 200 OK.", callId, e);
                    }
                }
            } else if (methodName.equals(Request.REGISTER)) {
                // 클라이언트인 경우에만 REGISTER 200 OK 수신
                if (isUseClient) {
                    FromHeader inviteFromHeader = (FromHeader) response.getHeader(FromHeader.NAME);
                    String fromNo = inviteFromHeader.getAddress().getDisplayName();

                    // TODO
                    /*if (FrameManager.getInstance().processRegisterToFrame(fromNo)) {
                        Logger.d("(%s) Success to process the register to [%s] frame.", ServiceManager.CLIENT_FRAME_NAME, callId);
                    }*/
                    //

                    Logger.d("(%s) Success to register. (mdn=%s)", callId, fromNo);
                }
            }
        } catch (Exception e) {
            Logger.w("(%s) Fail to process the 200 OK response for the %s request", callId, methodName, e);
        }
    }

    private String getRandomStr(int size) {
        if(size > 0) {
            char[] tmp = new char[size];
            for(int i=0; i<tmp.length; i++) {
                int div = (int) Math.floor( Math.random() * 2 );
                if(div == 0) { // 0이면 숫자로
                    tmp[i] = (char) (Math.random() * 10 + '0') ;
                }else { //1이면 알파벳
                    tmp[i] = (char) (Math.random() * 26 + 'A') ;
                }
            }
            return new String(tmp);
        }

        return null;
    }

}
