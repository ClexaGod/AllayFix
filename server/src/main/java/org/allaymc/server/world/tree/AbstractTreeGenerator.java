package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Base helpers for tree generators.
 */
public abstract class AbstractTreeGenerator implements TreeGenerator {
    protected final BlockType<?> logType;
    protected final BlockType<?> leavesType;

    protected AbstractTreeGenerator(BlockType<?> logType, BlockType<?> leavesType) {
        this.logType = logType;
        this.leavesType = leavesType;
    }

    protected boolean canGrowInto(TreePlacer placer, int x, int y, int z) {
        return placer.canGrowInto(x, y, z);
    }

    protected void placeLog(TreePlacer placer, int x, int y, int z, PillarAxis axis) {
        placer.setLog(x, y, z, logType, axis);
    }

    protected void placeLeaves(TreePlacer placer, int x, int y, int z) {
        placer.setLeaves(x, y, z, leavesType);
    }

    protected void placeDirt(TreePlacer placer, int x, int y, int z) {
        var state = BlockTypes.DIRT.getDefaultState();
        placer.setBlock(x, y, z, state);
    }

    protected static int randBetween(Random random, int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
    }

    @Override
    public abstract boolean generate(TreePlacer placer, Random random, Vector3i basePos);
}
