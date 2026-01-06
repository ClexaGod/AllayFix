package org.allaymc.server.entity.impl;

import org.allaymc.api.component.Component;
import lombok.experimental.Delegate;
import org.allaymc.api.entity.EntityInitInfo;
import org.allaymc.api.entity.component.EntityLivingComponent;
import org.allaymc.api.entity.interfaces.EntityZombie;
import org.allaymc.server.component.ComponentProvider;

import java.util.List;

public class EntityZombieImpl extends EntityImpl implements EntityZombie {
    @Delegate
    private EntityLivingComponent livingComponent;

    public EntityZombieImpl(EntityInitInfo initInfo,
                            List<ComponentProvider<? extends Component>> componentProviders) {
        super(initInfo, componentProviders);
    }
}
