package org.allaymc.server.world.storage.xodus;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.blockentity.BlockEntity;
import org.allaymc.api.entity.Entity;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.AllayNBTUtils;
import org.allaymc.api.utils.NBTIO;
import org.allaymc.api.utils.hash.HashUtils;
import org.allaymc.api.world.World;
import org.allaymc.api.world.WorldData;
import org.allaymc.api.world.biome.BiomeType;
import org.allaymc.api.world.biome.BiomeTypes;
import org.allaymc.api.world.chunk.Chunk;
import org.allaymc.api.world.chunk.ChunkState;
import org.allaymc.api.world.chunk.OperationType;
import org.allaymc.api.world.data.Difficulty;
import org.allaymc.api.world.data.DimensionInfo;
import org.allaymc.api.world.storage.WorldStorage;
import org.allaymc.api.world.storage.WorldStorageException;
import org.allaymc.server.AllayServer;
import org.allaymc.server.datastruct.palette.Palette;
import org.allaymc.server.datastruct.palette.PaletteException;
import org.allaymc.server.datastruct.palette.PaletteUtils;
import org.allaymc.server.network.NetworkHelper;
import org.allaymc.server.network.ProtocolInfo;
import org.allaymc.server.pdc.AllayPersistentDataContainer;
import org.allaymc.server.world.AllayWorldData;
import org.allaymc.server.world.chunk.AllayChunkBuilder;
import org.allaymc.server.world.chunk.AllayChunkSection;
import org.allaymc.server.world.chunk.AllayUnsafeChunk;
import org.allaymc.server.world.chunk.HeightMap;
import org.allaymc.server.world.chunk.ScheduledUpdateInfo;
import org.allaymc.server.world.gamerule.AllayGameRules;
import org.allaymc.server.world.storage.leveldb.ChunkVersion;
import org.allaymc.server.world.storage.leveldb.LevelDBKey;
import org.allaymc.server.world.storage.leveldb.StorageVersion;
import org.allaymc.server.world.storage.leveldb.VanillaChunkState;
import org.allaymc.updater.block.BlockStateUpdaters;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.nbt.util.stream.LittleEndianDataInputStream;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.jctools.maps.NonBlockingHashMap;
import org.joml.Vector3i;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.allaymc.server.network.NetworkHelper.toNetwork;

/**
 * WorldStorage implementation using JetBrains Xodus.
 * Uses safe transaction closures (executeInTransaction / computeInReadonlyTransaction) to prevent leaks.
 */
@Slf4j
public class AllayXodusWorldStorage implements WorldStorage {

    private static final String FILE_LEVEL_DAT = "level.dat";
    private static final String FILE_LEVEL_DAT_OLD = "level.dat_old";
    private static final String DIR_DB = "xodus_db";
    private static final String STORE_CHUNKS = "chunks";

    private static final int CURRENT_STORAGE_VERSION = StorageVersion.LEVEL_DATA_STRICT_SIZE.ordinal();
    private static final int CURRENT_CHUNK_VERSION = ChunkVersion.V1_21_40.ordinal();

    // World Data Keys
    private static final String TAG_DIFFICULTY = "Difficulty";
    private static final String TAG_GAME_TYPE = "GameType";
    private static final String TAG_DISPLAY_NAME = "LevelName";
    private static final String TAG_SPAWN_X = "SpawnX";
    private static final String TAG_SPAWN_Y = "SpawnY";
    private static final String TAG_SPAWN_Z = "SpawnZ";
    private static final String TAG_TOTAL_TIME = "Time";
    private static final String TAG_TIME_OF_DAY = "TimeOfDay";
    private static final String TAG_WORLD_START_COUNT = "WorldStartCount";
    private static final String TAG_PDC = "PDC";
    
    // Generator Keys
    private static final String TAG_GENERATOR = "Generator";
    private static final String TAG_RANDOM_SEED = "RandomSeed";
    private static final String TAG_STORAGE_VERSION = "StorageVersion";
    private static final String TAG_NETWORK_VERSION = "NetworkVersion";
    private static final String TAG_LAST_PLAYED = "LastPlayed";
    private static final String TAG_ACHIEVEMENTS_DISABLED = "hasBeenLoadedInCreative";
    private static final String TAG_COMMANDS_ENABLED = "commandsEnabled";
    private static final String TAG_LAST_OPENED_WITH_VERSION = "lastOpenedWithVersion";
    private static final String TAG_IS_EDU = "eduLevel";
    private static final String TAG_FORCE_GAME_TYPE = "ForceGameType";

    // Scheduled Updates
    private static final String TAG_CURRENT_TICK = "currentTick";
    private static final String TAG_TICK_LIST = "tickList";

    private static final int HEIGHTMAP_SIZE = 256;

    private final Path path;
    private final String worldName;
    private final Environment env;
    private World world;

    public AllayXodusWorldStorage(Path path) {
        this.path = path;
        this.worldName = path.getName(path.getNameCount() - 1).toString();

        var file = path.toFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new WorldStorageException("Failed to create world directory!");
        }

        var dbFolder = path.resolve(DIR_DB).toFile();
        if (!dbFolder.exists() && !dbFolder.mkdirs()) {
            throw new WorldStorageException("Failed to create Xodus DB directory!");
        }

        this.env = Environments.newInstance(dbFolder.getAbsolutePath());
    }

    private Store getStore(Transaction txn) {
        return env.openStore(STORE_CHUNKS, StoreConfig.WITHOUT_DUPLICATES, txn);
    }

    // --- Helper Methods to adapt LevelDB keys to Xodus ByteIterable ---

    // --- Serialization Methods (Pass Transaction and Store) ---

    private static void serializeSections(Transaction txn, Store store, AllayUnsafeChunk chunk) {
        for (int ySection = chunk.getDimensionInfo().minSectionY(); ySection <= chunk.getDimensionInfo().maxSectionY(); ySection++) {
            var section = chunk.getSection(ySection);
            if (!section.hasDirtyBlockLayer()) {
                continue;
            }

            int finalYSection = ySection;
            byte[] key = LevelDBKey.CHUNK_SECTION_PREFIX.createKey(chunk.getX(), chunk.getZ(), ySection, chunk.getDimensionInfo());
            byte[] value = withByteBufToArray(buffer -> {
                buffer.writeByte(AllayChunkSection.CURRENT_CHUNK_SECTION_VERSION);
                buffer.writeByte(AllayChunkSection.LAYER_COUNT);
                buffer.writeByte(finalYSection);
                for (int i = 0; i < AllayChunkSection.LAYER_COUNT; i++) {
                    var palette = section.blockLayers()[i];
                    palette.compact();
                    palette.writeToStorage(buffer, BlockState::getBlockStateNBT);
                    palette.setDirty(false);
                }
            });
            store.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value));
        }
    }

    private static void deserializeSections(Store store, Transaction txn, AllayChunkBuilder builder) {
        var dimensionInfo = builder.getDimensionInfo();
        var sections = new AllayChunkSection[dimensionInfo.chunkSectionCount()];
        var minSectionY = dimensionInfo.minSectionY();

        for (int ySection = minSectionY; ySection <= dimensionInfo.maxSectionY(); ySection++) {
            byte[] key = LevelDBKey.CHUNK_SECTION_PREFIX.createKey(builder.getChunkX(), builder.getChunkZ(), ySection, dimensionInfo);
            ByteIterable entry = store.get(txn, new ArrayByteIterable(key));
            
            if (entry == null) {
                continue;
            }

            var byteBuf = Unpooled.wrappedBuffer(entry.getBytesUnsafe());
            var subChunkVersion = byteBuf.readByte();
            var layers = AllayChunkSection.LAYER_COUNT;

            switch (subChunkVersion) {
                case 9, 8:
                    layers = byteBuf.readByte();
                    if (subChunkVersion == 9) byteBuf.readByte();
                case 1:
                    AllayChunkSection section;
                    if (layers <= AllayChunkSection.LAYER_COUNT) {
                        section = new AllayChunkSection((byte) ySection);
                    } else {
                        log.warn("Loading chunk section ({}, {}, {}) with {} layers, corruption suspected!", builder.getChunkX(), ySection, builder.getChunkZ(), layers);
                        @SuppressWarnings("rawtypes") Palette[] palettes = new Palette[layers];
                        Arrays.fill(palettes, new Palette<>(BlockTypes.AIR.getDefaultState()));
                        section = new AllayChunkSection((byte) ySection, palettes);
                    }

                    for (int layer = 0; layer < layers; layer++) {
                        var palette = section.blockLayers()[layer];
                        palette.readFromStorage(byteBuf, AllayXodusWorldStorage::fastBlockStateDeserializer);
                        palette.setDirty(false);
                    }
                    sections[ySection - minSectionY] = section;
                    break;
                default:
                    log.warn("Unknown subchunk version {} at ({}, {}, {})", subChunkVersion, builder.getChunkX(), ySection, builder.getChunkZ());
            }
        }
        builder.sections(fillNullSections(sections, dimensionInfo));
    }

    private static BlockState fastBlockStateDeserializer(ByteBuf buffer) {
        int blockStateHash;
        try (var bufInputStream = new ByteBufInputStream(buffer);
             var input = new LittleEndianDataInputStream(bufInputStream);
             var nbtInputStream = new NBTInputStream(input)) {
            blockStateHash = PaletteUtils.fastReadBlockStateHash(input, buffer);
            if (blockStateHash == PaletteUtils.HASH_NOT_LATEST) {
                var oldNbtMap = (NbtMap) nbtInputStream.readTag();
                var newNbtMap = BlockStateUpdaters.updateBlockState(oldNbtMap, ProtocolInfo.BLOCK_STATE_UPDATER.getVersion());
                var states = new TreeMap<>(newNbtMap.getCompound("states"));
                var tag = NbtMap.builder()
                        .putString("name", newNbtMap.getString("name"))
                        .putCompound("states", NbtMap.fromMap(states))
                        .build();
                blockStateHash = HashUtils.fnv1a_32_nbt(tag);
            }
        } catch (IOException e) {
            throw new PaletteException(e);
        }

        BlockState blockState = Registries.BLOCK_STATE_PALETTE.get(blockStateHash);
        if (blockState != null) return blockState;

        log.error("Unknown block state hash {} while loading chunk section", blockStateHash);
        return BlockTypes.UNKNOWN.getDefaultState();
    }

    private static AllayChunkSection[] fillNullSections(AllayChunkSection[] sections, DimensionInfo dimensionInfo) {
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == null) {
                sections[i] = new AllayChunkSection((byte) (i + dimensionInfo.minSectionY()));
            }
        }
        return sections;
    }

    private static void serializeHeightAndBiome(Transaction txn, Store store, AllayUnsafeChunk chunk) {
        byte[] key = LevelDBKey.DATA_3D.createKey(chunk.getX(), chunk.getZ(), chunk.getDimensionInfo());
        byte[] value = withByteBufToArray(heightAndBiomesBuffer -> {
            for (var height : chunk.calculateAndGetHeightMap().getHeights()) {
                heightAndBiomesBuffer.writeShortLE(height - chunk.getDimensionInfo().minHeight());
            }
            Palette<BiomeType> lastPalette = null;
            for (int y = chunk.getDimensionInfo().minSectionY(); y <= chunk.getDimensionInfo().maxSectionY(); y++) {
                AllayChunkSection section = chunk.getSection(y);
                section.biomes().compact();
                section.biomes().writeToStorage(heightAndBiomesBuffer, BiomeType::getId, lastPalette);
            }
        });
        store.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value));
    }

    private static void deserializeHeightAndBiome(Store store, Transaction txn, AllayChunkBuilder builder) {
        byte[] key = LevelDBKey.DATA_3D.createKey(builder.getChunkX(), builder.getChunkZ(), builder.getDimensionInfo());
        ByteIterable entry = store.get(txn, new ArrayByteIterable(key));
        
        if (entry == null) {
            deserializeHeightAndBiomeOld(store, txn, builder);
            return;
        }

        ByteBuf heightAndBiomesBuffer = Unpooled.wrappedBuffer(entry.getBytesUnsafe());

        short[] heights = new short[HEIGHTMAP_SIZE];
        for (int i = 0; i < HEIGHTMAP_SIZE; i++) {
            heights[i] = (short) (heightAndBiomesBuffer.readUnsignedShortLE() + builder.getDimensionInfo().minHeight());
        }
        builder.heightMap(new HeightMap(heights));

        Palette<BiomeType> lastPalette = null;
        var minSectionY = builder.getDimensionInfo().minSectionY();
        for (int y = minSectionY; y <= builder.getDimensionInfo().maxSectionY(); y++) {
            AllayChunkSection section = builder.getSections()[y - minSectionY];
            if (section == null) continue;
            section.biomes().readFromStorage(heightAndBiomesBuffer, AllayXodusWorldStorage::getBiomeByIdNonNull, lastPalette);
            lastPalette = section.biomes();
        }
    }

    private static void deserializeHeightAndBiomeOld(Store store, Transaction txn, AllayChunkBuilder builder) {
        byte[] key = LevelDBKey.DATA_2D.createKey(builder.getChunkX(), builder.getChunkZ(), builder.getDimensionInfo());
        ByteIterable entry = store.get(txn, new ArrayByteIterable(key));
        if (entry == null) return;

        ByteBuf heightAndBiomesBuffer = Unpooled.wrappedBuffer(entry.getBytesUnsafe());
        short[] heights = new short[HEIGHTMAP_SIZE];
        for (int i = 0; i < HEIGHTMAP_SIZE; i++) {
            heights[i] = heightAndBiomesBuffer.readShortLE();
        }
        builder.heightMap(new HeightMap(heights));

        byte[] biomes = new byte[HEIGHTMAP_SIZE];
        heightAndBiomesBuffer.readBytes(biomes);

        var minSectionY = builder.getDimensionInfo().minSectionY();
        for (int y = minSectionY; y <= builder.getDimensionInfo().maxSectionY(); y++) {
            var section = builder.getSections()[y - minSectionY];
            if (section == null) continue;
            var biomePalette = section.biomes();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int sy = 0; sy < 16; sy++) {
                        biomePalette.set(HashUtils.hashChunkSectionXYZ(x, sy, z), getBiomeByIdNonNull(biomes[x + 16 * z]));
                    }
                }
            }
        }
    }

    private static void serializeBlockEntities(Transaction txn, Store store, AllayUnsafeChunk chunk) {
        var blockEntities = chunk.getBlockEntities().values();
        byte[] key = LevelDBKey.BLOCK_ENTITIES.createKey(chunk.getX(), chunk.getZ(), chunk.getDimensionInfo());
        
        if (blockEntities.isEmpty()) {
            store.delete(txn, new ArrayByteIterable(key));
            return;
        }

        byte[] value = withByteBufToArray(blockEntitiesBuffer -> {
            try (var writerLE = NbtUtils.createWriterLE(new ByteBufOutputStream(blockEntitiesBuffer))) {
                for (BlockEntity blockEntity : blockEntities) {
                    writerLE.writeTag(blockEntity.saveNBT());
                }
            } catch (IOException e) {
                throw new WorldStorageException(e);
            }
        });
        store.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value));
    }

    private static void serializeScheduledUpdates(Transaction txn, Store store, AllayUnsafeChunk chunk, World world) {
        var scheduledUpdates = chunk.getScheduledUpdates().values();
        byte[] key = LevelDBKey.PENDING_TICKS.createKey(chunk.getX(), chunk.getZ(), chunk.getDimensionInfo());
        
        if (scheduledUpdates.isEmpty()) {
            store.delete(txn, new ArrayByteIterable(key));
            return;
        }

        byte[] value = withByteBufToArray(scheduledUpdatesBuffer -> {
            try (var writerLE = NbtUtils.createWriterLE(new ByteBufOutputStream(scheduledUpdatesBuffer))) {
                var nbt = NbtMap.builder()
                        .putInt(TAG_CURRENT_TICK, (int) world.getTick())
                        .putList(TAG_TICK_LIST, NbtType.COMPOUND, scheduledUpdates.stream().map(ScheduledUpdateInfo::toNBT).toList())
                        .build();
                writerLE.writeTag(nbt);
            } catch (IOException e) {
                throw new WorldStorageException(e);
            }
        });
        store.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value));
    }

    private static void deserializeScheduledUpdates(Store store, Transaction txn, AllayChunkBuilder builder) {
        DimensionInfo dimensionInfo = builder.getDimensionInfo();
        byte[] key = LevelDBKey.PENDING_TICKS.createKey(builder.getChunkX(), builder.getChunkZ(), dimensionInfo);
        ByteIterable entry = store.get(txn, new ArrayByteIterable(key));
        if (entry == null) return;

        var nbt = AllayNBTUtils.bytesToNbtLE(entry.getBytesUnsafe());
        var tickList = nbt.getList(TAG_TICK_LIST, NbtType.COMPOUND);
        var scheduledUpdates = new NonBlockingHashMap<Integer, ScheduledUpdateInfo>(tickList.size());
        for (var e : tickList) {
            var info = ScheduledUpdateInfo.fromNBT(e);
            var pos = info.getPos();
            scheduledUpdates.put(HashUtils.hashChunkXYZ(pos.x() & 15, pos.y(), pos.z() & 15), info);
        }

        builder.scheduledUpdates(scheduledUpdates);
    }

    private static BiomeType getBiomeByIdNonNull(int id) {
        try {
            return Registries.BIOMES.getByK1(id);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warn("Unknown biome id: {}", id);
            return BiomeTypes.PLAINS;
        }
    }

    private static byte[] int2ByteArrayLE(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static byte[] withByteBufToArray(Consumer<ByteBuf> writer) {
        var buf = ByteBufAllocator.DEFAULT.buffer();
        try {
            writer.accept(buf);
            return ByteBufUtil.getBytes(buf);
        } finally {
            buf.release();
        }
    }

    // --- WorldStorage Implementation ---

    @Override
    public void setWorld(World world) {
        Preconditions.checkState(this.world == null, "World has already been set");
        this.world = world;
    }

    @Override
    public CompletableFuture<Chunk> readChunk(int chunkX, int chunkZ, DimensionInfo dimensionInfo) {
        return CompletableFuture
                .supplyAsync(() -> readChunkSync(chunkX, chunkZ, dimensionInfo), Server.getInstance().getVirtualThreadPool())
                .exceptionally(t -> {
                    log.error("Failed to read chunk ({}, {})", chunkX, chunkZ, t);
                    return AllayUnsafeChunk.builder().newChunk(chunkX, chunkZ, dimensionInfo).toSafeChunk();
                });
    }

    @Override
    public Chunk readChunkSync(int chunkX, int chunkZ, DimensionInfo dimensionInfo) {
        var builder = AllayUnsafeChunk.builder()
                .chunkX(chunkX)
                .chunkZ(chunkZ)
                .dimensionInfo(dimensionInfo)
                .state(ChunkState.NEW);

        // SAFE READ: computeInReadonlyTransaction automatically aborts txn on completion/exception
        env.computeInReadonlyTransaction(txn -> {
            if (!env.storeExists(STORE_CHUNKS, txn)) {
                return null;
            }
            Store store = getStore(txn);

            if (!containChunk(chunkX, chunkZ, dimensionInfo)) { // Optimized later if needed
                 // Proceed to try reading.
            }

            // Version
            ByteIterable versionEntry = store.get(txn, new ArrayByteIterable(LevelDBKey.VERSION.createKey(chunkX, chunkZ, dimensionInfo)));
            if (versionEntry == null) {
                versionEntry = store.get(txn, new ArrayByteIterable(LevelDBKey.LEGACY_VERSION.createKey(chunkX, chunkZ, dimensionInfo)));
            }
            
            if (versionEntry == null) {
                // Chunk might not exist
                return null;
            }

            // Finalized State
            ByteIterable finalizedEntry = store.get(txn, new ArrayByteIterable(LevelDBKey.CHUNK_FINALIZED_STATE.createKey(chunkX, chunkZ, dimensionInfo)));
            if (finalizedEntry != null && finalizedEntry.getBytesUnsafe()[0] != VanillaChunkState.DONE.ordinal()) {
                return null; 
            }

            // Chunk State
            ByteIterable stateEntry = store.get(txn, new ArrayByteIterable(LevelDBKey.ALLAY_CHUNK_STATE.createKey(chunkX, chunkZ, dimensionInfo)));
            if (stateEntry != null) {
                try {
                    builder.state(ChunkState.valueOf(new String(stateEntry.getBytesUnsafe())));
                } catch (IllegalArgumentException e) {
                    builder.state(ChunkState.FULL);
                }
            } else {
                builder.state(ChunkState.FULL);
            }

            deserializeSections(store, txn, builder);
            deserializeHeightAndBiome(store, txn, builder);
            
            // Block Entities
            ByteIterable beEntry = store.get(txn, new ArrayByteIterable(LevelDBKey.BLOCK_ENTITIES.createKey(builder.getChunkX(), builder.getChunkZ(), builder.getDimensionInfo())));
            if (beEntry != null) {
                var blockEntities = new NonBlockingHashMap<Integer, BlockEntity>();
                for (var nbt : AllayNBTUtils.bytesToNbtListLE(beEntry.getBytesUnsafe())) {
                     BlockEntity blockEntity;
                     try {
                         blockEntity = NBTIO.getAPI().fromBlockEntityNBT(world.getDimension(builder.getDimensionInfo().dimensionId()), nbt);
                     } catch (Throwable t) {
                         continue;
                     }
                     if (blockEntity == null) continue;
                     var position = blockEntity.getPosition();
                     var key = HashUtils.hashChunkXYZ(position.x() & 15, position.y(), position.z() & 15);
                     blockEntities.put(key, blockEntity);
                }
                builder.blockEntities(blockEntities);
            }

            deserializeScheduledUpdates(store, txn, builder);
            return null;
        });

        return builder.build().toSafeChunk();
    }

    @Override
    public CompletableFuture<Void> writeChunk(Chunk chunk) {
        return CompletableFuture
                .runAsync(() -> writeChunkSync(chunk), Server.getInstance().getVirtualThreadPool())
                .exceptionally(t -> {
                    log.error("Failed to write chunk ({}, {})", chunk.getX(), chunk.getZ(), t);
                    return null;
                });
    }

    @Override
    public void writeChunkSync(Chunk chunk) {
        // SAFE WRITE: executeInTransaction automatically commits or aborts on exception
        env.executeInTransaction(txn -> {
            Store store = getStore(txn);
            
            store.put(txn, new ArrayByteIterable(LevelDBKey.VERSION.createKey(chunk.getX(), chunk.getZ(), chunk.getDimensionInfo())), new ArrayByteIterable(new byte[]{(byte) CURRENT_CHUNK_VERSION}));
            
            store.put(txn, 
                new ArrayByteIterable(LevelDBKey.CHUNK_FINALIZED_STATE.createKey(chunk.getX(), chunk.getZ(), chunk.getDimensionInfo())), 
                new ArrayByteIterable(withByteBufToArray(buf -> buf.writeByte(VanillaChunkState.DONE.ordinal())))
            );
            
            store.put(txn,
                new ArrayByteIterable(LevelDBKey.ALLAY_CHUNK_STATE.createKey(chunk.getX(), chunk.getZ(), chunk.getDimensionInfo())),
                new ArrayByteIterable(chunk.getState().name().getBytes())
            );

            chunk.applyOperation(c -> {
                var allayUnsafeChunk = (AllayUnsafeChunk) c;
                serializeSections(txn, store, allayUnsafeChunk);
                serializeHeightAndBiome(txn, store, allayUnsafeChunk);
                serializeBlockEntities(txn, store, allayUnsafeChunk);
                serializeScheduledUpdates(txn, store, allayUnsafeChunk, world);
            }, OperationType.READ, OperationType.READ);
        });
    }

    @Override
    public CompletableFuture<Map<Long, Entity>> readEntities(int chunkX, int chunkZ, DimensionInfo dimensionInfo) {
        return CompletableFuture.supplyAsync(() -> readEntitiesSync(chunkX, chunkZ, dimensionInfo), Server.getInstance().getVirtualThreadPool());
    }

    @Override
    public Map<Long, Entity> readEntitiesSync(int chunkX, int chunkZ, DimensionInfo dimensionInfo) {
        return env.computeInReadonlyTransaction(txn -> {
            if (!env.storeExists(STORE_CHUNKS, txn)) {
                return Collections.emptyMap();
            }
            Store store = getStore(txn);
            byte[] idsKey = LevelDBKey.createEntityIdsKey(chunkX, chunkZ, dimensionInfo);
            ByteIterable idsEntry = store.get(txn, new ArrayByteIterable(idsKey));
            
            if (idsEntry == null) {
                // Try old format
                byte[] oldKey = LevelDBKey.ENTITIES.createKey(chunkX, chunkZ, dimensionInfo);
                ByteIterable oldEntry = store.get(txn, new ArrayByteIterable(oldKey));
                if (oldEntry == null) return Collections.emptyMap();
                
                var map = new Long2ObjectOpenHashMap<Entity>();
                for (var nbt : AllayNBTUtils.bytesToNbtListLE(oldEntry.getBytesUnsafe())) {
                    var entity = NBTIO.getAPI().fromEntityNBT(world.getDimension(dimensionInfo.dimensionId()), nbt);
                    if (entity != null) map.put(entity.getUniqueId().getLeastSignificantBits(), entity);
                }
                return map;
            }

            var map = new Long2ObjectOpenHashMap<Entity>();
            var idsBuf = Unpooled.wrappedBuffer(idsEntry.getBytesUnsafe());
            while (idsBuf.isReadable(Long.BYTES)) {
                long id = idsBuf.readLongLE();
                ByteIterable nbtEntry = store.get(txn, new ArrayByteIterable(LevelDBKey.indexEntity(id)));
                if (nbtEntry != null) {
                    var entity = NBTIO.getAPI().fromEntityNBT(world.getDimension(dimensionInfo.dimensionId()), AllayNBTUtils.bytesToNbtLE(nbtEntry.getBytesUnsafe()));
                    if (entity != null) map.put(entity.getUniqueId().getLeastSignificantBits(), entity);
                }
            }
            return map;
        });
    }

    @Override
    public CompletableFuture<Void> writeEntities(int chunkX, int chunkZ, DimensionInfo dimensionInfo, Map<Long, Entity> entities) {
        return CompletableFuture.runAsync(() -> writeEntitiesSync(chunkX, chunkZ, dimensionInfo, entities), Server.getInstance().getVirtualThreadPool());
    }

    @Override
    public void writeEntitiesSync(int chunkX, int chunkZ, DimensionInfo dimensionInfo, Map<Long, Entity> entities) {
        env.executeInTransaction(txn -> {
            Store store = getStore(txn);
            byte[] idsKey = LevelDBKey.createEntityIdsKey(chunkX, chunkZ, dimensionInfo);

            // Delete old
            ByteIterable oldIdsEntry = store.get(txn, new ArrayByteIterable(idsKey));
            if (oldIdsEntry != null) {
                var oldIdsBuf = Unpooled.wrappedBuffer(oldIdsEntry.getBytesUnsafe());
                while (oldIdsBuf.isReadable(Long.BYTES)) {
                    store.delete(txn, new ArrayByteIterable(LevelDBKey.indexEntity(oldIdsBuf.readLongLE())));
                }
            }

            // Write new
            var idsBuf = ByteBufAllocator.DEFAULT.buffer();
            try {
                for (var entry : entities.entrySet()) {
                    var entity = entry.getValue();
                    if (!entity.willBeSaved()) continue;
                    
                    idsBuf.writeLongLE(entry.getKey());
                    store.put(txn, new ArrayByteIterable(LevelDBKey.indexEntity(entry.getKey())), new ArrayByteIterable(AllayNBTUtils.nbtToBytesLE(entity.saveNBT())));
                }
                store.put(txn, new ArrayByteIterable(idsKey), new ArrayByteIterable(ByteBufUtil.getBytes(idsBuf)));
            } finally {
                idsBuf.release();
            }
        });
    }

    @Override
    public boolean containChunk(int x, int z, DimensionInfo dimensionInfo) {
        return env.computeInReadonlyTransaction(txn -> {
            if (!env.storeExists(STORE_CHUNKS, txn)) {
                return false;
            }
            Store store = getStore(txn);
            for (int ySection = dimensionInfo.minSectionY(); ySection <= dimensionInfo.maxSectionY(); ySection++) {
                if (store.get(txn, new ArrayByteIterable(LevelDBKey.CHUNK_SECTION_PREFIX.createKey(x, z, ySection, dimensionInfo))) != null) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public void writeWorldData(WorldData worldData) {
        var allayWorldData = (AllayWorldData) worldData;
        var levelDat = path.resolve(FILE_LEVEL_DAT).toFile();
        try (var output = new FileOutputStream(levelDat);
             var byteArrayOutputStream = new ByteArrayOutputStream();
             var nbtOutputStream = NbtUtils.createWriterLE(byteArrayOutputStream)) {
            if (levelDat.exists()) {
                Files.copy(path.resolve(FILE_LEVEL_DAT), path.resolve(FILE_LEVEL_DAT_OLD), StandardCopyOption.REPLACE_EXISTING);
            }

            output.write(int2ByteArrayLE(CURRENT_STORAGE_VERSION));
            nbtOutputStream.writeTag(writeWorldDataToNBT(allayWorldData));
            var data = byteArrayOutputStream.toByteArray();
            output.write(int2ByteArrayLE(data.length));
            output.write(data);
        } catch (IOException e) {
            throw new WorldStorageException(e);
        }
    }

    @Override
    public WorldData readWorldData() {
        var levelDat = path.resolve(FILE_LEVEL_DAT).toFile();
        if (!levelDat.exists()) {
            return createWorldData(worldName);
        }

        try (var input = new FileInputStream(levelDat)) {
            input.skip(8);
            try (NBTInputStream readerLE = NbtUtils.createReaderLE(new ByteArrayInputStream(input.readAllBytes()))) {
                NbtMap nbt = (NbtMap) readerLE.readTag();
                return readWorldDataFromNBT(nbt);
            }
        } catch (IOException e) {
            throw new WorldStorageException(e);
        }
    }

    @Override
    public String getName() {
        return "XODUS";
    }

    private WorldData createWorldData(String worldName) {
        var levelDat = path.resolve(FILE_LEVEL_DAT).toFile();
        try {
            levelDat.createNewFile();
            var worldData = AllayWorldData
                    .builder()
                    .displayName(worldName)
                    .build();
            writeWorldData(worldData);
            Files.copy(levelDat.toPath(), path.resolve(FILE_LEVEL_DAT_OLD), StandardCopyOption.REPLACE_EXISTING);
            return worldData;
        } catch (IOException e) {
            throw new WorldStorageException(e);
        }
    }

    private AllayWorldData readWorldDataFromNBT(NbtMap nbt) {
         var storageVersion = nbt.getInt(TAG_STORAGE_VERSION, Integer.MAX_VALUE);
        if (storageVersion == Integer.MAX_VALUE) {
            storageVersion = CURRENT_STORAGE_VERSION;
        }
        
        var pdc = new AllayPersistentDataContainer(Registries.PERSISTENT_DATA_TYPES);
        nbt.listenForCompound(TAG_PDC, pdc::putAll);

        return AllayWorldData.builder()
                .difficulty(Difficulty.from(nbt.getInt(TAG_DIFFICULTY, AllayServer.getSettings().genericSettings().defaultDifficulty().ordinal())))
                .gameMode(NetworkHelper.fromNetwork(GameType.from(nbt.getInt(TAG_GAME_TYPE, toNetwork(AllayServer.getSettings().genericSettings().defaultGameMode()).ordinal()))))
                .displayName(nbt.getString(TAG_DISPLAY_NAME, WorldData.DEFAULT_WORLD_DISPLAY_NAME))
                .spawnPoint(new Vector3i(nbt.getInt(TAG_SPAWN_X, 0), nbt.getInt(TAG_SPAWN_Y, 64), nbt.getInt(TAG_SPAWN_Z, 0)))
                .totalTime(nbt.getLong(TAG_TOTAL_TIME, 0))
                .timeOfDay(nbt.getInt(TAG_TIME_OF_DAY, WorldData.TIME_SUNRISE))
                .worldStartCount(nbt.getLong(TAG_WORLD_START_COUNT, 0))
                .persistentDataContainer(pdc)
                .gameRules(AllayGameRules.readFromNBT(nbt))
                .build();
    }

    private NbtMap writeWorldDataToNBT(AllayWorldData worldData) {
        var builder = NbtMap.builder();
        builder.putInt(TAG_DIFFICULTY, worldData.getDifficulty().ordinal());
        builder.putInt(TAG_GAME_TYPE, toNetwork(worldData.getGameMode()).ordinal());
        builder.putString(TAG_DISPLAY_NAME, worldData.getDisplayName());
        builder.putInt(TAG_SPAWN_X, worldData.getSpawnPoint().x());
        builder.putInt(TAG_SPAWN_Y, worldData.getSpawnPoint().y());
        builder.putInt(TAG_SPAWN_Z, worldData.getSpawnPoint().z());
        builder.putLong(TAG_TOTAL_TIME, worldData.getTotalTime());
        builder.putInt(TAG_TIME_OF_DAY, worldData.getTimeOfDay());
        builder.putLong(TAG_WORLD_START_COUNT, worldData.getWorldStartCount());

        var pdc = worldData.getPersistentDataContainer();
        if (!pdc.isEmpty()) {
            builder.put(TAG_PDC, pdc.toNbt());
        }

        builder.putInt(TAG_GENERATOR, 5);
        builder.putLong(TAG_RANDOM_SEED, 0);
        builder.putInt(TAG_STORAGE_VERSION, CURRENT_STORAGE_VERSION);
        builder.putInt(TAG_NETWORK_VERSION, ProtocolInfo.FEATURE_VERSION.getProtocolVersion());
        builder.putLong(TAG_LAST_PLAYED, System.currentTimeMillis() / 1000);
        builder.putByte(TAG_ACHIEVEMENTS_DISABLED, (byte) 1);
        builder.putByte(TAG_COMMANDS_ENABLED, (byte) 1);
        builder.putList(TAG_LAST_OPENED_WITH_VERSION, NbtType.INT, ProtocolInfo.getFeatureMinecraftVersion().toBoxedArray());
        builder.putByte(TAG_IS_EDU, (byte) 0);
        builder.putByte(TAG_FORCE_GAME_TYPE, (byte) 0);

        worldData.getGameRules().writeToNBT(builder);
        return builder.build();
    }

    @Override
    public void shutdown() {
        if (env != null && env.isOpen()) {
            env.close();
        }
    }
}