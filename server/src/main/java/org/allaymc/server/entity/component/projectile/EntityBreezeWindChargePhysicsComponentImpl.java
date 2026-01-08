package org.allaymc.server.entity.component.projectile;

import org.allaymc.api.world.particle.Particle;
import org.allaymc.api.world.particle.SimpleParticle;
import org.allaymc.api.world.sound.SimpleSound;

/**
 * Breeze wind charge projectile behavior.
 *
 * @author ClexaGod
 */
public class EntityBreezeWindChargePhysicsComponentImpl extends EntityWindChargePhysicsComponentImpl {
    @Override
    protected double getBurstRadius() {
        return 3.0;
    }

    @Override
    protected double getKnockbackStrength() {
        return 0.18;
    }

    @Override
    protected SimpleSound getBurstSound() {
        return SimpleSound.BREEZE_WIND_CHARGE_BURST;
    }

    @Override
    protected Particle getBurstParticle() {
        return SimpleParticle.BREEZE_WIND_EXPLOSION;
    }
}
