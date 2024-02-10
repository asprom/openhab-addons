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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 * This type can be used for functions which can load / save an item based on some input, e.g. profile by number.
 * The operation differs if it is a load or a save command.
 *
 * @author Roman Aspetsberger - Initial contribution
 *
 */
@NonNullByDefault
public class SaveLoadType implements PrimitiveType, State, Command {

    public enum Operation {
        SAVE,
        LOAD
    }

    private final Operation op;
    private final String value;

    protected SaveLoadType(Operation op, String value) {
        this.op = op;
        this.value = value;
    }

    public Operation getOperation() {
        return op;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String toFullString() {
        return value + "#" + op.name();
    }

    public static SaveLoadType valueOf(@Nullable String value) {
        if (value == null || value.length() < 5) {
            throw new IllegalArgumentException("Argument must have at least the operation (#save/#load) specified.");
        }
        Operation op = Operation.valueOf(value.substring(value.length() - 4).toUpperCase());
        return new SaveLoadType(op, value.substring(0, value.length() - 5));
    }

    @Override
    public String format(String pattern) {
        return String.format(pattern, value);
    }

    public static SaveLoadType getSaveType(Object value) {
        return new SaveLoadType(Operation.SAVE, value.toString());
    }

    public static SaveLoadType getLoadType(Object value) {
        return new SaveLoadType(Operation.LOAD, value.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, value);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SaveLoadType other = (SaveLoadType) obj;
        return op == other.op && Objects.equals(value, other.value);
    }
}
