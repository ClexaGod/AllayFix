package org.allaymc.server.container.processor;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.blockentity.interfaces.BlockEntityBeacon;
import org.allaymc.api.container.Container;
import org.allaymc.api.container.ContainerTypes;
import org.allaymc.api.container.interfaces.BeaconContainer;
import org.allaymc.api.entity.effect.EffectType;
import org.allaymc.api.entity.effect.EffectTypes;
import org.allaymc.api.item.data.ItemTags;
import org.allaymc.api.player.Player;
import org.allaymc.api.registry.Registries;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.BeaconPaymentAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DestroyAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType;

import java.util.Map;

/**
 * @author daoge_cmd
 */
@Slf4j
public class BeaconPaymentActionProcessor implements ContainerActionProcessor<BeaconPaymentAction> {
    @Override
    public ActionResponse handle(BeaconPaymentAction action, Player player, int currentActionIndex, ItemStackRequestAction[] actions, Map<String, Object> dataPool) {
        var container = player.getControlledEntity().getContainer(ContainerTypes.BEACON);
        var itemType = container.getBeaconPayment().getItemType();
        if (!itemType.hasItemTag(ItemTags.BEACON_PAYMENT)) {
            log.warn("Invalid item type for beacon payment: {}", itemType.getIdentifier());
            return error();
        }

        if (actions.length == currentActionIndex + 1 ||
            !(actions[currentActionIndex + 1] instanceof DestroyAction destroyAction)) {
            log.warn("Destroy action not found after beacon payment action");
            return error();
        }

        if (!validateDestoryAction(container, destroyAction)) {
            log.warn("Invalid destroy action");
            return error();
        }

        var blockPos = container.getBlockPos();
        if (!(blockPos.dimension().getBlockEntity(blockPos) instanceof BlockEntityBeacon blockEntityBeacon)) {
            log.warn("Beacon block entity not found at {}", blockPos);
            return error();
        }

        var primaryEffect = resolveBeaconEffect(action.getPrimaryEffect(), true);
        if (primaryEffect != null) {
            blockEntityBeacon.setPrimaryEffect(primaryEffect);
        }
        var secondaryEffect = resolveBeaconEffect(action.getSecondaryEffect(), false);
        if (secondaryEffect != null) {
            blockEntityBeacon.setSecondaryEffect(secondaryEffect);
        }
        return null;
    }

    private static EffectType resolveBeaconEffect(int effectId, boolean primary) {
        if (effectId == 0) {
            // Some protocol builds use 0-based ids for beacon effects.
            return primary ? EffectTypes.SPEED : null;
        }

        var effect = Registries.EFFECTS.getByK1(effectId);
        if (isBeaconEffect(effect)) {
            return effect;
        }

        effect = Registries.EFFECTS.getByK1(effectId + 1);
        return isBeaconEffect(effect) ? effect : null;
    }

    private static boolean isBeaconEffect(EffectType effectType) {
        return effectType == EffectTypes.SPEED ||
               effectType == EffectTypes.HASTE ||
               effectType == EffectTypes.RESISTANCE ||
               effectType == EffectTypes.JUMP_BOOST ||
               effectType == EffectTypes.STRENGTH ||
               effectType == EffectTypes.REGENERATION;
    }

    protected boolean validateDestoryAction(Container container, DestroyAction destroyAction) {
        var source = destroyAction.getSource();
        return destroyAction.getCount() == 1 &&
               source.getContainerName().getContainer() == ContainerSlotType.BEACON_PAYMENT &&
               ContainerActionProcessor.fromNetworkSlotIndex(container, source.getSlot()) == BeaconContainer.BEACON_PAYMENT_SLOT;
    }

    @Override
    public ItemStackRequestActionType getType() {
        return ItemStackRequestActionType.BEACON_PAYMENT;
    }
}
