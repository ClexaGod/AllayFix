package org.allaymc.server.entity.component.projectile;

import org.allaymc.api.block.dto.Block;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.damage.DamageContainer;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.math.location.Location3d;
import org.allaymc.api.world.particle.Particle;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.api.world.sound.SimpleSound;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;

/**
 * Wind charge projectile behavior.
 *
 * @author ClexaGod
 */
public class EntityWindChargePhysicsComponentImpl extends EntityProjectilePhysicsComponentImpl {
    private static final double KNOCKBACK_Y = 0.3;

    @Override
    public boolean hasGravity() {
        return false;
    }

    @Override
    public double getGravity() {
        return 0.0;
    }

    @Override
    public double getDragFactorInAir() {
        return 0.01;
    }

    @Override
    public boolean applyMotion() {
        if (motion.lengthSquared() == 0) {
            return false;
        }

        var location = thisEntity.getLocation();
        var newPos = new Location3d(location);
        newPos.add(motion);

        var dimension = thisEntity.getDimension();
        var dimensionInfo = dimension.getDimensionInfo();
        if (newPos.y() < dimensionInfo.minHeight() - 1 || newPos.y() > dimensionInfo.maxHeight() + 1) {
            playBurstEffect();
            thisEntity.remove();
            return true;
        }

        if (dimension.getChunkManager().getChunkByDimensionPos((int) newPos.x(), (int) newPos.z()) == null) {
            thisEntity.remove();
            return true;
        }

        return super.applyMotion();
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
        dimension.addParticle(location, getBurstParticle());
    }

    protected Particle getBurstParticle() {
        return SimpleParticle.WIND_EXPLOSION;
    }

    protected double getBurstRadius() {
        return 2.0;
    }

    protected double getKnockbackStrength() {
        return 0.2;
    }

    protected SimpleSound getBurstSound() {
        return SimpleSound.WIND_CHARGE_BURST;
    }
}
