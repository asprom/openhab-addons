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
package org.openhab.binding.aten.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link AtenConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Roman Aspetsberger - Initial contribution
 */
@NonNullByDefault
public class AtenConfiguration {

    public String hostname = "";
    public int telnetPort;
    public boolean telnet = true;

    public String username = "";
    public String password = "";

    public Integer numberOfOutputZones = Integer.valueOf(0);
    public Integer numberOfInputSources = Integer.valueOf(0);

    public Integer refreshInterval = Integer.valueOf(30);

    public String inputNames = "";
    public String profileNames = "";

    public String getHostname() {
        return hostname;
    }

    public int getTelnetPort() {
        return telnetPort;
    }

    public boolean isTelnet() {
        return telnet;
    }

    public boolean isSerial() {
        return !isTelnet();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public short getNumberOfInputSources() {
        return numberOfInputSources.shortValue();
    }

    public short getNumberOfOutputZones() {
        return numberOfOutputZones.shortValue();
    }

    public int getRefreshInterval() {
        return refreshInterval.intValue();
    }

    public String getInputNames() {
        return inputNames;
    }

    public String getProfileNames() {
        return profileNames;
    }
}
