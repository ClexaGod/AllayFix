package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Acacia (savanna) tree generator.
 */
public class AcaciaTreeGenerator extends AbstractTreeGenerator {

    public AcaciaTreeGenerator(BlockType<?> logType, BlockType<?> leavesType) {
        super(logType, leavesType);
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int height = random.nextInt(3) + random.nextInt(3) + 5;
        if (!canPlace(placer, basePos, height)) {
            return false;
        }

        placeDirt(placer, basePos.x, basePos.y - 1, basePos.z);

        int trunkTop = 0;
        int x = basePos.x;
        int z = basePos.z;
        int bendStart = height - random.nextInt(4) - 1;
        int bendLength = 3 - random.nextInt(3);
        int dx = 0;
        int dz = 0;
        int dir = random.nextInt(4);
        if (dir == 0) {
            dx = 1;
        } else if (dir == 1) {
            dx = -1;
        } else if (dir == 2) {
            dz = 1;
        } else {
            dz = -1;
        }

        for (int i = 0; i < height; i++) {
            int y = basePos.y + i;
            if (i >= bendStart && bendLength > 0) {
                x += dx;
                z += dz;
                bendLength--;
            }

            if (canGrowInto(placer, x, y, z)) {
                placeLog(placer, x, y, z, PillarAxis.Y);
                trunkTop = y;
            }
        }

        var crownBase = new Vector3i(x, trunkTop, z);
        placeAcaciaCrown(placer, crownBase);

        int secondDir = random.nextInt(4);
        if (secondDir != dir) {
            int x2 = basePos.x;
            int z2 = basePos.z;
            int bendStart2 = bendStart - random.nextInt(2) - 1;
            int bendLen2 = 1 + random.nextInt(3);
            int dx2 = 0;
            int dz2 = 0;
            if (secondDir == 0) {
                dx2 = 1;
            } else if (secondDir == 1) {
                dx2 = -1;
            } else if (secondDir == 2) {
                dz2 = 1;
            } else {
                dz2 = -1;
            }

            int top2 = 0;
            for (int i = bendStart2; i < height && bendLen2 > 0; i++) {
                int y = basePos.y + i;
                x2 += dx2;
                z2 += dz2;
                if (canGrowInto(placer, x2, y, z2)) {
                    placeLog(placer, x2, y, z2, PillarAxis.Y);
                    top2 = y;
                }
                bendLen2--;
            }

            if (top2 > 0) {
                placeAcaciaCrown(placer, new Vector3i(x2, top2, z2));
            }
        }

        return placer.isValid();
    }

    private boolean canPlace(TreePlacer placer, Vector3i basePos, int height) {
        for (int y = 0; y <= height + 1; y++) {
            int radius = 1;
            if (y == 0) {
                radius = 0;
            } else if (y >= height - 1) {
                radius = 2;
            }

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (!canGrowInto(placer, basePos.x + x, basePos.y + y, basePos.z + z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void placeAcaciaCrown(TreePlacer placer, Vector3i top) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) != 3 || Math.abs(dz) != 3) {
                    placeLeaves(placer, top.x + dx, top.y, top.z + dz);
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placeLeaves(placer, top.x + dx, top.y + 1, top.z + dz);
            }
        }

        placeLeaves(placer, top.x + 2, top.y + 1, top.z);
        placeLeaves(placer, top.x - 2, top.y + 1, top.z);
        placeLeaves(placer, top.x, top.y + 1, top.z + 2);
        placeLeaves(placer, top.x, top.y + 1, top.z - 2);
    }
}
