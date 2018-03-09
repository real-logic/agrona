/*
 * Copyright 2014-2018 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.concurrent;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.spi.MidiDeviceProvider;
import java.lang.reflect.Method;

/**
 * Control the supporting of high resolution timers for scheduled events by enabling the {@link MidiDevice}.
 */
public class HighResolutionTimer
{
    private static final MidiDevice MIDI_DEVICE;

    static
    {
        MidiDevice midiDevice = null;
        try
        {
            final Class<?> mediaProviderClass = Class.forName("com.sun.media.sound.MidiOutDeviceProvider");
            final Method getDeviceInfoMethod = mediaProviderClass.getMethod("getDeviceInfo");
            final MidiDeviceProvider provider = (MidiDeviceProvider)mediaProviderClass.newInstance();
            final MidiDevice.Info[] info = (MidiDevice.Info[])getDeviceInfoMethod.invoke(provider);

            midiDevice = info.length > 0 ? provider.getDevice(info[0]) : null;
        }
        catch (final Exception ignore)
        {
        }

        MIDI_DEVICE = midiDevice;
    }

    private static volatile boolean isEnabled = false;

    /**
     * Has the high resolution timer been enabled?
     *
     * @return true if the we believe it is enabled otherwise false.
     */
    public static boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * Attempt to enable the {@link MidiDevice} which requires the high resolution timer.
     */
    public static synchronized void enable()
    {
        if (null != MIDI_DEVICE && !MIDI_DEVICE.isOpen())
        {
            try
            {
                MIDI_DEVICE.open();
            }
            catch (final MidiUnavailableException ignore)
            {
                isEnabled = false;
                return;
            }

            isEnabled = true;
        }
    }

    /**
     * Attempt to disable the high resolution timer by closing the {@link MidiDevice}.
     */
    public static synchronized void disable()
    {
        if (MIDI_DEVICE != null && MIDI_DEVICE.isOpen())
        {
            MIDI_DEVICE.close();
        }

        isEnabled = false;
    }
}