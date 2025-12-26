package org.allaymc.server.block.component;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.dto.Block;
import org.allaymc.api.block.dto.PlayerInteractInfo;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.type.ItemTypes;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.server.world.tree.TreePlacer;
import org.allaymc.server.world.tree.TreeSpecies;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sapling growth logic (random tick + bone meal).
 */
public class BlockSaplingBaseComponentImpl extends BlockBaseComponentImpl {
    private static final int MIN_LIGHT_LEVEL = 8;
    private static final int RANDOM_GROW_CHANCE = 7;
    private static final float BONE_MEAL_GROW_CHANCE = 0.45f;

    private final TreeSpecies species;

    public BlockSaplingBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
        this.species = TreeSpecies.fromSapling(blockType);
    }

    @Override
    public boolean place(Dimension dimension, BlockState blockState, Vector3ic placeBlockPos, PlayerInteractInfo placementInfo) {
        if (placementInfo == null) {
            return dimension.setBlockState(placeBlockPos, blockState);
        }

        if (placementInfo.blockFace() != BlockFace.UP) {
            return false;
        }

        if (!canPlaceOn(placementInfo.getClickedBlock().getBlockType())) {
            return false;
        }

        return dimension.setBlockState(placeBlockPos.x(), placeBlockPos.y(), placeBlockPos.z(), blockState, placementInfo);
    }

    @Override
    public void onNeighborUpdate(Block block, Block neighbor, BlockFace face) {
        super.onNeighborUpdate(block, neighbor, face);
        if (face == BlockFace.DOWN && !canPlaceOn(neighbor.getBlockType())) {
            block.breakBlock();
        }
    }

    @Override
    public void onRandomUpdate(Block block) {
        super.onRandomUpdate(block);
        if (species == null) {
            return;
        }

        var pos = block.getPosition();
        if (block.getDimension().getLightEngine().getInternalLight(pos.x(), pos.y() + 1, pos.z()) < MIN_LIGHT_LEVEL) {
            return;
        }

        if (ThreadLocalRandom.current().nextInt(RANDOM_GROW_CHANCE) != 0) {
            return;
        }

        if (!block.getPropertyValue(BlockPropertyTypes.AGE_BIT)) {
            block.updateBlockProperty(BlockPropertyTypes.AGE_BIT, true);
            return;
        }

        tryGrow(block);
    }

    @Override
    public boolean onInteract(ItemStack itemStack, Dimension dimension, PlayerInteractInfo interactInfo) {
        if (super.onInteract(itemStack, dimension, interactInfo)) {
            return true;
        }

        if (itemStack.getItemType() != ItemTypes.BONE_MEAL) {
            return false;
        }

        var block = interactInfo.getClickedBlock();
        if (species != null && ThreadLocalRandom.current().nextFloat() < BONE_MEAL_GROW_CHANCE) {
            tryGrow(block);
        }

        interactInfo.player().tryConsumeItemInHand();
        dimension.addParticle(MathUtils.center(interactInfo.clickedBlockPos()), SimpleParticle.BONE_MEAL);
        return true;
    }

    @Override
    public boolean canRandomUpdate() {
        return true;
    }

    private boolean tryGrow(Block block) {
        if (species == null) {
            return false;
        }

        var basePos = new Vector3i(block.getPosition());
        var dimension = block.getDimension();
        var saplingType = block.getBlockType();
        boolean useLarge = false;
        Vector3i anchor = basePos;
        List<Vector3i> saplingPositions = new ArrayList<>();

        if (species.requires2x2() || species.hasLargeGenerator()) {
            var found = find2x2Saplings(dimension, basePos, saplingType);
            if (found != null) {
                anchor = found;
                useLarge = true;
                saplingPositions.add(found);
                saplingPositions.add(new Vector3i(found.x + 1, found.y, found.z));
                saplingPositions.add(new Vector3i(found.x, found.y, found.z + 1));
                saplingPositions.add(new Vector3i(found.x + 1, found.y, found.z + 1));
            } else if (species.requires2x2()) {
                return false;
            }
        }

        if (!useLarge) {
            saplingPositions.add(basePos);
        }

        var placer = new TreePlacer(dimension, saplingType, species.allowWater());
        var generator = species.selectGenerator(useLarge);
        if (generator == null) {
            return false;
        }

        if (!generator.generate(placer, ThreadLocalRandom.current(), anchor) || !placer.isValid()) {
            return false;
        }

        placer.apply();
        for (var pos : saplingPositions) {
            dimension.updateAround(pos);
        }
        return true;
    }

    private Vector3i find2x2Saplings(Dimension dimension, Vector3i pos, BlockType<?> saplingType) {
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                int x = pos.x + dx;
                int z = pos.z + dz;
                if (dimension.getBlockState(x, pos.y, z).getBlockType() == saplingType &&
                    dimension.getBlockState(x + 1, pos.y, z).getBlockType() == saplingType &&
                    dimension.getBlockState(x, pos.y, z + 1).getBlockType() == saplingType &&
                    dimension.getBlockState(x + 1, pos.y, z + 1).getBlockType() == saplingType) {
                    return new Vector3i(x, pos.y, z);
                }
            }
        }
        return null;
    }

    private boolean canPlaceOn(BlockType<?> blockType) {
        return blockType == BlockTypes.GRASS_BLOCK ||
               blockType == BlockTypes.DIRT ||
               blockType == BlockTypes.PODZOL ||
               blockType == BlockTypes.MYCELIUM ||
               blockType == BlockTypes.FARMLAND ||
               blockType == BlockTypes.MOSS_BLOCK ||
               blockType == BlockTypes.MUD ||
               blockType == BlockTypes.DIRT_WITH_ROOTS ||
               blockType == BlockTypes.MUDDY_MANGROVE_ROOTS;
    }
}
