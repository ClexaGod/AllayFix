package org.allaymc.server.entity.impl;

import lombok.experimental.Delegate;
import org.allaymc.api.component.Component;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.component.EntityPhysicsComponent;
import org.allaymc.api.entity.interfaces.EntityZombie;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityZombieImpl extends EntityImpl implements EntityZombie, EntityPhysicsComponent {
    @Delegate
    private EntityLivingComponent livingComponent;
    @Delegate
    private EntityPhysicsComponent physicsComponent;

    public EntityZombieImpl(EntityInitInfo initInfo,
                            List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
