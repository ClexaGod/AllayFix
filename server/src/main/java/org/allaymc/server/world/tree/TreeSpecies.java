package org.allaymc.server.world.tree;

import org.allaymc.api.block.type.BlockType;
import org.allaymc.api.block.type.BlockTypes;

public enum TreeSpecies {
    OAK(
            BlockTypes.OAK_SAPLING,
            new SimpleTreeGenerator(BlockTypes.OAK_LOG, BlockTypes.OAK_LEAVES, 4, 3),
            null,
            false,
            false
    ),
    BIRCH(
            BlockTypes.BIRCH_SAPLING,
            new SimpleTreeGenerator(BlockTypes.BIRCH_LOG, BlockTypes.BIRCH_LEAVES, 5, 2),
            null,
            false,
            false
    ),
    SPRUCE(
            BlockTypes.SPRUCE_SAPLING,
            new SpruceTreeGenerator(BlockTypes.SPRUCE_LOG, BlockTypes.SPRUCE_LEAVES),
            new BigSpruceTreeGenerator(BlockTypes.SPRUCE_LOG, BlockTypes.SPRUCE_LEAVES, 0.25f, 4),
            false,
            false
    ),
    JUNGLE(
            BlockTypes.JUNGLE_SAPLING,
            new JungleTreeGenerator(BlockTypes.JUNGLE_LOG, BlockTypes.JUNGLE_LEAVES, 4, 7),
            new BigJungleTreeGenerator(BlockTypes.JUNGLE_LOG, BlockTypes.JUNGLE_LEAVES, 10, 20),
            false,
            false
    ),
    ACACIA(
            BlockTypes.ACACIA_SAPLING,
            new AcaciaTreeGenerator(BlockTypes.ACACIA_LOG, BlockTypes.ACACIA_LEAVES),
            null,
            false,
            false
    ),
    DARK_OAK(
            BlockTypes.DARK_OAK_SAPLING,
            new DarkOakTreeGenerator(BlockTypes.DARK_OAK_LOG, BlockTypes.DARK_OAK_LEAVES),
            null,
            true,
            false
    ),
    CHERRY(
            BlockTypes.CHERRY_SAPLING,
            new CherryTreeGenerator(BlockTypes.CHERRY_LOG, BlockTypes.CHERRY_LEAVES),
            null,
            false,
            false
    ),
    PALE_OAK(
            BlockTypes.PALE_OAK_SAPLING,
            new SimpleTreeGenerator(BlockTypes.PALE_OAK_LOG, BlockTypes.PALE_OAK_LEAVES, 4, 3),
            null,
            false,
            false
    ),
    MANGROVE(
            BlockTypes.MANGROVE_PROPAGULE,
            new MangroveTreeGenerator(BlockTypes.MANGROVE_LOG, BlockTypes.MANGROVE_LEAVES),
            null,
            false,
            true
    );

    private final BlockType<?> saplingType;
    private final TreeGenerator smallGenerator;
    private final TreeGenerator largeGenerator;
    private final boolean requires2x2;
    private final boolean allowWater;

    TreeSpecies(BlockType<?> saplingType, TreeGenerator smallGenerator, TreeGenerator largeGenerator, boolean requires2x2, boolean allowWater) {
        this.saplingType = saplingType;
        this.smallGenerator = smallGenerator;
        this.largeGenerator = largeGenerator;
        this.requires2x2 = requires2x2;
        this.allowWater = allowWater;
    }

    public static TreeSpecies fromSapling(BlockType<?> type) {
        for (var species : values()) {
            if (species.saplingType == type) {
                return species;
            }
        }
        return null;
    }

    public BlockType<?> getSaplingType() {
        return saplingType;
    }

    public boolean requires2x2() {
        return requires2x2;
    }

    public boolean hasLargeGenerator() {
        return largeGenerator != null;
    }

    public TreeGenerator selectGenerator(boolean large) {
        if (large && largeGenerator != null) {
            return largeGenerator;
        }
        return smallGenerator;
    }

    public boolean allowWater() {
        return allowWater;
    }
}
