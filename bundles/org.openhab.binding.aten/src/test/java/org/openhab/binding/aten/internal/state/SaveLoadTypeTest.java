/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SaveLoadType}.
 *
 * @author Roman Aspetsberger - Initial contribution.
 *
 */
@NonNullByDefault
public class SaveLoadTypeTest {

    @Test
    void testValueOf() {
        SaveLoadType load = SaveLoadType.getLoadType("foo");

        String s = load.toFullString();

        SaveLoadType recovered = SaveLoadType.valueOf(s);
        assertTrue(recovered instanceof SaveLoadType);
        assertEquals(SaveLoadType.Operation.LOAD, recovered.getOperation());
        assertEquals(load, recovered);

        SaveLoadType save = SaveLoadType.getSaveType("hello world");
        s = save.toFullString();
        recovered = SaveLoadType.valueOf(s);
        assertTrue(recovered instanceof SaveLoadType);
        assertEquals(SaveLoadType.Operation.SAVE, recovered.getOperation());
        assertEquals(save, recovered);

        // test with empty value, ignoring case of operation string
        SaveLoadType result = SaveLoadType.valueOf("#LoaD");
        assertEquals(SaveLoadType.Operation.LOAD, result.getOperation());

        // test invalid strings
        assertThrowsExactly(IllegalArgumentException.class, () -> SaveLoadType.valueOf(null));

        final String tooShort = "";
        assertThrowsExactly(IllegalArgumentException.class, () -> SaveLoadType.valueOf(tooShort));

        final String wrongOp = "#read";
        assertThrowsExactly(IllegalArgumentException.class, () -> SaveLoadType.valueOf(wrongOp));
    }
}
