package com.jamesj.voip_phone_android.media.sdp.base.time;


import com.jamesj.voip_phone_android.media.sdp.base.SdpFactory;
import com.jamesj.voip_phone_android.media.sdp.base.field.TimeField;

/**
 * @class public class TimeDescriptionFactory
 * @brief TimeDescriptionFactory class
 */
public class TimeDescriptionFactory extends SdpFactory {

    // Mandatory
    private TimeField timeField;

    ////////////////////////////////////////////////////////////////////////////////

    public TimeDescriptionFactory(
            char timeType,
            String startTime, String endTime) {
        this.timeField = new TimeField(
                timeType,
                startTime,
                endTime
        );
    }

    ////////////////////////////////////////////////////////////////////////////////

    public TimeField getTimeField() {
        return timeField;
    }

    public void setTimeField(TimeField timeField) {
        this.timeField = timeField;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getData() {
        return timeField.getTimeType() + "=" +
                timeField.getStartTime() + " " +
                timeField.getEndTime() +
                CRLF;
    }

}
