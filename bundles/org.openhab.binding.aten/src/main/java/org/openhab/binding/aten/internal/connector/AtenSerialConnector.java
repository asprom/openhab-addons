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
 * todo: serial implementation not yet finalized.
 *
 * @author Roman Aspetsberger - Initial contribution.
 *
 */
@NonNullByDefault
public class AtenSerialConnector extends AtenConnector {

    public AtenSerialConnector(AtenConfiguration config, AtenConnectorListener listener,
            ScheduledExecutorService scheduler, String thingUID) {
        super(config, listener, scheduler, thingUID);
    }

    @Override
    public void connect() {

        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void internalSendCommand(String command) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }
}
