/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package ct.buildcraft.api.core;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

public final class BuildCraftAPI {
    public static IFakePlayerProvider fakePlayerProvider;

    public static final Set<Block> softBlocks = Sets.newHashSet();
    public static final HashMap<String, IWorldProperty> worldProperties = Maps.newHashMap();

    /** Deactivate constructor */
    private BuildCraftAPI() {}

    public static String getVersion() {
        Optional<? extends ModContainer> container = ModList.get().getModContainerById("buildcraftlib");
        if (container.isPresent()) {
            return container.get().getModInfo().getVersion().getQualifier();
        }
        return "UNKNOWN VERSION";
    }

    public static IWorldProperty getWorldProperty(String name) {
        return worldProperties.get(name);
    }

    public static void registerWorldProperty(String name, IWorldProperty property) {
        if (worldProperties.containsKey(name)) {
            BCLog.logger.warn("The WorldProperty key '" + name + "' is being overridden with " + property.getClass().getSimpleName() + "!");
        }
        worldProperties.put(name, property);
    }

    public static boolean isSoftBlock(Level world, BlockPos pos) {
        return worldProperties.get("soft").get(world, pos);
    }

/*    public static ResourceLocation nameToResourceLocation(String name) {
        if (name.indexOf(':') > 0) return new ResourceLocation(name);
        ModContainer modContainer = ModList.get().;
        if (modContainer == null) {
            throw new IllegalStateException("Illegal recipe name " + name + ". Provide domain id to register it correctly.");
        }
        return new ResourceLocation(modContainer.getModId(), name);
    }*/
}
