package org.allaymc.server.world.tree;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.data.DimensionInfo;
/**
 * Collects tree block placements before applying them to the world.
 */
public class TreePlacer {
    private final Dimension dimension;
    private final BlockType<?> saplingType;
    private final boolean allowWater;
    private final DimensionInfo dimensionInfo;
    private final Long2ObjectOpenHashMap<BlockState> placements = new Long2ObjectOpenHashMap<>();
    private boolean valid = true;

    public TreePlacer(Dimension dimension, BlockType<?> saplingType) {
        this(dimension, saplingType, false);
    }

    public TreePlacer(Dimension dimension, BlockType<?> saplingType, boolean allowWater) {
        this.dimension = dimension;
        this.saplingType = saplingType;
        this.allowWater = allowWater;
        this.dimensionInfo = dimension.getDimensionInfo();
    }

    public boolean isValid() {
        return valid;
    }

    public boolean canGrowInto(int x, int y, int z) {
        if (!isWithinWorld(y)) {
            return false;
        }

        var state = getCurrentState(x, y, z);
        var type = state.getBlockType();
        if (type == saplingType || type == BlockTypes.AIR) {
            return true;
        }

        if (type.hasBlockTag(BlockTags.LEAVES)) {
            return true;
        }

        if (type.hasBlockTag(BlockTags.REPLACEABLE) &&
                (allowWater || !type.hasBlockTag(BlockTags.WATER)) &&
                !type.hasBlockTag(BlockTags.LAVA)) {
            return true;
        }

        return type.hasBlockTag(BlockTags.DIRT) || type.hasBlockTag(BlockTags.GRASS);
    }

    public boolean canPlaceRoot(int x, int y, int z) {
        if (!isWithinWorld(y)) {
            return false;
        }

        var state = getCurrentState(x, y, z);
        var type = state.getBlockType();
        if (type == BlockTypes.AIR) {
            return true;
        }
        if (type.hasBlockTag(BlockTags.WATER)) {
            return true;
        }
        if (type.hasBlockTag(BlockTags.REPLACEABLE) && !type.hasBlockTag(BlockTags.LAVA)) {
            return true;
        }
        return false;
    }

    public boolean isWater(int x, int y, int z) {
        if (!isWithinWorld(y)) {
            return false;
        }
        return getCurrentState(x, y, z).getBlockType().hasBlockTag(BlockTags.WATER);
    }

    public void setLog(int x, int y, int z, BlockType<?> logType, PillarAxis axis) {
        if (!canGrowInto(x, y, z)) {
            valid = false;
            return;
        }

        var state = logType.getDefaultState();
        if (state.getPropertyValues().containsKey(BlockPropertyTypes.PILLAR_AXIS)) {
            state = state.setPropertyValue(BlockPropertyTypes.PILLAR_AXIS, axis);
        }

        placements.put(hash(x, y, z), state);
    }

    public void setRoot(int x, int y, int z, BlockState rootState) {
        if (!canPlaceRoot(x, y, z)) {
            return;
        }

        placements.put(hash(x, y, z), rootState);
    }

    public void setBlock(int x, int y, int z, BlockState blockState) {
        if (!canGrowInto(x, y, z)) {
            valid = false;
            return;
        }

        placements.put(hash(x, y, z), blockState);
    }

    public void setLeaves(int x, int y, int z, BlockType<?> leavesType) {
        if (!canGrowInto(x, y, z)) {
            return;
        }

        var current = placements.get(hash(x, y, z));
        if (current != null) {
            return;
        }

        var state = leavesType.getDefaultState();
        if (state.getPropertyValues().containsKey(BlockPropertyTypes.PERSISTENT_BIT)) {
            state = state.setPropertyValue(BlockPropertyTypes.PERSISTENT_BIT, false);
        }
        if (state.getPropertyValues().containsKey(BlockPropertyTypes.UPDATE_BIT)) {
            state = state.setPropertyValue(BlockPropertyTypes.UPDATE_BIT, false);
        }

        placements.put(hash(x, y, z), state);
    }

    public void apply() {
        if (!valid) {
            return;
        }

        for (var entry : placements.long2ObjectEntrySet()) {
            var pos = decode(entry.getLongKey());
            dimension.setBlockState(pos.x(), pos.y(), pos.z(), entry.getValue(), 0, true, false, true, null);
        }
        for (var entry : placements.long2ObjectEntrySet()) {
            var pos = decode(entry.getLongKey());
            dimension.updateAround(pos.x(), pos.y(), pos.z());
        }
    }

    private BlockState getCurrentState(int x, int y, int z) {
        var planned = placements.get(hash(x, y, z));
        return planned != null ? planned : dimension.getBlockState(x, y, z);
    }

    private boolean isWithinWorld(int y) {
        return y >= dimensionInfo.minHeight() && y <= dimensionInfo.maxHeight();
    }

    private static long hash(int x, int y, int z) {
        return ((long) y << 52) + (((long) z & 0x3ffffff) << 26) + ((long) x & 0x3ffffff);
    }

    private static Vec3 decode(long hash) {
        int x = (int) (hash << 38 >> 38);
        int z = (int) (hash << 12 >> 38);
        int y = (int) (hash >> 52);
        return new Vec3(x, y, z);
    }

    private record Vec3(int x, int y, int z) {
    }
}
