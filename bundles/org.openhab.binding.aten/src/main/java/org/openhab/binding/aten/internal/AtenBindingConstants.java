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
package org.openhab.binding.aten.internal;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelGroupTypeUID;

/**
 * The {@link AtenBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Roman Aspetsberger - Initial contribution
 */
@NonNullByDefault
public class AtenBindingConstants {

    public static final String BINDING_ID = "aten";

    // configuration properties
    public static final String REFRESH_INTERVAL = "refreshInterval";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_HDMI_MATRIX_SWITCH = new ThingTypeUID(BINDING_ID, "hdmiMatrixSwitch");

    // List of all Channel ids

    public static final String CHANNEL_ZONE_PREFIX = "zone";
    public static final String CHANNEL_MUTE_SUFFIX = "#mute";
    public static final String CHANNEL_VIDEO_SUFFIX = "#video";
    public static final String CHANNEL_AUDIO_SUFFIX = "#audio";
    public static final String CHANNEL_INPUT_SUFFIX = "#input";
    public static final String CHANNEL_CEC_SUFFIX = "#cec";
    public static final String CHANNEL_PROFILE_SUFFIX = "#profile";

    public static final ChannelGroupTypeUID CHANNEL_GROUP_TYPE_OUTPUT_ZONE = new ChannelGroupTypeUID(BINDING_ID,
            "outputZone");

    // List of commands - note, commands are case sensitive!
    public static final String READ_COMMAND = "read";
    public static final String SWITCH_COMMAND_FORMAT = "sw i%02d o%02d";
    public static final String MUTE_COMMAND_FORMAT = "mute o%02d %s";
    public static final String VIDEO_COMMAND_FORMAT = "sw o%02d %s";
    public static final String CEC_COMMAND_FORMAT = "cec o%02d %s";
    public static final String PROFILE_COMMAND_FORMAT = "profile f%d %s";

    // List of result patterns
    public static final Pattern READ_ZONE_STATUS_PATTERN = Pattern
            .compile("^o([0-9]{2}) i([0-9]{2}) video (.+) audio (.+)$");

    public static final Pattern CONNECTION_ESTABLISHED_PATTERN = Pattern.compile("^Connection to (.+) is established$");
    public static final String COMMAND_OK = "Command OK";
    public static final String COMMAND_NOK = "Command incorrect";

    public static final String DEFAULT_INPUT_NAME_PREFIX = "INPUT ";
    public static final String DEFAULT_PROFILE_NAME_PREFIX = "PROFILE ";
}
