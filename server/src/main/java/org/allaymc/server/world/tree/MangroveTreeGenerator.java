package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Mangrove tree generator.
 */
public class MangroveTreeGenerator extends AbstractTreeGenerator {

    public MangroveTreeGenerator(BlockType<?> logType, BlockType<?> leavesType) {
        super(logType, leavesType);
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int height = random.nextInt(3) + 8;
        int topY = basePos.y + height;

        if (!canPlace(placer, basePos, height)) {
            return false;
        }

        // Roots + trunk
        for (int i = 0; i <= height + 1; i++) {
            if (i > 2) {
                placeLog(placer, basePos.x, basePos.y + i, basePos.z, PillarAxis.Y);
            } else {
                placeRoot(placer, basePos.x + 1, basePos.y + i, basePos.z);
                placeRoot(placer, basePos.x - 1, basePos.y + i, basePos.z);
                placeRoot(placer, basePos.x, basePos.y + i, basePos.z + 1);
                placeRoot(placer, basePos.x, basePos.y + i, basePos.z - 1);
            }
        }

        placeRoot(placer, basePos.x + 2, basePos.y, basePos.z);
        placeRoot(placer, basePos.x - 2, basePos.y, basePos.z);
        placeRoot(placer, basePos.x, basePos.y, basePos.z + 2);
        placeRoot(placer, basePos.x, basePos.y, basePos.z - 2);

        for (int dx = -2; dx <= 1; ++dx) {
            for (int dz = -2; dz <= 1; ++dz) {
                int offsetX = random.nextInt(2);
                int offsetY = random.nextInt(2);
                int offsetZ = random.nextInt(2);
                placeLeaves(placer, basePos.x + dx + offsetX, topY + 1 + offsetY, basePos.z + dz + offsetZ);
                placeLeaves(placer, basePos.x - dx + offsetX, topY + 1 + offsetY, basePos.z + dz + offsetZ);
                placeLeaves(placer, basePos.x + dx + offsetX, topY + 1 + offsetY, basePos.z - dz + offsetZ);
                placeLeaves(placer, basePos.x - dx + offsetX, topY + 1 + offsetY, basePos.z - dz + offsetZ);

                placeLeaves(placer, basePos.x + dx, topY, basePos.z + dz);
                placeLeaves(placer, basePos.x - dx, topY, basePos.z + dz);
                placeLeaves(placer, basePos.x + dx, topY, basePos.z - dz);
                placeLeaves(placer, basePos.x - dx, topY, basePos.z - dz);

                placeLeaves(placer, basePos.x + dx, topY + 1, basePos.z + dz);
                placeLeaves(placer, basePos.x - dx, topY + 1, basePos.z + dz);
                placeLeaves(placer, basePos.x + dx, topY + 1, basePos.z - dz);
                placeLeaves(placer, basePos.x - dx, topY + 1, basePos.z - dz);

                int offX = random.nextInt(2) - 1;
                int offY = random.nextInt(2) - 1;
                int offZ = random.nextInt(2) - 1;
                placeLeaves(placer, basePos.x + dx + offX, topY + 2 + offY, basePos.z + dz + offZ);
                placeLeaves(placer, basePos.x - dx + offX, topY + 2 + offY, basePos.z + dz + offZ);
                placeLeaves(placer, basePos.x + dx + offX, topY + 2 + offY, basePos.z - dz + offZ);
                placeLeaves(placer, basePos.x - dx + offX, topY + 2 + offY, basePos.z - dz + offZ);
            }
        }

        placeLeaves(placer, basePos.x, topY + 2, basePos.z);
        return placer.isValid();
    }

    private boolean canPlace(TreePlacer placer, Vector3i basePos, int height) {
        for (int y = 0; y <= height + 2; y++) {
            int radius = 1;
            if (y == 0) {
                radius = 0;
            } else if (y >= height) {
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

    private void placeRoot(TreePlacer placer, int x, int y, int z) {
        placer.setBlock(x, y, z, BlockTypes.MANGROVE_ROOTS.getDefaultState());
    }
}
