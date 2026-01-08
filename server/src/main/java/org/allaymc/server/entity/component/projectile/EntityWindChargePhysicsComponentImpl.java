package org.allaymc.server.entity.component.projectile;

import org.allaymc.api.block.dto.Block;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.world.particle.CustomParticle;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;

/**
 * Wind charge projectile behavior.
 *
 * @author ClexaGod
 */
public class EntityWindChargePhysicsComponentImpl extends EntityProjectilePhysicsComponentImpl {
    private static final CustomParticle BURST_PARTICLE = new CustomParticle("minecraft:wind_explosion");
    private static final double KNOCKBACK_Y = 0.3;

    @Override
    public double getGravity() {
        return 0.0;
    }

    @Override
    public double getDragFactorInAir() {
        return 0.01;
    }

    @Override
    protected void onHitEntity(Entity other, Vector3dc hitPos) {
        if (thisEntity.willBeDespawnedNextTick()) {
            return;
        }

        if (other instanceof EntityLiving living) {
            var damage = DamageContainer.projectile(thisEntity, 1);
            damage.setHasKnockback(false);
            if (living.attack(damage) && other instanceof EntityPhysicsComponent physicsComponent) {
                applyKnockback(physicsComponent);
            }
        } else if (other instanceof EntityPhysicsComponent physicsComponent) {
            applyKnockback(physicsComponent);
        }

        playBurstEffect();
        thisEntity.remove();
    }

    @Override
    protected void onHitBlock(Block block, Vector3dc hitPos) {
        if (thisEntity.willBeDespawnedNextTick()) {
            return;
        }

        knockbackNearbyEntities();
        playBurstEffect();
        thisEntity.remove();
    }

    protected void knockbackNearbyEntities() {
        var radius = getBurstRadius();
        var aabb = MathUtils.grow(new AABBd(thisEntity.getOffsetAABB()), new Vector3d(radius, radius, radius));
        var entities = thisEntity.getDimension().getEntityManager().getPhysicsService().computeCollidingEntities(aabb);
        var location = thisEntity.getLocation();
        var radiusSquared = radius * radius;
        for (var entity : entities) {
            if (entity == thisEntity || !(entity instanceof EntityLiving living)) {
                continue;
            }
            if (living.getLocation().distanceSquared(location) > radiusSquared) {
                continue;
            }
            if (living instanceof EntityPhysicsComponent physicsComponent) {
                physicsComponent.knockback(location, getKnockbackStrength(), KNOCKBACK_Y);
            }
        }
    }

    protected void applyKnockback(EntityPhysicsComponent physicsComponent) {
        physicsComponent.knockback(thisEntity.getLocation(), getKnockbackStrength(), KNOCKBACK_Y);
    }

    protected void playBurstEffect() {
        var location = thisEntity.getLocation();
        var dimension = thisEntity.getDimension();
        dimension.addSound(location, getBurstSound());
        dimension.addParticle(location, BURST_PARTICLE);
    }

    protected double getBurstRadius() {
        return 2.0;
    }

    protected double getKnockbackStrength() {
        return 0.2;
    }

    protected CustomSound getBurstSound() {
        return new CustomSound(SoundNames.WIND_CHARGE_BURST);
    }
}
