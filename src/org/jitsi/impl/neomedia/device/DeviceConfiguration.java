/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device;

import java.awt.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.media.*;
import javax.media.format.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.codec.video.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.util.*;
import org.jitsi.util.event.*;

/**
 * This class aims to provide a simple configuration interface for JMF. It
 * retrieves stored configuration when started or listens to ConfigurationEvent
 * for property changes and configures the JMF accordingly.
 *
 * @author Martin Andre
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 */
public class DeviceConfiguration
    extends PropertyChangeNotifier
    implements PropertyChangeListener
{

    /**
     * The name of the <tt>DeviceConfiguration</tt> property which represents
     * the device used by <tt>DeviceConfiguration</tt> for audio capture.
     */
    public static final String AUDIO_CAPTURE_DEVICE
        = CaptureDevices.PROP_DEVICE;

    /**
     * The name of the <tt>DeviceConfiguration</tt> property which represents
     * the device used by <tt>DeviceConfiguration</tt> for audio notify.
     */
    public static final String AUDIO_NOTIFY_DEVICE
        = NotifyDevices.PROP_DEVICE;

    /**
     * The name of the <tt>DeviceConfiguration</tt> property which represents
     * the device used by <tt>DeviceConfiguration</tt> for audio playback.
     */
    public static final String AUDIO_PLAYBACK_DEVICE
        = PlaybackDevices.PROP_DEVICE;

    /**
     * The list of class names of custom <tt>Renderer</tt> implementations to be
     * registered with JMF.
     */
    private static final String[] CUSTOM_RENDERERS
        = new String[]
        {
            OSUtils.IS_ANDROID ? ".audio.AudioTrackRenderer" : null,
            OSUtils.IS_ANDROID ? ".audio.OpenSLESRenderer" : null,
            OSUtils.IS_LINUX ? ".audio.PulseAudioRenderer" : null,
            OSUtils.IS_WINDOWS ? ".audio.WASAPIRenderer" : null,
            OSUtils.IS_ANDROID ? null : ".audio.PortAudioRenderer",
            ".video.JAWTRenderer"
        };

    /**
     * The default value to be used for the {@link #PROP_AUDIO_DENOISE} property
     * when it does not have a value.
     */
    public static final boolean DEFAULT_AUDIO_DENOISE = true;

    /**
     * The default value to be used for the {@link #PROP_AUDIO_ECHOCANCEL}
     * property when it does not have a value.
     */
    public static final boolean DEFAULT_AUDIO_ECHOCANCEL = true;

    /**
     * The default value to be used for the
     * {@link #PROP_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS} property when it
     * does not have a value. The recommended filter length is approximately the
     * third of the room reverberation time. For example, in a small room,
     * reverberation time is in the order of 300 ms, so a filter length of 100
     * ms is a good choice (800 samples at 8000 Hz sampling rate).
     */
    public static final long DEFAULT_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS
        = 100;

    /**
     * The default frame rate, <tt>-1</tt> unlimited.
     */
    public static final int DEFAULT_VIDEO_FRAMERATE = -1;

    /**
     * The default video height.
     */
    public static final int DEFAULT_VIDEO_HEIGHT = 240; // 480;

    /**
     * The default value for video maximum bandwidth.
     */
    public static final int DEFAULT_VIDEO_RTP_PACING_THRESHOLD = 192; // 256;

    /**
     * The default value for video codec bitrate.
     */
    public static final int DEFAULT_VIDEO_BITRATE = 128;

    /**
     * The default video width.
     */
    public static final int DEFAULT_VIDEO_WIDTH = 320; // 640;

    /**
     * The name of the <tt>boolean</tt> property which determines whether noise
     * suppression is to be performed for captured audio.
     */
    static final String PROP_AUDIO_DENOISE
        = "net.java.sip.communicator.impl.neomedia.denoise";

    /**
     * The name of the <tt>boolean</tt> property which determines whether echo
     * cancellation is to be performed for captured audio.
     */
    static final String PROP_AUDIO_ECHOCANCEL
        = "net.java.sip.communicator.impl.neomedia.echocancel";

    /**
     * The name of the <tt>long</tt> property which determines the filter length
     * in milliseconds to be used by the echo cancellation implementation. The
     * recommended filter length is approximately the third of the room
     * reverberation time. For example, in a small room, reverberation time is
     * in the order of 300 ms, so a filter length of 100 ms is a good choice
     * (800 samples at 8000 Hz sampling rate).
     */
    static final String PROP_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS
        = "net.java.sip.communicator.impl.neomedia.echocancel.filterLengthInMillis";

    public static final String PROP_AUDIO_SYSTEM
        = "net.java.sip.communicator.impl.neomedia.audioSystem";

    public static final String PROP_AUDIO_SYSTEM_DEVICES
        = PROP_AUDIO_SYSTEM + "." + DeviceSystem.PROP_DEVICES;

    /**
     * The <tt>ConfigurationService</tt> property which stores the device used
     * by <tt>DeviceConfiguration</tt> for video capture.
     */
    private static final String PROP_VIDEO_DEVICE
        = "net.java.sip.communicator.impl.neomedia.videoDevice";

    /**
     * The property we use to store the video framerate settings.
     */
    private static final String PROP_VIDEO_FRAMERATE
        = "net.java.sip.communicator.impl.neomedia.video.framerate";

    /**
     * The name of the property which specifies the height of the video.
     */
    private static final String PROP_VIDEO_HEIGHT
        = "net.java.sip.communicator.impl.neomedia.video.height";

    /**
     * The property we use to store the settings for maximum allowed video
     * bandwidth (used to normalize RTP traffic, and not in codec configuration)
     */
    private static final String PROP_VIDEO_RTP_PACING_THRESHOLD
        = "net.java.sip.communicator.impl.neomedia.video.maxbandwidth";

    /**
     * The property we use to store the settings for video codec bitrate.
     */
    private static final String PROP_VIDEO_BITRATE
        = "net.java.sip.communicator.impl.neomedia.video.bitrate";

    /**
     * The name of the property which specifies the width of the video.
     */
    private static final String PROP_VIDEO_WIDTH
        = "net.java.sip.communicator.impl.neomedia.video.width";

    /**
     * The currently supported resolutions we will show as option
     * and user can select.
     */
    public static final Dimension[] SUPPORTED_RESOLUTIONS
        = new Dimension[]
            {
                // QVGA
                new Dimension(160, 100),
                //QCIF
                new Dimension(176, 144),
                // QVGA
                new Dimension(320, 200),
                // QVGA
                new Dimension(320, 240),
                //CIF
                new Dimension(352, 288),
             // Higher resolutions not supported on Accession Mobile and Desktop does not
             // renegotiate as it should.
//                             // VGA
//                             new Dimension(640, 480),
//                             // HD 720
//                             new Dimension(1280, 720)
            };

    /**
     * The name of the <tt>DeviceConfiguration</tt> property which
     * represents the device used by <tt>DeviceConfiguration</tt> for video
     * capture.
     */
    public static final String VIDEO_CAPTURE_DEVICE = "VIDEO_CAPTURE_DEVICE";

    /**
     * The currently selected audio system.
     */
    private AudioSystem audioSystem;

    /**
     * The frame rate.
     */
    private int frameRate = DEFAULT_VIDEO_FRAMERATE;

    /**
     * The <tt>Logger</tt> used by this instance for logging output.
     */
    private Logger logger = Logger.getLogger(DeviceConfiguration.class);

    /**
     * The device that we'll be using for video capture.
     */
    private CaptureDeviceInfo videoCaptureDevice;

    /**
     * Current setting for video maximum bandwidth.
     */
    private int videoMaxBandwidth = -1;

    /**
     * Current setting for video codec bitrate.
     */
    private int videoBitrate = -1;

    /**
     * The current resolution settings.
     */
    private Dimension videoSize;

    /**
     * Initializes a new <tt>DeviceConfiguration</tt> instance.
     */
    public DeviceConfiguration()
    {
        // these seem to be throwing exceptions every now and then so we'll
        // blindly catch them for now
        try
        {
            DeviceSystem.initializeDeviceSystems();
            extractConfiguredCaptureDevices();

            ConfigurationService cfg = LibJitsi.getConfigurationService();

            if (cfg != null)
            {
                cfg.addPropertyChangeListener(PROP_VIDEO_HEIGHT, this);
                cfg.addPropertyChangeListener(PROP_VIDEO_WIDTH, this);
                cfg.addPropertyChangeListener(PROP_VIDEO_FRAMERATE, this);
                cfg.addPropertyChangeListener(PROP_VIDEO_RTP_PACING_THRESHOLD,
                                              this);
            }
        }
        catch (Exception ex)
        {
            logger.error("Failed to initialize media.", ex);
        }

        registerCustomRenderers();
        fixRenderers();

        //Registers this device configuration to all reloadable device sytem.
        registerToDeviceSystemPropertyChangeListener();
    }

    /**
     * Fixes the list of <tt>Renderer</tt>s registered with FMJ in order to
     * resolve operating system-specific issues.
     */
    private static void fixRenderers()
    {
        @SuppressWarnings("unchecked")
        Vector<String> renderers
            = PlugInManager.getPlugInList(null, null, PlugInManager.RENDERER);

        /*
         * JMF is no longer in use, FMJ is used in its place. FMJ has its own
         * JavaSoundRenderer which is also extended into a JMF-compatible one.
         */
        PlugInManager.removePlugIn(
                "com.sun.media.renderer.audio.JavaSoundRenderer",
                PlugInManager.RENDERER);

        if (OSUtils.IS_WINDOWS)
        {
            if (OSUtils.IS_WINDOWS32 &&
                    (OSUtils.IS_WINDOWS_VISTA || OSUtils.IS_WINDOWS_7))
            {
                /*
                 * DDRenderer will cause 32-bit Windows Vista/7 to switch its
                 * theme from Aero to Vista Basic so try to pick up a different
                 * Renderer.
                 */
                if (renderers.contains(
                        "com.sun.media.renderer.video.GDIRenderer"))
                {
                    PlugInManager.removePlugIn(
                            "com.sun.media.renderer.video.DDRenderer",
                            PlugInManager.RENDERER);
                }
            }
            else if (OSUtils.IS_WINDOWS64)
            {
                /*
                 * Remove the native Renderers for 64-bit Windows because native
                 * JMF libs are not available for 64-bit machines.
                 */
                PlugInManager.removePlugIn(
                        "com.sun.media.renderer.video.GDIRenderer",
                        PlugInManager.RENDERER);
                PlugInManager.removePlugIn(
                        "com.sun.media.renderer.video.DDRenderer",
                        PlugInManager.RENDERER);
            }
        }
        else if (!OSUtils.IS_LINUX32)
        {
            if (renderers.contains(
                        "com.sun.media.renderer.video.LightWeightRenderer")
                    || renderers.contains(
                            "com.sun.media.renderer.video.AWTRenderer"))
            {
                // Remove XLibRenderer because it is native and JMF is supported
                // on 32-bit machines only.
                PlugInManager.removePlugIn(
                        "com.sun.media.renderer.video.XLibRenderer",
                        PlugInManager.RENDERER);
            }
        }
    }

    /**
     * Detects capture devices configured through JMF and disable audio and/or
     * video transmission if none were found.
     */
    private void extractConfiguredCaptureDevices()
    {
        extractConfiguredAudioCaptureDevices();
        extractConfiguredVideoCaptureDevices();
    }

    /**
     * Returns the configured video capture device with the specified
     * output format.
     * @param format the output format of the video format.
     * @return CaptureDeviceInfo for the video device.
     */
    private CaptureDeviceInfo extractConfiguredVideoCaptureDevice(Format format)
    {
        @SuppressWarnings("unchecked")
        List<CaptureDeviceInfo> videoCaptureDevices
            = CaptureDeviceManager.getDeviceList(format);
        CaptureDeviceInfo videoCaptureDevice = null;

        if (videoCaptureDevices.size() > 0)
        {
            ConfigurationService cfg = LibJitsi.getConfigurationService();
            String videoDevName
                = (cfg == null) ? null : cfg.getString(PROP_VIDEO_DEVICE);

            if (videoDevName == null)
                videoCaptureDevice = videoCaptureDevices.get(0);
            else
            {
                for (CaptureDeviceInfo captureDeviceInfo : videoCaptureDevices)
                {
                    if (videoDevName.equals(captureDeviceInfo.getName()))
                    {
                        videoCaptureDevice = captureDeviceInfo;
                        break;
                    }
                }
            }

            if ((videoCaptureDevice != null) && logger.isInfoEnabled())
            {
                logger.info(
                        "Found "
                            + videoCaptureDevice.getName()
                            + " as a "
                            + format
                            + " video capture device.");
            }
        }
        return videoCaptureDevice;
    }

    /**
     * Returns a device that we could use for audio capture.
     *
     * @return the CaptureDeviceInfo of a device that we could use for audio
     *         capture.
     */
    public CaptureDeviceInfo2 getAudioCaptureDevice()
    {
        AudioSystem audioSystem = getAudioSystem();

        return
            (audioSystem == null)
                ? null
                : audioSystem.getSelectedDevice(AudioSystem.DataFlow.CAPTURE);
    }

    /**
     * Gets the list of audio capture devices which are available through this
     * <tt>DeviceConfiguration</tt>, amongst which is
     * {@link #getAudioCaptureDevice()} and represent acceptable values
     * for {@link #setAudioCaptureDevice(CaptureDeviceInfo, boolean)}
     *
     * @return an array of <tt>CaptureDeviceInfo</tt> describing the audio
     *         capture devices available through this
     *         <tt>DeviceConfiguration</tt>
     */
    public List<CaptureDeviceInfo2> getAvailableAudioCaptureDevices()
    {
        return audioSystem.getDevices(AudioSystem.DataFlow.CAPTURE);
    }

    public AudioSystem getAudioSystem()
    {
        return audioSystem;
    }

    /**
     * Returns a list of available <tt>AudioSystem</tt>s. By default,  an
     * <tt>AudioSystem</tt> is considered available if it reports at least one
     * device.
     *
     * @return an array of available <tt>AudioSystem</tt>s
     */
    public AudioSystem[] getAvailableAudioSystems()
    {
        AudioSystem[] audioSystems =  AudioSystem.getAudioSystems();

        if ((audioSystems == null) || (audioSystems.length == 0))
            return audioSystems;
        else
        {
            List<AudioSystem> audioSystemsWithDevices
                = new ArrayList<AudioSystem>();

            for (AudioSystem audioSystem : audioSystems)
            {
                if (!NoneAudioSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                        audioSystem.getLocatorProtocol()))
                {
                    List<CaptureDeviceInfo2> captureDevices
                        = audioSystem.getDevices(AudioSystem.DataFlow.CAPTURE);

                    if ((captureDevices == null)
                            || (captureDevices.size() <= 0))
                    {
                        if ((AudioSystem.FEATURE_NOTIFY_AND_PLAYBACK_DEVICES
                                    & audioSystem.getFeatures())
                                == 0)
                        {
                            continue;
                        }
                        else
                        {
                            List<CaptureDeviceInfo2> notifyDevices
                                = audioSystem.getDevices(
                                        AudioSystem.DataFlow.NOTIFY);

                            if ((notifyDevices == null)
                                    || (notifyDevices.size() <= 0))
                            {
                                List<CaptureDeviceInfo2> playbackDevices
                                    = audioSystem.getDevices(
                                        AudioSystem.DataFlow.PLAYBACK);
    
                                if ((playbackDevices == null)
                                        || (playbackDevices.size() <= 0))
                                {
                                    continue;
                                }
                            }
                        }
                    }
                }
                audioSystemsWithDevices.add(audioSystem);
            }

            int audioSystemsWithDevicesCount = audioSystemsWithDevices.size();

            return
                (audioSystemsWithDevicesCount == audioSystems.length)
                    ? audioSystems
                    : audioSystemsWithDevices.toArray(
                            new AudioSystem[audioSystemsWithDevicesCount]);
        }
    }

    public void setAudioSystem(AudioSystem audioSystem, boolean save)
    {
        if (this.audioSystem != audioSystem)
        {
            // Removes the registration to change listener only if this audio
            // sytem does not supports reinitialize.
            if (this.audioSystem != null
                    && (this.audioSystem.getFeatures()
                        & DeviceSystem.FEATURE_REINITIALIZE) == 0)
            {
                this.audioSystem.removePropertyChangeListener(this);
            }

            AudioSystem oldValue = this.audioSystem;

            this.audioSystem = audioSystem;

            // Registers the new selected audio system.  Even if every
            // reloadable audio systems are already registered, the check for
            // dupplicate entries will be done by the addPropertyChangeListener
            // function.
            if (this.audioSystem != null)
            {
                this.audioSystem.addPropertyChangeListener(this);
            }

            if (save)
            {
                ConfigurationService cfg = LibJitsi.getConfigurationService();

                if (cfg != null)
                {
                    if (this.audioSystem == null)
                        cfg.removeProperty(PROP_AUDIO_SYSTEM);
                    else
                        cfg.setProperty(
                                PROP_AUDIO_SYSTEM,
                                this.audioSystem.getLocatorProtocol());
                }
            }

            firePropertyChange(PROP_AUDIO_SYSTEM, oldValue, this.audioSystem);
        }
    }

    /**
     * Gets the list of video capture devices which are available through this
     * <tt>DeviceConfiguration</tt>, amongst which is
     * {@link #getVideoCaptureDevice(MediaUseCase)} and represent acceptable
     * values for {@link #setVideoCaptureDevice(CaptureDeviceInfo, boolean)}
     *
     * @param useCase extract video capture devices that correspond to this
     * <tt>MediaUseCase</tt>
     * @return an array of <tt>CaptureDeviceInfo</tt> describing the video
     *         capture devices available through this
     *         <tt>DeviceConfiguration</tt>
     */
    public List<CaptureDeviceInfo> getAvailableVideoCaptureDevices(
            MediaUseCase useCase)
    {
        Format[] formats
            = new Format[]
                    {
                        new AVFrameFormat(),
                        new VideoFormat(VideoFormat.RGB),
                        new VideoFormat(VideoFormat.YUV),
                        new VideoFormat(Constants.H264)
                    };
        Set<CaptureDeviceInfo> videoCaptureDevices
            = new HashSet<CaptureDeviceInfo>();

        for (Format format : formats)
        {
            @SuppressWarnings("unchecked")
            Vector<CaptureDeviceInfo> cdis
                = CaptureDeviceManager.getDeviceList(format);

            if (useCase != MediaUseCase.ANY)
            {
                for (CaptureDeviceInfo cdi : cdis)
                {
                    MediaUseCase cdiUseCase
                        = DeviceSystem.LOCATOR_PROTOCOL_IMGSTREAMING
                                .equalsIgnoreCase(
                                        cdi.getLocator().getProtocol())
                            ? MediaUseCase.DESKTOP
                            : MediaUseCase.CALL;

                    if (cdiUseCase.equals(useCase))
                        videoCaptureDevices.add(cdi);
                }
            }
            else
            {
                videoCaptureDevices.addAll(cdis);
            }
        }

        return new ArrayList<CaptureDeviceInfo>(videoCaptureDevices);
    }

    /**
     * Returns a device that we could use for video capture.
     *
     * @param useCase <tt>MediaUseCase</tt> that will determined device
     * we will use
     * @return the CaptureDeviceInfo of a device that we could use for video
     *         capture.
     */
    public CaptureDeviceInfo getVideoCaptureDevice(MediaUseCase useCase)
    {
        CaptureDeviceInfo dev = null;

        switch (useCase)
        {
        case ANY:
        case CALL:
            dev = videoCaptureDevice;
            break;
        case DESKTOP:
            List<CaptureDeviceInfo> devs
                = getAvailableVideoCaptureDevices(MediaUseCase.DESKTOP);

            if (devs.size() > 0)
                dev = devs.get(0);
            break;
        default:
            break;
        }

        return dev;
    }

    /**
     * Sets the device which is to be used by this
     * <tt>DeviceConfiguration</tt> for video capture.
     *
     * @param device a <tt>CaptureDeviceInfo</tt> describing device to be
     *            used by this <tt>DeviceConfiguration</tt> for video
     *            capture.
     * @param save whether we will save this option or not.
     */
    public void setVideoCaptureDevice(CaptureDeviceInfo device, boolean save)
    {
        if (videoCaptureDevice != device)
        {
            CaptureDeviceInfo oldDevice = videoCaptureDevice;

            videoCaptureDevice = device;

            if (save)
            {
                ConfigurationService cfg = LibJitsi.getConfigurationService();

                if (cfg != null)
                {
                    cfg.setProperty(
                            PROP_VIDEO_DEVICE,
                            (videoCaptureDevice == null)
                                ? NoneAudioSystem.LOCATOR_PROTOCOL
                                : videoCaptureDevice.getName());
                }
            }

            firePropertyChange(VIDEO_CAPTURE_DEVICE, oldDevice, device);
        }
    }

    /**
     * @return the audioNotifyDevice
     */
    public CaptureDeviceInfo getAudioNotifyDevice()
    {
        AudioSystem audioSystem = getAudioSystem();

        return
            (audioSystem == null)
                ? null
                : audioSystem.getSelectedDevice(AudioSystem.DataFlow.NOTIFY);
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
            cfg.setProperty(PROP_AUDIO_ECHOCANCEL, echoCancel);
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
            cfg.setProperty(PROP_AUDIO_DENOISE, denoise);
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
        boolean value = DEFAULT_AUDIO_ECHOCANCEL;

        if (cfg != null)
            value = cfg.getBoolean(PROP_AUDIO_ECHOCANCEL, value);
        return value;
    }

    /**
     * Get the echo cancellation filter length (in milliseconds).
     *
     * @return echo cancel filter length in milliseconds
     */
    public long getEchoCancelFilterLengthInMillis()
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();
        long value = DEFAULT_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS;

        if (cfg != null)
        {
            value
                = cfg.getLong(
                        PROP_AUDIO_ECHOCANCEL_FILTER_LENGTH_IN_MILLIS,
                        value);
        }
        return value;
    }

    /**
     * Gets the indicator which determines whether noise suppression is to be
     * performed for captured audio
     *
     * @return <tt>true</tt> if noise suppression is to be performed for
     * captured audio; otherwise, <tt>false</tt>
     */
    public boolean isDenoise()
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();
        boolean value = DEFAULT_AUDIO_DENOISE;

        if (cfg != null)
            value = cfg.getBoolean(PROP_AUDIO_DENOISE, value);

        return value;
    }

    /**
     * Registers the custom <tt>Renderer</tt> implementations defined by class
     * name in {@link #CUSTOM_RENDERERS} with JMF.
     */
    private void registerCustomRenderers()
    {
        @SuppressWarnings("unchecked")
        Vector<String> renderers
            = PlugInManager.getPlugInList(null, null, PlugInManager.RENDERER);
        boolean commit = false;

        for (String customRenderer : CUSTOM_RENDERERS)
        {
            if (customRenderer == null)
                continue;
            if (customRenderer.startsWith("."))
            {
                customRenderer
                    = "org.jitsi.impl.neomedia"
                        + ".jmfext.media.renderer"
                        + customRenderer;
            }
            if ((renderers == null) || !renderers.contains(customRenderer))
            {
                try
                {
                    Renderer customRendererInstance
                        = (Renderer)
                            Class.forName(customRenderer).newInstance();

                    PlugInManager.addPlugIn(
                            customRenderer,
                            customRendererInstance.getSupportedInputFormats(),
                            null,
                            PlugInManager.RENDERER);
                    commit = true;
                }
                catch (Throwable t)
                {
                    logger.error(
                            "Failed to register custom Renderer "
                                 + customRenderer
                                 + " with JMF.",
                             t);
                }
            }
        }

        /*
         * Just in case, bubble our JMF contributions at the top so that they
         * are considered preferred.
         */
        int pluginType = PlugInManager.RENDERER;
        @SuppressWarnings("unchecked")
        Vector<String> plugins
            = PlugInManager.getPlugInList(null, null, pluginType);

        if (plugins != null)
        {
            int pluginCount = plugins.size();
            int pluginBeginIndex = 0;

            for (int pluginIndex = pluginCount - 1;
                 pluginIndex >= pluginBeginIndex;)
            {
                String plugin = plugins.get(pluginIndex);

                if (plugin.startsWith("org.jitsi.")
                        || plugin.startsWith("net.java.sip.communicator."))
                {
                    plugins.remove(pluginIndex);
                    plugins.add(0, plugin);
                    pluginBeginIndex++;
                    commit = true;
                }
                else
                    pluginIndex--;
            }
            PlugInManager.setPlugInList(plugins, pluginType);
            if (logger.isTraceEnabled())
                logger.trace("Reordered plug-in list:" + plugins);
        }

        if (commit && !MediaServiceImpl.isJmfRegistryDisableLoad())
        {
            try
            {
                PlugInManager.commit();
            }
            catch (IOException ioex)
            {
                logger.warn(
                        "Failed to commit changes to the JMF plug-in list.");
            }
        }
    }

    /**
     * Gets the maximum allowed video bandwidth.
     *
     * @return the maximum allowed video bandwidth. The default value is
     * {@link #DEFAULT_VIDEO_RTP_PACING_THRESHOLD}.
     */
    public int getVideoRTPPacingThreshold()
    {
        if (videoMaxBandwidth == -1)
        {
            ConfigurationService cfg = LibJitsi.getConfigurationService();
            int value = DEFAULT_VIDEO_RTP_PACING_THRESHOLD;

            if (cfg != null)
                value = cfg.getInt(PROP_VIDEO_RTP_PACING_THRESHOLD, value);

            if(value > 0)
                videoMaxBandwidth = value;
            else
                videoMaxBandwidth = DEFAULT_VIDEO_RTP_PACING_THRESHOLD;
        }
        return videoMaxBandwidth;
    }

    /**
     * Sets and stores the maximum allowed video bandwidth.
     *
     * @param videoMaxBandwidth the maximum allowed video bandwidth
     */
    public void setVideoRTPPacingThreshold(int videoMaxBandwidth)
    {
        this.videoMaxBandwidth = videoMaxBandwidth;

        ConfigurationService cfg = LibJitsi.getConfigurationService();

        if (cfg != null)
        {
            if (videoMaxBandwidth != DEFAULT_VIDEO_RTP_PACING_THRESHOLD)
                cfg.setProperty(PROP_VIDEO_RTP_PACING_THRESHOLD,
                                videoMaxBandwidth);
            else
                cfg.removeProperty(PROP_VIDEO_RTP_PACING_THRESHOLD);
        }
    }

    /**
     * Gets the video bitrate.
     *
     * @return the video codec bitrate. The default value is
     * {@link #DEFAULT_VIDEO_BITRATE}.
     */
    public int getVideoBitrate()
    {
        if (videoBitrate == -1)
        {
            ConfigurationService cfg = LibJitsi.getConfigurationService();
            int value = DEFAULT_VIDEO_BITRATE;

            if (cfg != null)
                value = cfg.getInt(PROP_VIDEO_BITRATE, value);

            if(value > 0)
                videoBitrate = value;
            else
                videoBitrate = DEFAULT_VIDEO_BITRATE;
        }
        return videoBitrate;
    }

    /**
     * Sets and stores the video bitrate.
     *
     * @param videoBitrate the video codec bitrate
     */
    public void setVideoBitrate(int videoBitrate)
    {
        this.videoBitrate = videoBitrate;

        ConfigurationService cfg = LibJitsi.getConfigurationService();

        if (cfg != null)
        {
            if (videoBitrate != DEFAULT_VIDEO_BITRATE)
                cfg.setProperty(PROP_VIDEO_BITRATE, videoBitrate);
            else
                cfg.removeProperty(PROP_VIDEO_BITRATE);
        }
    }

    /**
     * Gets the frame rate set on this <tt>DeviceConfiguration</tt>.
     *
     * @return the frame rate set on this <tt>DeviceConfiguration</tt>. The
     * default value is {@link #DEFAULT_VIDEO_FRAMERATE}
     */
    public int getFrameRate()
    {
        if (frameRate == -1)
        {
            ConfigurationService cfg = LibJitsi.getConfigurationService();
            int value = DEFAULT_VIDEO_FRAMERATE;

            if (cfg != null)
                value = cfg.getInt(PROP_VIDEO_FRAMERATE, value);

            frameRate = value;
        }
        return frameRate;
    }

    /**
     * Sets and stores the frame rate.
     *
     * @param frameRate the frame rate to be set on this
     * <tt>DeviceConfiguration</tt>
     */
    public void setFrameRate(int frameRate)
    {
        this.frameRate = frameRate;

        ConfigurationService cfg = LibJitsi.getConfigurationService();

        if (cfg != null)
        {
            if (frameRate != DEFAULT_VIDEO_FRAMERATE)
                cfg.setProperty(PROP_VIDEO_FRAMERATE, frameRate);
            else
                cfg.removeProperty(PROP_VIDEO_FRAMERATE);
        }
    }

    /**
     * Gets the video size set on this <tt>DeviceConfiguration</tt>.
     *
     * @return the video size set on this <tt>DeviceConfiguration</tt>
     */
    public Dimension getVideoSize()
    {
        if(videoSize == null)
        {
            ConfigurationService cfg = LibJitsi.getConfigurationService();
            int height = DEFAULT_VIDEO_HEIGHT;
            int width = DEFAULT_VIDEO_WIDTH;

            if (cfg != null)
            {
                height = cfg.getInt(PROP_VIDEO_HEIGHT, height);
                width = cfg.getInt(PROP_VIDEO_WIDTH, width);
            }

            videoSize = new Dimension(width, height);
        }
        return videoSize;
    }

    /**
     * Sets and stores the video size.
     *
     * @param videoSize the video size to be set on this
     * <tt>DeviceConfiguration</tt>
     */
    public void setVideoSize(Dimension videoSize)
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();

        if (cfg != null)
        {
            if ((videoSize.getHeight() != DEFAULT_VIDEO_HEIGHT)
                    || (videoSize.getWidth() != DEFAULT_VIDEO_WIDTH))
            {
                cfg.setProperty(PROP_VIDEO_HEIGHT, videoSize.height);
                cfg.setProperty(PROP_VIDEO_WIDTH, videoSize.width);
            }
            else
            {
                cfg.removeProperty(PROP_VIDEO_HEIGHT);
                cfg.removeProperty(PROP_VIDEO_WIDTH);
            }
        }

        this.videoSize = videoSize;

        firePropertyChange(
                VIDEO_CAPTURE_DEVICE,
                videoCaptureDevice, videoCaptureDevice);
    }

    /**
     * Listens for changes in the configuration and if such happen
     * we reset local values so next time we will update from
     * the configuration.
     *
     * @param ev the property change event
     */
    public void propertyChange(PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();

        if (AUDIO_CAPTURE_DEVICE.equals(propertyName)
                || AUDIO_NOTIFY_DEVICE.equals(propertyName)
                || AUDIO_PLAYBACK_DEVICE.equals(propertyName))
        {
            CaptureDeviceInfo oldValue = (CaptureDeviceInfo) ev.getOldValue();
            CaptureDeviceInfo newValue = (CaptureDeviceInfo) ev.getNewValue();

            // Try to switch to a new active audio system if we are currently
            // using the "none" system.
            switchFromNoneToActiveAudioSystem(newValue);

            CaptureDeviceInfo device = (oldValue == null) ? newValue : oldValue;

            // Fire an event on the selected device only if the event is
            // generated by the selected audio system.
            if((device == null)
                    || device.getLocator().getProtocol().equals(
                            getAudioSystem().getLocatorProtocol()))
            {
                firePropertyChange(propertyName, oldValue, newValue);
            }
        }
        else if (DeviceSystem.PROP_DEVICES.equals(propertyName))
        {
            if (ev.getSource() instanceof AudioSystem)
            {
                // Try to switch to a new active audio system if we are
                // currently using the "none" system.
                @SuppressWarnings("unchecked")
                List<CaptureDeviceInfo> newValue
                    = (List<CaptureDeviceInfo>) ev.getNewValue();

                switchFromNoneToActiveAudioSystem(
                        ((newValue == null) || newValue.isEmpty())
                            ? null
                            : newValue.get(0));

                firePropertyChange(
                        PROP_AUDIO_SYSTEM_DEVICES,
                        ev.getOldValue(),
                        newValue);
            }
        }
        else if (PROP_VIDEO_FRAMERATE.equals(propertyName))
        {
            frameRate = -1;
        }
        else if (PROP_VIDEO_HEIGHT.equals(propertyName)
                || PROP_VIDEO_WIDTH.equals(propertyName))
        {
            videoSize = null;
        }
        else if (PROP_VIDEO_RTP_PACING_THRESHOLD.equals(propertyName))
        {
            videoMaxBandwidth = -1;
        }
    }

    /**
     * Detects audio capture devices configured through JMF and disable audio if
     * none was found.
     */
    private void extractConfiguredAudioCaptureDevices()
    {
        if (logger.isInfoEnabled())
            logger.info("Looking for configured audio devices.");

        AudioSystem[] availableAudioSystems = getAvailableAudioSystems();

        if ((availableAudioSystems != null)
                && (availableAudioSystems.length != 0))
        {
            AudioSystem audioSystem = getAudioSystem();

            if (audioSystem != null)
            {
                boolean audioSystemIsAvailable = false;

                for (AudioSystem availableAudioSystem : availableAudioSystems)
                {
                    if (availableAudioSystem.equals(audioSystem))
                    {
                        audioSystemIsAvailable = true;
                        break;
                    }
                }
                if (!audioSystemIsAvailable)
                    audioSystem = null;
            }

            if (audioSystem == null)
            {
                ConfigurationService cfg = LibJitsi.getConfigurationService();

                if (cfg != null)
                {
                    String locatorProtocol = cfg.getString(PROP_AUDIO_SYSTEM);

                    if (locatorProtocol != null)
                    {
                        for (AudioSystem availableAudioSystem
                                : availableAudioSystems)
                        {
                            if (locatorProtocol.equalsIgnoreCase(
                                    availableAudioSystem.getLocatorProtocol()))
                            {
                                audioSystem = availableAudioSystem;
                                break;
                            }
                        }
                    }
                }

                if (audioSystem == null)
                    audioSystem = availableAudioSystems[0];

                setAudioSystem(audioSystem, false);
            }
        }
    }

    /**
     * Detects video capture devices configured through JMF and disable video if
     * none was found.
     */
    private void extractConfiguredVideoCaptureDevices()
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();
        String videoCaptureDeviceString
            = (cfg == null) ? null : cfg.getString(PROP_VIDEO_DEVICE);

        if (NoneAudioSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                videoCaptureDeviceString))
        {
            videoCaptureDevice = null;
        }
        else
        {
            if (logger.isInfoEnabled())
                logger.info("Scanning for configured Video Devices.");

            Format[] formats
                = new Format[]
                        {
                            new AVFrameFormat(),
                            new VideoFormat(VideoFormat.RGB),
                            new VideoFormat(VideoFormat.YUV),
                            new VideoFormat(Constants.H264)
                        };

            for (Format format : formats)
            {
                videoCaptureDevice
                    = extractConfiguredVideoCaptureDevice(format);
                if (videoCaptureDevice != null)
                    break;
            }
            if ((videoCaptureDevice == null) && logger.isInfoEnabled())
                logger.info("No Video Device was found.");
        }
    }

    /**
     * Registers this device configuration to all reloadable device sytem, in
     * order to receive events from device systems which are not currently
     * selected. I.e. a device system which changes its number of device from 0
     * to 1 or more, is becoming available and can be selected by the user.
     */
    private void registerToDeviceSystemPropertyChangeListener()
    {
        // Look at all kind of device systems: audio and video.
        for(MediaType mediaType: MediaType.values())
        {
            DeviceSystem[] deviceSystems
                = DeviceSystem.getDeviceSystems(mediaType);
            if(deviceSystems != null)
            {
                for (DeviceSystem deviceSystem : deviceSystems)
                {
                    // If the device system is reloadable, then register this
                    // device configuration.
                    if((deviceSystem.getFeatures()
                            & DeviceSystem.FEATURE_REINITIALIZE) != 0)
                    {
                        deviceSystem.addPropertyChangeListener(this);
                    }
                }
            }
        }
    }

    /**
     * Tries to automatically switch from the none audio system to a new active
     * audio system: detected by an event showing that there is at least one
     * device active for this system.
     *
     * @param newActiveDevice A device that have been recently detected has
     * available.
     */
    private void switchFromNoneToActiveAudioSystem(
            CaptureDeviceInfo newActiveDevice)
    {
        if(newActiveDevice != null)
        {
            String newActiveLocatorProtocol
                = newActiveDevice.getLocator().getProtocol();

            // If we are currently using the "none" system, and that the new
            // available device uses a different system: then switch to the new
            // audio system.
            if(!newActiveLocatorProtocol.equals(
                        NoneAudioSystem.LOCATOR_PROTOCOL)
                    && getAudioSystem().getLocatorProtocol().equals(
                            NoneAudioSystem.LOCATOR_PROTOCOL))
            {
                // If the AUDIO media type is disabled via
                // MediaServiceImpl.DISABLE_AUDIO_SUPPORT_PNAME, then the
                // DeviceSystem.initializeDeviceSystems will not instantiate
                // any other audio system (except the "none" one). Thereby, the
                // Audio.getAudioSystem will return null.
                AudioSystem newActiveAudioSystem
                    = AudioSystem.getAudioSystem(newActiveLocatorProtocol);

                if(newActiveAudioSystem != null)
                    setAudioSystem(newActiveAudioSystem, false);
            }
        }
        // The device has been unplugged checks. It may be good to check if the
        // current active audio system still has active devices. If not, then
        // we will choose another audio system, or the "none" one if no audio
        // system is available.
        else
        {
            extractConfiguredAudioCaptureDevices();
        }
    }
}
