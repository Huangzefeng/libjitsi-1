/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device;

import java.io.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.media.*;
import javax.media.Renderer;
import javax.sound.sampled.*;
import javax.swing.*;

import org.jitsi.impl.neomedia.control.*;
import org.jitsi.impl.neomedia.jmfext.media.renderer.audio.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * Represents a <tt>DeviceSystem</tt> which provides support for the devices to
 * capture and play back audio (media). Examples include implementations which
 * integrate the native PortAudio, PulseAudio libraries.
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 */
public abstract class AudioSystem
    extends DeviceSystem
{
    /**
     * Enumerates the different types of media data flow of
     * <tt>CaptureDeviceInfo2</tt>s contributed by an <tt>AudioSystem</tt>.
     *
     * @author Lyubomir Marinov
     */
    public enum DataFlow
    {
        CAPTURE,
        NOTIFY,
        PLAYBACK
    }

    /**
     * The constant/flag (to be) returned by {@link #getFeatures()} in order to
     * indicate that the respective <tt>AudioSystem</tt> supports toggling its
     * denoise functionality between on and off. The UI will look for the
     * presence of the flag in order to determine whether a check box is to be
     * shown to the user to enable toggling the denoise functionality.
     */
    public static final int FEATURE_DENOISE = 2;

    /**
     * The constant/flag (to be) returned by {@link #getFeatures()} in order to
     * indicate that the respective <tt>AudioSystem</tt> supports toggling its
     * echo cancellation functionality between on and off. The UI will look for
     * the presence of the flag in order to determine whether a check box is to
     * be shown to the user to enable toggling the echo cancellation
     * functionality.
     */
    public static final int FEATURE_ECHO_CANCELLATION = 4;

    /**
     * The constant/flag (to be) returned by {@link #getFeatures()} in order to
     * indicate that the respective <tt>AudioSystem</tt> differentiates between
     * playback and notification audio devices. The UI, for example, will look
     * for the presence of the flag in order to determine whether separate combo
     * boxes are to be shown to the user to allow the configuration of the
     * preferred playback and notification audio devices.
     */
    public static final int FEATURE_NOTIFY_AND_PLAYBACK_DEVICES = 8;

    public static final String LOCATOR_PROTOCOL_AUDIORECORD = "audiorecord";

    public static final String LOCATOR_PROTOCOL_JAVASOUND = "javasound";

    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying
     * <tt>CaptureDeviceInfo</tt>s contributed by <tt>MacCoreaudioSystem</tt>.
     */
    public static final String LOCATOR_PROTOCOL_MACCOREAUDIO = "maccoreaudio";

    public static final String LOCATOR_PROTOCOL_OPENSLES = "opensles";

    public static final String LOCATOR_PROTOCOL_PORTAUDIO = "portaudio";

    public static final String LOCATOR_PROTOCOL_PULSEAUDIO = "pulseaudio";

    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying
     * <tt>CaptureDeviceInfo</tt>s contributed by <tt>WASAPISystem</tt>.
     */
    public static final String LOCATOR_PROTOCOL_WASAPI = "wasapi";

    /**
     * The <tt>Logger</tt> used by this instance for logging output.
     */
    private static Logger logger = Logger.getLogger(AudioSystem.class);

    /**
     * The (base) name of the <tt>ConfigurationService</tt> property which
     * indicates whether noise suppression is to be performed for the captured
     * audio.
     */
    protected static final String PNAME_DENOISE = "denoise";

    /**
     * The (base) name of the <tt>ConfigurationService</tt> property which
     * indicates whether noise cancellation is to be performed for the captured
     * audio.
     */
    protected static final String PNAME_ECHOCANCEL = "echocancel";

    public static AudioSystem getAudioSystem(String locatorProtocol)
    {
        AudioSystem[] audioSystems = getAudioSystems();
        AudioSystem audioSystemWithLocatorProtocol = null;

        if (audioSystems != null)
        {
            for (AudioSystem audioSystem : audioSystems)
            {
                if (audioSystem.getLocatorProtocol().equalsIgnoreCase(
                        locatorProtocol))
                {
                    audioSystemWithLocatorProtocol = audioSystem;
                    break;
                }
            }
        }
        return audioSystemWithLocatorProtocol;
    }

    public static AudioSystem[] getAudioSystems()
    {
        DeviceSystem[] deviceSystems
            = DeviceSystem.getDeviceSystems(MediaType.AUDIO);
        List<AudioSystem> audioSystems;

        if (deviceSystems == null)
        {
            audioSystems = null;
        }
        else
        {
            audioSystems = new ArrayList<AudioSystem>(deviceSystems.length);
            for (DeviceSystem deviceSystem : deviceSystems)
            {
                if (deviceSystem instanceof AudioSystem)
                {
                    audioSystems.add((AudioSystem) deviceSystem);
                }
            }
        }
        return
            (audioSystems == null)
                ? null
                : audioSystems.toArray(new AudioSystem[audioSystems.size()]);
    }

    /**
     * The list of devices detected by this <tt>AudioSystem</tt> indexed by
     * their category which is among {@link #CAPTURE_INDEX},
     * {@link #NOTIFY_INDEX} and {@link #PLAYBACK_INDEX}.
     */
    private Devices[] devices;

    protected AudioSystem(String locatorProtocol)
        throws Exception
    {
        this(locatorProtocol, 0);
    }

    protected AudioSystem(String locatorProtocol, int features)
        throws Exception
    {
        super(MediaType.AUDIO, locatorProtocol, features);
    }

    /**
     * {@inheritDoc}
     *
     * Delegates to {@link #createRenderer(boolean)} with the value of the
     * <tt>playback</tt> argument set to true.
     */
    @Override
    public Renderer createRenderer()
    {
        return createRenderer(true);
    }

    /**
     * Initializes a new <tt>Renderer</tt> instance which is to either perform
     * playback on or sound a notification through a device contributed by this
     * system. The (default) implementation of <tt>AudioSystem</tt> ignores the
     * value of the <tt>playback</tt> argument and delegates to
     * {@link DeviceSystem#createRenderer()}.
     *
     * @param playback <tt>true</tt> if the new instance is to perform playback
     * or <tt>false</tt> if the new instance is to sound a notification
     * @return a new <tt>Renderer</tt> instance which is to either perform
     * playback on or sound a notification through a device contributed by this
     * system
     */
    public Renderer createRenderer(boolean playback)
    {
        String className = getRendererClassName();
        Renderer renderer;

        if (className == null)
        {
            /*
             * There is no point in delegating to the super's createRenderer()
             * because it will not have a class to instantiate.
             */
            renderer = null;
        }
        else
        {
            Class<?> clazz;

            try
            {
                clazz = Class.forName(className);
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                {
                    throw (ThreadDeath) t;
                }
                else
                {
                    clazz = null;
                    logger.error("Failed to get class " + className, t);
                }
            }
            if (clazz == null)
            {
                /*
                 * There is no point in delegating to the super's
                 * createRenderer() because it will fail to get the class.
                 */
                renderer = null;
            }
            else if (!Renderer.class.isAssignableFrom(clazz))
            {
                /*
                 * There is no point in delegating to the super's
                 * createRenderer() because it will fail to cast the new
                 * instance to a Renderer.
                 */
                renderer = null;
            }
            else
            {
                boolean superCreateRenderer;

                if (((getFeatures() & FEATURE_NOTIFY_AND_PLAYBACK_DEVICES) != 0)
                        && AbstractAudioRenderer.class.isAssignableFrom(clazz))
                {
                    Constructor<?> constructor = null;

                    try
                    {
                        constructor = clazz.getConstructor(boolean.class);
                    }
                    catch (NoSuchMethodException nsme)
                    {
                        /*
                         * Such a constructor is optional so the failure to get
                         * it will be swallowed and the super's
                         * createRenderer() will be invoked.
                         */
                    }
                    catch (SecurityException se)
                    {
                    }
                    if ((constructor != null))
                    {
                        superCreateRenderer = false;
                        try
                        {
                            renderer
                                = (Renderer) constructor.newInstance(playback);
                        }
                        catch (Throwable t)
                        {
                            if (t instanceof ThreadDeath)
                            {
                                throw (ThreadDeath) t;
                            }
                            else
                            {
                                renderer = null;
                                logger.error(
                                        "Failed to initialize a new "
                                            + className + " instance",
                                        t);
                            }
                        }
                        if ((renderer != null) && !playback)
                        {
                            CaptureDeviceInfo device
                                = getSelectedDevice(DataFlow.NOTIFY);

                            if (device == null)
                            {
                                /*
                                 * If there is no notification device, then no
                                 * notification is to be sounded.
                                 */
                                renderer = null;
                            }
                            else
                            {
                                MediaLocator locator = device.getLocator();

                                if (locator != null)
                                {
                                    ((AbstractAudioRenderer<?>) renderer)
                                        .setLocator(locator);
                                }
                            }
                        }
                    }
                    else
                    {
                        /*
                         * The super's createRenderer() will be invoked because
                         * either there is no non-default constructor or it is
                         * not meant to be invoked by the public.
                         */
                        superCreateRenderer = true;
                        renderer = null;
                    }
                }
                else
                {
                    /*
                     * The super's createRenderer() will be invoked because
                     * either this AudioSystem does not distinguish between
                     * playback and notify data flows or the Renderer
                     * implementation class in not familiar.
                     */
                    superCreateRenderer = true;
                    renderer = null;
                }

                if (superCreateRenderer && (renderer == null))
                {
                    renderer = super.createRenderer();
                }
            }
        }
        return renderer;
    }

    /**
     * Obtains an audio input stream from the URL provided.
     * @param uri a valid uri to a sound resource.
     * @return the input stream to audio data.
     * @throws IOException if an I/O exception occurs
     */
    public InputStream getAudioInputStream(String uri)
        throws IOException
    {
        ResourceManagementService resources
            = LibJitsi.getResourceManagementService();
        URL url
            = (resources == null)
                ? null
                : resources.getSoundURLForPath(uri);
        AudioInputStream audioStream = null;

        try
        {
            // Not found by the class loader? Perhaps it is a local file.
            if (url == null)
            {
                url = new URL(uri);
            }

            audioStream
                = javax.sound.sampled.AudioSystem.getAudioInputStream(url);
        }
        catch (MalformedURLException murle)
        {
            // Do nothing, the value of audioStream will remain equal to null.
        }
        catch (UnsupportedAudioFileException uafe)
        {
            logger.error("Unsupported format of audio stream " + url, uafe);
        }

        return audioStream;
    }

    /**
     * Gets a <tt>CaptureDeviceInfo2</tt> which has been contributed by this
     * <tt>AudioSystem</tt>, supports a specific flow of media data (i.e.
     * capture, notify or playback) and is identified by a specific
     * <tt>MediaLocator</tt>.
     *
     * @param dataFlow the flow of the media data supported by the
     * <tt>CaptureDeviceInfo2</tt> to be returned
     * @param locator the <tt>MediaLocator</tt> of the
     * <tt>CaptureDeviceInfo2</tt> to be returned
     * @return a <tt>CaptureDeviceInfo2</tt> which has been contributed by this
     * instance, supports the specified <tt>dataFlow</tt> and is identified by
     * the specified <tt>locator</tt>
     */
    public CaptureDeviceInfo2 getDevice(DataFlow dataFlow, MediaLocator locator)
    {
        return devices[dataFlow.ordinal()].getDevice(locator);
    }

    /**
     * Gets the list of devices with a specific data flow: capture, notify or
     * playback.
     *
     * @param dataFlow the data flow of the devices to retrieve: capture, notify
     * or playback
     * @return the list of devices with the specified <tt>dataFlow</tt>
     */
    public List<CaptureDeviceInfo2> getDevices(DataFlow dataFlow)
    {
        return devices[dataFlow.ordinal()].getDevices();
    }

    /**
     * Gets the list of all devices in user preferences with a specific data
     * flow: capture, notify or playback.
     *
     * @param dataFlow the data flow of the devices to retrieve: capture, notify
     * or playback
     * @return the list of all devices with the specified <tt>dataFlow</tt>
     */
    public LinkedHashMap<String, String> getAllDevices(DataFlow dataFlow)
    {
        return devices[dataFlow.ordinal()].getAllDevices();
    }

    /**
     * Returns the FMJ format of a specific <tt>InputStream</tt> providing audio
     * media.
     *
     * @param audioInputStream the <tt>InputStream</tt> providing audio media to
     * determine the FMJ format of
     * @return the FMJ format of the specified <tt>audioInputStream</tt> or
     * <tt>null</tt> if such an FMJ format could not be determined
     */
    public javax.media.format.AudioFormat getFormat(
            InputStream audioInputStream)
    {
        if ((audioInputStream instanceof AudioInputStream))
        {
            AudioFormat af = ((AudioInputStream) audioInputStream).getFormat();

            return
                new javax.media.format.AudioFormat(
                        javax.media.format.AudioFormat.LINEAR,
                        af.getSampleRate(),
                        af.getSampleSizeInBits(),
                        af.getChannels());
        }
        return null;
    }

    /**
     * Gets the (full) name of the <tt>ConfigurationService</tt> property which
     * is associated with a (base) <tt>AudioSystem</tt>-specific property name.
     *
     * @param basePropertyName the (base) <tt>AudioSystem</tt>-specific property
     * name of which the associated (full) <tt>ConfigurationService</tt>
     * property name is to be returned
     * @return the (full) name of the <tt>ConfigurationService</tt> property
     * which is associated with the (base) <tt>AudioSystem</tt>-specific
     * property name
     */
    protected String getPropertyName(String basePropertyName)
    {
        return
            DeviceConfiguration.PROP_AUDIO_SYSTEM + "." + getLocatorProtocol()
                + "." + basePropertyName;
    }

    /**
     * Gets the selected device for a specific data flow: capture, notify or
     * playback.
     *
     * @param dataFlow the data flow of the selected device to retrieve:
     * capture, notify or playback.
     * @return the selected device for the specified <tt>dataFlow</tt>
     */
    public CaptureDeviceInfo2 getSelectedDevice(DataFlow dataFlow)
    {
        return
            devices[dataFlow.ordinal()].getSelectedDevice(getDevices(dataFlow));
    }

    /**
     * Gets the indicator which determines whether noise suppression is to be
     * performed for captured audio.
     *
     * @return <tt>true</tt> if noise suppression is to be performed for
     * captured audio; otherwise, <tt>false</tt>
     */
    public boolean isDenoise()
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();
        boolean value = ((getFeatures() & FEATURE_DENOISE) == FEATURE_DENOISE);

        if (cfg != null)
        {
            value = cfg.getBoolean(getPropertyName(PNAME_DENOISE), value);
        }
        return value;
    }

    /**
     * Gets the indicator which determines whether echo cancellation is to be
     * performed for captured audio.
     *
     * @return <tt>true</tt> if echo cancellation is to be performed for
     * captured audio; otherwise, <tt>false</tt>
     */
    public boolean isEchoCancel()
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();
        boolean value
            = ((getFeatures() & FEATURE_ECHO_CANCELLATION)
                    == FEATURE_ECHO_CANCELLATION);

        if (cfg != null)
        {
            value = cfg.getBoolean(getPropertyName(PNAME_ECHOCANCEL), value);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * Because <tt>AudioSystem</tt> may support playback and notification audio
     * devices apart from capture audio devices, fires more specific
     * <tt>PropertyChangeEvent</tt>s than <tt>DeviceSystem</tt>
     */
    @Override
    protected void postInitialize()
    {
        try
        {
            try
            {
                postInitializeSpecificDevices(DataFlow.CAPTURE);
            }
            finally
            {
                if ((FEATURE_NOTIFY_AND_PLAYBACK_DEVICES & getFeatures()) != 0)
                {
                    try
                    {
                        postInitializeSpecificDevices(DataFlow.NOTIFY);
                    }
                    finally
                    {
                        postInitializeSpecificDevices(DataFlow.PLAYBACK);
                    }
                }
            }
        }
        finally
        {
            super.postInitialize();
        }
    }

    /**
     * Sets the device lists after the different audio systems (PortAudio,
     * PulseAudio, etc) have finished detecting their devices.
     *
     * @param dataFlow the data flow of the devices to perform
     * post-initialization on
     */
    protected void postInitializeSpecificDevices(DataFlow dataFlow)
    {
        // Gets all current active devices.
        List<CaptureDeviceInfo2> activeDevices = getDevices(dataFlow);
        // Gets the default device.
        Devices devices = this.devices[dataFlow.ordinal()];
        CaptureDeviceInfo2 selectedActiveDevice
            = devices.getSelectedDevice(activeDevices);

        if (logger.isDebugEnabled())
        {
            StringBuilder activeDevicesString = new StringBuilder();

            for (CaptureDeviceInfo2 activeDevice : activeDevices)
            {
                activeDevicesString.append(activeDevice.getName());
            }

            logger.debug("Active devices " + activeDevicesString);
            logger.debug("About to set device to " + selectedActiveDevice);
        }

        // Sets the default device as selected. The function will fire a
        // property change only if the device has changed from a previous
        // configuration. The "set" part is important because only the fired
        // property event provides a way to get the hotplugged devices working
        // during a call.
        devices.setDevice(selectedActiveDevice, false);
    }

    /**
     * {@inheritDoc}
     *
     * Removes any capture, playback and notification devices previously
     * detected by this <tt>AudioSystem</tt> and prepares it for the execution
     * of its {@link DeviceSystem#doInitialize()} implementation (which detects
     * all devices to be provided by this instance).
     */
    @Override
    protected void preInitialize()
    {
        super.preInitialize();

        if (devices == null)
        {
            devices = new Devices[3];
            devices[DataFlow.CAPTURE.ordinal()] = new CaptureDevices(this);
            devices[DataFlow.NOTIFY.ordinal()] = new NotifyDevices(this);
            devices[DataFlow.PLAYBACK.ordinal()] = new PlaybackDevices(this);
        }
    }

    /**
     * Fires a new <tt>PropertyChangeEvent</tt> to the
     * <tt>PropertyChangeListener</tt>s registered with this
     * <tt>PropertyChangeNotifier</tt> in order to notify about a change in the
     * value of a specific property which had its old value modified to a
     * specific new value. <tt>PropertyChangeNotifier</tt> does not check
     * whether the specified <tt>oldValue</tt> and <tt>newValue</tt> are indeed
     * different.
     *
     * @param property the name of the property of this
     * <tt>PropertyChangeNotifier</tt> which had its value changed
     * @param oldValue the value of the property with the specified name before
     * the change
     * @param newValue the value of the property with the specified name after
     * the change
     */
    void propertyChange(String property, Object oldValue, Object newValue)
    {
        firePropertyChange(property, oldValue, newValue);
    }

    /**
     * Sets the list of a kind of devices: capture, notify or playback.
     *
     * @param captureDevices The list of a kind of devices: capture, notify or
     * playback.
     */
    protected void setCaptureDevices(List<CaptureDeviceInfo2> captureDevices)
    {
        devices[DataFlow.CAPTURE.ordinal()].setDevices(captureDevices);
    }

    /**
     * Sets the indicator which determines whether noise suppression is to be
     * performed for captured audio.
     *
     * @param denoise <tt>true</tt> if noise suppression is to be performed for
     * captured audio; otherwise, <tt>false</tt>
     */
    public void setDenoise(boolean denoise)
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();

        if (cfg != null)
            cfg.setProperty(getPropertyName(PNAME_DENOISE), denoise);
    }

    /**
     * Selects the active device.
     *
     * @param dataFlow the data flow of the device to set: capture, notify or
     * playback
     * @param device The selected active device.
     * @param save Flag set to true in order to save this choice in the
     * configuration. False otherwise.
     */
    public void setDevice(
            DataFlow dataFlow,
            CaptureDeviceInfo2 device,
            boolean save)
    {
        devices[dataFlow.ordinal()].setDevice(device,save);
    }

    /**
     * Sets the indicator which determines whether echo cancellation is to be
     * performed for captured audio.
     *
     * @param echoCancel <tt>true</tt> if echo cancellation is to be performed
     * for captured audio; otherwise, <tt>false</tt>
     */
    public void setEchoCancel(boolean echoCancel)
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();

        if (cfg != null)
            cfg.setProperty(getPropertyName(PNAME_ECHOCANCEL), echoCancel);
    }

    /**
     * Sets the list of the active devices.
     *
     * @param playbackDevices The list of the active devices.
     */
    protected void setPlaybackDevices(List<CaptureDeviceInfo2> playbackDevices)
    {
        devices[DataFlow.PLAYBACK.ordinal()].setDevices(playbackDevices);
        // The notify devices are the same as the playback devices.
        devices[DataFlow.NOTIFY.ordinal()].setDevices(playbackDevices);
    }

    /**
     * Encapsulates the monitoring of the functional health of
     * procedures/processes represented as <tt>DiagnosticsControl</tt>
     * implementations.
     */
    public static class DiagnosticsControlMonitor
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
        private static final long MALFUNCTIONING_TIMEOUT = 1500;

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
                {
                    executor = Executors.newSingleThreadExecutor();
                }
                if (command == null)
                {
                    command
                        = new Runnable()
                        {
                            @Override
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
                            @Override
                            public void run()
                            {
                                reportMalfunctioning(malfunctioning);
                            }
                        });
                return;
            }

            /*
             * Prepare a message to be displayed to the user listing the names
             * of the audio device which are malfunctioning.
             */
            ResourceManagementService r = LibJitsi.getResourceManagementService();
            if (r == null)
            {
                return;
            }
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
                        {
                            continue;
                        }

                        param.append(name).append(lineSeparator);
                        malfunctioningCount++;
                    }
                }
            }
            if (malfunctioningCount == 0)
            {
                return;
            }

            /*
             * Display the list of malfunctioning audio devices to the user.
             */
            final String message
                = r.getI18NString(
                        "impl.neomedia.device.audiosystem"
                            + ".diagnosticscontrolmonitor.MESSAGE",
                        new String[] { param.toString() });
            logger.warn("Reporting device as malfunctioning: " + message);
            showWarningPopup(message);
            logger.dumpThreads();

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
                    {
                        diagnosticsControls.put(key, true);
                    }
                }
            }

            /*
             * Slight hack :)  To ensure that we know when this has happened,
             * throw an exception on a new thread, which will get caught by our
             * error reporting code.
             */
            new Thread() {
                @Override
                public void run()
                {
                    // Just throw an exception with our malfunctioning message
                    // in it.
                    throw new RuntimeException(
                        "Device has malfunctioned: " + message);
                }
            }.start();
        }

        /**
         * Reports a malfunctioning audio system to the user.
         *
         * @param message The audio system-specific message to be displayed to
         * the user
         */
        private static void reportSystemUnavailable(final String message)
        {
            if (!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                reportSystemUnavailable(message);
                            }
                        });
                return;
            }

            showWarningPopup(message);
        }

        /**
         * Show an error dialog to the user indicating that something has gone
         * wrong with the audio system.
         *
         * @param message The specific error text to show
         */
        private static void showWarningPopup(String message)
        {
            /*
             * If the dialog is shown, do not report any subsequent errors
             * until the dialog is hidden in order to prevent multiple dialogs.
             */
            JDialog dialog;
            if (DiagnosticsControlMonitor.dialog == null)
            {
                dialog = null;
            }
            else
            {
                dialog = DiagnosticsControlMonitor.dialog.get();
                if ((dialog != null) && dialog.isVisible())
                {
                    return;
                }
            }

            ResourceManagementService r = LibJitsi.getResourceManagementService();
            if (r == null)
            {
                return;
            }

            String title = r.getI18NString(
                "impl.neomedia.device.audiosystem.diagnosticscontrolmonitor.TITLE");

            JOptionPane optionPane = new JOptionPane();

            optionPane.setMessage(message);
            optionPane.setMessageType(JOptionPane.WARNING_MESSAGE);
            optionPane.setOptionType(JOptionPane.DEFAULT_OPTION);

            dialog = optionPane.createDialog(null, title);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setModal(false);

            DiagnosticsControlMonitor.dialog =
                new WeakReference<JDialog>(dialog);

            dialog.setVisible(true);
            dialog.setAlwaysOnTop(true);
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
                    {
                        break;
                    }

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
                    {
                        continue;
                    }

                    /*
                     * XXX The array keys will live as much as possible in order
                     * to reduce allocations. However, its elements should be
                     * referenced as little as possible in order to not prevent
                     * their garbage collection.
                     */
                    keys[i] = null;

                    /*
                     * The audio device represented by the DiagnosticsControl
                     * may have already been disconnected. We do not have
                     * reliable way of detecting that fact here so we will rely
                     * on the garbage collector and the implementation of
                     * DiagnosticsControl#toString().
                     */
                    keyCount++;

                    long malfunctioningSince = key.getMalfunctioningSince();

                    if (malfunctioningSince == DiagnosticsControl.NEVER)
                    {
                        continue;
                    }
                    if (now - malfunctioningSince < MALFUNCTIONING_TIMEOUT)
                    {
                        continue;
                    }

                    if (malfunctioning == null)
                    {
                        malfunctioning
                            = new LinkedList<WeakReference<DiagnosticsControl>>();
                    }
                    malfunctioning.add(
                            new WeakReference<DiagnosticsControl>(key));
                }
                if (keyCount == 0)
                {
                    break;
                }

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

    /**
     * Reports the audio system as being unavailable.  This is currently used
     * to indicate that the Windows audio service is not running and therefore
     * that we can't use any of the audio devices.
     *
     * @param diagnosticsControl the <tt>DiagnosticsControl</tt> to be used to
     * report the issue
     */
    public static void reportAudioSystemUnavailable(String message)
    {
        DiagnosticsControlMonitor.reportSystemUnavailable(message);
    }
}
