package com.jamesj.voip_phone_android.media.protocol.rtp.util;

import com.orhanobut.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @class public class RtpUtil
 * @brief RtpUtil class
 */
public class RtpUtil {

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public static byte[] changeByteOrder(byte[] value)
     * @brief Byte order 를 변환하는 함수
     * @param data Sampled data
     * @return Byte order 가 변환된 Data 반환
     */
    public static byte[] changeByteOrder(byte[] data) {
        int dataLength = data.length;
        byte[] convertedData = new byte[dataLength];

        for (int i = 0; i < dataLength; i++) {
            convertedData[i] = data[dataLength - (i + 1)];
        }

        return convertedData;
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public static byte[] upSample(byte[] data)
     * @brief Sampling rate 를 두 배 올리는 함수
     * @param data Sampled data
     * @return Up-sampled data 반환
     */
    public static byte[] upSamplingRateDouble(byte[] data) {
        byte[] resampledData = new byte[data.length * 2];

        for (int i = 0; i < data.length; i += 2) {
            resampledData[i * 2] = data[i];
            resampledData[i * 2 + 1] = data[i + 1];
            resampledData[i * 2 + 2] = data[i];
            resampledData[i * 2 + 3] = data[i + 1];
        }

        return resampledData;
    }

    /**
     * @fn public static byte[] downSample(byte[] data)
     * @brief Sampling rate 를 두 배 낮추는 함수
     * @param data Sampled data
     * @return Down-sampled data 반환
     */
    public static byte[] downSamplingRateDouble(byte[] data) {
        byte[] resampledData = new byte[data.length / 2];

        for (int i = 0; i < resampledData.length; i++) {
            resampledData[i] = i % 2 == 0 ? data[i * 2] : data[i * 2 + 1];
        }

        return resampledData;
    }

    ////////////////////////////////////////////////////////////////////////////////
    
    /**
     * @fn private static byte[] getBytesFromInputStream(InputStream inputStream)
     * @brief 지정한 InputStream 에서 Byte Array Data 를 읽는 함수
     * @param inputStream InputStream
     * @return 성공 시 Byte Array Data, 실패 시 null 반환
     */
    public static byte[] getBytesFromInputStream(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            while (true) {
                byte[] buffer = new byte[100];
                int readBytes = inputStream.read(buffer);
                if (readBytes == -1) {
                    break;
                } else {
                    byteArrayOutputStream.write(
                            buffer,
                            0,
                            readBytes
                    );
                }
            }

            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
        } catch (Exception e) {
            Logger.w("ChannelHandler.getBytesFromInputStream.Exception", e);
            return null;
        }

        return byteArrayOutputStream.toByteArray();
    }

    ////////////////////////////////////////////////////////////////////////////////

    public static byte[] shortToByte(short[] shortData, boolean isBigEndian) {
        byte[] data = new byte[shortData.length * 2];
        int size = shortData.length;
        if (!isBigEndian) {
            for (int i = 0; i < size; i++) {
                data[i * 2] = (byte) shortData[i];
                data[i * 2 + 1] = (byte) (shortData[i] >> 8);
            }
        } else {
            // big Endian
            for (int i = 0; i < size; i++) {
                data[i * 2] = (byte) (shortData[i] >> 8);
                data[i * 2 + 1] = (byte) shortData[i];
            }
        }
        return data;
    }

    public static short[] byteToShort(byte[] byteData, boolean isBigEndian) {
        short[] data = new short[byteData.length / 2];
        int size = data.length;
        byte lb, hb;
        if (!isBigEndian) {
            for (int i = 0; i < size; i++) {
                lb = byteData[i * 2];
                hb = byteData[i * 2 + 1];
                data[i] = (short) (((short) hb << 8) | lb & 0xff);
            }
        } else {
            for (int i = 0; i < size; i++) {
                lb = byteData[i * 2];
                hb = byteData[i * 2 + 1];
                data[i] = (short) (((short) lb << 8) | hb & 0xff);
            }

        }
        return data;
    }

    public static int[] byteToInt(byte[] src, boolean isBigEndian) {
        int dstLength = src.length >>> 2;
        int[] dst = new int[dstLength];

        for (int i = 0; i < dstLength; i++) {
            int j = i << 2;
            int x = 0;

            if (isBigEndian) {
                // big endian
                x += (src[j++] & 0xff) << 24;
                x += (src[j++] & 0xff) << 16;
                x += (src[j++] & 0xff) << 8;
                x += (src[j] & 0xff);
            } else {
                // little endian
                x += (src[j++] & 0xff);
                x += (src[j++] & 0xff) << 8;
                x += (src[j++] & 0xff) << 16;
                x += (src[j] & 0xff) << 24;
            }

            dst[i] = x;
        }

        return dst;
    }

    public static byte[] intToByte(int[] src, boolean isBigEndian) {
        int srcLength = src.length;
        byte[] dst = new byte[srcLength << 2];

        for (int i = 0; i < srcLength; i++) {
            int x = src[i];
            int j = i << 2;

            if (isBigEndian) {
                dst[j++] = (byte) ((x >>> 24) & 0xff);
                dst[j++] = (byte) ((x >>> 16) & 0xff);
                dst[j++] = (byte) ((x >>> 8) & 0xff);
                dst[j] = (byte) ((x) & 0xff);
            } else {
                dst[j++] = (byte) ((x) & 0xff);
                dst[j++] = (byte) ((x >>> 8) & 0xff);
                dst[j++] = (byte) ((x >>> 16) & 0xff);
                dst[j] = (byte) ((x >>> 24) & 0xff);
            }
        }

        return dst;
    }

    public static double[] byteToDouble (byte[] data) {
        ByteBuffer prevByteBuffer = ByteBuffer.wrap(data);

        double[] doubles = new double[data.length / Double.BYTES];
        for(int i = 0; i < doubles.length; i++) {
            doubles[i] = prevByteBuffer.getDouble();
        }

        return doubles;
    }

    public static byte[] doubleToByte (double[] data) {
        ByteBuffer afterByteBuffer = ByteBuffer.allocate(data.length * Double.BYTES);

        for(double datum : data) {
            afterByteBuffer.putDouble(datum);
        }

        return afterByteBuffer.array();
    }

    public static byte[] floatToByte (float[] data) {
        ByteBuffer afterByteBuffer = ByteBuffer.allocate(data.length * Float.BYTES);

        for(float datum : data) {
            afterByteBuffer.putFloat(datum);
        }

        return afterByteBuffer.array();
    }

}
