package org.allaymc.server.entity.component.projectile;

import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.SoundNames;
import org.allaymc.api.world.particle.CustomParticle;

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
    protected CustomSound getBurstSound() {
        return new CustomSound(SoundNames.BREEZE_WIND_CHARGE_BURST);
    }

    @Override
    protected CustomParticle getBurstParticle() {
        return new CustomParticle("minecraft:breeze_wind_explosion");
    }
}
