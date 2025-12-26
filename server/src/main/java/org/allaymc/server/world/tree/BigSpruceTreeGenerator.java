package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Mega spruce (2x2) generator.
 */
public class BigSpruceTreeGenerator extends SpruceTreeGenerator {
    private final float leafStartHeightMultiplier;
    private final int baseLeafRadius;

    public BigSpruceTreeGenerator(BlockType<?> logType, BlockType<?> leavesType, float leafStartHeightMultiplier, int baseLeafRadius) {
        super(logType, leavesType);
        this.leafStartHeightMultiplier = leafStartHeightMultiplier;
        this.baseLeafRadius = baseLeafRadius;
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int treeHeight = random.nextInt(15) + 15;
        int topSize = treeHeight - (int) (treeHeight * leafStartHeightMultiplier);

        if (!canPlace(placer, basePos, treeHeight)) {
            return false;
        }

        placeDirt(placer, basePos.x, basePos.y - 1, basePos.z);
        placeDirt(placer, basePos.x + 1, basePos.y - 1, basePos.z);
        placeDirt(placer, basePos.x, basePos.y - 1, basePos.z + 1);
        placeDirt(placer, basePos.x + 1, basePos.y - 1, basePos.z + 1);

        placeTrunk2x2(placer, basePos, treeHeight - random.nextInt(3));
        placeLeaves(placer, random, basePos, treeHeight, topSize, baseLeafRadius);
        return placer.isValid();
    }

    protected void placeTrunk2x2(TreePlacer placer, Vector3i basePos, int trunkHeight) {
        for (int yy = 0; yy < trunkHeight; yy++) {
            for (int xx = 0; xx < 2; xx++) {
                for (int zz = 0; zz < 2; zz++) {
                    placeLog(placer, basePos.x + xx, basePos.y + yy, basePos.z + zz, PillarAxis.Y);
                }
            }
        }
    }

    @Override
    protected void placeLeaves(TreePlacer placer, Random random, Vector3i basePos, int treeHeight, int topSize, int leafRadius) {
        int radius = random.nextInt(2);
        int maxR = 1;
        int minR = 0;

        for (int yy = 0; yy <= topSize; yy++) {
            int y = basePos.y + treeHeight - yy;
            for (int xx = basePos.x - radius; xx <= basePos.x + radius + 1; xx++) {
                for (int zz = basePos.z - radius; zz <= basePos.z + radius + 1; zz++) {
                    if (topSize - yy > 1 && radius > 0) {
                        if (Math.abs(xx - basePos.x) > radius && Math.abs(zz - basePos.z) > radius) {
                            continue;
                        }
                        if (xx - basePos.x <= -radius && zz - basePos.z <= -radius) {
                            continue;
                        }
                        if (Math.abs(xx - basePos.x) > radius && zz - basePos.z <= -radius) {
                            continue;
                        }
                        if (xx - basePos.x <= -radius && Math.abs(zz - basePos.z) > radius) {
                            continue;
                        }
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
