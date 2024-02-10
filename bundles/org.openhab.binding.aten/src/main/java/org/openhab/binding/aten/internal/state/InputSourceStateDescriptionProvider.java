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
package org.openhab.binding.aten.internal.state;

import static org.openhab.binding.aten.internal.AtenBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.aten.internal.config.InputNameConfiguration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.type.DynamicStateDescriptionProvider;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * This class is responsible to define the right amount of input state values depending on thing configuration.
 * The thing handler of the Aten switch is responsible to update this provider after parsing the configuration.
 *
 * @author Roman Aspetsberger - Initial contribution.
 *
 */
@NonNullByDefault
@Component(service = { DynamicStateDescriptionProvider.class, InputSourceStateDescriptionProvider.class })
public class InputSourceStateDescriptionProvider implements DynamicStateDescriptionProvider {

    private List<StateOption> stateOptionsForInput = new ArrayList<>();
    private List<StateOption> stateOptionsForProfile = new ArrayList<>();

    private @Nullable InputNameConfiguration config;

    public void setStateOptionsForInput(short numberOfInputSources) {
        stateOptionsForInput = new ArrayList<>();
        stateOptionsForProfile = new ArrayList<>();

        final InputNameConfiguration localConfig = this.config;

        // handle input names
        for (int i = 1; i <= numberOfInputSources; i++) {
            String inputName = DEFAULT_INPUT_NAME_PREFIX + i;
            if (localConfig != null) {
                String nameFromConfig = localConfig.getInputName(i);
                if (!nameFromConfig.isBlank()) {
                    inputName = nameFromConfig;
                }
            }
            stateOptionsForInput.add(new StateOption("" + i, inputName));
        }

        // handle profile names
        for (int i = 1; i <= 16; i++) { // how to get max nr. of profiles?
            String profileName = DEFAULT_PROFILE_NAME_PREFIX + i;
            if (localConfig != null) {
                String nameFromConfig = localConfig.getProfileName(i);
                if (!nameFromConfig.isBlank()) {
                    profileName = nameFromConfig;
                }
            }
            stateOptionsForProfile.add(new StateOption("" + i, profileName));
        }
    }

    @Override
    public @Nullable StateDescription getStateDescription(Channel channel, @Nullable StateDescription original,
            @Nullable Locale locale) {
        if (channel.getUID().getId().endsWith(CHANNEL_INPUT_SUFFIX) && !stateOptionsForInput.isEmpty()) {
            StateDescriptionFragmentBuilder builder = (original == null) ? StateDescriptionFragmentBuilder.create()
                    : StateDescriptionFragmentBuilder.create(original);
            return builder.withOptions(stateOptionsForInput).build().toStateDescription();
        }

        if (channel.getUID().getId().endsWith(CHANNEL_PROFILE_SUFFIX) && !stateOptionsForProfile.isEmpty()) {
            StateDescriptionFragmentBuilder builder = (original == null) ? StateDescriptionFragmentBuilder.create()
                    : StateDescriptionFragmentBuilder.create(original);
            return builder.withOptions(stateOptionsForProfile).build().toStateDescription();
        }

        // for all others nothing to change here
        return null;
    }

    public void setConfiguration(InputNameConfiguration config) {
        this.config = config;
    }

    @Deactivate
    public void deactivate() {
        stateOptionsForInput.clear();
    }
}
