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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Roman Aspetsberger - Initial contribution
 *
 */
@NonNullByDefault
public class InputNameConfiguration {

    private final Map<Integer, String> inputNames = new HashMap<>();
    private final Map<Integer, String> profileNames = new HashMap<>();

    /**
     * Creates a new input name configuration from given comma separated list. The names will be assigned to the
     * channels starting with 1 in the same order.
     * If there are not enough names given for all channels, the remaining will get a default name string.
     *
     * @param configString comma separated list of names for the input channels.
     */
    public InputNameConfiguration(String configString, String profileNamesString) {
        AtomicInteger i = new AtomicInteger(0);
        Arrays.stream(configString.split(",")).forEach(s -> inputNames.put(Integer.valueOf(i.incrementAndGet()), s));

        i.set(0);
        Arrays.stream(profileNamesString.split(","))
                .forEach(s -> profileNames.put(Integer.valueOf(i.incrementAndGet()), s));
    }

    /**
     *
     * @param inputNr number of the channel for which to retrieve the name configuration.
     * @return the configured input name if set or empty string.
     */
    public String getInputName(int inputNr) {
        return inputNames.getOrDefault(Integer.valueOf(inputNr), "");
    }

    public String getProfileName(int profileNr) {
        return profileNames.getOrDefault(Integer.valueOf(profileNr), "");
    }
}
