/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.aten.internal.state;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;

/**
 * DTO for the status of a single output zone when fetched from the switch.
 *
 * @author Roman Aspetsberger - initial contribution
 *
 */
@NonNullByDefault
public final class AtenOutputZoneState {

    // TODO: how to support EDID
    private final short zoneNr;
    private OnOffType zoneMute = OnOffType.OFF;
    private OnOffType zoneVideo = OnOffType.ON;
    private State zoneInput = StringType.EMPTY;
    private boolean cecEnabled = false;

    public AtenOutputZoneState(short zoneNr) {
        this.zoneNr = zoneNr;
    }

    public State getZoneMute() {
        return zoneMute;
    }

    public void setZoneMute(OnOffType zoneMute) {
        this.zoneMute = zoneMute;
    }

    public State getZoneInput() {
        return zoneInput;
    }

    public void setZoneInput(State zoneInput) {
        this.zoneInput = zoneInput;
    }

    public boolean isCECenabled() {
        return cecEnabled;
    }

    public void setCECenabled(boolean cecEnabled) {
        this.cecEnabled = cecEnabled;
    }

    public short getZoneNr() {
        return zoneNr;
    }

    public State getZoneVideo() {
        return zoneVideo;
    }

    public State getZoneAudio() {
        // audio is inverted mute
        return zoneMute == OnOffType.OFF ? OnOffType.ON : OnOffType.OFF;
    }

    public void setZoneVideo(OnOffType zoneVideo) {
        this.zoneVideo = zoneVideo;
    }
}
