/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution.
 */

package ct.buildcraft.api.core;

public enum EnumHandlerPriority implements Comparable<EnumHandlerPriority> {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST;

    public static final EnumHandlerPriority[] VALUES = values();
}
