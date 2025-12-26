package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Small jungle tree generator (1x1).
 */
public class JungleTreeGenerator extends AbstractTreeGenerator {
    private final int minHeight;
    private final int maxExtraHeight;

    public JungleTreeGenerator(BlockType<?> logType, BlockType<?> leavesType, int minHeight, int maxExtraHeight) {
        super(logType, leavesType);
        this.minHeight = minHeight;
        this.maxExtraHeight = maxExtraHeight;
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int height = random.nextInt(maxExtraHeight) + minHeight;
        if (!canPlace(placer, basePos, height)) {
            return false;
        }

        placeDirt(placer, basePos.x, basePos.y - 1, basePos.z);

        // Leaves
        for (int yy = basePos.y - 3 + height; yy <= basePos.y + height; yy++) {
            int yOff = yy - (basePos.y + height);
            int radius = 1 - (yOff >> 1);
            for (int xx = basePos.x - radius; xx <= basePos.x + radius; xx++) {
                int xOff = xx - basePos.x;
                for (int zz = basePos.z - radius; zz <= basePos.z + radius; zz++) {
                    int zOff = zz - basePos.z;
                    if (Math.abs(xOff) != radius || Math.abs(zOff) != radius || random.nextInt(2) != 0 && yOff != 0) {
                        placeLeaves(placer, xx, yy, zz);
                    }
                }
            }
        }

        // Trunk
        for (int yy = 0; yy < height; yy++) {
            if (canGrowInto(placer, basePos.x, basePos.y + yy, basePos.z)) {
                placeLog(placer, basePos.x, basePos.y + yy, basePos.z, PillarAxis.Y);
            }
        }

        return placer.isValid();
    }

    private boolean canPlace(TreePlacer placer, Vector3i basePos, int height) {
        for (int y = 0; y <= height + 1; ++y) {
            int radius = 1;
            if (y == 0) {
                radius = 0;
            } else if (y >= height - 1) {
                radius = 2;
            }

            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    if (!canGrowInto(placer, basePos.x + x, basePos.y + y, basePos.z + z)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
