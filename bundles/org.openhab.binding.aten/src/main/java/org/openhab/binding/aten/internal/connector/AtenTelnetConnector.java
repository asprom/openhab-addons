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

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.aten.internal.config.AtenConfiguration;
import org.openhab.binding.aten.internal.connector.telnet.AtenTelnetClientThread;
import org.openhab.binding.aten.internal.connector.telnet.AtenTelnetListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class makes the connection to the receiver and manages it.
 * It is also responsible for sending commands to the receiver.
 *
 * This implementation is taken from the Denon/Marantz binding and adopted accordingly.
 *
 * @author Roman Aspetsberger - Initial contribution
 */
@NonNullByDefault
public class AtenTelnetConnector extends AtenConnector implements AtenTelnetListener {

    private final Logger logger = LoggerFactory.getLogger(AtenTelnetConnector.class);

    private @Nullable AtenTelnetClientThread telnetClientThread;
    protected boolean disposing = false;
    private @Nullable Future<?> telnetStateRequest;

    public AtenTelnetConnector(AtenConfiguration config, AtenConnectorListener listener,
            ScheduledExecutorService scheduler, String thingUID) {
        super(config, listener, scheduler, thingUID);
    }

    /**
     * Set up the connection to the receiver. Either using Telnet or serial.
     */
    @Override
    public void connect() {
        final AtenTelnetClientThread telnetClientThread = new AtenTelnetClientThread(config, this);
        telnetClientThread.setName("OH-binding-" + thingUID);
        telnetClientThread.start();

        this.telnetClientThread = telnetClientThread;
    }

    @Override
    protected void internalSendCommand(String command) {
        logger.debug("Sending command '{}'", command);
        if (command.isBlank()) {
            logger.warn("Trying to send empty command");
            return;
        }

        final AtenTelnetClientThread telnetClientThread = this.telnetClientThread;
        if (telnetClientThread != null) {
            telnetClientThread.sendCommand(command.toLowerCase()); // aten switch only allows lower case
        }
    }

    @Override
    public void telnetClientConnected(boolean connected) {
        if (!connected) {
            if (config.isTelnet() && !disposing) {
                logger.debug("Telnet client disconnected.");
                callbackListener.connectionError("Error connecting to the telnet port.");
            }
        } else {
            scheduler.submit(() -> {
                // first try to login!
                internalSendCommand(config.username);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    logger.trace("telnetClientConnected() - Interrupted while sending username.");
                    Thread.currentThread().interrupt();
                }

                internalSendCommand(config.password);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    logger.trace("telnetClientConnected() - Interrupted while sending password.");
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    /**
     * Shutdown the telnet client (if initialized)
     */
    @Override
    public void dispose() {
        logger.debug("disposing connector");
        disposing = true;

        final Future<?> telnetStateRequest = this.telnetStateRequest;

        if (telnetStateRequest != null) {
            telnetStateRequest.cancel(true);
            this.telnetStateRequest = null;
        }

        final AtenTelnetClientThread telnetClientThread = this.telnetClientThread;

        if (telnetClientThread != null) {
            telnetClientThread.interrupt();
            // Invoke a shutdown after interrupting the thread to close the socket immediately,
            // otherwise the client keeps running until a line was received from the telnet connection
            telnetClientThread.shutdown();
            this.telnetClientThread = null;
        }
    }
}
