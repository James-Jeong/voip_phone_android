/*
 * Copyright (C) 2021. Uangel Corp. All rights reserved.
 */
package com.jamesj.voip_phone_android.media.sdp;

import com.jamesj.voip_phone_android.media.sdp.base.Sdp;
import com.jamesj.voip_phone_android.media.sdp.base.attribute.base.AttributeFactory;
import com.jamesj.voip_phone_android.media.sdp.base.attribute.base.FmtpAttributeFactory;
import com.jamesj.voip_phone_android.media.sdp.base.attribute.base.RtpMapAttributeFactory;
import com.jamesj.voip_phone_android.media.sdp.base.media.MediaDescriptionFactory;
import com.jamesj.voip_phone_android.media.sdp.base.media.MediaFactory;
import com.jamesj.voip_phone_android.media.sdp.base.session.SessionDescriptionFactory;
import com.jamesj.voip_phone_android.media.sdp.base.time.TimeDescriptionFactory;
import com.orhanobut.logger.Logger;
import com.telestax.tavax.sdp.SessionDescriptionImpl;
import com.telestax.tavax.sdp.parser.SDPAnnounceParser;

import java.util.Vector;

import tavax.sdp.Attribute;
import tavax.sdp.BandWidth;
import tavax.sdp.Connection;
import tavax.sdp.Media;
import tavax.sdp.MediaDescription;
import tavax.sdp.TimeDescription;


public class SdpParser {

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @param sdpStr sdp message
     * @return Sdp
     * @fn public Sdp parseSdp (String callId, String sdpStr) throws Exception
     */
    public Sdp parseSdp(String callId, String sdpStr) throws Exception {
        if (sdpStr == null || sdpStr.length() == 0) {
            return null;
        }

        Sdp sdp = new Sdp(callId);

        SDPAnnounceParser parser = new SDPAnnounceParser(sdpStr);
        SessionDescriptionImpl sdi = parser.parse();
        if (sdi.getVersion().getVersion() != 0) {
            Logger.w("(%s) sdp version is not 0. sdp=%s", callId, sdpStr);
            return null;
        }

        // 1) Session Description
        SessionDescriptionFactory sessionDescriptionFactory = new SessionDescriptionFactory(
                sdi.getVersion().getTypeChar(),
                sdi.getVersion().getVersion(),
                sdi.getOrigin().getTypeChar(),
                sdi.getOrigin().getUsername(),
                sdi.getOrigin().getAddress(),
                sdi.getOrigin().getAddressType(),
                sdi.getOrigin().getNetworkType(),
                sdi.getOrigin().getSessionId(),
                sdi.getOrigin().getSessionVersion(),
                sdi.getSessionName().getTypeChar(),
                sdi.getSessionName().getValue()
        );

        Connection connection = sdi.getConnection();
        if (connection != null) {
            sessionDescriptionFactory.setConnectionField(
                    connection.getTypeChar(),
                    connection.getAddress(),
                    connection.getAddressType(),
                    connection.getNetworkType()
            );
        }

        sdp.setSessionDescriptionFactory(sessionDescriptionFactory);
        //logger.debug("(%s) Session Description=\n%s", callId, sdp.getSessionDescriptionFactory().getData());

        // 2) Time Description
        Vector tdVector = sdi.getTimeDescriptions(false);
        if (tdVector == null || tdVector.isEmpty()) {
            Logger.w("(%s) sdp hasn't time description. sdp=%s", callId, sdpStr);
            return null;
        }

        for (Object o : tdVector) {
            TimeDescription timeDescription = (TimeDescription) o;
            if (timeDescription != null) {
                String timeData = timeDescription.toString();

                int equalPos = timeData.indexOf("=");
                int spacePos = timeData.indexOf(" ");
                int crlfPos = timeData.indexOf("\r\n");

                String startTimeStr = timeData.substring(equalPos + 1, spacePos);
                String endTimeStr = timeData.substring(spacePos + 1, crlfPos);

                TimeDescriptionFactory timeDescriptionFactory = new TimeDescriptionFactory(
                        timeDescription.getTime().getTypeChar(),
                        startTimeStr,
                        endTimeStr
                );
                sdp.setTimeDescriptionFactory(timeDescriptionFactory);
                break;
            }
        }
        //logger.debug("(%s) Time Description=\n%s", callId, sdp.getTimeDescriptionFactory().getData());

        // 3) Media Description
        Vector mdVector = sdi.getMediaDescriptions(false);
        if (mdVector == null || mdVector.isEmpty()) {
            Logger.w("(%s) sdp hasn't media description. sdp=%s", callId, sdpStr);
            return null;
        }

        MediaDescriptionFactory mediaDescriptionFactory = new MediaDescriptionFactory();

        for (Object o1 : mdVector) {
            MediaDescription md = (MediaDescription) o1;
            if (md != null) {
                Media media = md.getMedia();
                if (media != null && media.getMediaType().equals(Sdp.AUDIO)) {
                    // Media
                    MediaFactory mediaFactory = new MediaFactory(
                            media.getTypeChar(),
                            media.getMediaType(),
                            media.getMediaFormats(false),
                            media.getMediaPort(),
                            media.getProtocol(),
                            media.getPortCount()
                    );

                    // Bandwidth
                    Vector bwVector = md.getBandwidths(false);
                    if (bwVector != null && !bwVector.isEmpty()) {
                        for (Object o : bwVector) {
                            BandWidth bandWidth = (BandWidth) o;
                            if (bandWidth == null) {
                                continue;
                            }

                            mediaFactory.addBandwidthField(
                                    bandWidth.getTypeChar(),
                                    bandWidth.getType(),
                                    bandWidth.getValue()
                            );
                        }
                    }

                    // Connection
                    /*Connection mediaConnection = md.getConnection();
                    if (mediaConnection != null) {
                        mediaFactory.setConnectionField(
                                mediaConnection.getTypeChar(),
                                mediaConnection.getAddress(),
                                mediaConnection.getAddressType(),
                                mediaConnection.getNetworkType()
                        );
                    }*/

                    // Attributes
                    Vector adVector = md.getAttributes(false);
                    if (adVector == null || adVector.isEmpty()) {
                        Logger.w("(%s) sdp hasn't attribute description. sdp=%s", callId, sdpStr);
                        return null;
                    }

                    //for (int i = adVector.size() - 1; i >= 0; i--) {
                    for (Object o : adVector) {
                        Attribute attribute = (Attribute) o;
                        if (attribute == null) {
                            continue;
                        }

                        String attributeName = attribute.getName();
                        if (attributeName == null) {
                            continue;
                        }

                        if (attributeName.equals(MediaFactory.RTPMAP)) {
                            RtpMapAttributeFactory rtpMapAttributeFactory = new RtpMapAttributeFactory(attribute.getTypeChar(), attribute.getName(), attribute.getValue(), mediaFactory.getMediaField().getMediaFormats());
                            mediaFactory.addRtpAttributeFactory(rtpMapAttributeFactory);
                            //logger.debug("(%s) [%s] RtpMapAttributeFactory=\n%s", callId, rtpMapAttributeFactory.getPayloadId(), rtpMapAttributeFactory.getData());
                        } else if (attributeName.equals(MediaFactory.FMTP)) {
                            FmtpAttributeFactory fmtpAttributeFactory = new FmtpAttributeFactory(attribute.getTypeChar(), attribute.getName(), attribute.getValue(), mediaFactory.getMediaField().getMediaFormats());
                            mediaFactory.addFmtpAttributeFactory(fmtpAttributeFactory);
                            //logger.debug("(%s) [%s] FmtpAttributeFactory=\n%s", callId, fmtpAttributeFactory.getPayloadId(), fmtpAttributeFactory.getData());
                        } else {
                            AttributeFactory attributeFactory = new AttributeFactory(attribute.getTypeChar(), attribute.getName(), attribute.getValue(), mediaFactory.getMediaField().getMediaFormats());
                            mediaFactory.addAttributeFactory(attributeFactory);
                            //logger.debug("(%s) [%s] AttributeFactory=\n%s", callId, attributeFactory.getPayloadId(), attributeFactory.getData());
                        }
                    }

                    mediaDescriptionFactory.addMediaFactory(mediaFactory);
                    //logger.debug("(%s) Media=\n%s", callId, mediaFactory.getData(false));
                    break;
                }
            }
        }

        sdp.setMediaDescriptionFactory(mediaDescriptionFactory);
        //logger.debug("(%s) Media Description=\n%s", callId, sdp.getMediaDescriptionFactory().getData(false));

        return sdp;
    }

}