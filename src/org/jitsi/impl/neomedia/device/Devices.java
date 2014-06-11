/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device;

import java.util.*;
import java.util.regex.*;

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
     * Dummy UID if a device isn't connected, or we don't have any UUIDs for it
     */
    private static final String UID_DUMMY = "1234";

    /**
     * The audio system managing this device list.
     */
    private final AudioSystem audioSystem;

    /**
     * The selected active device.
     */
    private CaptureDeviceInfo2 device = null;

    /**
     * The list of device names saved by the configuration service and
     * previously saved given user preference order.
     *
     * Access should be synchronized on the object.
     */
    private final List<String> devicePreferences = new ArrayList<String>();

    /**
     * A map from device name to their UIDs saved by the configuration service
     * and previously saved given user preference order.
     *
     * Access should be synchronized based on devicePreferences.
     */
    private final HashMap<String, List<String>> deviceUIDs =
                                      new LinkedHashMap<String, List<String>>();

    /**
     * Whether we have already loaded device configuration from the config
     * service.
     */
    private boolean loadedDeviceConfig = false;

    /**
     * The list of <tt>CaptureDeviceInfo2</tt>s which are active/plugged-in.
     */
    private List<CaptureDeviceInfo2> activeDevices = new ArrayList<CaptureDeviceInfo2>();

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
     * @param newDeviceName The name of the new device
     * @param newDeviceIdentifier The identifier of the device to add
     * @param isSelected True if the device is the selected one.
     *
     * @return <tt>true</tt> if a change was made, <tt>false</tt> otherwise
     */
    private boolean addToDevicePreferences(
            String newDeviceName,
            String newDeviceUID,
            boolean isSelected)
    {
        synchronized(devicePreferences)
        {
            // Nothing to do if we already know about this device and it is not
            // selected
            if (!isSelected &&
                devicePreferences.contains(newDeviceName) &&
                !isNewUID(newDeviceUID))
            {
                logger.debug("Not adding device: " + newDeviceName + " (" + newDeviceUID + ")" +
                             " as it isn't selected and we already know about it");
                return false;
            }

            logger.debug("Adding new device: " + newDeviceName +
                    ", with uid=" + newDeviceUID +
                    " and isSelected=" + isSelected);

            // A selected device is placed on top of the list: this is the new
            // preferred device.
            if(isSelected)
            {
                devicePreferences.remove(newDeviceName);
                devicePreferences.add(0, newDeviceName);
            }
            else if (!devicePreferences.contains(newDeviceName))
            {
                logger.debug("Device name " + newDeviceName +
                             " has not been seen before");
                devicePreferences.add(newDeviceName);
            }

            // The same process is repeated for the UIDs existing for this device
            List<String> uids = deviceUIDs.get(newDeviceName);

            if (uids != null)
            {
                if (isSelected)
                {
                    uids.remove(newDeviceUID);
                    uids.add(0, newDeviceUID);
                }
                else if (!uids.contains(newDeviceUID))
                {
                    logger.debug("UID " + newDeviceUID + " has not been seen " +
                                 "before for device " + newDeviceName);
                    uids.add(newDeviceUID);
                }
            }
            else
            {
                logger.debug("Create UID store for new device " + newDeviceName);
                uids = new ArrayList<String>();
                uids.add(newDeviceUID);
                deviceUIDs.put(newDeviceName, uids);
            }
        }

        logger.debug("Added device: " + newDeviceName +
                     " with UID: " + newDeviceUID +
                     " to list of audio devices");
        return true;
    }

    /**
     * Determines if the given UID is one we haven't seen before
     *
     * @param newDeviceUID the device UID to check
     * @return Whether this is a new UID
     */
    private boolean isNewUID(String newDeviceUID)
    {
        synchronized(devicePreferences)
        {
            for (List<String> uidList : deviceUIDs.values())
            {
                if (uidList.contains(newDeviceUID))
                {
                    return false;
                }
            }
        }

        return true;
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
    public LinkedHashMap<String, String> getAllDevices()
    {
        LinkedHashMap<String, String> deviceNames = new LinkedHashMap<String, String>();

        synchronized(devicePreferences)
        {
            for (String devicePref : devicePreferences)
            {
                boolean isActiveDevice = false;
                List<String> matchingDevicesList = new ArrayList<String>();

                // Find the list of active devices that match this device
                // preference
                for (CaptureDeviceInfo2 activeDevice : activeDevices)
                {
                    if (devicePref.equals(activeDevice.getName()))
                    {
                        matchingDevicesList.add(activeDevice.getUID());
                    }
                }

                // If there are multiple connected devices of the same name
                // then we number the duplicates so the user can tell them
                // apart. We must update the map so we can correctly associate
                // a device UID with the new name we have given it.
                for (CaptureDeviceInfo2 activeDevice : activeDevices)
                {
                    if (devicePref.equals(activeDevice.getName()))
                    {
                        String deviceName = (matchingDevicesList.size() > 1) ?
                                            devicePref + " " + (matchingDevicesList.indexOf(activeDevice.getUID())+1) :
                                            devicePref;

                        // This device is currently plugged in
                        deviceNames.put(deviceName, activeDevice.getUID());
                        isActiveDevice = true;
                    }
                }

                // If this device is not plugged in, we still want to show it
                // in the preferences list
                if (!isActiveDevice)
                {
                    deviceNames.put(devicePref, null);
                }
            }
        }

        return deviceNames;
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
            logger.debug("Got " + activeDevices.size() + " " +
                         getDataflowType() + " active devices");
            String property = getPropDevice();

            loadDevicePreferences(property);
            synchronized(devicePreferences)
            {
                boolean isEmptyList = devicePreferences.isEmpty();

                // Search if an active device is a new one (is not stored in the
                // preferences yet). If true, then active this device and set it as
                // default device (only for USB devices since the user has
                // deliberately plugged in the device).
                for (int i = activeDevices.size() - 1; i >= 0; i--)
                {
                    CaptureDeviceInfo2 activeDevice = activeDevices.get(i);
                    logger.debug("Examining " + getDataflowType() + " device: " +
                                 activeDevice.getName() + " UID: " + activeDevice.getUID());

                    boolean isSelected = false;
                    if (!devicePreferences.contains(activeDevice.getName()))
                    {
                        logger.debug(getDataflowType() +
                                     "device preferences do not contain: " +
                                     activeDevice.getName());

                        // By default, select automatically the USB devices.
                        isSelected
                            = activeDevice.isSameTransportType("USB");
                        ConfigurationService cfg
                            = LibJitsi.getConfigurationService();

                        // Deactivate the USB device automatic selection if the
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
                    }

                    // Adds the device in the preference list (to the end of the
                    // list, or on top if selected.
                    saveDevice(property, activeDevice, isSelected);
                }

                // Search if an active device match one of the previously
                // configured in the preferences.
                for (String devicePreference : devicePreferences)
                {
                    logger.debug("Searching preferred devices, looking at " +
                                                              devicePreference);

                    List<CaptureDeviceInfo2> matchingDevices =
                                            new ArrayList<CaptureDeviceInfo2>();

                    // Go through each active device and check for a device
                    // name match with user preferences
                    for (CaptureDeviceInfo2 activeDevice : activeDevices)
                    {
                        // If we have found the "preferred" device among active
                        // device.
                        if (devicePreference.equals(activeDevice.getName()))
                        {
                            logger.debug("Selected device: " + activeDevice.getName());

                            matchingDevices.add(activeDevice);
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

                    if (matchingDevices.size() > 0)
                    {
                        // We have a list of devices that match on name. We
                        // now must check the UIDs of the devices to determine
                        // the most preferred device
                        List<String> uids = deviceUIDs.get(devicePreference);

                        if (uids != null)
                        {
                            // The list of UIDs is ordered, so loop through
                            // this returning the first matching device
                            for (String uid : uids)
                            {
                                for (CaptureDeviceInfo2 matchingDevice : matchingDevices)
                                {
                                    if (uid.equals(matchingDevice.getUID()))
                                    {
                                        return matchingDevice;
                                    }
                                }
                            }

                            // If we haven't returned yet then we have a
                            // matching device by name, but not by UID.
                            // Therefore return the first matching device by
                            // name.
                            return matchingDevices.get(0);
                        }
                        else
                        {
                            logger.debug("No UIDs stored for device " +
                                         devicePreference +
                                         " using first one found");
                            return matchingDevices.get(0);
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

            // We attempt to load using the new config style (V2.9.00+), if it
            // is not found then we load using the old config style (which
            // doesn't include device UIDs), and then migrate this to the new
            // config.

            ConfigurationService cfg = LibJitsi.getConfigurationService();
            if (cfg != null)
            {
                String newProperty = audioSystem.getPropertyName(property + "_list2");
                String deviceIdentifiersString = cfg.getString(newProperty);

                logger.debug("Loading " + getDataflowType() + " " +
                		"device preferences to be " +
                        deviceIdentifiersString);

                if (deviceIdentifiersString != null)
                {
                    devicePreferences.clear();
                    // We must parse the string in order to load the device
                    // list.
                    String[] deviceIdentifiers = deviceIdentifiersString
                        .substring(2, deviceIdentifiersString.length() - 2)
                        .split("\", \"");

                    // Device identifiers are now in the form:
                    // "name:<name> uid:<uid>"
                    Pattern pattern = Pattern.compile("name:(.+) uid:(.+)");
                    for (String device : deviceIdentifiers)
                    {
                        Matcher m = pattern.matcher(device);
                        if (m.find())
                        {
                            String deviceName = m.group(1);
                            String[] deviceUIDsList = m.group(2).replace("[", "").replace("]", "").split(";");

                            devicePreferences.add(deviceName);
                            List<String> uids = deviceUIDs.get(deviceName);

                            if (uids == null)
                            {
                                logger.warn("Loaded device " + deviceName +
                                             "had no saved UIDs");
                                uids = new ArrayList<String>();
                            }

                            for (String uid : deviceUIDsList)
                            {
                                uids.add(uid);
                            }
                            deviceUIDs.put(deviceName, uids);
                        }
                    }
                }
                else
                {
                    // Use the old/legacy property to load the last preferred
                    // device.
                    logger.info("Migrating old device preferences");
                    String oldProperty = audioSystem.getPropertyName(property + "_list");
                    deviceIdentifiersString = cfg.getString(oldProperty);
                    if (deviceIdentifiersString == null)
                    {
                        loadedDeviceConfig = true;
                        return;
                    }

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

                        // Migration code to add device UIDs to the current
                        // list of preferences
                        for (CaptureDeviceInfo2 activeDevice : activeDevices)
                        {
                            if (activeDevice.getName().equals(deviceName))
                            {
                                List<String> uidsList = deviceUIDs.get(deviceName);
                                if (uidsList == null)
                                {
                                    uidsList = new ArrayList<String>();
                                }

                                uidsList.add(activeDevice.getUID());

                                deviceUIDs.put(deviceName, uidsList);

                                logger.debug("Adding uid " + activeDevice.getUID()
                                             + " to device " + deviceName);
                            }
                        }

                        // If this device isn't currently connected then we must
                        // write a dummy UID in order to keep the device
                        // preference order.
                        if (deviceUIDs.get(deviceName) == null)
                        {
                            List<String> uidsList = new ArrayList<String>();
                            uidsList.add(UID_DUMMY);
                            deviceUIDs.put(deviceName, uidsList);
                        }

                    }

                    // Now replace any old device config (old PortAudio config) that has
                    // the UID of the devices with the names instead.
                    renameToDeviceNames(activeDevices);
                }

                loadedDeviceConfig = true;
            }
            else
            {
                logger.error("Failed to get config service when loading devices");
            }
        }

    }

    /**
     * Renames any devices with UIDs as their name to have names.
     *
     * @param activeDevices The list of the active devices.
     */
    private void renameToDeviceNames(
            List<CaptureDeviceInfo2> activeDevices)
    {
        if (activeDevices != null)
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
        if (addToDevicePreferences(selectedDeviceIdentifier,
                                   device.getUID(),
                                   isSelected))
        {
            // Saves the user preferences.
            logger.info("Devices changed: saving changed to " + getDataflowType() +
                    " device:" + device +
                    " to: " + property +
                    " selected: " + isSelected);
            writeDevicePreferences(property);
        }

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
            property = audioSystem.getPropertyName(property + "_list2");

            StringBuilder value = new StringBuilder("[\"");

            synchronized(devicePreferences)
            {
                int devicePreferenceCount = devicePreferences.size();

                if(devicePreferenceCount != 0)
                {
                    String deviceName = devicePreferences.get(0);

                    value.append("name:" + deviceName + " uid:" +
                                       getUIDsList(deviceName));

                    for(int i = 1; i < devicePreferenceCount; i++)
                    {
                        deviceName = devicePreferences.get(i);
                        value.append("\", \"");
                        value.append("name:" + deviceName);
                        value.append(" uid:" + getUIDsList(deviceName));
                    }
                }
            }
            value.append("\"]");

            cfg.setProperty(property, value.toString());
        }
    }

    /**
     * Returns a string representation of the list of UIDs, separated by
     * semi-colons
     *
     * @param list the list of strings to serialize
     * @return the string representation of the input list of UIDs
     */
    private String getUIDsList(String deviceName)
    {
        List<String> uids;

        synchronized(devicePreferences)
        {
            uids = deviceUIDs.get(deviceName);
        }

        if (uids == null || (uids.size() == 0))
        {
            logger.error("No configured UIDs for " + deviceName +
                         " using dummy UID " + UID_DUMMY + " instead");

            return UID_DUMMY;
        }

        String uidList = null;
        for (String uid : uids)
        {
            if (uidList == null)
            {
                uidList = uid;
            }
            else
            {
                uidList = uidList + ";" + uid;
            }
        }

        return uidList;
    }

    protected abstract String getDataflowType();
}
