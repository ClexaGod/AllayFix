package org.allaymc.server.world.tree;

import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.type.BlockType;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Dark oak tree generator (2x2).
 */
public class DarkOakTreeGenerator extends AbstractTreeGenerator {

    public DarkOakTreeGenerator(BlockType<?> logType, BlockType<?> leavesType) {
        super(logType, leavesType);
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int height = random.nextInt(3) + random.nextInt(2) + 6;
        if (!canPlace(placer, basePos, height)) {
            return false;
        }

        placeDirt(placer, basePos.x, basePos.y - 1, basePos.z);
        placeDirt(placer, basePos.x + 1, basePos.y - 1, basePos.z);
        placeDirt(placer, basePos.x, basePos.y - 1, basePos.z + 1);
        placeDirt(placer, basePos.x + 1, basePos.y - 1, basePos.z + 1);

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

        int bendStart = height - random.nextInt(4);
        int bendLen = 2 - random.nextInt(3);
        int x = basePos.x;
        int z = basePos.z;
        int topY = basePos.y + height - 1;

        for (int y = 0; y < height; ++y) {
            if (y >= bendStart && bendLen > 0) {
                x += dx;
                z += dz;
                --bendLen;
            }
            int yy = basePos.y + y;
            placeLog(placer, x, yy, z, PillarAxis.Y);
            placeLog(placer, x + 1, yy, z, PillarAxis.Y);
            placeLog(placer, x, yy, z + 1, PillarAxis.Y);
            placeLog(placer, x + 1, yy, z + 1, PillarAxis.Y);
        }

        for (int dx2 = -2; dx2 <= 0; ++dx2) {
            for (int dz2 = -2; dz2 <= 0; ++dz2) {
                int y = topY - 1;
                placeLeaves(placer, x + dx2, y, z + dz2);
                placeLeaves(placer, x + 1 - dx2, y, z + dz2);
                placeLeaves(placer, x + dx2, y, z + 1 - dz2);
                placeLeaves(placer, x + 1 - dx2, y, z + 1 - dz2);

                if ((dx2 > -2 || dz2 > -1) && (dx2 != -1 || dz2 != -2)) {
                    y = topY + 1;
                    placeLeaves(placer, x + dx2, y, z + dz2);
                    placeLeaves(placer, x + 1 - dx2, y, z + dz2);
                    placeLeaves(placer, x + dx2, y, z + 1 - dz2);
                    placeLeaves(placer, x + 1 - dx2, y, z + 1 - dz2);
                }
            }
        }

        if (random.nextBoolean()) {
            placeLeaves(placer, x, topY + 2, z);
            placeLeaves(placer, x + 1, topY + 2, z);
            placeLeaves(placer, x + 1, topY + 2, z + 1);
            placeLeaves(placer, x, topY + 2, z + 1);
        }

        for (int dx2 = -3; dx2 <= 4; ++dx2) {
            for (int dz2 = -3; dz2 <= 4; ++dz2) {
                if ((dx2 != -3 || dz2 != -3) && (dx2 != -3 || dz2 != 4) && (dx2 != 4 || dz2 != -3) &&
                    (dx2 != 4 || dz2 != 4) && (Math.abs(dx2) < 3 || Math.abs(dz2) < 3)) {
                    placeLeaves(placer, x + dx2, topY, z + dz2);
                }
            }
        }

        for (int dx2 = -1; dx2 <= 2; ++dx2) {
            for (int dz2 = -1; dz2 <= 2; ++dz2) {
                if ((dx2 < 0 || dx2 > 1 || dz2 < 0 || dz2 > 1) && random.nextInt(3) == 0) {
                    int branchLen = random.nextInt(3) + 2;
                    for (int i = 0; i < branchLen; ++i) {
                        placeLog(placer, basePos.x + dx2, topY - i - 1, basePos.z + dz2, PillarAxis.Y);
                    }
                    for (int lx = -1; lx <= 1; ++lx) {
                        for (int lz = -1; lz <= 1; ++lz) {
                            placeLeaves(placer, x + dx2 + lx, topY, z + dz2 + lz);
                        }
                    }
                    for (int lx = -2; lx <= 2; ++lx) {
                        for (int lz = -2; lz <= 2; ++lz) {
                            if (Math.abs(lx) != 2 || Math.abs(lz) != 2) {
                                placeLeaves(placer, x + dx2 + lx, topY - 1, z + dz2 + lz);
                            }
                        }
                    }
                }
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
