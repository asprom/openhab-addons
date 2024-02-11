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
package org.openhab.binding.aten.internal.connector.telnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.aten.internal.config.AtenConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage telnet connection to the Aten HDMI matrix switch.
 * This implementation is taken from the Denon/Marantz binding and adopted accordingly.
 *
 * @author Jeroen Idserda - Initial contribution (1.x Binding)
 * @author Jan-Willem Veldhuis - Refactored for 2.x
 * @author Roman Aspetsberger - Adapted for Aten binding.
 */
@NonNullByDefault
public class AtenTelnetClientThread extends Thread {

    private Logger logger = LoggerFactory.getLogger(AtenTelnetClientThread.class);

    private static final Integer RECONNECT_DELAY = 60000; // 1 minute
    private static final Integer TIMEOUT = 60000; // 1 minute

    private AtenConfiguration config;
    private AtenTelnetListener listener;

    private boolean connected = false;

    private @Nullable Socket socket;
    private @Nullable OutputStreamWriter out;
    private @Nullable BufferedReader in;

    public AtenTelnetClientThread(AtenConfiguration config, AtenTelnetListener listener) {
        logger.debug("Aten listener created");
        this.config = config;
        this.listener = listener;
    }

    @Override
    public void run() {
        logger.debug("Starting client thread.");
        while (!isInterrupted()) {
            if (!connected) {
                connectTelnetSocket();
            }

            // improve thread safety by fixing the references for the method -> they must not change during a connection
            // lifetime anyway.
            final BufferedReader in = this.in;
            final OutputStreamWriter out = this.out;
            if (in == null || out == null) {
                logger.debug("Reconnect has failed.");
                break;
            }

            while (!isInterrupted() && connected) {
                try {
                    String line = in.readLine();
                    if (line == null) {
                        logger.debug("No more data read from client. Disconnecting..");
                        listener.telnetClientConnected(false);
                        disconnect();
                        break;
                    }
                    logger.trace("Received from {}: {}", config.getHostname(), line);
                    if (!line.isBlank()) {
                        listener.receivedLine(line);
                    }
                } catch (SocketTimeoutException e) {
                    logger.trace("Socket timeout");
                    // Disconnects are not always detected unless you write to the socket.
                    try {
                        out.write("\r");
                        out.flush();
                    } catch (IOException e2) {
                        logger.debug("Error writing to socket");
                        connected = false;
                    }
                } catch (IOException e) {
                    if (!isInterrupted()) {
                        // only log if we don't stop this on purpose causing a SocketClosed
                        logger.debug("Error in telnet connection ", e);
                    }
                    connected = false;
                    listener.telnetClientConnected(false);
                }
            }
        }
        disconnect();
        logger.debug("Stopped client thread");
    }

    public void sendCommand(String command) {
        final OutputStreamWriter out = this.out;
        if (out != null) {
            try {
                out.write(command + '\r');
                out.flush();
            } catch (IOException e) {
                logger.debug("Error sending command", e);
            }
        } else {
            logger.debug("Cannot send command, no telnet connection");
        }
    }

    public void shutdown() {
        disconnect();
    }

    private void connectTelnetSocket() {
        disconnect();
        int delay = 0;
        Socket socket = this.socket;
        while (!isInterrupted() && (socket == null || !socket.isConnected())) {
            try {
                Thread.sleep(delay);
                logger.debug("Connecting to {}", config.getHostname());

                // Use raw socket instead of TelnetClient here because TelnetClient sends an
                // extra newline char after each write which causes the connection to become
                // unresponsive.
                socket = new Socket();
                socket.connect(new InetSocketAddress(config.getHostname(), config.getTelnetPort()), TIMEOUT);
                socket.setKeepAlive(true);
                socket.setSoTimeout(TIMEOUT);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");

                this.socket = socket;

                connected = true;
                listener.telnetClientConnected(true);
                logger.debug("Telnet client connected to {}", config.getHostname());
            } catch (IOException e) {
                logger.debug("Cannot connect to {}", config.getHostname(), e);
                listener.telnetClientConnected(false);
            } catch (InterruptedException e) {
                logger.debug("Interrupted while connecting to {}", config.getHostname(), e);
                Thread.currentThread().interrupt();
            }
            delay = RECONNECT_DELAY;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private void disconnect() {
        final Socket socket = this.socket;
        if (socket != null) {
            logger.debug("Disconnecting socket");
            try {
                socket.close();
            } catch (IOException e) {
                logger.debug("Error while disconnecting telnet client", e);
            } finally {
                this.socket = null;
                out = null;
                in = null;
                listener.telnetClientConnected(false);
            }
        }
    }
}
