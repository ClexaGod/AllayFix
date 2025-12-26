package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Spruce tree generator (1x1).
 */
public class SpruceTreeGenerator extends AbstractTreeGenerator {

    public SpruceTreeGenerator(BlockType<?> logType, BlockType<?> leavesType) {
        super(logType, leavesType);
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int treeHeight = random.nextInt(4) + 6;
        int topSize = treeHeight - (1 + random.nextInt(2));
        int leafRadius = 2 + random.nextInt(2);

        if (!canPlace(placer, basePos, treeHeight)) {
            return false;
        }

        placeDirt(placer, basePos.x, basePos.y - 1, basePos.z);
        placeTrunk(placer, basePos, treeHeight);
        placeLeaves(placer, random, basePos, treeHeight, topSize, leafRadius);
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

    protected void placeTrunk(TreePlacer placer, Vector3i basePos, int height) {
        for (int yy = 0; yy < height; yy++) {
            placeLog(placer, basePos.x, basePos.y + yy, basePos.z, PillarAxis.Y);
        }
    }

    protected void placeLeaves(TreePlacer placer, Random random, Vector3i basePos, int treeHeight, int topSize, int leafRadius) {
        int radius = random.nextInt(2);
        int maxR = 1;
        int minR = 0;

        for (int yy = 0; yy <= topSize; yy++) {
            int y = basePos.y + treeHeight - yy;
            for (int xx = basePos.x - radius; xx <= basePos.x + radius; xx++) {
                int xOff = Math.abs(xx - basePos.x);
                for (int zz = basePos.z - radius; zz <= basePos.z + radius; zz++) {
                    int zOff = Math.abs(zz - basePos.z);
                    if (xOff == radius && zOff == radius && radius > 0) {
                        continue;
                    }
                    placeLeaves(placer, xx, y, zz);
                }
            }

            if (radius >= maxR) {
                radius = minR;
                minR = 1;
                if (++maxR > leafRadius) {
                    maxR = leafRadius;
                }
            } else {
                radius++;
            }
        }
    }
}
