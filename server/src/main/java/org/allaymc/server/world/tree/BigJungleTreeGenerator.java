package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Big jungle tree (2x2) generator.
 */
public class BigJungleTreeGenerator extends HugeTreeGenerator {

    public BigJungleTreeGenerator(BlockType<?> logType, BlockType<?> leavesType, int baseHeight, int extraRandomHeight) {
        super(logType, leavesType, baseHeight, extraRandomHeight);
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int height = getHeight(random);
        if (!ensureGrowable(placer, basePos, height)) {
            return false;
        }

        createCrown(placer, new Vector3i(basePos.x, basePos.y + height, basePos.z), 2);

        for (int y = basePos.y + height - 2 - random.nextInt(4); y > basePos.y + (height >> 1); y -= 2 + random.nextInt(4)) {
            float angle = random.nextFloat() * 6.2831855f;
            int x = (int) (basePos.x + 0.5f + Math.cos(angle) * 4.0f);
            int z = (int) (basePos.z + 0.5f + Math.sin(angle) * 4.0f);

            for (int i = 0; i < 5; ++i) {
                x = (int) (basePos.x + 1.5f + Math.cos(angle) * i);
                z = (int) (basePos.z + 1.5f + Math.sin(angle) * i);
                placeLog(placer, x, y - 3 + (i >> 1), z, PillarAxis.Y);
            }

            int branchHeight = 1 + random.nextInt(2);
            for (int yy = y - branchHeight; yy <= y; ++yy) {
                int dy = yy - y;
                growLeavesLayer(placer, new Vector3i(x, yy, z), 1 - dy);
            }
        }

        for (int yy = 0; yy < height; ++yy) {
            int x = basePos.x;
            int y = basePos.y + yy;
            int z = basePos.z;

            if (canGrowInto(placer, x, y, z)) {
                placeLog(placer, x, y, z, PillarAxis.Y);
            }
            if (yy < height - 1) {
                if (canGrowInto(placer, x + 1, y, z)) {
                    placeLog(placer, x + 1, y, z, PillarAxis.Y);
                }
                if (canGrowInto(placer, x + 1, y, z + 1)) {
                    placeLog(placer, x + 1, y, z + 1, PillarAxis.Y);
                }
                if (canGrowInto(placer, x, y, z + 1)) {
                    placeLog(placer, x, y, z + 1, PillarAxis.Y);
                }
            }
        }

        return placer.isValid();
    }

    private void createCrown(TreePlacer placer, Vector3i pos, int radius) {
        for (int y = -2; y <= 0; ++y) {
            growLeavesLayerStrict(placer, new Vector3i(pos.x, pos.y + y, pos.z), radius + 1 - y);
        }
    }
}
