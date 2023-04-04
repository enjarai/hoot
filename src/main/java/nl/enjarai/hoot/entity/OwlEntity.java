package nl.enjarai.hoot.entity;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ParticleUtil;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import nl.enjarai.hoot.entity.ai.DeliveryNavigation;
import nl.enjarai.hoot.entity.ai.ThrowAroundItemGoal;
import nl.enjarai.hoot.entity.ai.TravelToDestinationGoal;
import nl.enjarai.hoot.entity.ai.WanderNearHomeGoal;
import nl.enjarai.hoot.registry.ModRegistries;
import nl.enjarai.hoot.registry.ModSoundEvents;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;

import static software.bernie.geckolib.constant.DefaultAnimations.*;

public class OwlEntity extends TameableEntity implements GeoEntity, VariantHolder<OwlVariant>, Flutterer, InventoryOwner {
    private static final TrackedData<OwlVariant> VARIANT =
            DataTracker.registerData(OwlEntity.class, ModRegistries.OWL_VARIANT_DATA);
    private static final TrackedData<Integer> COLLAR_COLOR =
            DataTracker.registerData(OwlEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final Set<Item> TAMING_INGREDIENTS = Set.of(Items.RABBIT);
    private static final int LEASH_TIME_BEFORE_HOME = 20 * 60 * 20; // 20 minutes, 24000 ticks, one minecraft day

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    public DeliveryNavigation deliveryNavigation;
    private int leashedTime;
    private GlobalPos homePos;
    private float flapSpeed;
    private float flapping = 1.0f;
    private float nextFlap = 1.0f;
    private final SimpleInventory inventory = new SimpleInventory(1);

    protected OwlEntity(EntityType<? extends OwlEntity> entityType, World world) {
        super(entityType, world);
        moveControl = new FlightMoveControl(this, 10, false);
        deliveryNavigation = new DeliveryNavigation();
        setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0f);
        setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0f);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new EscapeDangerGoal(this, 1.25));
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(2, new SitGoal(this));
        goalSelector.add(3, new TravelToDestinationGoal(this, 1.5, 24));
        goalSelector.add(4, new WanderNearHomeGoal(this, 1.0, 14));
//        goalSelector.add(4, new FollowOwnerGoal(this, 1.0, 5.0f, 1.0f, true));
        goalSelector.add(4, new ParrotEntity.FlyOntoTreeGoal(this, 1.0));
        goalSelector.add(5, new ThrowAroundItemGoal(this));
        goalSelector.add(5, new FollowMobGoal(this, 1.0, 3.0f, 7.0f));
        goalSelector.add(9, new AttackGoal(this));
        targetSelector.add(1, new UntamedActiveTargetGoal<>(this, RabbitEntity.class, false, null));
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation birdNavigation = new BirdNavigation(this, world);
        birdNavigation.setCanPathThroughDoors(false);
        birdNavigation.setCanSwim(true);
        birdNavigation.setCanEnterOpenDoors(true);
        return birdNavigation;
    }

    public static DefaultAttributeContainer.Builder createOwlAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0f)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6f)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2f)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0f);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        var biome = world.getBiome(getBlockPos());
        setVariant(OwlVariant.fromBiome(biome));
        if (entityData == null) {
            entityData = new PassiveEntity.PassiveData(false);
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();

        if (!world.isClient && isAlive() && age % 10 == 0) {
            heal(1.0f);
        }

        flapWings();
    }

    public void onDeliver() {
        playHappySound();
        dropHeldItem();
    }

    public void onReturn() {
    }

    public void spawnTeleportParticles() {
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.POOF,
                    getX(), getY(), getZ(),
                    10,
                    0.5, 0.5, 0.5,
                    0.0
            );
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();
        if (!isTamed()) {
            if (TAMING_INGREDIENTS.contains(item)) {
                decrementStackUnlessInCreative(player, itemStack);
                playHappySound();
                if (!world.isClient) {
                    if (random.nextInt(5) == 0) {
                        setOwner(player);
                        world.sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
                    } else {
                        world.sendEntityStatus(this, EntityStatuses.ADD_NEGATIVE_PLAYER_REACTION_PARTICLES);
                    }
                }
                return ActionResult.success(world.isClient);
            }
        } else {
            if (item instanceof DyeItem dye) {
                DyeColor dyeColor = dye.getColor();
                if (dyeColor == getCollarColor()) return super.interactMob(player, hand);
                setCollarColor(dyeColor);
                decrementStackUnlessInCreative(player, itemStack);
                return ActionResult.SUCCESS;
            }
            if (item == Items.COMPASS && isOwner(player) &&
                    !getEquippedStack(EquipmentSlot.MAINHAND).isEmpty() && CompassItem.hasLodestone(itemStack)) {
                GlobalPos lodestonePos = CompassItem.createLodestonePos(itemStack.getOrCreateNbt());
                if (lodestonePos != null &&
                        lodestonePos.getDimension().equals(world.getRegistryKey()) &&
                        tryStartDelivery(lodestonePos.getPos().up())) {
                    playHappySound();
                }
                return ActionResult.SUCCESS;
            }
            if (player.isSneaking() && isOwner(player) && isDelivering()) {
                playHurtSound(DamageSource.GENERIC);
                showEmoteParticle(false);
                completeDelivery(false);
                return ActionResult.SUCCESS;
            }
            if (replaceHeldItem(player, hand)) {
                return ActionResult.SUCCESS;
            }
            if (!isInAir() && isOwner(player)) {
                if (!world.isClient) {
                    setSitting(!isSitting());
                }
                return ActionResult.success(world.isClient);
            }
        }
        return super.interactMob(player, hand);
    }
    
    public boolean replaceHeldItem(PlayerEntity player, Hand hand) {
        ItemStack playerHand = player.getStackInHand(hand);
        ItemStack owlHand = getStackInHand(Hand.MAIN_HAND);
        if (owlHand.isEmpty() && !playerHand.isEmpty() && hand == Hand.MAIN_HAND) {
            ItemStack stack = playerHand.copy();
            stack.setCount(1);
            setStackInHand(Hand.MAIN_HAND, stack);
            decrementStackUnlessInCreative(player, playerHand);
            world.playSoundFromEntity(
                    null, this,
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, getSoundCategory(),
                    2.0f, 1.0f
            );
            return true;
        }
        if (!owlHand.isEmpty() && hand == Hand.MAIN_HAND && playerHand.isEmpty()) {
            equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            world.playSoundFromEntity(
                    null, this,
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, getSoundCategory(),
                    2.0f, 1.0f
            );
            player.giveItemStack(owlHand);
            return true;
        }
        return false;
    }

    public void dropHeldItem() {
        ItemStack stack = getStackInHand(Hand.MAIN_HAND);
        if (!stack.isEmpty()) {
            equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            dropStack(stack);
        }
    }

    @Override
    public void updateLeash() {
        super.updateLeash();
        if (isTamed()) {
            if (isLeashed() && getHoldingEntity() instanceof LeashKnotEntity knot && getHome() == null) {
                leashedTime += 1;
                if (leashedTime > LEASH_TIME_BEFORE_HOME) {
                    setHome(GlobalPos.create(world.getRegistryKey(), knot.getBlockPos()));
                    playHappySound();
                    world.sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
                }
            } else {
                leashedTime = 0;
            }
        }
    }

    /* start code that idk if it'll do anything */
    @Override
    public boolean canEquip(ItemStack stack) {
        EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(stack);
        if (!getEquippedStack(equipmentSlot).isEmpty()) {
            return false;
        }
        return equipmentSlot == EquipmentSlot.MAINHAND && super.canEquip(stack);
    }

    @Override
    protected void loot(ItemEntity item) {
        ItemStack itemStack;
        if (getEquippedStack(EquipmentSlot.MAINHAND).isEmpty() && canPickupItem(itemStack = item.getStack())) {
            triggerItemPickedUpByEntityCriteria(item);
            equipStack(EquipmentSlot.MAINHAND, itemStack);
            updateDropChances(EquipmentSlot.MAINHAND);
            sendPickup(item, itemStack.getCount());
            item.discard();
        }
    }
    /* end code that idk if it'll do anything */

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    public Identifier getTexture() {
        return getVariant().texture();
    }

    @Override
    public OwlVariant getVariant() {
        return dataTracker.get(VARIANT);
    }

    @Override
    public void setVariant(OwlVariant variant) {
        dataTracker.set(VARIANT, variant);
    }

    public DyeColor getCollarColor() {
        return DyeColor.byId(dataTracker.get(COLLAR_COLOR));
    }

    public void setCollarColor(DyeColor color) {
        dataTracker.set(COLLAR_COLOR, color.getId());
    }

    @Nullable
    public GlobalPos getHome() {
        return homePos;
    }

    public void setHome(@Nullable GlobalPos pos) {
        homePos = pos;
    }

    public boolean tryStartDelivery(BlockPos destination) {
        if (!isDelivering()) {
            deliveryNavigation.setSource(getBlockPos());
            deliveryNavigation.setDestination(destination);
            deliveryNavigation.setState(DeliveryNavigation.State.DELIVERING);
            setSitting(false);

            return true;
        }
        return false;
    }

    public boolean tryStartDelivery(Entity target) {
        if (!isDelivering()) {
            deliveryNavigation.setSource(getBlockPos());
            deliveryNavigation.setDestinationEntityUUID(target.getUuid());
            deliveryNavigation.setDestination(target.getBlockPos());
            deliveryNavigation.setState(DeliveryNavigation.State.DELIVERING);
            setSitting(false);

            return true;
        }
        return false;
    }

    public boolean isDelivering() {
        return deliveryNavigation.getState() != DeliveryNavigation.State.IDLE;
    }

    public void completeDelivery(boolean success) {
        if (deliveryNavigation.getState() == DeliveryNavigation.State.DELIVERING) {
            if (success) onDeliver();

            if (getHome() != null && getHome().getDimension().equals(world.getRegistryKey())) {
                deliveryNavigation.setDestination(getHome().getPos());
                deliveryNavigation.setDestinationEntityUUID(null);
            } else if (getOwner() != null && getOwner().world.getRegistryKey().equals(world.getRegistryKey())) {
                deliveryNavigation.setDestination(getOwner().getBlockPos());
                deliveryNavigation.setDestinationEntityUUID(getOwner().getUuid());
            } else if (deliveryNavigation.getSource().isPresent()) {
                deliveryNavigation.setDestination(deliveryNavigation.getSource().get());
                deliveryNavigation.setDestinationEntityUUID(null);
            }

            deliveryNavigation.setSource(getBlockPos());
            deliveryNavigation.setState(DeliveryNavigation.State.RETURNING);
        } else if (deliveryNavigation.getState() == DeliveryNavigation.State.RETURNING) {
            if (success) onReturn();
            deliveryNavigation.setDestination(null);
            deliveryNavigation.setDestinationEntityUUID(null);
            deliveryNavigation.setSource(null);
            deliveryNavigation.setState(DeliveryNavigation.State.IDLE);
        }
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    }

    @Override
    public boolean tryAttack(Entity target) {
        return target.damage(DamageSource.mob(this), (float) getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
    }

    @Override
    protected Vec3d getLeashOffset() {
        return super.getLeashOffset().add(0, -0.3, -0.1);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.ENTITY_OWL_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.ENTITY_OWL_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.ENTITY_OWL_DEATH;
    }

    public void playHappySound() {
        if (!isSilent()) {
            world.playSoundFromEntity(
                    null, this,
                    ModSoundEvents.ENTITY_OWL_EAT, getSoundCategory(), 1.0f,
                    1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f
            );
        }
    }

    private void flapWings() {
        flapSpeed += (float) (onGround || hasVehicle() ? -1 : 4) * 0.3f;
        flapSpeed = MathHelper.clamp(flapSpeed, 0.0f, 1.0f);
        if (!onGround && flapping < 1.0f) {
            flapping = 1.0f;
        }
        flapping *= 0.9f;
        Vec3d vec3d = getVelocity();
        if (!onGround && vec3d.y < 0.0) {
            setVelocity(vec3d.multiply(1.0, 0.6, 1.0));
        }
    }

    @Override
    protected boolean isFlappingWings() {
        return speed > nextFlap;
    }

    @Override
    protected void addFlapEffects() {
        playSound(SoundEvents.ENTITY_PARROT_FLY, 0.15f, 1.0f);
        nextFlap = speed + flapSpeed / 2.0f;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(VARIANT, ModRegistries.OWL_VARIANT.getOrThrow(OwlVariant.WOOD_OWL_KEY));
        dataTracker.startTracking(COLLAR_COLOR, DyeColor.RED.getId());
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        writeInventory(nbt);
        nbt.putString("variant", ModRegistries.OWL_VARIANT.getId(getVariant()).toString());
        nbt.putByte("CollarColor", (byte) getCollarColor().getId());
        nbt.put("navigation", DeliveryNavigation.CODEC
                .encodeStart(NbtOps.INSTANCE, deliveryNavigation)
                .result().orElse(new NbtCompound()));
        nbt.putInt("leashedTime", leashedTime);
        if (homePos != null) {
            nbt.put("homePos", GlobalPos.CODEC
                    .encodeStart(NbtOps.INSTANCE, homePos)
                    .result().orElse(new NbtCompound()));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        readInventory(nbt);
        OwlVariant variant = ModRegistries.OWL_VARIANT.get(Identifier.tryParse(nbt.getString("variant")));
        if (variant != null) {
            setVariant(variant);
        }
        if (nbt.contains("CollarColor", NbtElement.NUMBER_TYPE)) {
            setCollarColor(DyeColor.byId(nbt.getInt("CollarColor")));
        }
        if (nbt.contains("navigation", NbtElement.COMPOUND_TYPE)) {
            deliveryNavigation = DeliveryNavigation.CODEC
                    .decode(NbtOps.INSTANCE, nbt.getCompound("navigation"))
                    .result().map(Pair::getFirst).orElse(deliveryNavigation);
        }
        if (nbt.contains("leashedTime", NbtElement.NUMBER_TYPE)) {
            leashedTime = nbt.getInt("leashedTime");
        }
        if (nbt.contains("homePos", NbtElement.COMPOUND_TYPE)) {
            homePos = GlobalPos.CODEC
                    .decode(NbtOps.INSTANCE, nbt.getCompound("homePos"))
                    .result().map(Pair::getFirst).orElse(null);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, "fly/idle", 1, state -> {
                    if (isInSittingPose()) {
                        return state.setAndContinue(SIT);
                    }

                    return state.setAndContinue(state.isMoving() || !isOnGround() ? FLY : IDLE);
                })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public boolean isInAir() {
        return !isOnGround();
    }

    @Override
    public SimpleInventory getInventory() {
        return inventory;
    }

    private void decrementStackUnlessInCreative(PlayerEntity player, ItemStack stack) {
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
    }
}
