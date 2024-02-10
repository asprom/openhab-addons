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

import static org.openhab.binding.aten.internal.AtenBindingConstants.*;

import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.aten.internal.config.AtenConfiguration;
import org.openhab.binding.aten.internal.state.AtenOutputZoneState;
import org.openhab.binding.aten.internal.state.SaveLoadType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class containing common functionality for the connectors.
 *
 * @author Roman Aspetsberger - Initial contribution
 */
@NonNullByDefault
public abstract class AtenConnector {

    private static final int MAX_NUMBER_OF_SUPPORTED_PROFILES = 16; // TODO: might be different between hardware -
                                                                    // configurable?

    private final Logger logger = LoggerFactory.getLogger(AtenConnector.class);

    protected final ScheduledExecutorService scheduler;
    protected final AtenConfiguration config;
    protected final AtenConnectorListener callbackListener;
    protected final String thingUID;

    public AtenConnector(AtenConfiguration config, AtenConnectorListener listener, ScheduledExecutorService scheduler,
            String thingUID) {
        this.config = config;
        this.callbackListener = listener;
        this.thingUID = thingUID;
        this.scheduler = scheduler;
    }

    /**
     * Try to connect to the matrix switch.
     */
    public abstract void connect();

    /**
     * This method must be called whenever the binding is disposed. The implementation has to take care of closing links
     * and freeing all resources.
     */
    public abstract void dispose();

    /**
     * Depending on the underlying link, this method will bring the command string on the line.
     *
     * @param command
     */
    protected abstract void internalSendCommand(String command);

    /**
     * Sends a switch command to the matrix switch in order to change the input channel for an output zone.
     *
     * @param command StringType command containing the input number string.
     * @param zone the output zone to change
     * @throws UnsupportedCommandTypeException is thrown if the type of given command is not supported or if the given
     *             output zone is not available.
     */
    public void sendInputCommand(Command command, short zone) throws UnsupportedCommandTypeException {
        zoneCheck(command, zone);
        if (command instanceof StringType) {
            String cmd = String.format(SWITCH_COMMAND_FORMAT, Short.parseShort(command.toString()), zone);
            internalSendCommand(cmd);
        } else {
            checkLastOptionRefresh(command);
        }
    }

    /**
     * Sends the command for muting an output zone.
     *
     * @param command
     * @param zone
     * @throws UnsupportedCommandTypeException
     */
    public void sendMuteCommand(Command command, short zone) throws UnsupportedCommandTypeException {
        zoneCheck(command, zone);

        if (command instanceof OnOffType) {
            String cmd = String.format(MUTE_COMMAND_FORMAT, zone, command);
            internalSendCommand(cmd);
        } else {
            checkLastOptionRefresh(command);
        }
    }

    public void sendVideoCommand(Command command, short zone) throws UnsupportedCommandTypeException {
        zoneCheck(command, zone);

        if (command instanceof OnOffType) {
            String cmd = String.format(VIDEO_COMMAND_FORMAT, zone, command);
            internalSendCommand(cmd);
        } else {
            checkLastOptionRefresh(command);
        }
    }

    public void sendCecCommand(Command command, short zone) throws UnsupportedCommandTypeException {
        zoneCheck(command, zone);

        if (command instanceof OnOffType) {
            String cmd = String.format(CEC_COMMAND_FORMAT, zone, command);
            internalSendCommand(cmd);
        } else {
            checkLastOptionRefresh(command);
        }
    }

    /**
     * Sends the read command to the matrix switch. It will respond with an overview string we can parse for getting the
     * actual zone setup.
     *
     * @param command
     */
    public void sendReadCommand(RefreshType command) {
        internalSendCommand(READ_COMMAND);
    }

    public void sendProfileCommand(Command command) throws UnsupportedCommandTypeException {
        if (command instanceof SaveLoadType) {
            SaveLoadType sfCommand = (SaveLoadType) command;
            short profileNumber = Short.parseShort(sfCommand.getValue());
            if (profileNumber < 1 || profileNumber > MAX_NUMBER_OF_SUPPORTED_PROFILES) {
                throw new UnsupportedCommandTypeException(command, "Profile number must be in range [1-"
                        + MAX_NUMBER_OF_SUPPORTED_PROFILES + "], profileNumber: " + profileNumber);
            }

            String cmd = String.format(PROFILE_COMMAND_FORMAT, profileNumber, sfCommand.getOperation());
            internalSendCommand(cmd);
        } else {
            throw new UnsupportedCommandTypeException(command);
        }
    }

    /**
     * Called by the underlying connection implementation whenever answer from switch is received.
     *
     * @param line
     */
    public void receivedLine(String line) {
        Matcher m = READ_ZONE_STATUS_PATTERN.matcher(line);
        if (m.matches()) {
            short outputZone = Short.parseShort(m.group(1));
            if (outputZone < 1 || outputZone > config.numberOfOutputZones) {
                callbackListener.connectionError(
                        "Read status doesn't fit to configuration: invalid zone number presented from switch: "
                                + outputZone);
                return;
            }

            short inputZone = Short.parseShort(m.group(2));

            boolean videoOn = "ON".equalsIgnoreCase(m.group(3));
            boolean audioOn = "ON".equalsIgnoreCase(m.group(4).trim());

            AtenOutputZoneState zoneState = new AtenOutputZoneState(outputZone);

            zoneState.setZoneMute(OnOffType.from(!audioOn));
            zoneState.setZoneVideo(OnOffType.from(videoOn));

            zoneState.setZoneInput(new StringType("" + inputZone));

            logger.debug("Zone config: output={} input={} video={} audio={}", outputZone, inputZone, videoOn, audioOn);

            callbackListener.zoneStateUpdated(zoneState);
        } else if (CONNECTION_ESTABLISHED_PATTERN.matcher(line).matches()) {
            callbackListener.loginSuccessfull();

            // We could successfully log in -> there's no need to do the refresh here, this is the job of the handler
            // which gets informed by the callback
        } else if (COMMAND_OK.equals(line)) {
            logger.debug("Received ACK for last command");
        } else if (COMMAND_NOK.equals(line)) {
            logger.error("Recived NACK for last command!");
        }
    }

    private void zoneCheck(Command command, short zone) throws UnsupportedCommandTypeException {
        if (zone < 1 || zone > config.getNumberOfOutputZones()) {
            throw new UnsupportedCommandTypeException(command,
                    "Zone must be in range [1-" + config.getNumberOfOutputZones() + "], zone: " + zone);
        }
    }

    private void checkLastOptionRefresh(Command command) throws UnsupportedCommandTypeException {
        if (command instanceof RefreshType) {
            sendReadCommand((RefreshType) command);
        } else {
            throw new UnsupportedCommandTypeException(command);
        }
    }
}
