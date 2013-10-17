/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device;

import java.util.*;

import javax.media.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.util.*;

/**
 * Manages the list of active (currently plugged-in) capture/notify/playback
 * devices and manages user preferences between all known devices (previously
 * and actually plugged-in).
 *
 * @author Vincent Lucas
 * @author Lyubomir Marinov
 */
public abstract class Devices
{
    /**
     * The <tt>Logger</tt> used by this instance for logging output.
     */
    private static Logger logger = Logger.getLogger(Devices.class);

    /**
     * The name of the <tt>ConfigurationService</tt> <tt>boolean</tt> property
     * which indicates whether the automatic selection of USB devices must be
     * disabled. The default value is <tt>false</tt>.
     */
    private static final String PROP_DISABLE_USB_DEVICE_AUTO_SELECTION
        = "org.jitsi.impl.neomedia.device.disableUsbDeviceAutoSelection";

    /**
     * The audio system managing this device list.
     */
    private final AudioSystem audioSystem;

    /**
     * The selected active device.
     */
    private CaptureDeviceInfo2 device = null;

    /**
     * The list of device ID/names saved by the configuration service and
     * previously saved given user preference order.
     */
    private final List<String> devicePreferences = new ArrayList<String>();

    /**
     * Whether we have already loaded device configuration from the config
     * service.
     */
    private boolean loadedDeviceConfig = false;

    /**
     * The list of <tt>CaptureDeviceInfo2</tt>s which are active/plugged-in.
     */
    private List<CaptureDeviceInfo2> activeDevices;

    /**
     * Initializes the device list management.
     *
     * @param audioSystem The audio system managing this device list.
     */
    public Devices(AudioSystem audioSystem)
    {
        this.audioSystem = audioSystem;
    }

    /**
     * Adds a new device in the preferences (at the first active position if the
     * isSelected argument is true).
     *
     * @param newsDeviceIdentifier The identifier of the device to add int first
     * active position of the preferences.
     * @param isSelected True if the device is the selected one.
     */
    private void addToDevicePreferences(
            String newDeviceIdentifier,
            boolean isSelected)
    {
        synchronized(devicePreferences)
        {
            devicePreferences.remove(newDeviceIdentifier);
            // A selected device is placed on top of the list: this is the new
            // preferred device.
            if(isSelected)
            {
                devicePreferences.add(0, newDeviceIdentifier);
            }
            // If there is no active device or the device is not selected, then
            // set the new device to the end of the device preference list.
            else
            {
                devicePreferences.add(newDeviceIdentifier);
            }
        }
    }

    /**
     * Gets a <tt>CapatureDeviceInfo2</tt> which is known to this instance and
     * is identified by a specific <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> of the
     * <tt>CaptureDeviceInfo2</tt> to be returned
     * @return a <tt>CaptureDeviceInfo2</tt> which is known to this instance and
     * is identified by the specified <tt>locator</tt>
     */
    public CaptureDeviceInfo2 getDevice(MediaLocator locator)
    {
        CaptureDeviceInfo2 device = null;

        if ((locator != null) && (activeDevices != null))
        {
            for (CaptureDeviceInfo2 aDevice : activeDevices)
            {
                MediaLocator aLocator = aDevice.getLocator();

                if (locator.equals(aLocator))
                {
                    device = aDevice;
                    break;
                }
            }
        }
        return device;
    }

    /**
     * Returns the list of the <tt>CaptureDeviceInfo2</tt>s which are
     * active/plugged-in.
     *
     * @return the list of the <tt>CaptureDeviceInfo2</tt>s which are
     * active/plugged-in
     */
    public List<CaptureDeviceInfo2> getDevices()
    {
        List<CaptureDeviceInfo2> devices;

        if (this.activeDevices == null)
        {
            devices = Collections.emptyList();
        }
        else
        {
            devices = new ArrayList<CaptureDeviceInfo2>(this.activeDevices);
        }
        return devices;
    }

    /**
     * Returns the list of the <tt>CaptureDeviceInfo2</tt>s which have ever
     * been seen (and are therefore saved in config).
     *
     * @return the list of the <tt>CaptureDeviceInfo2</tt>s which have ever
     * been seen
     */
    public String[] getAllDevices()
    {
        synchronized(devicePreferences)
        {
            return devicePreferences.toArray(
                new String[devicePreferences.size()]);
        }
    }

    /**
     * Returns the property of the capture devices.
     *
     * @return The property of the capture devices.
     */
    protected abstract String getPropDevice();

    /**
     * Gets the selected active device.
     *
     * @param activeDevices the list of the active devices
     * @return the selected active device
     */
    public CaptureDeviceInfo2 getSelectedDevice(
            List<CaptureDeviceInfo2> activeDevices)
    {
        if (activeDevices != null)
        {
            logger.debug("Got some active devices");
            String property = getPropDevice();

            loadDevicePreferences(property);

            boolean isEmptyList = devicePreferences.isEmpty();

            // Search if an active device is a new one (is not stored in the
            // preferences yet). If true, then active this device and set it as
            // default device (only for USB devices since the user has
            // deliberately plugged in the device).
            for (int i = activeDevices.size() - 1; i >= 0; i--)
            {
                CaptureDeviceInfo2 activeDevice = activeDevices.get(i);
                logger.debug("Examining " + activeDevice.getName());

                if (!devicePreferences.contains(activeDevice.getName()))
                {
                    logger.debug("Device preferences does not contain model");

                    // By default, select automatically the USB devices.
                    boolean isSelected
                        = activeDevice.isSameTransportType("USB");
                    ConfigurationService cfg
                        = LibJitsi.getConfigurationService();
                    // Desactivate the USB device automatic selection if the
                    // property is set to true.
                    if ((cfg != null) && cfg.getBoolean(
                                PROP_DISABLE_USB_DEVICE_AUTO_SELECTION,
                                false))
                    {
                        isSelected = false;
                    }

                    // When initiates the first list (when there is no user
                    // preferences yet), set the Bluetooh and Airplay to the end
                    // of the list (this corresponds to move all other type
                    // of devices on top of the preference list).
                    if(isEmptyList
                            && !activeDevice.isSameTransportType("Bluetooth")
                            && !activeDevice.isSameTransportType("AirPlay"))
                    {
                        isSelected = true;
                    }
                    logger.debug("Is selected " + isSelected);

                    // Adds the device in the preference list (to the end of the
                    // list, or on top if selected.
                    saveDevice(property, activeDevice, isSelected);
                }
            }

            // Search if an active device match one of the previously configured
            // in the preferences.
            synchronized(devicePreferences)
            {
                for (String devicePreference : devicePreferences)
                {
                    logger.debug("Searching preferred devices, looking at " +
                                                              devicePreference);

                    for (CaptureDeviceInfo2 activeDevice : activeDevices)
                    {
                        // If we have found the "preferred" device among active
                        // device.
                        if (devicePreference.equals(activeDevice.getName()))
                        {
                            logger.debug("Found a preferred active device");
                            return activeDevice;
                        }
                        // If the "none" device is the "preferred" device among
                        // "active" device.
                        else if (devicePreference.equals(
                                    NoneAudioSystem.LOCATOR_PROTOCOL))
                        {
                            logger.debug("Found a none device");
                            return null;
                        }
                    }
                }
            }
        }

        // Else if nothing was found, then returns null.
        return null;
    }

    /**
     * Loads device name ordered with user's preference from the
     * <tt>ConfigurationService</tt>.
     *
     * @param property the name of the <tt>ConfigurationService</tt> property
     * which specifies the user's preference.
     */
    private void loadDevicePreferences(String property)
    {
        synchronized (devicePreferences)
        {
            // Don't load the preferences from the config service if we already
            // have them available.  Note:  the calling code really shouldn't
            // be calling this method in this case but it currently does (e.g.
            // over 100 times during startup).
            if (loadedDeviceConfig)
            {
                return;
            }

            ConfigurationService cfg = LibJitsi.getConfigurationService();

            if (cfg != null)
            {
                String newProperty = audioSystem.getPropertyName(property + "_list");
                String deviceIdentifiersString = cfg.getString(newProperty);

                logger.debug("Loading device preferences to be " +
                        deviceIdentifiersString);

                if (deviceIdentifiersString != null)
                {
                    devicePreferences.clear();
                    // We must parse the string in order to load the device
                    // list.
                    String[] deviceIdentifiers = deviceIdentifiersString
                        .substring(2, deviceIdentifiersString.length() - 2)
                        .split("\", \"");
                    for(int i = 0; i < deviceIdentifiers.length; ++i)
                    {
                        // XXX: Temporary hack to handle migration from old
                        // WASAPI devices - old config for USB devices has a
                        // USB port number in it which we now strip in
                        // getIMMDeviceFriendlyName().
                        String pattern = "\\([0-9]+- ";
                        String deviceName =
                            deviceIdentifiers[i].replaceAll(pattern, "(");

                        // If we've already added this device name to the list,
                        // don't do so again.
                        if (devicePreferences.contains(deviceName))
                        {
                            logger.debug(
                                "Removing duplicate device from config: " +
                                deviceName);
                            continue;
                        }

                        devicePreferences.add(deviceName);
                    }
                }
                else
                {
                    // Use the old/legacy property to load the last preferred
                    // device.
                    String oldProperty = audioSystem.getPropertyName(property);

                    deviceIdentifiersString = cfg.getString(oldProperty);
                    if ((deviceIdentifiersString != null)
                            && !NoneAudioSystem.LOCATOR_PROTOCOL
                                .equalsIgnoreCase(deviceIdentifiersString))
                    {
                        devicePreferences.clear();
                        devicePreferences.add(deviceIdentifiersString);
                    }
                }

                loadedDeviceConfig = true;
            }
        }

        // Now replace any old device config (old PortAudio config) that has
        // the UID of the devices with the names instead.
        renameToDeviceNames(activeDevices);
    }

    /**
     * Renames any devices with UIDs as their name to have names.
     *
     * @param activeDevices The list of the active devices.
     */
    private void renameToDeviceNames(
            List<CaptureDeviceInfo2> activeDevices)
    {
        for (CaptureDeviceInfo2 activeDevice : activeDevices)
        {
            String name = activeDevice.getName();
            String id = activeDevice.getModelIdentifier();

            // If the name and identifier for the device don't match and the
            // identifier is currently used in the device preferences, replace
            // it with the name.
            if (!name.equals(id))
            {
                synchronized (devicePreferences)
                {
                    do
                    {
                        int idIndex = devicePreferences.indexOf(id);
                        if (idIndex == -1)
                        {
                            // Not in device preferences so nothing to do.
                            break;
                        }
                        else
                        {
                            // The id is in the preferences.  We need to either
                            // remove it (if the name is also in there) or
                            // replace it (if the name isn't there).
                            int nameIndex = devicePreferences.indexOf(name);
                            if (nameIndex == -1)
                            {
                                // No name, replace id with name.
                                devicePreferences.set(idIndex, name);
                            }
                            else
                            {
                                // Name exists, just remove id.
                                devicePreferences.remove(idIndex);
                            }
                        }
                    }
                    while (true);
                }
            }
        }
    }

    /**
     * Saves the new selected device in top of the user preferences.
     *
     * @param property the name of the <tt>ConfigurationService</tt> property
     * into which the user's preference with respect to the specified
     * <tt>CaptureDeviceInfo</tt> is to be saved
     * @param selectedDevice The device selected by the user.
     * @param isSelected True if the device is the selected one.
     */
    private void saveDevice(
            String property,
            CaptureDeviceInfo2 device,
            boolean isSelected)
    {
        String selectedDeviceIdentifier
            = (device == null) ? NoneAudioSystem.LOCATOR_PROTOCOL :
                device.getName();

        // Sorts the user preferences to put the selected device on top.
        addToDevicePreferences(
                selectedDeviceIdentifier,
                isSelected);

        // Saves the user preferences.
        writeDevicePreferences(property);
    }

    /**
     * Selects the active device.
     *
     * @param device the selected active device
     * @param save <tt>true</tt> to save the choice in the configuration;
     * <tt>false</tt>, otherwise
     */
    public void setDevice(
            CaptureDeviceInfo2 device,
            boolean save)
    {
        // Checks if there is a change.
        if ((device == null) || !device.equals(this.device))
        {
            String property = getPropDevice();
            CaptureDeviceInfo2 oldValue = this.device;

            // Saves the new selected device in top of the user preferences.
            if (save)
                saveDevice(property, device, true);

            this.device = device;

            audioSystem.propertyChange(property, oldValue, this.device);
        }
    }

    /**
     * Sets the list of <tt>CaptureDeviceInfo2</tt>s which are
     * active/plugged-in.
     *
     * @param devices the list of <tt>CaptureDeviceInfo2</tt>s which are
     * active/plugged-in
     */
    public void setDevices(List<CaptureDeviceInfo2> devices)
    {
        this.activeDevices
            = (devices == null)
                ? null
                : new ArrayList<CaptureDeviceInfo2>(devices);
    }

    /**
     * Saves the device preferences and write it to the configuration file.
     *
     * @param property the name of the <tt>ConfigurationService</tt> property
     */
    private void writeDevicePreferences(String property)
    {
        ConfigurationService cfg = LibJitsi.getConfigurationService();

        if (cfg != null)
        {
            property = audioSystem.getPropertyName(property + "_list");

            StringBuilder value = new StringBuilder("[\"");

            synchronized(devicePreferences)
            {
                int devicePreferenceCount = devicePreferences.size();

                if(devicePreferenceCount != 0)
                {
                    value.append(devicePreferences.get(0));
                    for(int i = 1; i < devicePreferenceCount; i++)
                    {
                        value.append("\", \"").append(devicePreferences.get(i));
                    }
                }
            }
            value.append("\"]");

            cfg.setProperty(property, value.toString());
        }
    }
}
