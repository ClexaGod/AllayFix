package org.allaymc.server.world.tree;

import org.joml.Vector3i;

import java.util.Random;

/**
 * Generates a tree into a {@link TreePlacer} using a base position.
 */
@FunctionalInterface
public interface TreeGenerator {
    boolean generate(TreePlacer placer, Random random, Vector3i basePos);
}
