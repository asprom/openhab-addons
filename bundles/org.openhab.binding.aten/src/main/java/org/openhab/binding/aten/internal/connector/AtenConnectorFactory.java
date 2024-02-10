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
package org.openhab.binding.aten.internal.connector;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.aten.internal.config.AtenConfiguration;

/**
 * Returns the connector based on the configuration.
 * Currently there are 2 types supported: Serial and Telnet
 *
 * @author Roman Aspetsberger - Initial contribution
 */
@NonNullByDefault
public class AtenConnectorFactory {

    public AtenConnector getConnector(AtenConfiguration config, AtenConnectorListener listener,
            ScheduledExecutorService scheduler, String thingUID) {
        if (config.isTelnet()) {
            return new AtenTelnetConnector(config, listener, scheduler, thingUID);
        } else {
            return new AtenSerialConnector(config, listener, scheduler, thingUID);
        }
    }
}
