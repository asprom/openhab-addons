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
package org.openhab.binding.aten.internal.handler;

import static org.openhab.binding.aten.internal.AtenBindingConstants.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.aten.internal.config.AtenConfiguration;
import org.openhab.binding.aten.internal.config.InputNameConfiguration;
import org.openhab.binding.aten.internal.connector.AtenConnector;
import org.openhab.binding.aten.internal.connector.AtenConnectorFactory;
import org.openhab.binding.aten.internal.connector.AtenConnectorListener;
import org.openhab.binding.aten.internal.connector.UnsupportedCommandTypeException;
import org.openhab.binding.aten.internal.state.AtenOutputZoneState;
import org.openhab.binding.aten.internal.state.InputSourceStateDescriptionProvider;
import org.openhab.binding.aten.internal.state.SaveLoadType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AtenHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Roman Aspetsberger - Initial contribution
 */
@NonNullByDefault
public class AtenHandler extends BaseThingHandler implements AtenConnectorListener {

    private final Logger logger = LoggerFactory.getLogger(AtenHandler.class);
    private static final int MAX_NUMBER_OF_SUPPORTED_ZONES = 32; // haven't seen any product with more zones
    private static final int MIN_REFRESH_TIME = 10; // value in seconds
    private static final int RETRY_TIME_SECONDS = 30;
    private static final Pattern ZONE_COMMAND_PATTERN = Pattern.compile("^zone(\\d+)(#.+)$");
    private static final Pattern GENERAL_COMMAND_PATTERN = Pattern.compile("^general(#.+)$");

    private final InputSourceStateDescriptionProvider stateDescriptionProvider;
    private @Nullable AtenConnector connector;
    private AtenConnectorFactory connectorFactory = new AtenConnectorFactory();
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> retryJob;

    public AtenHandler(Thing thing, InputSourceStateDescriptionProvider stateDescriptionProvider) {
        super(thing);
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    @Override
    public void initialize() {
        cancelRetry();

        final AtenConfiguration internalConfig = getConfigAs(AtenConfiguration.class);

        if (!checkConfiguration(internalConfig)) {
            return;
        }

        InputNameConfiguration inputNameConfig = new InputNameConfiguration(internalConfig.getInputNames(),
                internalConfig.getProfileNames());
        stateDescriptionProvider.setConfiguration(inputNameConfig);

        configureZoneChannels(internalConfig);
        updateStatus(ThingStatus.UNKNOWN);

        // create connection (either Telnet or Serial)
        // ThingStatus ONLINE/OFFLINE is set when the switch connection has been enabled / has failed.
        createConnection();
    }

    private void configureZoneChannels(AtenConfiguration config) {
        logger.debug("Configuring zone channels");

        ThingHandlerCallback callback = getCallback();

        ThingBuilder thingBuilder = editThing();

        if (callback != null) {
            for (int i = 1; i <= config.numberOfOutputZones; i++) {

                for (ChannelBuilder channelBuilder : callback.createChannelBuilders(
                        new ChannelGroupUID(getThing().getUID(), CHANNEL_ZONE_PREFIX + i),
                        CHANNEL_GROUP_TYPE_OUTPUT_ZONE)) {
                    Channel newChannel = channelBuilder.build(),
                            existingChannel = getThing().getChannel(newChannel.getUID().getId());
                    if (existingChannel != null) {
                        logger.trace("Thing '{}' already has an existing channel '{}'. Omit adding new channel '{}'.",
                                getThing().getUID(), existingChannel.getUID(), newChannel.getUID());
                        continue;
                    }
                    thingBuilder.withChannel(newChannel);
                }
            }

            // removing not needed channels!
            for (int i = config.getNumberOfOutputZones() + 1; i <= MAX_NUMBER_OF_SUPPORTED_ZONES; i++) {
                for (ChannelBuilder channelBuilder : callback.createChannelBuilders(
                        new ChannelGroupUID(getThing().getUID(), CHANNEL_ZONE_PREFIX + i),
                        CHANNEL_GROUP_TYPE_OUTPUT_ZONE)) {
                    Channel handledChannel = channelBuilder.build(),
                            existingChannel = getThing().getChannel(handledChannel.getUID().getId());
                    if (existingChannel != null) {
                        logger.trace(
                                "Thing '{}' removes existing channel '{}' because channel nr '{}' higher then max nr of supported output channels '{}'.",
                                getThing().getUID(), existingChannel.getUID(), i, config.getNumberOfOutputZones());
                        thingBuilder.withoutChannels(handledChannel);
                    }

                }
            }
        }

        // update possible input sources
        stateDescriptionProvider.setStateOptionsForInput(config.getNumberOfInputSources());

        updateThing(thingBuilder.build());
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(AtenActions.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        AtenConnector connector = this.connector;
        if (connector == null) {
            return;
        }

        try {
            Matcher m = ZONE_COMMAND_PATTERN.matcher(channelUID.getId());
            if (m.matches()) {
                short zone = Short.parseShort(m.group(1));
                String commandSuffix = m.group(2);
                if (CHANNEL_MUTE_SUFFIX.equals(commandSuffix)) {
                    connector.sendMuteCommand(command, zone);
                    if (command instanceof OnOffType) {
                        stateChanged(channelUID, (OnOffType) command);
                        // we have to update audio state, too
                        ChannelUID audioUID = getZoneUID(zone, CHANNEL_AUDIO_SUFFIX);
                        stateChanged(audioUID, invertOnOffType((OnOffType) command));
                    }
                } else if (CHANNEL_INPUT_SUFFIX.equals(commandSuffix)) {
                    connector.sendInputCommand(command, zone);
                    if (command instanceof StringType) {
                        stateChanged(channelUID, (StringType) command);
                    }
                } else if (CHANNEL_VIDEO_SUFFIX.equals(commandSuffix)) {
                    connector.sendVideoCommand(command, zone);
                    if (command instanceof OnOffType) {
                        stateChanged(channelUID, (OnOffType) command);
                    }
                } else if (CHANNEL_AUDIO_SUFFIX.equals(commandSuffix)) {
                    if (command instanceof OnOffType) {
                        // invert the state for the mute command
                        OnOffType muteState = invertOnOffType((OnOffType) command);
                        connector.sendMuteCommand(muteState, zone);
                        stateChanged(channelUID, (OnOffType) command);

                        ChannelUID muteChannel = getZoneUID(zone, CHANNEL_MUTE_SUFFIX);
                        stateChanged(muteChannel, muteState);
                    } else {
                        connector.sendMuteCommand(command, zone);
                    }
                } else if (CHANNEL_CEC_SUFFIX.equals(commandSuffix)) {
                    connector.sendCecCommand(command, zone);
                    if (command instanceof OnOffType) {
                        stateChanged(channelUID, (OnOffType) command);
                    }
                }
            } else if (((m = GENERAL_COMMAND_PATTERN.matcher(channelUID.getId())).matches())) {
                String commandSuffix = m.group(1);
                if (CHANNEL_PROFILE_SUFFIX.equals(commandSuffix)) {
                    connector.sendProfileCommand(SaveLoadType.getLoadType(command));
                    if (command instanceof StringType) {
                        stateChanged(channelUID, (StringType) command);
                    }
                }
            }
        } catch (UnsupportedCommandTypeException e) {
            logger.error("Error in command handling.", e);
        }
    }

    public boolean checkConfiguration(AtenConfiguration config) {
        // prevent too low values for polling interval
        if (config.refreshInterval < MIN_REFRESH_TIME) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "The polling interval should be at least " + MIN_REFRESH_TIME + " seconds!");
            return false;
        }
        // Check input and output zone count is within supported range
        if (config.getNumberOfInputSources() < 1 || config.getNumberOfInputSources() > MAX_NUMBER_OF_SUPPORTED_ZONES) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "This binding supports 1 to "
                    + MAX_NUMBER_OF_SUPPORTED_ZONES + " input zones. Please update the number of HDMI input zones.");
            return false;
        }
        if (config.getNumberOfOutputZones() < 1 || config.getNumberOfOutputZones() > MAX_NUMBER_OF_SUPPORTED_ZONES) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "This binding supports 1 to "
                    + MAX_NUMBER_OF_SUPPORTED_ZONES + " output zones. Please update the number of HDMI output zones.");
            return false;
        }
        return true;
    }

    @Override
    public void zoneStateUpdated(AtenOutputZoneState zoneState) {
        ChannelUID channelUID = getZoneUID(zoneState.getZoneNr(), CHANNEL_MUTE_SUFFIX);
        updateState(channelUID, zoneState.getZoneMute());

        channelUID = getZoneUID(zoneState.getZoneNr(), CHANNEL_AUDIO_SUFFIX);
        updateState(channelUID, zoneState.getZoneAudio());

        channelUID = getZoneUID(zoneState.getZoneNr(), CHANNEL_INPUT_SUFFIX);
        updateState(channelUID, zoneState.getZoneInput());

        channelUID = getZoneUID(zoneState.getZoneNr(), CHANNEL_VIDEO_SUFFIX);
        updateState(channelUID, zoneState.getZoneVideo());
    }

    private ChannelUID getZoneUID(short zone, String suffix) {
        return new ChannelUID(getThing().getUID(), CHANNEL_ZONE_PREFIX + zone + suffix);
    }

    private void stateChanged(ChannelUID channelID, State state) {
        logger.debug("Received state {} for channelID {}", state, channelID);

        // Don't flood the log with thing 'updated: ONLINE' each time a single channel changed
        if (this.getThing().getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
        updateState(channelID, state);
    }

    private OnOffType invertOnOffType(OnOffType original) {
        return original == OnOffType.ON ? OnOffType.OFF : OnOffType.ON;
    }

    @Override
    public void connectionError(@Nullable String errorMessage) {
        if (this.getThing().getStatus() != ThingStatus.OFFLINE) {
            // Don't flood the log with thing 'updated: OFFLINE' when already offline
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
        }
        disposeConnector();
        retryJob = scheduler.schedule(this::createConnection, RETRY_TIME_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void loginSuccessfull() {
        updateStatus(ThingStatus.ONLINE);

        Object property = getConfig().get(REFRESH_INTERVAL);
        if (property != null && property.getClass().isAssignableFrom(BigDecimal.class)) {
            int refreshInterval = ((Number) property).intValue();
            logger.debug("Starting read polling job.");
            pollingJob = scheduler.scheduleWithFixedDelay(this::pollingRefreshState, 0, refreshInterval,
                    TimeUnit.SECONDS);
        } else {
            logger.info("No refresh interval setting found: refresh state polling will be disabled.");
        }
    }

    /**
     * Saves current input setup as profile with given number
     *
     * @param profileNumber allowed values are 1 - 16
     */
    public void saveProfile(short profileNumber) {
        try {
            final AtenConnector connector = this.connector;
            if (connector != null) {
                connector.sendProfileCommand(SaveLoadType.getSaveType("" + profileNumber));
            }
        } catch (UnsupportedCommandTypeException e) {
            logger.error("Couldn't save profile.", e);
        }
    }

    private void pollingRefreshState() {
        final AtenConnector connector = this.connector;
        if (connector != null) {
            logger.trace("Send command for update state.");
            connector.sendReadCommand(RefreshType.REFRESH);
        }
    }

    @Override
    public void dispose() {

        disposeConnector();
        cancelRetry();
        super.dispose();
    }

    private void createConnection() {
        disposeConnector();
        final AtenConfiguration config = getConfigAs(AtenConfiguration.class);
        final AtenConnector connector = connectorFactory.getConnector(config, this, scheduler,
                this.getThing().getUID().getAsString());
        connector.connect();
        this.connector = connector;
    }

    private void cancelRetry() {
        final ScheduledFuture<?> localRetryJob = retryJob;
        if (localRetryJob != null && !localRetryJob.isDone()) {
            localRetryJob.cancel(false);
        }
    }

    private void disposeConnector() {
        final AtenConnector connector = this.connector;
        if (connector != null) {
            connector.dispose();
            this.connector = null;
        }

        final ScheduledFuture<?> job = pollingJob;

        if (job != null) {
            logger.trace("stopping read polling job");
            job.cancel(true);
            pollingJob = null;
        }
    }
}
