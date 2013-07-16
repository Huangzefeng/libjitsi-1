/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi;

import org.jitsi.util.*;

/**
 * Defines the  interface to Windows Audio Session API (WASAPI) and
 * related Core Audio APIs such as Multimedia Device (MMDevice) API as used by
 * <tt>WASAPISystem</tt> and its associated <tt>CaptureDevice</tt>,
 * <tt>DataSource</tt> and <tt>Renderer</tt> implementations.
 *
 * @author Lyubomir Marinov
 */
public class WASAPIWithLogging
{
    private static final Logger logger = Logger.getLogger(WASAPIWithLogging.class);
    
    public static int AUDCLNT_E_NOT_STOPPED;

    public static final int AUDCLNT_SHAREMODE_SHARED = 0;

    public static final int AUDCLNT_STREAMFLAGS_EVENTCALLBACK = 0x00040000;

    public static final int AUDCLNT_STREAMFLAGS_NOPERSIST = 0x00080000;

    public static final int CLSCTX_ALL
        = /* CLSCTX_INPROC_SERVER */ 0x1
            | /* CLSCTX_INPROC_HANDLER */ 0x2
            | /* CLSCTX_LOCAL_SERVER */ 0x4
            | /* CLSCTX_REMOTE_SERVER */ 0x10;

    public static final String CLSID_MMDeviceEnumerator
        = "{bcde0395-e52f-467c-8e3d-c4579291692e}";

    public static final int COINIT_MULTITHREADED = 0x0;

    public static final int DEVICE_STATE_ACTIVE = 0x1;

    public static final int eAll = 2;

    public static final int eCapture = 1;

    public static final int eRender = 0;

    static final int FACILIY_AUDCLNT = 0x889;

    public static final String IID_IAudioCaptureClient
        = "{c8adbd64-e71e-48a0-a4de-185c395cd317}";

    public static final String IID_IAudioClient
        = "{1cb9ad4c-dbfa-4c32-b178-c2f568a703b2}";

    public static final String IID_IAudioRenderClient
        = "{f294acfc-3146-4483-a7bf-addca7c260e2}";

    public static final String IID_IMMDeviceEnumerator
        = "{a95664d2-9614-4f35-a746-de8db63617e6}";

    public static final String IID_IMMEndpoint
        = "{1be09788-6894-4089-8586-9a2a6c265ac5}";

    public static long PKEY_Device_FriendlyName;

    public static final int RPC_E_CHANGED_MODE = 0x80010106;

    public static final int S_FALSE = 1;

    public static final int S_OK = 0;

    static final int SEVERITY_ERROR = 1;

    public static final int STGM_READ = 0x0;

    /**
     * The return value of {@link #WaitForSingleObject(long, long)} which
     * indicates that the specified object is a mutex that was not released by
     * the thread that owned the mutex before the owning thread terminated.
     * Ownership of the mutex is granted to the calling thread and the mutex
     * state is set to non-signaled.
     */
    public static final int WAIT_ABANDONED = 0x00000080;

    /**
     * The return value of {@link #WaitForSingleObject(long, long)} which
     * indicates that the function has failed. Normally, the function will throw
     * an {@link HResultException} in the case and
     * {@link HResultException#getHResult()} will return <tt>WAIT_FAILED</tt>.
     */
    public static final int WAIT_FAILED = 0xffffffff;

    /**
     * The return value of {@link #WaitForSingleObject(long, long)} which
     * indicates that the specified object is signaled.
     */
    public static final int WAIT_OBJECT_0 = 0x00000000;

    /**
     * The return value of {@link #WaitForSingleObject(long, long)} which
     * indicates that the specified time-out interval has elapsed and the state
     * of the specified object is non-signaled.
     */
    public static final int WAIT_TIMEOUT = 0x00000102;

    public static final char WAVE_FORMAT_PCM = 1;

    static
    {
        System.loadLibrary("jnwasapi");

        AUDCLNT_E_NOT_STOPPED
            = MAKE_HRESULT(SEVERITY_ERROR, FACILIY_AUDCLNT, 5);

        /*
         * XXX The pointer to  memory returned by PSPropertyKeyFromString
         * is to be freed via CoTaskMemFree.
         */
        try
        {
            PKEY_Device_FriendlyName
                = PSPropertyKeyFromString(
                        "{a45c254e-df1c-4efd-8020-67d146a850e0} 14");
            if (PKEY_Device_FriendlyName == 0)
                throw new IllegalStateException("PKEY_Device_FriendlyName");
        }
        catch (HResultException hre)
        {
            logger.error("PSPropertyKeyFromString", hre);

            throw new RuntimeException(hre);
        }
    }
    
    public static void CloseHandle(long hObject) throws HResultException
    {
        logger.trace(String.format("CloseHandle: %s", hObject));
        WASAPI.CloseHandle(hObject);
    }

    public static String CoCreateGuid() throws HResultException
    {
        logger.trace("CoCreateGuid");
        String coCreateGuid = WASAPI.CoCreateGuid();
        logger.trace(String.format("CoCreateGuid returned: %s", coCreateGuid));
        
        return coCreateGuid;
    }

    public static long CoCreateInstance(String clsid, long pUnkOuter,
            int dwClsContext, String iid) throws HResultException
    {
        logger.trace(String.format("CoCreateInstance: %s, %s, %s, %s", clsid, pUnkOuter, dwClsContext, iid));
        long coCreateInstance = WASAPI.CoCreateInstance(clsid, pUnkOuter, dwClsContext, iid);
        logger.trace(String.format("coCreateInstance returned: %s", coCreateInstance));
        return coCreateInstance;
    }

    public static int CoInitializeEx(long pvReserved, int dwCoInit)
            throws HResultException
    {
        logger.trace(String.format("CoInitializeEx: %s, %s", pvReserved, dwCoInit));
        int coInitializeEx = WASAPI.CoInitializeEx(pvReserved, dwCoInit);
        logger.trace(String.format("coInitializeEx returned: %s", coInitializeEx));
        return coInitializeEx;
    }

    public static void CoTaskMemFree(long pv)
    {
        logger.trace(String.format("CoTaskMemFree: %s", pv));
        WASAPI.CoTaskMemFree(pv);
    }

    // public static void CoUninitialize();

    public static long CreateEvent(long lpEventAttributes,
            boolean bManualReset, boolean bInitialState, String lpName)
            throws HResultException
    {
        logger.trace(String.format("CreateEvent: %s, %s, %s, %s", lpEventAttributes, bManualReset,
                bInitialState, lpName)); 
        long createEvent = WASAPI.CreateEvent(lpEventAttributes, bManualReset,
                bInitialState, lpName);
        logger.trace(String.format("createEvent returned: %s", createEvent));
        return createEvent;
    }

    /**
     * Determines whether a specific <tt>HRESULT</tt> value indicates failure.
     * 
     * @param hresult
     *            the <tt>HRESULT</tt> value to be checked whether it indicates
     *            failure
     * @return <tt>true</tt> if the specified <tt>hresult</tt> indicates
     *         failure; otherwise, <tt>false</tt>
     */
    public static boolean FAILED(int hresult)
    {
        return (hresult < 0);
    }

    public static int IAudioCaptureClient_GetNextPacketSize(long thiz)
            throws HResultException
    {
        logger.trace(String.format("IAudioCaptureClient_GetNextPacketSize: %s", thiz));
        int iAudioCaptureClient_GetNextPacketSize = WASAPI.IAudioCaptureClient_GetNextPacketSize(thiz);
        logger.trace(String.format("iAudioCaptureClient_GetNextPacketSize returned: %s", iAudioCaptureClient_GetNextPacketSize));
        return iAudioCaptureClient_GetNextPacketSize;
    }

    public static int IAudioCaptureClient_Read(long thiz, byte[] data,
            int offset, int length, int srcSampleSize, int srcChannels,
            int dstSampleSize, int dstChannels) throws HResultException
    {
        logger.trace(String.format("IAudioCaptureClient_Read: %s, %s, %s, %s, %s, %s, %s, %s", thiz, data, offset, length,
                srcSampleSize, srcChannels, dstSampleSize, dstChannels)); 
        int iAudioCaptureClient_Read = WASAPI.IAudioCaptureClient_Read(thiz, data, offset, length,
                srcSampleSize, srcChannels, dstSampleSize, dstChannels);
        logger.trace(String.format("iAudioCaptureClient_Read returned: %s", iAudioCaptureClient_Read));
        return iAudioCaptureClient_Read;
    }

    public static void IAudioCaptureClient_Release(long thiz)
    {
        logger.trace(String.format("IAudioCaptureClient_Release: %s", thiz));
        WASAPI.IAudioCaptureClient_Release(thiz);
    }

    public static int IAudioClient_GetBufferSize(long thiz)
            throws HResultException
    {
        logger.trace(String.format("IAudioClient_GetBufferSize: %s", thiz));
        int iAudioClient_GetBufferSize = WASAPI.IAudioClient_GetBufferSize(thiz);
        logger.trace(String.format("iAudioClient_GetBufferSize returned: %s", iAudioClient_GetBufferSize));
        return iAudioClient_GetBufferSize;
    }

    public static int IAudioClient_GetCurrentPadding(long thiz)
            throws HResultException
    {
        logger.trace(String.format("IAudioClient_GetCurrentPadding %s", thiz));
        int iAudioClient_GetCurrentPadding = WASAPI.IAudioClient_GetCurrentPadding(thiz);
        logger.trace(String.format("iAudioClient_GetCurrentPadding returned: %s", iAudioClient_GetCurrentPadding));
        return iAudioClient_GetCurrentPadding;
    }

    public static long IAudioClient_GetDefaultDevicePeriod(long thiz)
            throws HResultException
    {
        logger.trace(String.format("IAudioClient_GetDefaultDevicePeriod: %s", thiz));
        long iAudioClient_GetDefaultDevicePeriod = WASAPI.IAudioClient_GetDefaultDevicePeriod(thiz);
        logger.trace(String.format("iAudioClient_GetDefaultDevicePeriod returned: %s", iAudioClient_GetDefaultDevicePeriod));
        return iAudioClient_GetDefaultDevicePeriod;
    }

    // public static long IAudioClient_GetMinimumDevicePeriod(long thiz)
    // throws HResultException;

    public static long IAudioClient_GetService(long thiz, String iid)
            throws HResultException
    {
        logger.trace(String.format("IAudioClient_GetService: %s, %s", thiz, iid));
        long iAudioClient_GetService = WASAPI.IAudioClient_GetService(thiz, iid);
        logger.trace(String.format("iAudioClient_GetService returned: %s", iAudioClient_GetService));
        return iAudioClient_GetService;
    }

    public static int IAudioClient_Initialize(long thiz, int shareMode,
            int streamFlags, long hnsBufferDuration, long hnsPeriodicity,
            long pFormat, String audioSessionGuid) throws HResultException
    {
        logger.trace(String.format("IAudioClient_Initialize: %s, %s, %s, %s, %s, %s, %s", thiz, shareMode, streamFlags,
                hnsBufferDuration, hnsPeriodicity, pFormat, audioSessionGuid));
        int iAudioClient_Initialize = WASAPI.IAudioClient_Initialize(thiz, shareMode, streamFlags,
                hnsBufferDuration, hnsPeriodicity, pFormat, audioSessionGuid);
        logger.trace(String.format("iAudioClient_Initialize returned: %s", iAudioClient_Initialize));
        return iAudioClient_Initialize;
    }

    public static long IAudioClient_IsFormatSupported(long thiz, int shareMode,
            long pFormat) throws HResultException
    {
        logger.trace(String.format("IAudioClient_IsFormatSupported: %s, %s, %s", thiz, shareMode, pFormat));
        long iAudioClient_IsFormatSupported = WASAPI.IAudioClient_IsFormatSupported(thiz, shareMode, pFormat);
        logger.trace(String.format("iAudioClient_IsFormatSupported returned: %s", iAudioClient_IsFormatSupported));
        return iAudioClient_IsFormatSupported;
    }

    public static void IAudioClient_Release(long thiz)
    {
        logger.trace(String.format("IAudioClient_Release: %s", thiz));
        WASAPI.IAudioClient_Release(thiz);
    }

    public static void IAudioClient_SetEventHandle(long thiz, long eventHandle)
            throws HResultException
    {
        logger.trace(String.format("IAudioClient_SetEventHandle: %s, %s", thiz, eventHandle));
        WASAPI.IAudioClient_SetEventHandle(thiz, eventHandle);
    }

    public static int IAudioClient_Start(long thiz) throws HResultException
    {
        logger.trace(String.format("IAudioClient_Start: %s", thiz));
        int iAudioClient_Start = WASAPI.IAudioClient_Start(thiz);
        logger.trace(String.format("iAudioClient_Start returned: %s", iAudioClient_Start));
        return iAudioClient_Start;
    }

    public static int IAudioClient_Stop(long thiz) throws HResultException
    {
        logger.trace(String.format("IAudioClient_Stop: %s", thiz));
        int iAudioClient_Stop = WASAPI.IAudioClient_Stop(thiz);
        logger.trace(String.format("iAudioClient_Stop returned: %s", iAudioClient_Stop));
        return iAudioClient_Stop;
    }

    public static void IAudioRenderClient_Release(long thiz)
    {
        logger.trace(String.format("IAudioRenderClient_Release: %s", thiz));
        WASAPI.IAudioRenderClient_Release(thiz);
    }

    /**
     * Writes specific audio data into the rendering endpoint buffer of a
     * specific <tt>IAudioRenderClient</tt>. If the sample sizes and/or the
     * numbers of channels of the specified audio <tt>data</tt> and the
     * specified rendering endpoint buffer differ, the method may be able to
     * perform the necessary conversions.
     * 
     * @param thiz
     *            the <tt>IAudioRenderClient</tt> which abstracts the rendering
     *            endpoint buffer into which the specified audio <tt>data</tt>
     *            is to be written
     * @param data
     *            the bytes of the audio samples to be written into the
     *            specified rendering endpoint buffer
     * @param offset
     *            the offset in bytes within <tt>data</tt> at which valid audio
     *            samples begin
     * @param length
     *            the number of bytes of valid audio samples in <tt>data</tt>
     * @param srcSampleSize
     *            the size in bytes of an audio sample in <tt>data</tt>
     * @param srcChannels
     *            the number of channels of the audio signal provided in
     *            <tt>data</tt>
     * @param dstSampleSize
     *            the size in bytes of an audio sample in the rendering endpoint
     *            buffer
     * @param dstChannels
     *            the number of channels with which the rendering endpoint
     *            buffer has been initialized
     * @return the number of bytes which have been read from <tt>data</tt>
     *         (beginning at <tt>offset</tt>, of course) and successfully
     *         written into the rendering endpoint buffer
     * @throws HResultException
     *             if an HRESULT value indicating an error is returned by a
     *             function invoked by the method implementation or an I/O error
     *             is encountered during the execution of the method
     */
    public static int IAudioRenderClient_Write(long thiz, byte[] data,
            int offset, int length, int srcSampleSize, int srcChannels,
            int dstSampleSize, int dstChannels) throws HResultException
    {
        logger.trace(String.format("IAudioRenderClient_Write: %s, %s, %s, %s, %s, %s, %s, %s", thiz, data, offset, length,
                srcSampleSize, srcChannels, dstSampleSize, dstChannels));
        int iAudioRenderClient_Write = WASAPI.IAudioRenderClient_Write(thiz, data, offset, length,
                srcSampleSize, srcChannels, dstSampleSize, dstChannels);
        logger.trace(String.format("iAudioRenderClient_Write returned: %s", iAudioRenderClient_Write));
        return iAudioRenderClient_Write;
    }

    public static long IMMDevice_Activate(long thiz, String iid, int dwClsCtx,
            long pActivationParams) throws HResultException
    {
        logger.trace(String.format("IMMDevice_Activate: %s, %s, %s, %s", thiz, iid, dwClsCtx, pActivationParams));
        long immDevice_Activate = WASAPI
                .IMMDevice_Activate(thiz, iid, dwClsCtx, pActivationParams);
        logger.trace(String.format("immDevice_Activate returned: %s", immDevice_Activate));
        return immDevice_Activate;
    }

    public static String IMMDevice_GetId(long thiz) throws HResultException
    {
        logger.trace(String.format("IMMDevice_GetId: %s", thiz));
        String immDevice_GetId = WASAPI.IMMDevice_GetId(thiz);
        return immDevice_GetId;
    }

    //
    // public static int IMMDevice_GetState(long thiz)
    // throws HResultException;
    //
    public static long IMMDevice_OpenPropertyStore(long thiz, int stgmAccess)
            throws HResultException
    {
        logger.trace(String.format("IMMDevice_OpenPropertyStore: %s, %s", thiz, stgmAccess));
        long immDevice_OpenPropertyStore = WASAPI.IMMDevice_OpenPropertyStore(thiz, stgmAccess);
        logger.trace(String.format("immDevice_OpenPropertyStore returned: %s", immDevice_OpenPropertyStore));
        return immDevice_OpenPropertyStore;
    }

    public static long IMMDevice_QueryInterface(long thiz, String iid)
            throws HResultException
    {
        logger.trace(String.format("IMMDevice_QueryInterface: %s, %s", thiz, iid));
        long immDevice_QueryInterface = WASAPI.IMMDevice_QueryInterface(thiz, iid);
        logger.trace(String.format("immDevice_QueryInterface returned: %s", immDevice_QueryInterface));
        return immDevice_QueryInterface;
    }

    public static void IMMDevice_Release(long thiz)
    {
        logger.trace(String.format("IMMDevice_Release: %s", thiz));
        WASAPI.IMMDevice_Release(thiz);
    }

    public static int IMMDeviceCollection_GetCount(long thiz)
            throws HResultException
    {
        logger.trace(String.format("IMMDeviceCollection_GetCount: %s", thiz));
        int immDeviceCollection_GetCount = WASAPI.IMMDeviceCollection_GetCount(thiz);
        logger.trace(String.format("immDeviceCollection_GetCount returned: %s", immDeviceCollection_GetCount));
        return immDeviceCollection_GetCount;
    }

    public static long IMMDeviceCollection_Item(long thiz, int nDevice)
            throws HResultException
    {
        logger.trace(String.format("IMMDeviceCollection_Item: %s, %s", thiz, nDevice));
        long immDeviceCollection_Item = WASAPI.IMMDeviceCollection_Item(thiz, nDevice);
        logger.trace(String.format("immDeviceCollection_Item returned: %s", immDeviceCollection_Item));
        return immDeviceCollection_Item;
    }

    public static void IMMDeviceCollection_Release(long thiz)
    {
        logger.trace(String.format("IMMDeviceCollection_Release: %s", thiz));
        WASAPI.IMMDeviceCollection_Release(thiz);
    }

    public static long IMMDeviceEnumerator_EnumAudioEndpoints(long thiz,
            int dataFlow, int dwStateMask) throws HResultException
    {
        logger.trace(String.format("IMMDeviceEnumerator_EnumAudioEndpoints: %s, %s, %s", thiz, dataFlow, dwStateMask));
        long immDeviceEnumerator_EnumAudioEndpoints = WASAPI.IMMDeviceEnumerator_EnumAudioEndpoints(thiz, dataFlow,
                dwStateMask);
        logger.trace(String.format("immDeviceEnumerator_EnumAudioEndpoints returned: %s", immDeviceEnumerator_EnumAudioEndpoints));
        return immDeviceEnumerator_EnumAudioEndpoints;
    }

    public static long IMMDeviceEnumerator_GetDevice(long thiz, String pwstrId)
            throws HResultException
    {
        logger.trace(String.format("IMMDeviceEnumerator_GetDevice: %s, %s", thiz, pwstrId));
        long immDeviceEnumerator_GetDevice = WASAPI.IMMDeviceEnumerator_GetDevice(thiz, pwstrId);
        logger.trace(String.format("immDeviceEnumerator_GetDevice returned: %s", immDeviceEnumerator_GetDevice));
        return immDeviceEnumerator_GetDevice;
    }

    public static void IMMDeviceEnumerator_Release(long thiz)
    {
        logger.trace(String.format("IMMDeviceEnumerator_Release: %s", thiz));
        WASAPI.IMMDeviceEnumerator_Release(thiz);
    }

    public static int IMMEndpoint_GetDataFlow(long thiz)
            throws HResultException
    {
        logger.trace(String.format("IMMEndpoint_GetDataFlow: %s", thiz));
        int immEndpoint_GetDataFlow = WASAPI.IMMEndpoint_GetDataFlow(thiz);
        logger.trace(String.format("immEndpoint_GetDataFlow returned: %s", immEndpoint_GetDataFlow));
        return immEndpoint_GetDataFlow;
    }

    public static void IMMEndpoint_Release(long thiz)
    {
        logger.trace(String.format("IMMEndpoint_Release: %s", thiz));
        WASAPI.IMMEndpoint_Release(thiz);
    }

    public static String IPropertyStore_GetString(long thiz, long key)
            throws HResultException
    {
        logger.trace(String.format("IPropertyStore_GetString: %s, %s", thiz, key));
        String iPropertyStore_GetString = WASAPI.IPropertyStore_GetString(thiz, key);
        logger.trace(String.format("iPropertyStore_GetString returned: %s", iPropertyStore_GetString));
        return iPropertyStore_GetString;
    }

    public static void IPropertyStore_Release(long thiz)
    {
        logger.trace(String.format("IPropertyStore_Release: %s", thiz));
        WASAPI.IPropertyStore_Release(thiz);
    }

    static int MAKE_HRESULT(int sev, int fac, int code)
    {
        return ((sev & 0x1) << 31) | ((fac & 0x7fff) << 16) | (code & 0xffff);
    }

    public static long PSPropertyKeyFromString(String pszString)
            throws HResultException
    {
        logger.trace(String.format("PSPropertyKeyFromString: %s", pszString));
        long psPropertyKeyFromString = WASAPI.PSPropertyKeyFromString(pszString);
        logger.trace(String.format("psPropertyKeyFromString returned: %s", psPropertyKeyFromString));
        return psPropertyKeyFromString;
    }

    //
    // public static void ResetEvent(long hEvent)
    // throws HResultException;

    /**
     * Waits until the specified object is in the signaled state or the
     * specified time-out interval elapses.
     * 
     * @param hHandle
     *            a <tt>HANDLE</tt> to the object to wait for
     * @param dwMilliseconds
     *            the time-out interval in milliseconds to wait. If a nonzero
     *            value is specified, the function waits until the specified
     *            object is signaled or the specified time-out interval elapses.
     *            If <tt>dwMilliseconds</tt> is zero, the function does not
     *            enter a wait state if the specified object is not signaled; it
     *            always returns immediately. If <tt>dwMilliseconds</tt> is
     *            <tt>INFINITE</tt>, the function will return only when the
     *            specified object is signaled.
     * @return one of the <tt>WAIT_XXX</tt> constant values defined by the
     *         <tt>WASAPI</tt> class to indicate the event that caused the
     *         function to return
     * @throws HResultException
     *             if the return value is {@link #WAIT_FAILED}
     */
    public static int WaitForSingleObject(long hHandle, long dwMilliseconds)
            throws HResultException
    {
        logger.trace(String.format("WaitForSingleObject: %s, %s", hHandle, dwMilliseconds));
        int waitForSingleObject = WASAPI.WaitForSingleObject(hHandle, dwMilliseconds);
        logger.trace(String.format("waitForSingleObject returned: %s", waitForSingleObject));
        return waitForSingleObject;
    }

    public static long WAVEFORMATEX_alloc()
    {
        logger.trace(String.format("WAVEFORMATEX_alloc:"));
        long waveformatex_alloc = WASAPI.WAVEFORMATEX_alloc();
        logger.trace(String.format("waveformatex_alloc returned: %s", waveformatex_alloc));
        return waveformatex_alloc;
    }

    public static void WAVEFORMATEX_fill(long thiz, char wFormatTag,
            char nChannels, int nSamplesPerSec, int nAvgBytesPerSec,
            char nBlockAlign, char wBitsPerSample, char cbSize)
    {
        logger.trace(String.format("WAVEFORMATEX_fill: %s, %s, %s, %s, %s, %s, %s, %s", thiz, wFormatTag, nChannels, nSamplesPerSec,
                nAvgBytesPerSec, nBlockAlign, wBitsPerSample, cbSize));
        WASAPI.WAVEFORMATEX_fill(thiz, wFormatTag, nChannels, nSamplesPerSec,
                nAvgBytesPerSec, nBlockAlign, wBitsPerSample, cbSize);
    }

    // public static char WAVEFORMATEX_getCbSize(long thiz);
    //
    // public static int WAVEFORMATEX_getNAvgBytesPerSec(long thiz);
    //
    // public static char WAVEFORMATEX_getNBlockAlign(long thiz);
    //
    public static char WAVEFORMATEX_getNChannels(long thiz)
    {
        logger.trace(String.format("WAVEFORMATEX_getNChannels: %s", thiz));
        char waveformatex_getNChannels = WASAPI.WAVEFORMATEX_getNChannels(thiz);
        logger.trace(String.format("waveformatex_getNChannels returned: %s", waveformatex_getNChannels));
        return waveformatex_getNChannels;
    }

    //
    public static int WAVEFORMATEX_getNSamplesPerSec(long thiz)
    {
        logger.trace(String.format("WAVEFORMATEX_getNSamplesPerSec: %s", thiz));
        int waveformatex_getNSamplesPerSec = WASAPI.WAVEFORMATEX_getNSamplesPerSec(thiz);
        logger.trace(String.format("waveformatex_getNSamplesPerSec returned: %s", waveformatex_getNSamplesPerSec));
        return waveformatex_getNSamplesPerSec;
    }

    //
    public static char WAVEFORMATEX_getWBitsPerSample(long thiz)
    {
        logger.trace(String.format("WAVEFORMATEX_getWBitsPerSample: %s", thiz));
        char waveformatex_getWBitsPerSample = WASAPI.WAVEFORMATEX_getWBitsPerSample(thiz);
        logger.trace(String.format("waveformatex_getWBitsPerSample returned: %s", waveformatex_getWBitsPerSample));
        return waveformatex_getWBitsPerSample;
    }

    //
    public static char WAVEFORMATEX_getWFormatTag(long thiz)
    {
        logger.trace(String.format("WAVEFORMATEX_getWFormatTag: %s", thiz));
        char waveformatex_getWFormatTag = WASAPI.WAVEFORMATEX_getWFormatTag(thiz);
        logger.trace(String.format("waveformatex_getWFormatTag returned: %s", waveformatex_getWFormatTag));
        return waveformatex_getWFormatTag;
    }

    //
    // public static void WAVEFORMATEX_setCbSize(long thiz, char cbSize);

    // public static void WAVEFORMATEX_setNAvgBytesPerSec(
    // long thiz,
    // int nAvgBytesPerSec);
    //
    // public static void WAVEFORMATEX_setNBlockAlign(
    // long thiz,
    // char nBlockAlign);
    //
    // public static void WAVEFORMATEX_setNChannels(
    // long thiz,
    // char nChannels);
    //
    // public static void WAVEFORMATEX_setNSamplesPerSec(
    // long thiz,
    // int nSamplesPerSec);
    //
    // public static void WAVEFORMATEX_setWBitsPerSample(
    // long thiz,
    // char wBitsPerSample);

    // public static void WAVEFORMATEX_setWFormatTag(
    // long thiz,
    // char wFormatTag);

    public static int WAVEFORMATEX_sizeof()
    {
        logger.trace(String.format("WAVEFORMATEX_sizeof:"));
        int waveformatex_sizeof = WASAPI.WAVEFORMATEX_sizeof();
        logger.trace(String.format("waveformatex_sizeof returned: %s", waveformatex_sizeof));
        return waveformatex_sizeof;
    }

    /** Prevents the initialization of <tt>WASAPI</tt> instances. */
    private WASAPIWithLogging()
    {
    }
}
