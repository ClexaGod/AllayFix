package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Cherry tree generator.
 */
public class CherryTreeGenerator extends AbstractTreeGenerator {

    public CherryTreeGenerator(BlockType<?> logType, BlockType<?> leavesType) {
        super(logType, leavesType);
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int height = random.nextInt(2) + 9;
        int topY = basePos.y + height;

        if (!canPlace(placer, basePos, height)) {
            return false;
        }

        placeDirt(placer, basePos.x, basePos.y - 1, basePos.z);

        for (int i = 0; i < height + 1; i++) {
            placeLog(placer, basePos.x, basePos.y + i, basePos.z, PillarAxis.Y);
        }

        buildCrown(placer, random, basePos.x, basePos.y + 1, basePos.z, height);

        // Horizontal branch
        int half = height >> 1;
        int dir = random.nextInt(4);
        int dx = 0;
        int dz = 0;
        if (dir == 0) {
            dx = 1;
        } else if (dir == 1) {
            dx = -1;
        } else if (dir == 2) {
            dz = 1;
        } else {
            dz = -1;
        }

        int x = basePos.x;
        int z = basePos.z;
        for (int i = 0; i < half + 3; i++) {
            x += dx;
            z += dz;
            placeLog(placer, x, basePos.y + half, z, dx != 0 ? PillarAxis.X : PillarAxis.Z);
        }

        for (int i = half + 1; i < height + 1; i++) {
            placeLog(placer, x, basePos.y + i, z, PillarAxis.Y);
        }

        buildCrown(placer, random, x, basePos.y + 1, z, height);
        placeLeaves(placer, basePos.x, topY, basePos.z);
        placeLeaves(placer, x, topY, z);

        return placer.isValid();
    }

    private void buildCrown(TreePlacer placer, Random random, int x, int baseY, int z, int height) {
        for (int yy = baseY - 3 + height; yy <= baseY + height; ++yy) {
            double yOff = yy - (baseY + height);
            int mid = (int) (1 - yOff / 2) + 1;
            for (int xx = x - mid; xx <= x + mid; ++xx) {
                int xOff = Math.abs(xx - x);
                for (int zz = z - mid; zz <= z + mid; ++zz) {
                    int zOff = Math.abs(zz - z);
                    if (xOff == mid && zOff == mid && (yOff == 0 || random.nextInt(2) == 0)) {
                        continue;
                    }
                    placeLeaves(placer, xx, yy - 1, zz);
                }
            }
        }
    }

    private boolean canPlace(TreePlacer placer, Vector3i basePos, int height) {
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
}
