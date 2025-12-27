package org.allaymc.server.world.tree;

import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.data.BlockTags;
import org.allaymc.api.block.property.enums.PillarAxis;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.server.block.BlockPlaceHelper;
import org.joml.Vector3i;

import java.util.Random;

/**
 * Mangrove tree generator.
 */
public class MangroveTreeGenerator extends AbstractTreeGenerator {
    private static final int ROOT_MAX_DEPTH = 12;
    private static final int ROOT_MOUND_HEIGHT = 2;
    private static final int ROOT_ARM_MIN = 1;
    private static final int ROOT_ARM_MAX = 2;
    private static final int CANOPY_RADIUS = 2;
    private static final int CANOPY_OFFSET = 3;
    private static final int VINE_MAX_LENGTH = 3;
    private static final int VINE_CHANCE = 6;
    private static final int PROPAGULE_CHANCE = 12;
    private static final int BEE_NEST_CHANCE = 100;
    private static final int MOSS_CARPET_CHANCE = 2;

    public MangroveTreeGenerator(BlockType<?> logType, BlockType<?> leavesType) {
        super(logType, leavesType);
    }

    @Override
    public boolean generate(TreePlacer placer, Random random, Vector3i basePos) {
        int trunkHeight = random.nextInt(4) + 9;
        int trunkStartY = basePos.y;
        int topY = trunkStartY + trunkHeight;

        if (!canPlace(placer, basePos, trunkHeight + ROOT_MOUND_HEIGHT)) {
            return false;
        }

        int rootTopY = trunkStartY + ROOT_MOUND_HEIGHT - 1;
        placeRootMound(placer, basePos, rootTopY);
        placeRootArm(placer, random, basePos, 1, 0, rootTopY);
        placeRootArm(placer, random, basePos, -1, 0, rootTopY);
        placeRootArm(placer, random, basePos, 0, 1, rootTopY);
        placeRootArm(placer, random, basePos, 0, -1, rootTopY);

        // Trunk
        for (int y = trunkStartY; y <= topY; y++) {
            placeLog(placer, basePos.x, y, basePos.z, PillarAxis.Y);
        }

        // Canopy cross pattern
        int canopyBaseY = topY - 4;
        placeLeafCluster(placer, basePos.x, canopyBaseY, basePos.z, CANOPY_RADIUS, 3);
        placeLeafCluster(placer, basePos.x, canopyBaseY + 1, basePos.z + CANOPY_OFFSET, CANOPY_RADIUS, 3);
        placeLeafCluster(placer, basePos.x, canopyBaseY + 1, basePos.z - CANOPY_OFFSET, CANOPY_RADIUS, 3);
        placeLeafCluster(placer, basePos.x + CANOPY_OFFSET, canopyBaseY + 2, basePos.z, CANOPY_RADIUS, 3);
        placeLeafCluster(placer, basePos.x - CANOPY_OFFSET, canopyBaseY + 2, basePos.z, CANOPY_RADIUS, 3);
        placeLeafLayer(placer, basePos.x, canopyBaseY - 1, basePos.z, 1, true);

        // Vines + propagules
        int minX = basePos.x - (CANOPY_OFFSET + CANOPY_RADIUS);
        int maxX = basePos.x + (CANOPY_OFFSET + CANOPY_RADIUS);
        int minZ = basePos.z - (CANOPY_OFFSET + CANOPY_RADIUS);
        int maxZ = basePos.z + (CANOPY_OFFSET + CANOPY_RADIUS);
        int minY = canopyBaseY - 1;
        int maxY = canopyBaseY + 5;
        placeVinesAndPropagules(placer, random, minX, maxX, minY, maxY, minZ, maxZ);

        // Bee nest
        placeBeeNest(placer, random, basePos, canopyBaseY + 1, topY - 1);
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

    private void placeRootColumn(TreePlacer placer, int x, int y, int z) {
        int stopY = findRootStopY(placer, x, y, z);
        if (stopY > y) {
            return;
        }
        var rootState = BlockTypes.MANGROVE_ROOTS.getDefaultState();
        for (int yy = y; yy >= stopY; yy--) {
            placer.setRoot(x, yy, z, rootState);
        }
    }

    private int findRootStopY(TreePlacer placer, int x, int y, int z) {
        for (int yy = y, depth = 0; depth <= ROOT_MAX_DEPTH; yy--, depth++) {
            var type = placer.getBlockType(x, yy, z);
            if (type == BlockTypes.MUD) {
                return yy;
            }
            if (isRootPassable(type)) {
                continue;
            }
            return yy + 1;
        }
        return y + 1;
    }

    private boolean isRootPassable(BlockType<?> type) {
        if (type == BlockTypes.AIR) {
            return true;
        }
        if (type.hasBlockTag(BlockTags.WATER)) {
            return true;
        }
        if (type.hasBlockTag(BlockTags.REPLACEABLE) && !type.hasBlockTag(BlockTags.LAVA)) {
            return true;
        }
        return type == BlockTypes.MANGROVE_ROOTS || type == BlockTypes.MUDDY_MANGROVE_ROOTS;
    }

    private void placeRootMound(TreePlacer placer, Vector3i basePos, int rootTopY) {
        var rootState = BlockTypes.MANGROVE_ROOTS.getDefaultState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) != 1) {
                    continue;
                }
                placer.setRoot(basePos.x + dx, rootTopY, basePos.z + dz, rootState);
                placer.setRoot(basePos.x + dx, rootTopY - 1, basePos.z + dz, rootState);
            }
        }
    }

    private void placeRootArm(TreePlacer placer, Random random, Vector3i basePos, int dx, int dz, int rootTopY) {
        int length = ROOT_ARM_MIN + random.nextInt(ROOT_ARM_MAX - ROOT_ARM_MIN + 1);
        int x = basePos.x;
        int z = basePos.z;
        var rootState = BlockTypes.MANGROVE_ROOTS.getDefaultState();
        for (int i = 0; i < length; i++) {
            x += dx;
            z += dz;
            placer.setRoot(x, rootTopY, z, rootState);
            placer.setRoot(x, rootTopY - 1, z, rootState);
            if (random.nextBoolean()) {
                placer.setRoot(x + dz, rootTopY - 1, z + dx, rootState);
            }
        }

        placeRootColumn(placer, x, rootTopY - 1, z);
        placeMossCarpet(placer, random, x, rootTopY, z);
    }

    private void placeMossCarpet(TreePlacer placer, Random random, int x, int y, int z) {
        if (random.nextInt(MOSS_CARPET_CHANCE) != 0) {
            return;
        }
        if (!placer.isAir(x, y + 1, z)) {
            return;
        }
        placer.setBlock(x, y + 1, z, BlockTypes.MOSS_CARPET.getDefaultState());
    }

    private void placeLeafCluster(TreePlacer placer, int centerX, int centerY, int centerZ, int radius, int height) {
        for (int i = 0; i < height; i++) {
            int y = centerY + i;
            int r = Math.max(1, radius - (i > 0 ? 1 : 0));
            placeLeafLayer(placer, centerX, y, centerZ, r, i == height - 1);
        }
    }

    private void placeLeafLayer(TreePlacer placer, int centerX, int y, int centerZ, int radius, boolean hollowCorners) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (hollowCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                placeLeaves(placer, centerX + dx, y, centerZ + dz);
            }
        }
    }

    private void placeVinesAndPropagules(TreePlacer placer, Random random, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (placer.getBlockType(x, y, z) != leavesType) {
                        continue;
                    }
                    placeHangingPropagule(placer, random, x, y - 1, z);
                    placeVine(placer, random, x, y, z, BlockFace.NORTH, 0, 0, -1);
                    placeVine(placer, random, x, y, z, BlockFace.SOUTH, 0, 0, 1);
                    placeVine(placer, random, x, y, z, BlockFace.WEST, -1, 0, 0);
                    placeVine(placer, random, x, y, z, BlockFace.EAST, 1, 0, 0);
                }
            }
        }
    }

    private void placeVine(TreePlacer placer, Random random, int leafX, int leafY, int leafZ, BlockFace face, int dx, int dy, int dz) {
        if (random.nextInt(VINE_CHANCE) != 0) {
            return;
        }

        int x = leafX + dx;
        int y = leafY + dy;
        int z = leafZ + dz;
        if (!placer.isAir(x, y, z)) {
            return;
        }

        var vineState = BlockTypes.VINE.getDefaultState()
                .setPropertyValue(BlockPropertyTypes.VINE_DIRECTION_BITS, vineBitsForFace(face));
        placer.setBlock(x, y, z, vineState);

        int length = 1 + random.nextInt(VINE_MAX_LENGTH);
        for (int i = 1; i < length; i++) {
            int yy = y - i;
            if (!placer.isAir(x, yy, z)) {
                break;
            }
            placer.setBlock(x, yy, z, vineState);
        }
    }

    private int vineBitsForFace(BlockFace face) {
        return switch (face) {
            case SOUTH -> 1;
            case WEST -> 2;
            case NORTH -> 4;
            case EAST -> 8;
            default -> 1;
        };
    }

    private void placeHangingPropagule(TreePlacer placer, Random random, int x, int y, int z) {
        if (random.nextInt(PROPAGULE_CHANCE) != 0) {
            return;
        }
        if (!placer.isAir(x, y, z)) {
            return;
        }
        var stageMax = BlockPropertyTypes.PROPAGULE_STAGE.getMax();
        var state = BlockTypes.MANGROVE_PROPAGULE.getDefaultState()
                .setPropertyValue(BlockPropertyTypes.HANGING, true)
                .setPropertyValue(BlockPropertyTypes.PROPAGULE_STAGE, stageMax);
        placer.setBlock(x, y, z, state);
    }

    private void placeBeeNest(TreePlacer placer, Random random, Vector3i basePos, int minY, int maxY) {
        if (random.nextInt(BEE_NEST_CHANCE) != 0) {
            return;
        }

        int y = Math.max(minY, basePos.y + 2);
        if (y > maxY) {
            return;
        }

        BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};
        var face = faces[random.nextInt(faces.length)];
        int dx = face.getOffset().x();
        int dz = face.getOffset().z();
        int x = basePos.x + dx;
        int z = basePos.z + dz;
        if (!placer.isAir(x, y, z)) {
            return;
        }

        int dir = BlockPlaceHelper.EWSN_DIRECTION_4_MAPPER.get(face);
        var state = BlockTypes.BEE_NEST.getDefaultState()
                .setPropertyValue(BlockPropertyTypes.DIRECTION_4, dir)
                .setPropertyValue(BlockPropertyTypes.HONEY_LEVEL, 0);
        placer.setBlock(x, y, z, state);
    }
}
