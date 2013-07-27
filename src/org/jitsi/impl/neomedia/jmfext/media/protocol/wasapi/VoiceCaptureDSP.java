/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi;


/**
 * Defines the native interface of Voice Capture DSP as used by
 * <tt>WASAPISystem</tt> and its associated <tt>CaptureDevice</tt>,
 * <tt>DataSource</tt> and <tt>Renderer</tt> implementations.
 *
 * @author Lyubomir Marinov
 */
public class VoiceCaptureDSP
{
    public static native int DMO_MEDIA_TYPE_fill(
            long thiz,
            String majortype,
            String subtype,
            boolean bFixedSizeSamples,
            boolean bTemporalCompression,
            int lSampleSize,
            String formattype,
            long pUnk,
            int cbFormat,
            long pbFormat)
        throws HResultException;

    public static native void DMO_MEDIA_TYPE_setCbFormat(
            long thiz,
            int cbFormat);

    public static native int DMO_MEDIA_TYPE_setFormattype(
            long thiz,
            String formattype)
        throws HResultException;

    public static native void DMO_MEDIA_TYPE_setLSampleSize(
            long thiz,
            int lSampleSize);

    public static native void DMO_MEDIA_TYPE_setPbFormat(
            long thiz,
            long pbFormat);

    public static native long DMO_OUTPUT_DATA_BUFFER_alloc(
            long pBuffer,
            int dwStatus,
            long rtTimestamp,
            long rtTimelength);

    public static native int DMO_OUTPUT_DATA_BUFFER_getDwStatus(long thiz);

    public static native void DMO_OUTPUT_DATA_BUFFER_setDwStatus(
            long thiz,
            int dwStatus);

    public static native int IMediaBuffer_AddRef(long thiz);

    public static native long IMediaBuffer_GetBuffer(long thiz)
        throws HResultException;

    public static native int IMediaBuffer_GetLength(long thiz)
        throws HResultException;

    public static native int IMediaBuffer_GetMaxLength(long thiz)
        throws HResultException;

    public static native int IMediaBuffer_Release(long thiz);

    public static native void IMediaBuffer_SetLength(long thiz, int cbLength)
        throws HResultException;

    public static native int IMediaObject_Flush(long thiz)
        throws HResultException;

    public static native int IMediaObject_GetInputStatus(
            long thiz,
            int dwInputStreamIndex)
        throws HResultException;

    public static native int IMediaObject_ProcessInput(
            long thiz,
            int dwInputStreamIndex,
            long pBuffer,
            int dwFlags,
            long rtTimestamp,
            long rtTimelength)
        throws HResultException;

    public static native int IMediaObject_ProcessOutput(
            long thiz,
            int dwFlags,
            int cOutputBufferCount,
            long pOutputBuffers)
        throws HResultException;

    public static native long IMediaObject_QueryInterface(long thiz, String iid)
        throws HResultException;

    public static native void IMediaObject_Release(long thiz);

    public static native int IMediaObject_SetInputType(
            long thiz,
            int dwInputStreamIndex,
            long pmt,
            int dwFlags)
        throws HResultException;

    public static native int IMediaObject_SetOutputType(
            long thiz,
            int dwOutputStreamIndex,
            long pmt,
            int dwFlags)
        throws HResultException;

    public static native int IPropertyStore_SetValue(
            long thiz,
            long key, boolean value)
        throws HResultException;

    public static native int IPropertyStore_SetValue(
            long thiz,
            long key, int value)
        throws HResultException;

    public static native long MediaBuffer_alloc(int maxLength);

    public static native int MediaBuffer_pop(
            long thiz,
            byte[] buffer, int offset, int length)
        throws HResultException;

    public static native int MediaBuffer_push(
            long thiz,
            byte[] buffer, int offset, int length)
        throws HResultException;

    public static native long MoCreateMediaType(int cbFormat)
        throws HResultException;

    public static native void MoDeleteMediaType(long pmt)
        throws HResultException;

    public static native void MoFreeMediaType(long pmt)
        throws HResultException;

    public static native void MoInitMediaType(long pmt, int cbFormat)
        throws HResultException;

    /** Prevents the initialization of <tt>VoiceCaptureDSP</tt> instances. */
    private VoiceCaptureDSP() {}
}
