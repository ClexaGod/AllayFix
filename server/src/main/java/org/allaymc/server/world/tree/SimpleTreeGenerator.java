package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Basic tree generator used for oak/birch-like trees.
 */
public class SimpleTreeGenerator extends AbstractTreeGenerator {
    private final int minHeight;
    private final int maxExtraHeight;

    public SimpleTreeGenerator(BlockType<?> logType, BlockType<?> leavesType, int minHeight, int maxExtraHeight) {
        super(logType, leavesType);
        this.minHeight = minHeight;
        this.maxExtraHeight = maxExtraHeight;
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int height = minHeight + random.nextInt(Math.max(1, maxExtraHeight));
        if (!canPlace(placer, basePos, height)) {
            return false;
        }

        placeDirt(placer, basePos.x, basePos.y - 1, basePos.z);
        placeTrunk(placer, basePos, height - 1);
        placeLeavesLayers(placer, random, basePos, height);
        return placer.isValid();
    }

    protected boolean canPlace(TreePlacer placer, Vector3i basePos, int height) {
        int radius = 0;
        for (int yy = 0; yy < height + 3; yy++) {
            if (yy == 1 || yy == height) {
                radius++;
                continue;
            }

            for (int xx = -radius; xx <= radius; xx++) {
                for (int zz = -radius; zz <= radius; zz++) {
                    if (!canGrowInto(placer, basePos.x + xx, basePos.y + yy, basePos.z + zz)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected void placeTrunk(TreePlacer placer, Vector3i basePos, int trunkHeight) {
        for (int yy = 0; yy < trunkHeight; yy++) {
            if (!canGrowInto(placer, basePos.x, basePos.y + yy, basePos.z)) {
                continue;
            }
            placeLog(placer, basePos.x, basePos.y + yy, basePos.z, PillarAxis.Y);
        }
    }

    protected void placeLeavesLayers(TreePlacer placer, Random random, Vector3i basePos, int height) {
        for (int yy = basePos.y - 3 + height; yy <= basePos.y + height; yy++) {
            double yOff = yy - (basePos.y + height);
            int mid = (int) (1 - yOff / 2);
            for (int xx = basePos.x - mid; xx <= basePos.x + mid; xx++) {
                int xOff = Math.abs(xx - basePos.x);
                for (int zz = basePos.z - mid; zz <= basePos.z + mid; zz++) {
                    int zOff = Math.abs(zz - basePos.z);
                    if (xOff == mid && zOff == mid && (yOff == 0 || random.nextInt(2) == 0)) {
                        continue;
                    }
                    placeLeaves(placer, xx, yy, zz);
                }
            }
        }
    }
}
