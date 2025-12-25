package org.allaymc.server.blockentity.component;

import lombok.Getter;
import lombok.Setter;
import org.allaymc.api.block.data.BlockFace;
import org.allaymc.api.block.property.type.BlockPropertyTypes;
import org.allaymc.api.blockentity.BlockEntityInitInfo;
import org.allaymc.api.blockentity.component.BlockEntityContainerHolderComponent;
import org.allaymc.api.blockentity.component.BlockEntityHopperBaseComponent;
import org.allaymc.api.blockentity.component.BlockEntityPairableComponent;
import org.allaymc.api.container.Container;
import org.allaymc.api.entity.interfaces.EntityItem;
import org.allaymc.api.item.ItemStack;
import org.allaymc.api.item.interfaces.ItemAirStack;
import org.allaymc.api.math.position.Position3ic;
import org.allaymc.server.container.impl.DoubleChestContainerImpl;
import org.allaymc.server.component.annotation.Dependency;
import org.cloudburstmc.nbt.NbtMap;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * @author ClexaGod
 */
public class BlockEntityHopperBaseComponentImpl extends BlockEntityBaseComponentImpl implements BlockEntityHopperBaseComponent {

    protected static final String TAG_TRANSFER_COOLDOWN = "TransferCooldown";
    protected static final int TRANSFER_COOLDOWN_TICKS = 8;

    @Dependency
    protected BlockEntityContainerHolderComponent containerHolderComponent;

    @Getter
    @Setter
    protected int transferCooldown;

    public BlockEntityHopperBaseComponentImpl(BlockEntityInitInfo initInfo) {
        super(initInfo);
    }

    @Override
    public NbtMap saveNBT() {
        return super.saveNBT()
                .toBuilder()
                .putInt(TAG_TRANSFER_COOLDOWN, transferCooldown)
                .build();
    }

    @Override
    public void loadNBT(NbtMap nbt) {
        super.loadNBT(nbt);
        nbt.listenForInt(TAG_TRANSFER_COOLDOWN, value -> transferCooldown = value);
    }

    @Override
    public void tick(long currentTick) {
        super.tick(currentTick);
        if (transferCooldown > 0) {
            transferCooldown--;
            return;
        }

        if (isDisabled()) {
            return;
        }

        if (tryTransfer()) {
            transferCooldown = TRANSFER_COOLDOWN_TICKS;
        }
    }

    protected boolean tryTransfer() {
        var hopperContainer = containerHolderComponent.getContainer();
        if (tryPushItems(hopperContainer)) {
            return true;
        }
        return tryPullItems(hopperContainer);
    }

    protected boolean tryPushItems(Container hopperContainer) {
        if (hopperContainer.isEmpty()) {
            return false;
        }

        var targetContainer = getTargetContainer(getFacingPos());
        if (targetContainer == null) {
            return false;
        }

        var stacks = hopperContainer.getItemStackArray();
        for (int slot = 0; slot < stacks.length; slot++) {
            if (stacks[slot] == ItemAirStack.AIR_STACK) {
                continue;
            }
            if (tryMoveOneItem(hopperContainer, slot, targetContainer)) {
                return true;
            }
        }

        return false;
    }

    protected boolean tryPullItems(Container hopperContainer) {
        var dimension = position.dimension();
        if (dimension == null) {
            return false;
        }
        var sourcePos = new org.allaymc.api.math.position.Position3i(BlockFace.UP.offsetPos(position), dimension);
        var sourceContainer = getTargetContainer(sourcePos);
        if (sourceContainer != null && tryPullFromContainer(sourceContainer, hopperContainer)) {
            return true;
        }

        return tryPullFromItemEntities(hopperContainer);
    }

    protected boolean tryPullFromContainer(Container source, Container hopperContainer) {
        var stacks = source.getItemStackArray();
        for (int slot = 0; slot < stacks.length; slot++) {
            if (stacks[slot] == ItemAirStack.AIR_STACK) {
                continue;
            }
            if (tryMoveOneItem(source, slot, hopperContainer)) {
                return true;
            }
        }
        return false;
    }

    protected boolean tryPullFromItemEntities(Container hopperContainer) {
        var dimension = position.dimension();
        if (dimension == null) {
            return false;
        }

        var entities = dimension.getEntityManager()
                .getPhysicsService()
                .computeCollidingEntities(getPickupAABB(), entity -> entity instanceof EntityItem);
        for (var entity : entities) {
            if (!(entity instanceof EntityItem itemEntity)) {
                continue;
            }
            if (tryPullFromItemEntity(itemEntity, hopperContainer)) {
                return true;
            }
        }
        return false;
    }

    protected boolean tryPullFromItemEntity(EntityItem itemEntity, Container hopperContainer) {
        var stack = itemEntity.getItemStack();
        if (stack == null || stack == ItemAirStack.AIR_STACK) {
            return false;
        }

        if (!tryInsertOneItem(stack, hopperContainer)) {
            return false;
        }

        if (stack.getCount() <= 0) {
            itemEntity.remove();
        } else {
            itemEntity.setItemStack(stack);
        }
        return true;
    }

    protected boolean tryMoveOneItem(Container source, int sourceSlot, Container target) {
        var stack = source.getItemStack(sourceSlot);
        if (stack == ItemAirStack.AIR_STACK) {
            return false;
        }

        if (!tryInsertOneItem(stack, target)) {
            return false;
        }

        if (stack.getCount() <= 0) {
            source.clearSlot(sourceSlot);
        } else {
            source.notifySlotChange(sourceSlot);
        }
        return true;
    }

    protected boolean tryInsertOneItem(ItemStack sourceStack, Container target) {
        if (sourceStack == ItemAirStack.AIR_STACK) {
            return false;
        }

        var size = target.getItemStackArray().length;
        for (int slot = 0; slot < size; slot++) {
            var targetStack = target.getItemStack(slot);
            if (targetStack == ItemAirStack.AIR_STACK) {
                var moved = sourceStack.copy();
                moved.setCount(1);
                target.setItemStack(slot, moved);
                sourceStack.reduceCount(1);
                return true;
            }

            if (targetStack.canMerge(sourceStack, true) && !targetStack.isFull()) {
                targetStack.increaseCount(1);
                target.notifySlotChange(slot);
                sourceStack.reduceCount(1);
                return true;
            }
        }

        return false;
    }

    protected Container getTargetContainer(Position3ic targetPos) {
        var dimension = position.dimension();
        if (dimension == null) {
            return null;
        }

        var blockEntity = dimension.getBlockEntity(targetPos);
        if (!(blockEntity instanceof BlockEntityContainerHolderComponent holder)) {
            return null;
        }

        if (blockEntity instanceof BlockEntityPairableComponent pairable && pairable.isPaired()) {
            var pair = pairable.getPair();
            if (!(pair instanceof BlockEntityContainerHolderComponent pairHolder)) {
                return holder.getContainer();
            }

            var doubleChest = new DoubleChestContainerImpl();
            var left = holder.getContainer();
            var right = pairHolder.getContainer();
            if (!pairable.isLead()) {
                var temp = left;
                left = right;
                right = temp;
            }
            doubleChest.setLeft(left);
            doubleChest.setRight(right);
            return doubleChest;
        }

        return holder.getContainer();
    }

    protected Position3ic getFacingPos() {
        var dimension = position.dimension();
        if (dimension == null) {
            return position;
        }

        var blockState = dimension.getBlockState(position);
        var facingIndex = blockState.getPropertyValue(BlockPropertyTypes.FACING_DIRECTION);
        var facing = BlockFace.fromIndex(facingIndex);
        if (facing == null) {
            facing = BlockFace.DOWN;
        }
        return new org.allaymc.api.math.position.Position3i(facing.offsetPos(position), dimension);
    }

    protected boolean isDisabled() {
        var dimension = position.dimension();
        if (dimension == null) {
            return true;
        }

        var blockState = dimension.getBlockState(position);
        return blockState.getPropertyValue(BlockPropertyTypes.TOGGLE_BIT);
    }

    protected AABBdc getPickupAABB() {
        var x = position.x();
        var y = position.y();
        var z = position.z();
        return new AABBd(x, y + 1, z, x + 1, y + 2, z + 1);
    }
}
