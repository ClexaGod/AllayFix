package org.allaymc.server.item.component;

import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.item.ItemStackInitInfo;
import org.allaymc.api.world.Dimension;
import org.allaymc.api.world.particle.BlockBreakParticle;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.joml.Vector3d;

/**
 * @author ClexaGod
 */
public class ItemMaceBaseComponentImpl extends ItemBaseComponentImpl {

    private static final double SMASH_TRIGGER_FALL_DISTANCE = 1.5;
    private static final int SMASH_BLOCKS_HIGH = 3;
    private static final int SMASH_BLOCKS_MID = 5;
    private static final float HEAVY_SMASH_DAMAGE = 16f;

    public ItemMaceBaseComponentImpl(ItemStackInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public float calculateAttackDamage(Entity attacker, Entity target) {
        var baseDamage = getItemType().getItemData().attackDamage();
        return baseDamage + calculateSmashBonus(attacker);
    }

    @Override
    public void onAttackEntity(Entity attacker, Entity victim) {
        super.onAttackEntity(attacker, victim);

        if (!isSmashAttack(attacker)) {
            return;
        }

        applySmashEffects(attacker);
    }

    private boolean isSmashAttack(Entity attacker) {
        return getFallDistance(attacker) > SMASH_TRIGGER_FALL_DISTANCE;
    }

    private float calculateSmashBonus(Entity attacker) {
        var fallDistance = getFallDistance(attacker);
        if (fallDistance <= SMASH_TRIGGER_FALL_DISTANCE) {
            return 0f;
        }

        var fallBlocks = (int) Math.floor(fallDistance);
        var bonus = 0f;
        for (int i = 1; i <= fallBlocks; i++) {
            if (i <= SMASH_BLOCKS_HIGH) {
                bonus += 4f;
            } else if (i <= SMASH_BLOCKS_HIGH + SMASH_BLOCKS_MID) {
                bonus += 2f;
            } else {
                bonus += 1f;
            }
        }

        return bonus;
    }

    private double getFallDistance(Entity attacker) {
        if (attacker instanceof EntityPhysicsComponent physicsComponent) {
            return physicsComponent.getFallDistance();
        }

        return 0;
    }

    private void applySmashEffects(Entity attacker) {
        if (!(attacker instanceof EntityPhysicsComponent physicsComponent)) {
            return;
        }

        var dimension = attacker.getDimension();
        var location = attacker.getLocation();
        var smashDamage = getItemType().getItemData().attackDamage() + calculateSmashBonus(attacker);

        physicsComponent.resetFallDistance();

        var motion = physicsComponent.getMotion();
        if (motion.y() < 0) {
            physicsComponent.setMotion(new Vector3d(motion.x(), 0, motion.z()));
        }

        if (physicsComponent.isOnGround()) {
            if (smashDamage >= HEAVY_SMASH_DAMAGE) {
                dimension.addSound(location, new CustomSound(SoundNames.MACE_HEAVY_SMASH_GROUND));
            } else {
                dimension.addSound(location, new CustomSound(SoundNames.MACE_SMASH_GROUND));
            }
        } else {
            dimension.addSound(location, new CustomSound(SoundNames.MACE_SMASH_AIR));
        }

        spawnSmashParticles(dimension, location.x(), location.y(), location.z());
    }

    private void spawnSmashParticles(Dimension dimension, double x, double y, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int blockY = (int) Math.floor(y) - 1;
        int minY = dimension.getDimensionInfo().minHeight();

        BlockState blockState = null;
        while (blockY >= minY) {
            var candidate = dimension.getBlockState(blockX, blockY, blockZ);
            if (candidate.getBlockType() != BlockTypes.AIR) {
                blockState = candidate;
                break;
            }
            blockY--;
        }

        if (blockState == null) {
            return;
        }

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                dimension.addParticle(
                        x + 0.5 + ox,
                        y + 0.1,
                        z + 0.5 + oz,
                        new BlockBreakParticle(blockState)
                );
            }
        }
    }
}
