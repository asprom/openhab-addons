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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.aten.internal.state.AtenOutputZoneState;

/**
 * Listener interface for callbacks from the connector to the handler when data is received from the switch.
 *
 * @author Roman Aspetsberger - initial contribution
 *
 */
@NonNullByDefault
public interface AtenConnectorListener {

    /**
     * Connector has detected a successful login to the switch interface.
     */
    public void loginSuccessfull();

    /**
     * There was an error when tried to connect to the matrix switch. Details can be found in given error message.
     *
     * @param errorMessage
     */
    public void connectionError(@Nullable String errorMessage);

    /**
     * The connector has received a state update from the switch.
     *
     * @param zoneState
     */
    public void zoneStateUpdated(AtenOutputZoneState zoneState);
}
