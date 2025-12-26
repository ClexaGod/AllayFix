package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Base for 2x2 large tree generators.
 */
public abstract class HugeTreeGenerator extends AbstractTreeGenerator {
    protected final int baseHeight;
    protected int extraRandomHeight;

    protected HugeTreeGenerator(BlockType<?> logType, BlockType<?> leavesType, int baseHeight, int extraRandomHeight) {
        super(logType, leavesType);
        this.baseHeight = baseHeight;
        this.extraRandomHeight = extraRandomHeight;
    }

    protected int getHeight(Random random) {
        int height = random.nextInt(3) + baseHeight;
        if (extraRandomHeight > 1) {
            height += random.nextInt(extraRandomHeight);
        }
        return height;
    }

    protected boolean ensureGrowable(TreePlacer placer, Vector3i basePos, int height) {
        return isSpaceAt(placer, basePos, height) && ensureDirtsUnderneath(placer, basePos);
    }

    private boolean isSpaceAt(TreePlacer placer, Vector3i basePos, int height) {
        for (int i = 0; i <= 1 + height; ++i) {
            int radius = 2;
            if (i == 0) {
                radius = 1;
            } else if (i >= 1 + height - 2) {
                radius = 2;
            }

            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    if (!canGrowInto(placer, basePos.x + x, basePos.y + i, basePos.z + z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean ensureDirtsUnderneath(TreePlacer placer, Vector3i basePos) {
        int x = basePos.x;
        int y = basePos.y - 1;
        int z = basePos.z;
        placeDirt(placer, x, y, z);
        placeDirt(placer, x + 1, y, z);
        placeDirt(placer, x, y, z + 1);
        placeDirt(placer, x + 1, y, z + 1);
        return placer.isValid();
    }

    protected void growLeavesLayer(TreePlacer placer, Vector3i center, int radius) {
        int r2 = radius * radius;
        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                if (x * x + z * z <= r2) {
                    placeLeaves(placer, center.x + x, center.y, center.z + z);
                }
            }
        }
    }

    protected void growLeavesLayerStrict(TreePlacer placer, Vector3i center, int radius) {
        int r2 = radius * radius;
        for (int x = -radius; x <= radius + 1; ++x) {
            for (int z = -radius; z <= radius + 1; ++z) {
                int dx = x - 1;
                int dz = z - 1;
                if (x * x + z * z <= r2 || dx * dx + dz * dz <= r2 || x * x + dz * dz <= r2 || dx * dx + z * z <= r2) {
                    placeLeaves(placer, center.x + x, center.y, center.z + z);
                }
            }
        }
    }

    protected void placeTrunk2x2(TreePlacer placer, Vector3i basePos, int height) {
        for (int y = 0; y < height; y++) {
            placeLog(placer, basePos.x, basePos.y + y, basePos.z, PillarAxis.Y);
            placeLog(placer, basePos.x + 1, basePos.y + y, basePos.z, PillarAxis.Y);
            placeLog(placer, basePos.x, basePos.y + y, basePos.z + 1, PillarAxis.Y);
            placeLog(placer, basePos.x + 1, basePos.y + y, basePos.z + 1, PillarAxis.Y);
        }
    }
}
