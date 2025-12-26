package org.allaymc.server.block.component;

import org.allaymc.api.block.BlockBehavior;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.data.BlockTags;
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

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mangrove propagule growth logic.
 */
public class BlockMangrovePropaguleBaseComponentImpl extends BlockBaseComponentImpl {
    private static final int MIN_LIGHT_LEVEL = 8;
    private static final int RANDOM_GROW_CHANCE = 7;
    private static final float BONE_MEAL_GROW_CHANCE = 0.45f;

    private final TreeSpecies species;

    public BlockMangrovePropaguleBaseComponentImpl(BlockType<? extends BlockBehavior> blockType) {
        super(blockType);
        this.species = TreeSpecies.fromSapling(blockType);
    }

    @Override
    public boolean place(Dimension dimension, BlockState blockState, Vector3ic placeBlockPos, PlayerInteractInfo placementInfo) {
        if (placementInfo == null) {
            return dimension.setBlockState(placeBlockPos, blockState);
        }

        var face = placementInfo.blockFace();
        if (face != BlockFace.UP && face != BlockFace.DOWN) {
            return false;
        }

        if (face == BlockFace.DOWN) {
            var upper = dimension.getBlockState(BlockFace.UP.offsetPos(placeBlockPos));
            if (upper.getBlockType().hasBlockTag(BlockTags.REPLACEABLE)) {
                return false;
            }
            blockState = blockState.setPropertyValue(BlockPropertyTypes.HANGING, true);
        } else {
            if (!canPlaceOn(placementInfo.getClickedBlock().getBlockType())) {
                return false;
            }
            blockState = blockState.setPropertyValue(BlockPropertyTypes.HANGING, false);
        }

        return dimension.setBlockState(placeBlockPos, blockState, placementInfo);
    }

    @Override
    public void onNeighborUpdate(Block block, Block neighbor, BlockFace face) {
        super.onNeighborUpdate(block, neighbor, face);
        var hanging = block.getPropertyValue(BlockPropertyTypes.HANGING);
        if (hanging && face == BlockFace.UP && neighbor.getBlockType().hasBlockTag(BlockTags.REPLACEABLE)) {
            block.breakBlock();
            return;
        }

        if (!hanging && face == BlockFace.DOWN && !canPlaceOn(neighbor.getBlockType())) {
            block.breakBlock();
        }
    }

    @Override
    public void onRandomUpdate(Block block) {
        super.onRandomUpdate(block);
        if (species == null) {
            return;
        }

        var hanging = block.getPropertyValue(BlockPropertyTypes.HANGING);
        var stage = block.getPropertyValue(BlockPropertyTypes.PROPAGULE_STAGE);
        if (hanging) {
            if (stage < BlockPropertyTypes.PROPAGULE_STAGE.getMax()) {
                block.updateBlockProperty(BlockPropertyTypes.PROPAGULE_STAGE, stage + 1);
            }
            return;
        }

        var pos = block.getPosition();
        if (block.getDimension().getLightEngine().getInternalLight(pos.x(), pos.y() + 1, pos.z()) < MIN_LIGHT_LEVEL) {
            return;
        }

        if (ThreadLocalRandom.current().nextInt(RANDOM_GROW_CHANCE) != 0) {
            return;
        }

        if (stage < BlockPropertyTypes.PROPAGULE_STAGE.getMax()) {
            block.updateBlockProperty(BlockPropertyTypes.PROPAGULE_STAGE, BlockPropertyTypes.PROPAGULE_STAGE.getMax());
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
        var stage = block.getPropertyValue(BlockPropertyTypes.PROPAGULE_STAGE);
        if (stage < BlockPropertyTypes.PROPAGULE_STAGE.getMax()) {
            block.updateBlockProperty(BlockPropertyTypes.PROPAGULE_STAGE, BlockPropertyTypes.PROPAGULE_STAGE.getMax());
        } else if (ThreadLocalRandom.current().nextFloat() < BONE_MEAL_GROW_CHANCE) {
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

        var dimension = block.getDimension();
        var placer = new TreePlacer(dimension, block.getBlockType(), species.allowWater());
        var generator = species.selectGenerator(false);
        if (generator == null) {
            return false;
        }

        if (!generator.generate(placer, ThreadLocalRandom.current(), new Vector3i(block.getPosition())) || !placer.isValid()) {
            return false;
        }

        placer.apply();
        dimension.updateAround(block.getPosition());
        return true;
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
