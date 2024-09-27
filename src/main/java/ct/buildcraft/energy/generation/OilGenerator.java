package ct.buildcraft.energy.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;

import ct.buildcraft.api.core.BCDebugging;
import ct.buildcraft.api.core.BCLog;
import ct.buildcraft.core.BCCoreBlocks;
import ct.buildcraft.energy.BCEnergyConfig;
import ct.buildcraft.energy.generation.features.OilGenStructure;
import ct.buildcraft.energy.generation.features.OilGenStructure.GenByPredicate;
import ct.buildcraft.energy.generation.features.OilGenStructure.ReplaceType;
import ct.buildcraft.lib.misc.RandUtil;
import ct.buildcraft.lib.misc.VecUtil;
import ct.buildcraft.lib.misc.data.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

public class OilGenerator {
    private OilGenerator() {}

    /** Random number, used to differentiate generators */
    private static final long MAGIC_GEN_NUMBER = 0xD0_46_B4_E4_0C_7D_07_CFL;

    /** The distance that oil generation will be checked to see if their structures overlap with the currently
     * generating chunk. This should be large enough that all oil generation can fit inside this radius. If this number
     * is too big then oil generation will be slightly slower */
    private static final int MAX_CHUNK_RADIUS = 5;

    public static final boolean DEBUG_OILGEN_BASIC = BCDebugging.shouldDebugLog("energy.oilgen");
    public static final boolean DEBUG_OILGEN_ALL = BCDebugging.shouldDebugComplex("energy.oilgen");

    private enum GenType {
        LARGE,
        MEDIUM,
        LAKE,
        NONE
    }

    public static void onPopulatePre(PopulateChunkEvent.Pre event) {
        WorldGenLevel world = pfc.level();
        BlockPos orginPos = pfc.origin();
        ChunkPos chunkPos = world.getChunk(orginPos).getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

/*        if (world.getLevelType() == LevelType.FLAT) {
            if (DEBUG_OILGEN_BASIC) {
                BCLog.logger.info(
                    "[energy.oilgen] Not generating oil in " + world + " chunk " + chunkX + ", " + chunkZ
                        + " because it's LevelType is FLAT."
                );
            }
            return;
        }*/
/*        boolean isExcludedDimension = BCEnergyConfig.excludedDimensions.contains(world.dimensionTypeId().location());
        if (isExcludedDimension == BCEnergyConfig.excludedDimensionsIsBlackList) {
            if (DEBUG_OILGEN_BASIC) {
                BCLog.logger.info(
                    "[energy.oilgen] Not generating oil in " + world + " chunk " + chunkX + ", " + chunkZ
                        + " because it's dimension is disabled."
                );
            }
            return;
        }
*/
//        world.profiler.startSection("bc_oil");
        int count = 0;
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        BlockPos min = new BlockPos(x, 0, z);
        Box box = new Box(min, min.offset(15, world.getHeight(), 15));

        for (int cdx = -MAX_CHUNK_RADIUS; cdx <= MAX_CHUNK_RADIUS; cdx++) {
            for (int cdz = -MAX_CHUNK_RADIUS; cdz <= MAX_CHUNK_RADIUS; cdz++) {
                int cx = chunkX + cdx;
                int cz = chunkZ + cdz;
//                world.getProfiler().startSection("scan");
                List<OilGenStructure> structures = getStructures(world, cx, cz, cdx == 0 && cdz == 0);
                OilGenStructure.Spring spring = null;
//                world.getProfiler().endStartSection("gen");
                for (OilGenStructure struct : structures) {
                    struct.generate(world, box);
                    if (struct instanceof OilGenStructure.Spring) {
                        spring = (OilGenStructure.Spring) struct;
                    }
                }
                if (spring != null && box.contains(spring.pos)) {
                    
                    for (OilGenStructure struct : structures) {
                        count += struct.countOilBlocks();
                    }
                    spring.generate(world, count);
                }
//                world.getProfiler().pop();;
            }
        }
//        world.getProfiler().pop();
		return count > 0;
    }

    public static List<OilGenStructure> getStructures(WorldGenLevel world, int cx, int cz) {
        return getStructures(world, cx, cz, false);
    }

    private static List<OilGenStructure> getStructures(WorldGenLevel world, int cx, int cz, boolean log) {
        RandomSource rand = RandUtil.createRandomForChunk(world, cx, cz, MAGIC_GEN_NUMBER);

        // shift to world coordinates
        int x = cx * 16 + 8 + rand.nextInt(16);
        int z = cz * 16 + 8 + rand.nextInt(16);

        Biome biome = world.getBiome(new BlockPos(x, 0, z)).get();

        // Do not generate oil in excluded biomes
        boolean isExcludedBiome = BCEnergyConfig.excludedBiomes.contains(ForgeRegistries.BIOMES.getKey(biome));
        if (isExcludedBiome == BCEnergyConfig.excludedBiomesIsBlackList) {
            if (DEBUG_OILGEN_BASIC & log) {
                BCLog.logger.info(
                    "[energy.oilgen] Not generating oil in " + toStr(world) + " chunk " + cx + ", " + cz
                        + " because the biome we found (" + ForgeRegistries.BIOMES.getKey(biome) + ") is disabled!"
                );
            }
            return ImmutableList.of();
        }

        if (isEndBiome(biome) && (Math.abs(x) < 1200 || Math.abs(z) < 1200)) {
            if (DEBUG_OILGEN_BASIC & log) {
                BCLog.logger.info(
                    "[energy.oilgen] Not generating oil in " + toStr(world) + " chunk " + cx + ", " + cz
                        + " because it's the end biome and we're within 1200 blocks of the ender dragon fight"
                );
            }
            return ImmutableList.of();
        }

        boolean oilBiome = BCEnergyConfig.surfaceDepositBiomes.contains(ForgeRegistries.BIOMES.getKey(biome));

        double bonus = oilBiome ? 3.0 : 1.0;
        bonus *= BCEnergyConfig.oilWellGenerationRate;
        if (BCEnergyConfig.excessiveBiomes.contains(ForgeRegistries.BIOMES.getKey(biome))) {
            bonus *= 30.0;
        }
        final GenType type;

        if (rand.nextDouble() <= BCEnergyConfig.largeOilGenProb * bonus) {
            // 0.04%
            type = GenType.LARGE;
        } else if (rand.nextDouble() <= BCEnergyConfig.mediumOilGenProb * bonus) {
            // 0.1%
            type = GenType.MEDIUM;
        } else if (oilBiome && rand.nextDouble() <= BCEnergyConfig.smallOilGenProb * bonus) {
            // 2%
            type = GenType.LAKE;
        } else {
            if (DEBUG_OILGEN_ALL & log) {
                BCLog.logger.info(
                    "[energy.oilgen] Not generating oil in " + toStr(world) + " chunk " + cx + ", " + cz
                        + " because none of the random numbers were above the thresholds for generation"
                );
            }
            return ImmutableList.of();
        }
        if (DEBUG_OILGEN_BASIC & log) {
            BCLog.logger.info(
                "[energy.oilgen] Generating an oil well (" + type.name().toLowerCase(Locale.ROOT)
                    + ") in " + toStr(world) + " chunk " + cx + ", " + cz + " at " + x + ", " + z
            );
        }

        List<OilGenStructure> structures = new ArrayList<>();
        int lakeRadius;
        int tendrilRadius;
        if (type == GenType.LARGE) {
            lakeRadius = 4;
            tendrilRadius = 25 + rand.nextInt(20);
        } else if (type == GenType.LAKE) {
            lakeRadius = 6;
            tendrilRadius = 25 + rand.nextInt(20);
        } else {
            lakeRadius = 2;
            tendrilRadius = 5 + rand.nextInt(10);
        }
        structures.add(createTendril(new BlockPos(x, 62, z), lakeRadius, tendrilRadius, rand));

        if (type != GenType.LAKE) {
            // Generate a spherical cave deposit
            int wellY = 20 + rand.nextInt(10);

            int radius;
            if (type == GenType.LARGE) {
                radius = 8 + rand.nextInt(9);
            } else {
                radius = 4 + rand.nextInt(4);
            }

            structures.add(createSphere(new BlockPos(x, wellY, z), radius));

            // Generate a spout
            if (BCEnergyConfig.enableOilSpouts) {
                int maxHeight, minHeight;

                if (type == GenType.LARGE) {
                    minHeight = BCEnergyConfig.largeSpoutMinHeight;
                    maxHeight = BCEnergyConfig.largeSpoutMaxHeight;
                    radius = 1;
                } else {
                    minHeight = BCEnergyConfig.smallSpoutMinHeight;
                    maxHeight = BCEnergyConfig.smallSpoutMaxHeight;
                    radius = 0;
                }
                final int height;
                if (maxHeight == minHeight) {
                    height = maxHeight;
                } else {
                    if (maxHeight < minHeight) {
                        int t = maxHeight;
                        maxHeight = minHeight;
                        minHeight = t;
                    }
                    height = minHeight + rand.nextInt(maxHeight - minHeight);
                }
                structures.add(createSpout(new BlockPos(x, wellY, z), height, radius));
            }

            // Generate a spring at the very bottom
            if (type == GenType.LARGE) {
                structures.add(createTube(new BlockPos(x, 1, z), wellY, radius, Axis.Y));
                if (BCCoreBlocks.SPRING != null) {
                    structures.add(createSpring(new BlockPos(x, 0, z)));
                }
            }
        }
        return structures;
    }

    private static String toStr(WorldGenLevel world) {
    	return world.dimensionType().effectsLocation().toString();
    }

    private static OilGenStructure createSpout(BlockPos start, int height, int radius) {
        return new OilGenStructure.Spout(start, ReplaceType.ALWAYS, radius, height);
    }

    public static OilGenStructure createTubeY(BlockPos base, int height, int radius) {
        return createTube(base, height, radius, Axis.Y);
    }

    public static OilGenStructure createSpring(BlockPos at) {
        return new OilGenStructure.Spring(at);
    }

    public static OilGenStructure createTube(BlockPos center, int length, int radius, Axis axis) {
        int valForAxis = VecUtil.getValue(center, axis);
        BlockPos min = VecUtil.replaceValue(center.offset(-radius, -radius, -radius), axis, valForAxis);
        BlockPos max = VecUtil.replaceValue(center.offset(radius, radius, radius), axis, valForAxis + length);
        double radiusSq = radius * radius;
        int toReplace = valForAxis;
        Predicate<BlockPos> tester = p -> VecUtil.replaceValue(p, axis, toReplace).distSqr(center) <= radiusSq;
        return new GenByPredicate(new Box(min, max), ReplaceType.ALWAYS, tester);
    }

    public static OilGenStructure createSphere(BlockPos center, int radius) {
        Box box = new Box(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius));
        double radiusSq = radius * radius + 0.01;
        Predicate<BlockPos> tester = p -> p.distSqr(center) <= radiusSq;
        return new GenByPredicate(box, ReplaceType.ALWAYS, tester);
    }

    public static OilGenStructure createTendril(BlockPos center, int lakeRadius, int radius, RandomSource rand) {
        BlockPos start = center.offset(-radius, 0, -radius);
        int diameter = radius * 2 + 1;
        boolean[][] pattern = new boolean[diameter][diameter];

        int x = radius;
        int z = radius;
        for (int dx = -lakeRadius; dx <= lakeRadius; dx++) {
            for (int dz = -lakeRadius; dz <= lakeRadius; dz++) {
                pattern[x + dx][z + dz] = dx * dx + dz * dz <= lakeRadius * lakeRadius;
            }
        }

        for (int w = 1; w < radius; w++) {
            float proba = (float) (radius - w + 4) / (float) (radius + 4);

            fillPatternIfProba(rand, proba, x, z + w, pattern);
            fillPatternIfProba(rand, proba, x, z - w, pattern);
            fillPatternIfProba(rand, proba, x + w, z, pattern);
            fillPatternIfProba(rand, proba, x - w, z, pattern);

            for (int i = 1; i <= w; i++) {
                fillPatternIfProba(rand, proba, x + i, z + w, pattern);
                fillPatternIfProba(rand, proba, x + i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z + i, pattern);
                fillPatternIfProba(rand, proba, x - w, z + i, pattern);

                fillPatternIfProba(rand, proba, x - i, z + w, pattern);
                fillPatternIfProba(rand, proba, x - i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z - i, pattern);
                fillPatternIfProba(rand, proba, x - w, z - i, pattern);
            }
        }

        int depth = rand.nextDouble() < 0.5 ? 1 : 2;
        return OilGenStructure.PatternTerrainHeight.create(start, ReplaceType.IS_FOR_LAKE, pattern, depth);
    }

    private static void fillPatternIfProba(RandomSource rand, float proba, int x, int z, boolean[][] pattern) {
        if (rand.nextFloat() <= proba) {
            pattern[x][z] = isSet(pattern, x, z - 1) | isSet(pattern, x, z + 1) //
                | isSet(pattern, x - 1, z) | isSet(pattern, x + 1, z);
        }
    }

    private static boolean isSet(boolean[][] pattern, int x, int z) {
        if (x < 0 || x >= pattern.length) return false;
        if (z < 0 || z >= pattern[x].length) return false;
        return pattern[x][z];
    }
    
    private static boolean isEndBiome(Biome biome) {
    	return ForgeRegistries.BIOMES.getKey(biome).getPath().contains("end");//maybe some problem
    }

}
