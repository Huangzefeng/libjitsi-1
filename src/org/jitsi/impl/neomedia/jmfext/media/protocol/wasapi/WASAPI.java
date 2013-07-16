/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi;

import org.jitsi.util.*;

public class WASAPI
{
    public static native void CloseHandle(long hObject)
        throws HResultException;
    
    public static native String CoCreateGuid()
        throws HResultException;

    public static native long CoCreateInstance(
            String clsid,
            long pUnkOuter,
            int dwClsContext,
            String iid)
        throws HResultException;

    public static native int CoInitializeEx(long pvReserved, int dwCoInit)
        throws HResultException;

    public static native void CoTaskMemFree(long pv);

    public static native void CoUninitialize();

    public static native long CreateEvent(
            long lpEventAttributes,
            boolean bManualReset,
            boolean bInitialState,
            String lpName)
        throws HResultException;

    public static native int IAudioCaptureClient_GetNextPacketSize(long thiz)
        throws HResultException;

    public static native int IAudioCaptureClient_Read(
            long thiz,
            byte[] data, int offset, int length,
            int srcSampleSize, int srcChannels,
            int dstSampleSize, int dstChannels)
        throws HResultException;

    public static native void IAudioCaptureClient_Release(long thiz);

    public static native int IAudioClient_GetBufferSize(long thiz)
        throws HResultException;

    public static native int IAudioClient_GetCurrentPadding(long thiz)
        throws HResultException;

    public static native long IAudioClient_GetDefaultDevicePeriod(long thiz)
        throws HResultException;

    public static native long IAudioClient_GetMinimumDevicePeriod(long thiz)
        throws HResultException;

    public static native long IAudioClient_GetService(long thiz, String iid)
        throws HResultException;

    public static native int IAudioClient_Initialize(
            long thiz,
            int shareMode,
            int streamFlags,
            long hnsBufferDuration,
            long hnsPeriodicity,
            long pFormat,
            String audioSessionGuid)
        throws HResultException;

    public static native long IAudioClient_IsFormatSupported(
            long thiz,
            int shareMode,
            long pFormat)
        throws HResultException;

    public static native void IAudioClient_Release(long thiz);

    public static native void IAudioClient_SetEventHandle(
            long thiz,
            long eventHandle)
        throws HResultException;

    public static native int IAudioClient_Start(long thiz)
        throws HResultException;

    public static native int IAudioClient_Stop(long thiz)
        throws HResultException;

    public static native void IAudioRenderClient_Release(long thiz);

    /**
     * Writes specific audio data into the rendering endpoint buffer of a
     * specific <tt>IAudioRenderClient</tt>. If the sample sizes and/or the
     * numbers of channels of the specified audio <tt>data</tt> and the
     * specified rendering endpoint buffer differ, the method may be able to
     * perform the necessary conversions.
     *
     * @param thiz the <tt>IAudioRenderClient</tt> which abstracts the rendering
     * endpoint buffer into which the specified audio <tt>data</tt> is to be
     * written
     * @param data the bytes of the audio samples to be written into the
     * specified rendering endpoint buffer
     * @param offset the offset in bytes within <tt>data</tt> at which valid
     * audio samples begin
     * @param length the number of bytes of valid audio samples in <tt>data</tt>
     * @param srcSampleSize the size in bytes of an audio sample in
     * <tt>data</tt>
     * @param srcChannels the number of channels of the audio signal provided
     * in <tt>data</tt>
     * @param dstSampleSize the size in bytes of an audio sample in the
     * rendering endpoint buffer
     * @param dstChannels the number of channels with which the rendering
     * endpoint buffer has been initialized
     * @return the number of bytes which have been read from <tt>data</tt>
     * (beginning at <tt>offset</tt>, of course) and successfully written into
     * the rendering endpoint buffer
     * @throws HResultException if an HRESULT value indicating an error is
     * returned by a function invoked by the method implementation or an I/O
     * error is encountered during the execution of the method
     */
    public static native int IAudioRenderClient_Write(
            long thiz,
            byte[] data, int offset, int length,
            int srcSampleSize, int srcChannels,
            int dstSampleSize, int dstChannels)
        throws HResultException;

    public static native long IMMDevice_Activate(
            long thiz,
            String iid,
            int dwClsCtx,
            long pActivationParams)
        throws HResultException;

    public static native String IMMDevice_GetId(long thiz)
        throws HResultException;

    public static native int IMMDevice_GetState(long thiz)
        throws HResultException;

    public static native long IMMDevice_OpenPropertyStore(
            long thiz,
            int stgmAccess)
        throws HResultException;

    public static native long IMMDevice_QueryInterface(long thiz, String iid)
        throws HResultException;

    public static native void IMMDevice_Release(long thiz);

    public static native int IMMDeviceCollection_GetCount(long thiz)
        throws HResultException;

    public static native long IMMDeviceCollection_Item(long thiz, int nDevice)
        throws HResultException;

    public static native void IMMDeviceCollection_Release(long thiz);

    public static native long IMMDeviceEnumerator_EnumAudioEndpoints(
            long thiz,
            int dataFlow,
            int dwStateMask)
        throws HResultException;

    public static native long IMMDeviceEnumerator_GetDevice(
            long thiz,
            String pwstrId)
        throws HResultException;

    public static native void IMMDeviceEnumerator_Release(long thiz);

    public static native int IMMEndpoint_GetDataFlow(long thiz)
        throws HResultException;

    public static native void IMMEndpoint_Release(long thiz);

    public static native String IPropertyStore_GetString(long thiz, long key)
        throws HResultException;

    public static native void IPropertyStore_Release(long thiz);

    public static native long PSPropertyKeyFromString(String pszString)
        throws HResultException;

    public static native void ResetEvent(long hEvent)
        throws HResultException;

    /**
     * Waits until the specified object is in the signaled state or the
     * specified time-out interval elapses.
     *
     * @param hHandle a <tt>HANDLE</tt> to the object to wait for
     * @param dwMilliseconds the time-out interval in milliseconds to wait. If a
     * nonzero value is specified, the function waits until the specified object
     * is signaled or the specified time-out interval elapses. If
     * <tt>dwMilliseconds</tt> is zero, the function does not enter a wait state
     * if the specified object is not signaled; it always returns immediately.
     * If <tt>dwMilliseconds</tt> is <tt>INFINITE</tt>, the function will return
     * only when the specified object is signaled.
     * @return one of the <tt>WAIT_XXX</tt> constant values defined by the
     * <tt>WASAPI</tt> class to indicate the event that caused the function to
     * return
     * @throws HResultException if the return value is {@link #WAIT_FAILED}
     */
    public static native int WaitForSingleObject(
            long hHandle,
            long dwMilliseconds)
        throws HResultException;

    public static native long WAVEFORMATEX_alloc();

    public static native void WAVEFORMATEX_fill(
            long thiz,
            char wFormatTag,
            char nChannels,
            int nSamplesPerSec,
            int nAvgBytesPerSec,
            char nBlockAlign,
            char wBitsPerSample,
            char cbSize);

    public static native char WAVEFORMATEX_getCbSize(long thiz);

    public static native int WAVEFORMATEX_getNAvgBytesPerSec(long thiz);

    public static native char WAVEFORMATEX_getNBlockAlign(long thiz);
//
    public static native char WAVEFORMATEX_getNChannels(long thiz);
//
    public static native int WAVEFORMATEX_getNSamplesPerSec(long thiz);
//
    public static native char WAVEFORMATEX_getWBitsPerSample(long thiz);
//
    public static native char WAVEFORMATEX_getWFormatTag(long thiz);
//
    public static native void WAVEFORMATEX_setCbSize(long thiz, char cbSize);

    public static native void WAVEFORMATEX_setNAvgBytesPerSec(
            long thiz,
            int nAvgBytesPerSec);

    public static native void WAVEFORMATEX_setNBlockAlign(
            long thiz,
            char nBlockAlign);

    public static native void WAVEFORMATEX_setNChannels(
            long thiz,
            char nChannels);

    public static native void WAVEFORMATEX_setNSamplesPerSec(
            long thiz,
            int nSamplesPerSec);

    public static native void WAVEFORMATEX_setWBitsPerSample(
            long thiz,
            char wBitsPerSample);

    public static native void WAVEFORMATEX_setWFormatTag(
            long thiz,
            char wFormatTag);

    public static native int WAVEFORMATEX_sizeof();

    /** Prevents the initialization of <tt>WASAPI</tt> instances. */
    private WASAPI() {}
}
