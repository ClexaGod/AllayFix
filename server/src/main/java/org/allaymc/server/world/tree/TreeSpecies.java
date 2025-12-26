package org.allaymc.server.world.tree;

import org.allaymc.api.block.type.BlockType;
import org.allaymc.server.block.data.BlockId;

import java.util.function.Supplier;

public enum TreeSpecies {
    OAK(
            BlockId.OAK_SAPLING,
            () -> new SimpleTreeGenerator(BlockId.OAK_LOG.getBlockType(), BlockId.OAK_LEAVES.getBlockType(), 4, 3),
            null,
            false,
            false
    ),
    BIRCH(
            BlockId.BIRCH_SAPLING,
            () -> new SimpleTreeGenerator(BlockId.BIRCH_LOG.getBlockType(), BlockId.BIRCH_LEAVES.getBlockType(), 5, 2),
            null,
            false,
            false
    ),
    SPRUCE(
            BlockId.SPRUCE_SAPLING,
            () -> new SpruceTreeGenerator(BlockId.SPRUCE_LOG.getBlockType(), BlockId.SPRUCE_LEAVES.getBlockType()),
            () -> new BigSpruceTreeGenerator(BlockId.SPRUCE_LOG.getBlockType(), BlockId.SPRUCE_LEAVES.getBlockType(), 0.25f, 4),
            false,
            false
    ),
    JUNGLE(
            BlockId.JUNGLE_SAPLING,
            () -> new JungleTreeGenerator(BlockId.JUNGLE_LOG.getBlockType(), BlockId.JUNGLE_LEAVES.getBlockType(), 4, 7),
            () -> new BigJungleTreeGenerator(BlockId.JUNGLE_LOG.getBlockType(), BlockId.JUNGLE_LEAVES.getBlockType(), 10, 20),
            false,
            false
    ),
    ACACIA(
            BlockId.ACACIA_SAPLING,
            () -> new AcaciaTreeGenerator(BlockId.ACACIA_LOG.getBlockType(), BlockId.ACACIA_LEAVES.getBlockType()),
            null,
            false,
            false
    ),
    DARK_OAK(
            BlockId.DARK_OAK_SAPLING,
            () -> new DarkOakTreeGenerator(BlockId.DARK_OAK_LOG.getBlockType(), BlockId.DARK_OAK_LEAVES.getBlockType()),
            null,
            true,
            false
    ),
    CHERRY(
            BlockId.CHERRY_SAPLING,
            () -> new CherryTreeGenerator(BlockId.CHERRY_LOG.getBlockType(), BlockId.CHERRY_LEAVES.getBlockType()),
            null,
            false,
            false
    ),
    PALE_OAK(
            BlockId.PALE_OAK_SAPLING,
            () -> new SimpleTreeGenerator(BlockId.PALE_OAK_LOG.getBlockType(), BlockId.PALE_OAK_LEAVES.getBlockType(), 4, 3),
            null,
            false,
            false
    ),
    MANGROVE(
            BlockId.MANGROVE_PROPAGULE,
            () -> new MangroveTreeGenerator(BlockId.MANGROVE_LOG.getBlockType(), BlockId.MANGROVE_LEAVES.getBlockType()),
            null,
            false,
            true
    );

    private final BlockId saplingId;
    private final Supplier<TreeGenerator> smallGeneratorSupplier;
    private final Supplier<TreeGenerator> largeGeneratorSupplier;
    private final boolean requires2x2;
    private final boolean allowWater;

    TreeSpecies(BlockId saplingId, Supplier<TreeGenerator> smallGeneratorSupplier, Supplier<TreeGenerator> largeGeneratorSupplier, boolean requires2x2, boolean allowWater) {
        this.saplingId = saplingId;
        this.smallGeneratorSupplier = smallGeneratorSupplier;
        this.largeGeneratorSupplier = largeGeneratorSupplier;
        this.requires2x2 = requires2x2;
        this.allowWater = allowWater;
    }

    public static TreeSpecies fromSapling(BlockType<?> type) {
        for (var species : values()) {
            if (species.saplingId.getIdentifier().equals(type.getIdentifier())) {
                return species;
            }
        }
        return null;
    }

    public boolean requires2x2() {
        return requires2x2;
    }

    public boolean hasLargeGenerator() {
        return largeGeneratorSupplier != null;
    }

    public TreeGenerator selectGenerator(boolean large) {
        if (large && largeGeneratorSupplier != null) {
            return largeGeneratorSupplier.get();
        }
        return smallGeneratorSupplier.get();
    }

    public boolean allowWater() {
        return allowWater;
    }
}
