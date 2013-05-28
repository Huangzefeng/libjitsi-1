/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.media.*;
import javax.media.format.*;
import javax.swing.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.control.*;
import org.jitsi.impl.neomedia.jmfext.media.renderer.audio.*;
import org.jitsi.impl.neomedia.portaudio.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * Creates PortAudio capture devices by enumerating all host devices that have
 * input channels.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class PortAudioSystem
    extends AudioSystem
{
    /**
     * Represents a listener which is to be notified before and after
     * PortAudio's native function <tt>Pa_UpdateAvailableDeviceList()</tt> is
     * invoked.
     */
    public interface PaUpdateAvailableDeviceListListener
        extends EventListener
    {
        /**
         * Notifies this listener that PortAudio's native function
         * <tt>Pa_UpdateAvailableDeviceList()</tt> was invoked.
         *
         * @throws Exception if this implementation encounters an error. Any
         * <tt>Throwable</tt> apart from <tt>ThreadDeath</tt> will be ignored
         * after it is logged for debugging purposes.
         */
        void didPaUpdateAvailableDeviceList()
            throws Exception;

        /**
         * Notifies this listener that PortAudio's native function
         * <tt>Pa_UpdateAvailableDeviceList()</tt> will be invoked.
         *
         * @throws Exception if this implementation encounters an error. Any
         * <tt>Throwable</tt> apart from <tt>ThreadDeath</tt> will be ignored
         * after it is logged for debugging purposes.
         */
        void willPaUpdateAvailableDeviceList()
            throws Exception;
    }

    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying PortAudio
     * <tt>CaptureDevice</tt>s
     */
    private static final String LOCATOR_PROTOCOL = LOCATOR_PROTOCOL_PORTAUDIO;

    /**
     * The <tt>Logger</tt> used by the <tt>PortAudioSystem</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(PortAudioSystem.class);

    /**
     * The number of times that {@link #willPaOpenStream()} has been
     * invoked without an intervening {@link #didPaOpenStream()} i.e. the
     * number of PortAudio clients which are currently executing
     * <tt>Pa_OpenStream</tt> and which are thus inhibiting
     * <tt>Pa_UpdateAvailableDeviceList</tt>.
     */
    private static int paOpenStream = 0;

    /**
     * The <tt>Object</tt> which synchronizes that access to
     * {@link #paOpenStream} and {@link #paUpdateAvailableDeviceList}.
     */
    private static final Object paOpenStreamSyncRoot = new Object();

    /**
     * The number of times that {@link #willPaUpdateAvailableDeviceList()}
     * has been invoked without an intervening
     * {@link #didPaUpdateAvailableDeviceList()} i.e. the number of
     * PortAudio clients which are currently executing
     * <tt>Pa_UpdateAvailableDeviceList</tt> and which are thus inhibiting
     * <tt>Pa_OpenStream</tt>.
     */
    private static int paUpdateAvailableDeviceList = 0;

    /**
     * The list of <tt>PaUpdateAvailableDeviceListListener</tt>s which are to be
     * notified before and after PortAudio's native function
     * <tt>Pa_UpdateAvailableDeviceList()</tt> is invoked.
     */
    private static final List<WeakReference<PaUpdateAvailableDeviceListListener>>
        paUpdateAvailableDeviceListListeners
            = new LinkedList<WeakReference<PaUpdateAvailableDeviceListListener>>();

    /**
     * The <tt>Object</tt> which ensures that PortAudio's native function
     * <tt>Pa_UpdateAvailableDeviceList()</tt> will not be invoked concurrently.
     * The condition should hold true on the native side but, anyway, it shoul
     * not hurt (much) to enforce it on the Java side as well.
     */
    private static final Object paUpdateAvailableDeviceListSyncRoot
        = new Object();

    /**
     * Adds a listener which is to be notified before and after PortAudio's
     * native function <tt>Pa_UpdateAvailableDeviceList()</tt> is invoked.
     * <p>
     * <b>Note</b>: The <tt>PortAudioSystem</tt> class keeps a
     * <tt>WeakReference</tt> to the specified <tt>listener</tt> in order to
     * avoid memory leaks.
     * </p>
     *
     * @param listener the <tt>PaUpdateAvailableDeviceListListener</tt> to be
     * notified before and after PortAudio's native function
     * <tt>Pa_UpdateAvailableDeviceList()</tt> is invoked
     */
    public static void addPaUpdateAvailableDeviceListListener(
            PaUpdateAvailableDeviceListListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (paUpdateAvailableDeviceListListeners)
        {
            Iterator<WeakReference<PaUpdateAvailableDeviceListListener>> i
                = paUpdateAvailableDeviceListListeners.iterator();
            boolean add = true;

            while (i.hasNext())
            {
                PaUpdateAvailableDeviceListListener l = i.next().get();

                if (l == null)
                    i.remove();
                else if (l.equals(listener))
                    add = false;
            }
            if (add)
            {
                paUpdateAvailableDeviceListListeners.add(
                        new WeakReference<PaUpdateAvailableDeviceListListener>(
                                listener));
            }
        }
    }

    /**
     * Notifies <tt>PortAudioSystem</tt> that a PortAudio client finished
     * executing <tt>Pa_OpenStream</tt>.
     */
    public static void didPaOpenStream()
    {
        synchronized (paOpenStreamSyncRoot)
        {
            paOpenStream--;
            if (paOpenStream < 0)
                paOpenStream = 0;

            paOpenStreamSyncRoot.notifyAll();
        }
    }

    /**
     * Notifies <tt>PortAudioSystem</tt> that a PortAudio client finished
     * executing <tt>Pa_UpdateAvailableDeviceList</tt>.
     */
    private static void didPaUpdateAvailableDeviceList()
    {
        synchronized (paOpenStreamSyncRoot)
        {
            paUpdateAvailableDeviceList--;
            if (paUpdateAvailableDeviceList < 0)
                paUpdateAvailableDeviceList = 0;

            paOpenStreamSyncRoot.notifyAll();
        }

        firePaUpdateAvailableDeviceListEvent(false);
    }

    /**
     * Notifies the registered <tt>PaUpdateAvailableDeviceListListener</tt>s
     * that PortAudio's native function <tt>Pa_UpdateAvailableDeviceList()</tt>
     * will be or was invoked.
     *
     * @param will <tt>true</tt> if PortAudio's native function
     * <tt>Pa_UpdateAvailableDeviceList()</tt> will be invoked or <tt>false</tt>
     * if it was invoked
     */
    private static void firePaUpdateAvailableDeviceListEvent(boolean will)
    {
        try
        {
            List<WeakReference<PaUpdateAvailableDeviceListListener>> ls;

            synchronized (paUpdateAvailableDeviceListListeners)
            {
                ls
                    = new ArrayList<WeakReference<PaUpdateAvailableDeviceListListener>>(
                            paUpdateAvailableDeviceListListeners);
            }

            for (WeakReference<PaUpdateAvailableDeviceListListener> wr : ls)
            {
                PaUpdateAvailableDeviceListListener l = wr.get();

                if (l != null)
                {
                    try
                    {
                        if (will)
                            l.willPaUpdateAvailableDeviceList();
                        else
                            l.didPaUpdateAvailableDeviceList();
                    }
                    catch (Throwable t)
                    {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                        else
                        {
                            logger.error(
                                    "PaUpdateAvailableDeviceListListener."
                                        + (will ? "will" : "did")
                                        + "PaUpdateAvailableDeviceList failed.",
                                    t);
                        }
                    }
                }
            }
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }
    }

    /**
     * Gets a sample rate supported by a PortAudio device with a specific device
     * index with which it is to be registered with JMF.
     *
     * @param input <tt>true</tt> if the supported sample rate is to be retrieved for
     * the PortAudio device with the specified device index as an input device
     * or <tt>false</tt> for an output device
     * @param deviceIndex the device index of the PortAudio device for which a
     * supported sample rate is to be retrieved
     * @param channelCount number of channel
     * @param sampleFormat sample format
     * @return a sample rate supported by the PortAudio device with the
     * specified device index with which it is to be registered with JMF
     */
    private static double getSupportedSampleRate(
            boolean input,
            int deviceIndex,
            int channelCount,
            long sampleFormat)
    {
        long deviceInfo = Pa.GetDeviceInfo(deviceIndex);
        double supportedSampleRate;

        if (deviceInfo != 0)
        {
            double defaultSampleRate
                = Pa.DeviceInfo_getDefaultSampleRate(deviceInfo);

            if (defaultSampleRate >= MediaUtils.MAX_AUDIO_SAMPLE_RATE)
                supportedSampleRate = defaultSampleRate;
            else
            {
                long streamParameters
                    = Pa.StreamParameters_new(
                            deviceIndex,
                            channelCount,
                            sampleFormat,
                            Pa.LATENCY_UNSPECIFIED);

                if (streamParameters == 0)
                    supportedSampleRate = defaultSampleRate;
                else
                {
                    try
                    {
                        long inputParameters;
                        long outputParameters;

                        if (input)
                        {
                            inputParameters = streamParameters;
                            outputParameters = 0;
                        }
                        else
                        {
                            inputParameters = 0;
                            outputParameters = streamParameters;
                        }

                        boolean formatIsSupported
                            = Pa.IsFormatSupported(
                                    inputParameters,
                                    outputParameters,
                                    Pa.DEFAULT_SAMPLE_RATE);

                        supportedSampleRate
                            = formatIsSupported
                                ? Pa.DEFAULT_SAMPLE_RATE
                                : defaultSampleRate;
                    }
                    finally
                    {
                        Pa.StreamParameters_free(streamParameters);
                    }
                }
            }
        }
        else
            supportedSampleRate = Pa.DEFAULT_SAMPLE_RATE;
        return supportedSampleRate;
    }

    /**
     * Places a specific <tt>DiagnosticsControl</tt> under monitoring of its
     * functional health because of a malfunction in its procedure/process. The
     * monitoring will automatically cease after the procedure/process resumes
     * executing normally or is garbage collected.
     *
     * @param diagnosticsControl the <tt>DiagnosticsControl</tt> to be placed
     * under monitoring of its functional health because of a malfunction in its
     * procedure/process
     */
    public static void monitorFunctionalHealth(
            DiagnosticsControl diagnosticsControl)
    {
      DiagnosticsControlMonitor.monitorFunctionalHealth(diagnosticsControl);
    }

    public static void removePaUpdateAvailableDeviceListListener(
            PaUpdateAvailableDeviceListListener listener)
    {
        if (listener == null)
            return;

        synchronized (paUpdateAvailableDeviceListListeners)
        {
            Iterator<WeakReference<PaUpdateAvailableDeviceListListener>> i
                = paUpdateAvailableDeviceListListeners.iterator();

            while (i.hasNext())
            {
                PaUpdateAvailableDeviceListListener l = i.next().get();

                if ((l == null) || l.equals(listener))
                    i.remove();
            }
        }
    }

    /**
     * Waits for all PortAudio clients to finish executing
     * <tt>Pa_OpenStream</tt>.
     */
    private static void waitForPaOpenStream()
    {
        boolean interrupted = false;

        while (paOpenStream > 0)
        {
            try
            {
                paOpenStreamSyncRoot.wait();
            }
            catch (InterruptedException ie)
            {
                interrupted = true;
            }
        }
        if (interrupted)
            Thread.currentThread().interrupt();
    }

    /**
     * Waits for all PortAudio clients to finish executing
     * <tt>Pa_UpdateAvailableDeviceList</tt>.
     */
    private static void waitForPaUpdateAvailableDeviceList()
    {
        boolean interrupted = false;

        while (paUpdateAvailableDeviceList > 0)
        {
            try
            {
                paOpenStreamSyncRoot.wait();
            }
            catch (InterruptedException ie)
            {
                interrupted = true;
            }
        }
        if (interrupted)
            Thread.currentThread().interrupt();
    }

    /**
     * Notifies <tt>PortAudioSystem</tt> that a PortAudio client will start
     * executing <tt>Pa_OpenStream</tt>.
     */
    public static void willPaOpenStream()
    {
        synchronized (paOpenStreamSyncRoot)
        {
            waitForPaUpdateAvailableDeviceList();

            paOpenStream++;
            paOpenStreamSyncRoot.notifyAll();
        }
    }

    /**
     * Notifies <tt>PortAudioSystem</tt> that a PortAudio client will start
     * executing <tt>Pa_UpdateAvailableDeviceList</tt>.
     */
    private static void willPaUpdateAvailableDeviceList()
    {
        synchronized (paOpenStreamSyncRoot)
        {
            waitForPaOpenStream();

            paUpdateAvailableDeviceList++;
            paOpenStreamSyncRoot.notifyAll();
        }

        firePaUpdateAvailableDeviceListEvent(true);
    }

    private Runnable devicesChangedCallback;

    /**
     * Initializes a new <tt>PortAudioSystem</tt> instance which creates
     * PortAudio capture and playback devices by enumerating all host devices
     * with input channels.
     *
     * @throws Exception if anything wrong happens while creating the PortAudio
     * capture and playback devices
     */
    PortAudioSystem()
        throws Exception
    {
        super(
                LOCATOR_PROTOCOL,
                FEATURE_DENOISE
                    | FEATURE_ECHO_CANCELLATION
                    | FEATURE_NOTIFY_AND_PLAYBACK_DEVICES
                    | FEATURE_REINITIALIZE);
    }

    /**
     * Sorts a specific list of <tt>CaptureDeviceInfo2</tt>s so that the
     * ones representing USB devices appear at the beginning/top of the
     * specified list.
     *
     * @param devices the list of <tt>CaptureDeviceInfo2</tt>s to be
     * sorted so that the ones representing USB devices appear at the
     * beginning/top of the list
     */
    private void bubbleUpUsbDevices(List<CaptureDeviceInfo2> devices)
    {
        if (!devices.isEmpty())
        {
            List<CaptureDeviceInfo2> nonUsbDevices
                = new ArrayList<CaptureDeviceInfo2>(devices.size());

            for (Iterator<CaptureDeviceInfo2> i = devices.iterator();
                    i.hasNext();)
            {
                CaptureDeviceInfo2 d = i.next();

                if (!d.isSameTransportType("USB"))
                {
                    nonUsbDevices.add(d);
                    i.remove();
                }
            }
            if (!nonUsbDevices.isEmpty())
            {
                for (CaptureDeviceInfo2 d : nonUsbDevices)
                    devices.add(d);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize()
        throws Exception
    {
        logger.debug("doInitialise called");

        /*
         * If PortAudio fails to initialize because of, for example, a missing
         * native counterpart, it will throw an exception here and the PortAudio
         * Renderer will not be initialized.
         */
        int deviceCount = Pa.GetDeviceCount();
        int channels = 1;
        int sampleSizeInBits = 16;
        long sampleFormat = Pa.getPaSampleFormat(sampleSizeInBits);
        int defaultInputDeviceIndex = Pa.GetDefaultInputDevice();
        int defaultOutputDeviceIndex = Pa.GetDefaultOutputDevice();
        List<CaptureDeviceInfo2> captureAndPlaybackDevices
            = new LinkedList<CaptureDeviceInfo2>();
        List<CaptureDeviceInfo2> captureDevices
            = new LinkedList<CaptureDeviceInfo2>();
        List<CaptureDeviceInfo2> playbackDevices
            = new LinkedList<CaptureDeviceInfo2>();
        final boolean loggerIsDebugEnabled = logger.isDebugEnabled();

        if(CoreAudioDevice.isLoaded)
            CoreAudioDevice.initDevices();

        for (int deviceIndex = 0; deviceIndex < deviceCount; deviceIndex++)
        {
            long deviceInfo = Pa.GetDeviceInfo(deviceIndex);
            String name = Pa.DeviceInfo_getName(deviceInfo);

            if (name != null)
                name = name.trim();

            int maxInputChannels
                = Pa.DeviceInfo_getMaxInputChannels(deviceInfo);
            int maxOutputChannels
                = Pa.DeviceInfo_getMaxOutputChannels(deviceInfo);
            String transportType
                = Pa.DeviceInfo_getTransportType(deviceInfo);
            String deviceUID
                = Pa.DeviceInfo_getDeviceUID(deviceInfo);

            String modelIdentifier;
            String locatorRemainder;

            if (deviceUID == null)
            {
                modelIdentifier = null;
                locatorRemainder = name;
            }
            else
            {
                modelIdentifier
                    = CoreAudioDevice.isLoaded
                        ? CoreAudioDevice.getDeviceModelIdentifier(deviceUID)
                        : null;
                locatorRemainder = deviceUID;
            }

            /*
             * TODO The intention of reinitialize() was to perform the
             * initialization from scratch. However, AudioSystem was later
             * changed to disobey. But we should at least search through both
             * CAPTURE_INDEX and PLAYBACK_INDEX.
             */
            List<CaptureDeviceInfo2> existingCdis
                = getDevices(DataFlow.CAPTURE);
            CaptureDeviceInfo2 cdi = null;

            if (existingCdis != null)
            {
                for (CaptureDeviceInfo2 existingCdi : existingCdis)
                {
                    /*
                     * The deviceUID is optional so a device may be identified
                     * by deviceUID if it is available or by name if the
                     * deviceUID is not available.
                     */
                    String id = existingCdi.getIdentifier();

                    if (id.equals(deviceUID) || id.equals(name))
                    {
                        cdi = existingCdi;
                        break;
                    }
                }
            }
            if (cdi == null)
            {
                cdi
                    = new CaptureDeviceInfo2(
                            name,
                            new MediaLocator(
                                    LOCATOR_PROTOCOL + ":#" + locatorRemainder),
                            new Format[]
                            {
                                new AudioFormat(
                                        AudioFormat.LINEAR,
                                        (maxInputChannels > 0)
                                            ? getSupportedSampleRate(
                                                    true,
                                                    deviceIndex,
                                                    channels,
                                                    sampleFormat)
                                            : Pa.DEFAULT_SAMPLE_RATE,
                                        sampleSizeInBits,
                                        channels,
                                        AudioFormat.LITTLE_ENDIAN,
                                        AudioFormat.SIGNED,
                                        Format.NOT_SPECIFIED /* frameSizeInBits */,
                                        Format.NOT_SPECIFIED /* frameRate */,
                                        Format.byteArray)
                            },
                            deviceUID,
                            transportType,
                            modelIdentifier);
            }

            /*
             * When we perform automatic selection of capture and
             * playback/notify devices, we would like to pick up devices from
             * one and the same hardware because that sound like a natural
             * expectation from the point of view of the user. In order to
             * achieve that, we will bring the devices which support both
             * capture and playback to the top.
             */
            if (maxInputChannels > 0)
            {
                List<CaptureDeviceInfo2> devices;

                if (maxOutputChannels > 0)
                    devices = captureAndPlaybackDevices;
                else
                    devices = captureDevices;

                if ((deviceIndex == defaultInputDeviceIndex)
                        || ((maxOutputChannels > 0)
                                && (deviceIndex == defaultOutputDeviceIndex)))
                {
                    devices.add(0, cdi);
                    if (loggerIsDebugEnabled)
                        logger.debug("Added default capture device: " + name);
                }
                else
                {
                    devices.add(cdi);
                    if (loggerIsDebugEnabled)
                        logger.debug("Added capture device: " + name);
                }
                if (loggerIsDebugEnabled && (maxInputChannels > 0))
                {
                    if (deviceIndex == defaultOutputDeviceIndex)
                        logger.debug("Added default playback device: " + name);
                    else
                        logger.debug("Added playback device: " + name);
                }
            }
            else if (maxOutputChannels > 0)
            {
                if (deviceIndex == defaultOutputDeviceIndex)
                {
                    playbackDevices.add(0, cdi);
                    if (loggerIsDebugEnabled)
                        logger.debug("Added default playback device: " + name);
                }
                else
                {
                    playbackDevices.add(cdi);
                    if (loggerIsDebugEnabled)
                        logger.debug("Added playback device: " + name);
                }
            }
        }
        if(CoreAudioDevice.isLoaded)
            CoreAudioDevice.freeDevices();

        /*
         * Make sure that devices which support both capture and playback are
         * reported as such and are preferred over devices which support either
         * capture or playback (in order to achieve our goal to have automatic
         * selection pick up devices from one and the same hardware).
         */
        bubbleUpUsbDevices(captureDevices);
        bubbleUpUsbDevices(playbackDevices);
        if (!captureDevices.isEmpty() && !playbackDevices.isEmpty())
        {
            /*
             * Event if we have not been provided with the information regarding
             * the matching of the capture and playback/notify devices from one
             * and the same hardware, we may still be able to deduce it by
             * examining their names.
             */
            matchDevicesByName(captureDevices, playbackDevices);
        }
        /*
         * Of course, of highest reliability is the fact that a specific
         * instance supports both capture and playback.
         */
        if (!captureAndPlaybackDevices.isEmpty())
        {
            bubbleUpUsbDevices(captureAndPlaybackDevices);
            for (int i = captureAndPlaybackDevices.size() - 1; i >= 0; i--)
            {
                CaptureDeviceInfo2 cdi
                    = captureAndPlaybackDevices.get(i);

                captureDevices.add(0, cdi);
                playbackDevices.add(0, cdi);
            }
        }

        setCaptureDevices(captureDevices);
        setPlaybackDevices(playbackDevices);

        if (devicesChangedCallback == null)
        {
            devicesChangedCallback
                = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            logger.debug("devices changed callback runing");
                            reinitialize();
                        }
                        catch (Throwable t)
                        {
                            if (t instanceof ThreadDeath)
                                throw (ThreadDeath) t;

                            logger.warn(
                                    "Failed to reinitialize PortAudio devices",
                                    t);
                        }
                    }
                };
            Pa.setDevicesChangedCallback(devicesChangedCallback);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getRendererClassName()
    {
        return PortAudioRenderer.class.getName();
    }

    /**
     * Attempts to reorder specific lists of capture and playback/notify
     * <tt>CaptureDeviceInfo2</tt>s so that devices from the same
     * hardware appear at the same indices in the respective lists. The judgment
     * with respect to the belonging to the same hardware is based on the names
     * of the specified <tt>CaptureDeviceInfo2</tt>s. The implementation
     * is provided as a fallback to stand in for scenarios in which more
     * accurate relevant information is not available.
     *
     * @param captureDevices
     * @param playbackDevices
     */
    private void matchDevicesByName(
            List<CaptureDeviceInfo2> captureDevices,
            List<CaptureDeviceInfo2> playbackDevices)
    {
        Iterator<CaptureDeviceInfo2> captureIter
            = captureDevices.iterator();
        Pattern pattern
            = Pattern.compile(
                    "array|headphones|microphone|speakers|\\p{Space}|\\(|\\)",
                    Pattern.CASE_INSENSITIVE);
        LinkedList<CaptureDeviceInfo2> captureDevicesWithPlayback
            = new LinkedList<CaptureDeviceInfo2>();
        LinkedList<CaptureDeviceInfo2> playbackDevicesWithCapture
            = new LinkedList<CaptureDeviceInfo2>();
        int count = 0;

        while (captureIter.hasNext())
        {
            CaptureDeviceInfo2 captureDevice = captureIter.next();
            String captureName = captureDevice.getName();

            if (captureName != null)
            {
                captureName = pattern.matcher(captureName).replaceAll("");
                if (captureName.length() != 0)
                {
                    Iterator<CaptureDeviceInfo2> playbackIter
                        = playbackDevices.iterator();
                    CaptureDeviceInfo2 matchingPlaybackDevice = null;

                    while (playbackIter.hasNext())
                    {
                        CaptureDeviceInfo2 playbackDevice
                            = playbackIter.next();
                        String playbackName = playbackDevice.getName();

                        if (playbackName != null)
                        {
                            playbackName
                                = pattern
                                    .matcher(playbackName)
                                        .replaceAll("");
                            if (captureName.equals(playbackName))
                            {
                                playbackIter.remove();
                                matchingPlaybackDevice = playbackDevice;
                                break;
                            }
                        }
                    }
                    if (matchingPlaybackDevice != null)
                    {
                        captureIter.remove();
                        captureDevicesWithPlayback.add(captureDevice);
                        playbackDevicesWithCapture.add(
                                matchingPlaybackDevice);
                        count++;
                    }
                }
            }
        }

        for (int i = count - 1; i >= 0; i--)
        {
            captureDevices.add(0, captureDevicesWithPlayback.get(i));
            playbackDevices.add(0, playbackDevicesWithCapture.get(i));
        }
    }

    /**
     * Reinitializes this <tt>PortAudioSystem</tt> in order to bring it up to
     * date with possible changes in the PortAudio devices. Invokes
     * <tt>Pa_UpdateAvailableDeviceList()</tt> to update the devices on the
     * native side and then {@link #initialize()} to reflect any changes on the
     * Java side. Invoked by PortAudio when it detects that the list of devices
     * has changed.
     *
     * @throws Exception if there was an error during the invocation of
     * <tt>Pa_UpdateAvailableDeviceList()</tt> and
     * <tt>DeviceSystem.initialize()</tt>
     */
    private void reinitialize()
        throws Exception
    {
        logger.debug("Reinitialize called");
        synchronized (paUpdateAvailableDeviceListSyncRoot)
        {
            willPaUpdateAvailableDeviceList();
            try
            {
                Pa.UpdateAvailableDeviceList();
            }
            finally
            {
                didPaUpdateAvailableDeviceList();
            }
        }

        /*
         * XXX We will likely minimize the risk of crashes on the native side
         * even further by invoking initialize() with
         * Pa_UpdateAvailableDeviceList locked. Unfortunately, that will likely
         * increase the risks of deadlocks on the Java side.
         */
        invokeDeviceSystemInitialize(this);
    }

    /**
     * {@inheritDoc}
     *
     * The implementation of <tt>PortAudioSystem</tt> always returns
     * &quot;PortAudio&quot;.
     */
    @Override
    public String toString()
    {
        return "PortAudio";
    }

    /**
     * Encapsulates the monitoring of the functional health of
     * procedures/processes represented as <tt>DiagnosticsControl</tt>
     * implementations.
     */
    private static class DiagnosticsControlMonitor
    {
        /**
         * The <tt>Runnable</tt> to be executed by {@link #executor} and to
         * monitor the functional health of {@link #diagnosticsControls}.
         */
        private static Runnable command;

        /**
         * The <tt>DiagnosticControl</tt>s representing procedures/processes
         * whose functional health is to be monitored.
         */
        private static final Map<DiagnosticsControl,Boolean> diagnosticsControls
            = new WeakHashMap<DiagnosticsControl,Boolean>();

        private static WeakReference<JDialog> dialog;

        private static ExecutorService executor;

        /**
         * The time in milliseconds of (uninterrupted) malfunctioning after
         * which the respective <tt>DiagnosticsControl</tt> is to be reported
         * (to the user).
         */
        private static final long MALFUNCTIONING_TIMEOUT = 10 * 1000;

        /**
         * The interval of time in milliseconds between subsequent checks upon
         * the functional health of the monitored <tt>DiagnosticsControl</tt>s.s
         */
        private static final long MONITOR_INTERVAL = 1000;

        /**
         * Places a specific <tt>DiagnosticsControl</tt> under monitoring of its
         * functional health because of a malfunction in its procedure/process.
         * The monitoring will automatically cease after the procedure/process
         * resumes executing normally or is garbage collected.
         *
         * @param diagnosticsControl the <tt>DiagnosticsControl</tt> to be
         * placed under monitoring of its functional health because of a
         * malfunction in its procedure/process
         */
        public static synchronized void monitorFunctionalHealth(
                DiagnosticsControl diagnosticsControl)
        {
            if (!diagnosticsControls.containsKey(diagnosticsControl))
            {
                diagnosticsControls.put(diagnosticsControl, null);

                if (executor == null)
                    executor = Executors.newSingleThreadExecutor();
                if (command == null)
                {
                    command
                        = new Runnable()
                        {
                            public void run()
                            {
                                runInCommand();
                            }
                        };
                }
                executor.execute(command);
            }
        }

        /**
         * Reports a specific list of malfunctioning
         * <tt>DiagnosticsControl</tt>s to the user.
         *
         * @param malfunctioning the list of malfunctioning
         * <tt>DiagnosticsControl</tt>s to be reported to the user
         */
        private static void reportMalfunctioning(
                final List<WeakReference<DiagnosticsControl>> malfunctioning)
        {
            if (!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                reportMalfunctioning(malfunctioning);
                            }
                        });
                return;
            }

            /*
             * If the dialog is shown, do not report any subsequent
             * malfunctioning until the dialog is hidden in order to prevent
             * multiple dialogs.
             */
            JDialog dialog;

            if (DiagnosticsControlMonitor.dialog == null)
                dialog = null;
            else
            {
                dialog = DiagnosticsControlMonitor.dialog.get();
                if ((dialog != null) && dialog.isVisible())
                    return;
            }

            /*
             * Prepare a message to be displayed to the user listing the names
             * of the audio device which are malfunctioning.
             */
            StringBuilder param = new StringBuilder();
            String lineSeparator = System.getProperty("line.separator");
            int malfunctioningCount = 0;

            synchronized (DiagnosticsControlMonitor.class)
            {
                for (WeakReference<DiagnosticsControl> aMalfunctioning
                        : malfunctioning)
                {
                    DiagnosticsControl key = aMalfunctioning.get();

                    if ((key != null)
                            && diagnosticsControls.containsKey(key)
                            && (diagnosticsControls.get(key) == null))
                    {
                        String name = key.toString();

                        if ((name == null) || (name.length() == 0))
                            continue;

                        param.append(name).append(lineSeparator);
                        malfunctioningCount++;
                    }
                }
            }
            if (malfunctioningCount == 0)
                return;

            ResourceManagementService r
                = LibJitsi.getResourceManagementService();

            if (r == null)
                return;

            /*
             * Do display the list of malfunctioning audio devices to the user.
             */
            String message
                = r.getI18NString(
                        "impl.neomedia.device.portaudiosystem"
                            + ".diagnosticscontrolmonitor.MESSAGE",
                        new String[] { param.toString() });
            String title
                = r.getI18NString(
                        "impl.neomedia.device.portaudiosystem"
                            + ".diagnosticscontrolmonitor.TITLE");

            JOptionPane optionPane = new JOptionPane();

            optionPane.setMessage(message);
            optionPane.setMessageType(JOptionPane.WARNING_MESSAGE);
            optionPane.setOptionType(JOptionPane.DEFAULT_OPTION);

            dialog = optionPane.createDialog(null, title);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setModal(false);

            DiagnosticsControlMonitor.dialog
                = new WeakReference<JDialog>(dialog);

            /*
             * Once a device is reported to be malfunctioning, do not report it
             * again until sufficient time has passed to warrant its new report.
             */
            synchronized (DiagnosticsControlMonitor.class)
            {
                for (WeakReference<DiagnosticsControl> aMalfunctioning
                        : malfunctioning)
                {
                    DiagnosticsControl key = aMalfunctioning.get();

                    if ((key != null) && diagnosticsControls.containsKey(key))
                        diagnosticsControls.put(key, true);
                }
            }

            dialog.setVisible(true);
        }

        /**
         * Implements {@link Runnable#run()} in {@link #command}. Monitors the
         * functional health of {@link #diagnosticsControls}.
         */
        private static void runInCommand()
        {
            DiagnosticsControl[] keys = new DiagnosticsControl[0];

            do
            {
                synchronized (DiagnosticsControlMonitor.class)
                {
                    if (diagnosticsControls.isEmpty())
                        break;

                    Set<DiagnosticsControl> keySet
                        = diagnosticsControls.keySet();

                    keys = keySet.toArray(keys);
                }

                int keyCount = 0;
                long now = System.currentTimeMillis();
                List<WeakReference<DiagnosticsControl>> malfunctioning = null;

                for (int i = 0; i < keys.length; i++)
                {
                    DiagnosticsControl key = keys[i];

                    if (key == null)
                        continue;

                    /*
                     * XXX The array keys will live as much as possible in order
                     * to reduce allocations. However, its elements should be
                     * referenced as little as possible in order to not prevent
                     * their garbage collection.
                     */
                    keys[i] = null;

                    /*
                     * The PortAudio device represented by the
                     * DiagnosticsControl may have already been disconnected. We
                     * do not have reliable way of detecting that fact here so
                     * we will rely on the garbage collector and the
                     * implementation of DiagnosticsControl#toString().
                     */
                    keyCount++;

                    long malfunctioningSince = key.getMalfunctioningSince();

                    if (malfunctioningSince == DiagnosticsControl.NEVER)
                        continue;
                    if (now - malfunctioningSince < MALFUNCTIONING_TIMEOUT)
                        continue;

                    if (malfunctioning == null)
                    {
                        malfunctioning
                            = new LinkedList<WeakReference<DiagnosticsControl>>();
                    }
                    malfunctioning.add(
                            new WeakReference<DiagnosticsControl>(key));
                }
                if (keyCount == 0)
                    break;

                if ((malfunctioning != null) && !malfunctioning.isEmpty())
                {
                    reportMalfunctioning(malfunctioning);
                    /*
                     * Make sure we are not accidentally preventing the garbage
                     * collection of DiagnosticsControl instances.
                     */
                    malfunctioning = null;
                }

                try
                {
                    Thread.sleep(MONITOR_INTERVAL);
                }
                catch (InterruptedException ie)
                {
                }
            }
            while (true);
        }
    }
}
