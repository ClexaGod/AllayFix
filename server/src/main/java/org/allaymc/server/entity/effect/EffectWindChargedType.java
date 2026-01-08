package org.allaymc.server.entity.effect;

import org.allaymc.api.entity.effect.AbstractEffectType;
import org.allaymc.api.entity.effect.EffectInstance;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.interfaces.EntityLiving;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.utils.identifier.Identifier;
import org.allaymc.api.world.particle.CustomParticle;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;

import java.awt.*;

/**
 * @author IWareQ
 */
public class EffectWindChargedType extends AbstractEffectType {
    public EffectWindChargedType() {
        super(32, new Identifier("minecraft:wind_charged"), new Color(0xbdc9ff), true);
    }

    @Override
    public void onEntityDies(EntityLiving entity, EffectInstance effectInstance) {
        var location = entity.getLocation();
        var dimension = entity.getDimension();
        dimension.addSound(location, new CustomSound(SoundNames.WIND_CHARGE_BURST));
        dimension.addParticle(location, new CustomParticle("minecraft:wind_explosion"));

        var radius = 2.0;
        var aabb = MathUtils.grow(new AABBd(entity.getOffsetAABB()), new Vector3d(radius, radius, radius));
        var entities = dimension.getEntityManager().getPhysicsService().computeCollidingEntities(aabb);
        var radiusSquared = radius * radius;
        for (var other : entities) {
            if (other == entity || !(other instanceof EntityLiving living)) {
                continue;
            }
            if (living.getLocation().distanceSquared(location) > radiusSquared) {
                continue;
            }
            if (living instanceof EntityPhysicsComponent physicsComponent) {
                physicsComponent.knockback(location, 0.2, 0.3);
            }
        }
    }
}
