/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.plugin.entityactivation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.effect.EntityWeatherEffect;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.explosive.FusedExplosive;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.config.category.EntityActivationModCategory;
import org.spongepowered.common.config.category.EntityActivationRangeCategory;
import org.spongepowered.common.entity.SpongeEntityType;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.entity.IMixinEntity;
import org.spongepowered.common.interfaces.world.IMixinWorld;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.mixin.plugin.entityactivation.interfaces.IModData_Activation;

import java.util.HashMap;
import java.util.Map;

public class EntityActivationRange {

    private static final ImmutableMap<Byte, String> activationTypeMappings = new ImmutableMap.Builder<Byte, String>()
            .put((byte) 1, "monster")
            .put((byte) 2, "creature")
            .put((byte) 3, "aquatic")
            .put((byte) 4, "ambient")
            .put((byte) 5, "misc")
            .build();

    static AxisAlignedBB maxBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB miscBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB creatureBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB monsterBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB aquaticBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB ambientBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    static AxisAlignedBB tileEntityBB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    @SuppressWarnings("serial") static Map<Byte, Integer> maxActivationRanges = new HashMap<Byte, Integer>() {

        {
            put((byte) 1, 32);
            put((byte) 2, 32);
            put((byte) 3, 32);
            put((byte) 4, 32);
            put((byte) 5, 16);
        }
    };

    /**
     * Initializes an entities type on construction to specify what group this
     * entity is in for activation ranges.
     *
     * @param entity Entity to get type for
     * @return group id
     */
    public static byte initializeEntityActivationType(Entity entity) {

        // account for entities that dont extend EntityMob, EntityAmbientCreature, EntityCreature
        if (((IMob.class.isAssignableFrom(entity.getClass())
                || IRangedAttackMob.class.isAssignableFrom(entity.getClass())) && (entity.getClass() != EntityMob.class))
                || SpongeImplHooks.isCreatureOfType(entity, EnumCreatureType.MONSTER)) {
            return 1; // Monster
        } else if (SpongeImplHooks.isCreatureOfType(entity, EnumCreatureType.CREATURE)) {
            return 2; // Creature
        } else if (SpongeImplHooks.isCreatureOfType(entity, EnumCreatureType.WATER_CREATURE)) {
            return 3; // Aquatic
        } else if (SpongeImplHooks.isCreatureOfType(entity, EnumCreatureType.AMBIENT)) {
            return 4; // Ambient
        } else {
            return 5; // Misc
        }
    }

    /**
     * These entities are excluded from Activation range checks.
     *
     * @param entity Entity to check
     * @return boolean If it should always tick.
     */
    public static boolean initializeEntityActivationState(Entity entity) {
        if (((IMixinWorld) entity.worldObj).isFake()) {
            return true;
        }

        // types that should always be active
        if (entity instanceof EntityPlayer && !SpongeImplHooks.isFakePlayer(entity)
            || entity instanceof EntityThrowable
            || entity instanceof EntityDragon
            || entity instanceof EntityDragonPart
            || entity instanceof EntityWither
            || entity instanceof EntityFireball
            || entity instanceof EntityWeatherEffect
            || entity instanceof EntityTNTPrimed
            || entity instanceof EntityEnderCrystal
            || entity instanceof EntityFireworkRocket
            || entity instanceof EntityFallingBlock) // Always tick falling blocks
        {
            return true;
        }

        EntityActivationRangeCategory config = ((IMixinWorldServer) entity.worldObj).getActiveConfig().getConfig().getEntityActivationRange();
        EntityType type = ((org.spongepowered.api.entity.Entity) entity).getType();

        IModData_Activation spongeEntity = (IModData_Activation) entity;
        if (type == EntityTypes.UNKNOWN || !(type instanceof SpongeEntityType)) {
            return false;
        }
        SpongeEntityType spongeType = (SpongeEntityType) type;

        byte activationType = spongeEntity.getActivationType();
        EntityActivationModCategory entityMod = config.getModList().get(spongeType.getModId().toLowerCase());
        int defaultActivationRange = config.getDefaultRanges().get(activationTypeMappings.get(activationType));
        if (entityMod == null) {
            // use default activation range
            spongeEntity.setActivationRange(defaultActivationRange);
            if (defaultActivationRange <= 0) {
                return true;
            }
            return false;
        } else if (!entityMod.isEnabled()) {
            spongeEntity.setActivationRange(defaultActivationRange);
            return true;
        }

        Integer defaultModActivationRange = entityMod.getDefaultRanges().get(activationTypeMappings.get(activationType));
        Integer entityActivationRange = entityMod.getEntityList().get(type.getName().toLowerCase());
        if (defaultModActivationRange != null && entityActivationRange == null) {
            spongeEntity.setActivationRange(defaultModActivationRange);
            if (defaultModActivationRange <= 0) {
                return true;
            }
            return false;
        } else if (entityActivationRange != null) {
            spongeEntity.setActivationRange(entityActivationRange);
            if (entityActivationRange <= 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Utility method to grow an AABB without creating a new AABB or touching
     * the pool, so we can re-use ones we have.
     *
     * @param target The AABB to modify
     * @param source The AABB to get initial coordinates from
     * @param x The x value to expand by
     * @param y The y value to expand by
     * @param z The z value to expand by
     */
    public static void growBb(AxisAlignedBB target, AxisAlignedBB source, int x, int y, int z) {
        target.minX = source.minX - x;
        target.minY = source.minY - y;
        target.minZ = source.minZ - z;
        target.maxX = source.maxX + x;
        target.maxY = source.maxY + y;
        target.maxZ = source.maxZ + z;
    }

    /**
     * Find what entities are in range of the players in the world and set
     * active if in range.
     *
     * @param world The world to perform activation checks in
     */
    public static void activateEntities(World world) {
        for (EntityPlayer player : world.playerEntities) {

            int maxRange = 0;
            for (Integer range : maxActivationRanges.values()) {
                if (range > maxRange) {
                    maxRange = range;
                }
            }

            maxRange = Math.min((SpongeImpl.getServer().getPlayerList().getViewDistance() << 4) - 8, maxRange);
            ((IModData_Activation) player).setActivatedTick(SpongeImpl.getServer().getTickCounter());
            growBb(maxBB, player.getEntityBoundingBox(), maxRange, 256, maxRange);

            final Chunk chunk = (net.minecraft.world.chunk.Chunk) ((IMixinEntity) player).getActiveChunk();
            if (chunk != null) {
                activateChunkEntities(player, chunk);
            }
        }
    }

    /**
     * Checks for the activation state of all entities in this chunk.
     *
     * @param chunk Chunk to check for activation
     */
    private static void activateChunkEntities(EntityPlayer player, Chunk chunk) {
        for (int i = 0; i < chunk.getEntityLists().length; ++i) {

            for (Object o : chunk.getEntityLists()[i]) {
                Entity entity = (Entity) o;
                EntityType type = ((org.spongepowered.api.entity.Entity) entity).getType();
                long currentTick = SpongeImpl.getServer().getTickCounter();
                if (type == EntityTypes.UNKNOWN) {
                    ((IModData_Activation) entity).setActivatedTick(currentTick);
                    continue;
                }

                if (currentTick > ((IModData_Activation) entity).getActivatedTick()) {
                    if (((IModData_Activation) entity).getDefaultActivationState()) {
                        ((IModData_Activation) entity).setActivatedTick(currentTick);
                        continue;
                    }

                    IModData_Activation spongeEntity = (IModData_Activation) entity;
                    // check if activation cache needs to be updated
                    if (spongeEntity.requiresActivationCacheRefresh()) {
                        EntityActivationRange.initializeEntityActivationState(entity);
                        spongeEntity.requiresActivationCacheRefresh(false);
                    }
                    // check for entity type overrides
                    byte activationType = ((IModData_Activation) entity).getActivationType();
                    int bbActivationRange = ((IModData_Activation) entity).getActivationRange();

                    if (activationType == 5) {
                        growBb(miscBB, player.getEntityBoundingBox(), bbActivationRange, 256, bbActivationRange);
                    } else if (activationType == 4) {
                        growBb(ambientBB, player.getEntityBoundingBox(), bbActivationRange, 256, bbActivationRange);
                    } else if (activationType == 3) {
                        growBb(aquaticBB, player.getEntityBoundingBox(), bbActivationRange, 256, bbActivationRange);
                    } else if (activationType == 2) {
                        growBb(creatureBB, player.getEntityBoundingBox(), bbActivationRange, 256, bbActivationRange);
                    } else {
                        growBb(monsterBB, player.getEntityBoundingBox(), bbActivationRange, 256, bbActivationRange);
                    }

                    switch (((IModData_Activation) entity).getActivationType()) {
                        case 1:
                            if (monsterBB.intersectsWith(entity.getEntityBoundingBox())) {
                                ((IModData_Activation) entity).setActivatedTick(currentTick);
                            }
                            break;
                        case 2:
                            if (creatureBB.intersectsWith(entity.getEntityBoundingBox())) {
                                ((IModData_Activation) entity).setActivatedTick(currentTick);
                            }
                            break;
                        case 3:
                            if (aquaticBB.intersectsWith(entity.getEntityBoundingBox())) {
                                ((IModData_Activation) entity).setActivatedTick(currentTick);
                            }
                            break;
                        case 4:
                            if (ambientBB.intersectsWith(entity.getEntityBoundingBox())) {
                                ((IModData_Activation) entity).setActivatedTick(currentTick);
                            }
                            break;
                        case 5:
                        default:
                            if (miscBB.intersectsWith(entity.getEntityBoundingBox())) {
                                ((IModData_Activation) entity).setActivatedTick(currentTick);
                            }
                    }
                }
            }
        }
    }

    /**
     * If an entity is not in range, do some more checks to see if we should
     * give it a shot.
     *
     * @param entity Entity to check
     * @return Whether entity should still be maintained active
     */
    public static boolean checkEntityImmunities(Entity entity) {
        // quick checks.
        if (entity.fire > 0) {
            return true;
        }
        if (!(entity instanceof Projectile)) {
            if (!entity.getPassengers().isEmpty() || entity.ridingEntity != null) {
                return true;
            }
        } else if (!((Projectile) entity).isOnGround()) {
            return true;
        }
        // special cases.
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase livingBase = (EntityLivingBase) entity;
            if (livingBase.getAITarget() != null) {
                return true;
            }
            if (entity instanceof EntityLiving) {
                EntityLiving living = (EntityLiving) entity;
                if (living.getAttackTarget() != null) {
                    return true;
                }
            }
            if (livingBase.hurtTime > 0 || livingBase.getActivePotionEffects().size() > 0) {
                return true;
            }
            if (entity instanceof EntityVillager && ((EntityVillager) entity).isMating()) {
                return true;
            }
            if (entity instanceof EntityAnimal) {
                EntityAnimal animal = (EntityAnimal) entity;
                if (animal.isChild() || animal.isInLove()) {
                    return true;
                }
                if (entity instanceof EntitySheep && ((EntitySheep) entity).getSheared()) {
                    return true;
                }
            }
            if (entity instanceof FusedExplosive && ((FusedExplosive) entity).isPrimed()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the entity is active for this tick.
     *
     * @param entity The entity to check for activity
     * @return Whether the given entity should be active
     */
    public static boolean checkIfActive(Entity entity) {
        // Never safe to skip fireworks or entities not yet added to chunk
        if (entity.worldObj.isRemote || !entity.addedToChunk || entity instanceof EntityFireworkRocket) {
            return true;
        }
        final IMixinChunk activeChunk = ((IMixinEntity) entity).getActiveChunk();
        if (activeChunk == null) {
            // Should never happen but just in case for mods, always tick
            return true;
        }

        // If in forced chunk or is player
        if (activeChunk.isPersistedChunk() || (!SpongeImplHooks.isFakePlayer(entity) && entity instanceof EntityPlayerMP)) {
            return true;
        }

        long currentTick = SpongeImpl.getServer().getTickCounter();
        IModData_Activation spongeEntity = (IModData_Activation) entity;
        boolean isActive = spongeEntity.getActivatedTick() >= currentTick || spongeEntity.getDefaultActivationState();

        // Should this entity tick?
        if (!isActive) {
            if ((currentTick - spongeEntity.getActivatedTick() - 1) % 20 == 0) {
                // Check immunities every 20 ticks.
                if (checkEntityImmunities(entity)) {
                    // Triggered some sort of immunity, give 20 full ticks before we check again.
                    spongeEntity.setActivatedTick(currentTick + 20);
                }
                isActive = true;
            }
            // Add a little performance juice to active entities. Skip 1/4 if not immune.
        } else if (!spongeEntity.getDefaultActivationState() && entity.ticksExisted % 4 == 0 && !checkEntityImmunities(entity)) {
            isActive = false;
        }

        if (isActive && !activeChunk.areNeighborsLoaded()) {
            isActive = false;
        }

        return isActive;
    }

    public static void addEntityToConfig(World world, SpongeEntityType type, byte activationType) {
        checkNotNull(world, "world");
        checkNotNull(type, "type");

        SpongeConfig<?> config = ((IMixinWorldServer) world).getActiveConfig();
        if (config == null || type == null || !config.getConfig().getEntityActivationRange().autoPopulateData()) {
            return;
        }

        String entityType = "misc";
        entityType = EntityActivationRange.activationTypeMappings.get(activationType);
        boolean requiresSave = false;
        final String entityModId = type.getModId().toLowerCase();
        EntityActivationRangeCategory activationCategory = config.getConfig().getEntityActivationRange();
        EntityActivationModCategory entityMod = activationCategory.getModList().get(entityModId);
        if (entityMod == null) {
            entityMod = new EntityActivationModCategory();
            activationCategory.getModList().put(entityModId, entityMod);
            requiresSave = true;
        }

        if (entityMod != null) {
            // check for activation type overrides
            Integer modActivationRange = entityMod.getDefaultRanges().get(entityType);
            if (modActivationRange == null) {
                entityMod.getDefaultRanges().put(entityType, activationType == 5 ? 16 : 32);
                requiresSave = true;
            } else if (modActivationRange != null) {
                // check max ranges
                if (modActivationRange > maxActivationRanges.get(activationType)) {
                    maxActivationRanges.put(activationType, modActivationRange);
                }
            }

            // check for entity overrides
            final String entityId = type.getName().toLowerCase();
            Integer entityActivationRange = entityMod.getEntityList().get(entityId);
            if (entityActivationRange == null) {
                entityMod.getEntityList().put(entityId, entityMod.getDefaultRanges().get(activationTypeMappings.get(activationType)));
                requiresSave = true;
            } else if (entityActivationRange != null) {
                // check max ranges
                if (entityActivationRange > maxActivationRanges.get(activationType)) {
                    maxActivationRanges.put(activationType, entityActivationRange);
                }
            }
        }

        if (requiresSave) {
            config.save();
        }
    }
}
