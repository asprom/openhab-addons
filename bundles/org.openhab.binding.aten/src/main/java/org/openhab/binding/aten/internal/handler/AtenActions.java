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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Roman Aspetsberger - Initial contribution.
 *
 */
@NonNullByDefault
@ThingActionsScope(name = "aten")
public class AtenActions implements ThingActions {

    public final Logger logger = LoggerFactory.getLogger(getClass());
    private @Nullable AtenHandler handler;

    @RuleAction(label = "@text/saveProfileActionLabel", description = "@text/saveProfileActionDescription")
    public void saveProfile(
            @ActionInput(name = "profileNumber", label = "@test/saveProfileActionInputProfileNumberLabel", description = "@text/saveProfileActionInputProfileNumberDescription") short profileNumber) {
        AtenHandler handler = this.handler;
        if (handler != null) {
            handler.saveProfile(profileNumber);
        }
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        this.handler = (AtenHandler) handler;
    }

    @Override
    public @Nullable AtenHandler getThingHandler() {
        return handler;
    }
}
