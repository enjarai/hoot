package nl.enjarai.hoot.entity;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EntityView;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import nl.enjarai.hoot.entity.ai.DeliveryNavigation;
import nl.enjarai.hoot.entity.ai.ThrowAroundItemGoal;
import nl.enjarai.hoot.entity.ai.TravelToDestinationGoal;
import nl.enjarai.hoot.entity.ai.WanderNearHomeGoal;
import nl.enjarai.hoot.registry.ModEntities;
import nl.enjarai.hoot.registry.ModItems;
import nl.enjarai.hoot.registry.ModRegistries;
import nl.enjarai.hoot.registry.ModSoundEvents;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;

import static software.bernie.geckolib.constant.DefaultAnimations.*;

public class OwlEntity extends TameableEntity implements GeoEntity, VariantHolder<OwlVariant>, Flutterer, InventoryOwner, Bucketable {
    private static final TrackedData<OwlVariant> VARIANT =
            DataTracker.registerData(OwlEntity.class, ModRegistries.OWL_VARIANT_DATA);
    private static final TrackedData<Integer> COLLAR_COLOR =
            DataTracker.registerData(OwlEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> FROM_BUCKET =
            DataTracker.registerData(OwlEntity.class, TrackedDataHandlerRegistry.BOOLEAN);


    private static final Set<Item> TAMING_INGREDIENTS = Set.of(Items.RABBIT);
    private static final int LEASH_TIME_BEFORE_HOME = 20 * 60 * 20; // 20 minutes, 24000 ticks, one minecraft day

    public static final RawAnimation DANCE_ANIMATION = RawAnimation.begin().thenLoop("misc.dance");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    public DeliveryNavigation deliveryNavigation;
    private int leashedTime;
    private GlobalPos homePos;
    private float flapSpeed;
    private float flapping = 1.0f;
    private float nextFlap = 1.0f;
    private final SimpleInventory inventory = new SimpleInventory(1);
    private boolean songPlaying;
    @Nullable
    private BlockPos songSource;

    public OwlEntity(EntityType<? extends OwlEntity> entityType, World world) {
        super(entityType, world);
        moveControl = new FlightMoveControl(this, 10, false);
        deliveryNavigation = new DeliveryNavigation();
        setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0f);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new EscapeDangerGoal(this, 1.25));
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(2, new SitGoal(this));
        goalSelector.add(3, new AnimalMateGoal(this, 1.0));
        goalSelector.add(4, new TravelToDestinationGoal(this, 1.5, 8));
        goalSelector.add(5, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.add(6, new ThrowAroundItemGoal(this));
        goalSelector.add(7, new WanderNearHomeGoal(this, 1.0, 14));
//        goalSelector.add(4, new FollowOwnerGoal(this, 1.0, 5.0f, 1.0f, true));
        goalSelector.add(7, new ParrotEntity.FlyOntoTreeGoal(this, 1.0));
//        goalSelector.add(6, new FollowMobGoal(this, 1.0, 3.0f, 7.0f));
        targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        targetSelector.add(2, new AttackWithOwnerGoal(this));
        targetSelector.add(3, new UntamedActiveTargetGoal<>(this, RabbitEntity.class, false, null));
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
        setVariant(OwlVariant.fromBiome(biome, getBlockPos()));
        if (entityData == null) {
            entityData = new PassiveEntity.PassiveData(false);
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public void tickMovement() {
        if (this.songSource == null || !this.songSource.isWithinDistance(this.getPos(), 3.46) || !this.getWorld().getBlockState(this.songSource).isOf(Blocks.JUKEBOX)) {
            this.songPlaying = false;
            this.songSource = null;
        }

        super.tickMovement();

        if (!getWorld().isClient && isAlive() && age % 10 == 0) {
            heal(1.0f);

            // TODO: maybe not keep this here
            setSitting(false);
        }

        flapWings();
    }

    @Override
    public void setNearbySongPlaying(BlockPos songPosition, boolean playing) {
        this.songSource = songPosition;
        this.songPlaying = playing;
    }

    public boolean isSongPlaying() {
        return this.songPlaying;
    }

    public void onDeliver() {
        playHappySound();
        dropHeldItem();
    }

    public void onReturn() {
    }

    public void spawnTeleportParticles(boolean acrossDimensions) {
        if (getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    acrossDimensions ? ParticleTypes.PORTAL : ParticleTypes.POOF,
                    getX(), getY(), getZ(),
                    acrossDimensions ? 100 : 10,
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
                if (!getWorld().isClient) {
                    if (random.nextInt(5) == 0) {
                        setOwner(player);
                        getWorld().sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
                    } else {
                        getWorld().sendEntityStatus(this, EntityStatuses.ADD_NEGATIVE_PLAYER_REACTION_PARTICLES);
                    }
                }
                return ActionResult.success(getWorld().isClient);
            }
        } else {
            if (player.isSneaking()) {
                if (isDelivering()) {
                    playHurtSound(getDamageSources().generic());
                    showEmoteParticle(false);
                    completeDelivery(false);
                    return ActionResult.SUCCESS;
                }
                if (replaceHeldItem(player, hand)){
                    return ActionResult.SUCCESS;
                }
            } else {
                var bucketResult = Bucketable.tryBucket(player, hand, this);
                if (bucketResult.isPresent()) return bucketResult.get();

                if (item instanceof DyeItem dye) {
                    DyeColor dyeColor = dye.getColor();
                    if (dyeColor == getCollarColor()) return super.interactMob(player, hand);
                    setCollarColor(dyeColor);
                    decrementStackUnlessInCreative(player, itemStack);
                    return ActionResult.SUCCESS;
                }
                if (item == Items.COMPASS && !getEquippedStack(EquipmentSlot.MAINHAND).isEmpty() && CompassItem.hasLodestone(itemStack)) {
                    GlobalPos lodestonePos = CompassItem.createLodestonePos(itemStack.getOrCreateNbt());
                    if (lodestonePos != null &&
                            (this.getVariant() == OwlVariant.INTERDIMENSIONAL_OWL || lodestonePos.getDimension().equals(getWorld().getRegistryKey())) &&
                            tryStartDelivery(GlobalPos.create(lodestonePos.getDimension(), lodestonePos.getPos().up()))) {
                        playHappySound();
                    }
                    return ActionResult.SUCCESS;
                }
                if (item == Items.PAPER && !getEquippedStack(EquipmentSlot.MAINHAND).isEmpty() && itemStack.hasCustomName()) {
                    if (!getWorld().isClient()) {
                        var name = itemStack.getName().getString();
                        var stream = getWorld().getServer().getPlayerManager().getPlayerList().stream();
                        if (this.getVariant() != OwlVariant.INTERDIMENSIONAL_OWL) {
                            stream = stream.filter(streamPlayer -> streamPlayer.getWorld() == getWorld());
                        }
                        var targetPlayer = stream.filter(p -> p.getGameProfile().getName().equals(name)).findFirst();

                        if (targetPlayer.isPresent() && tryStartDelivery(targetPlayer.get())) {
                            playHappySound();
                        } else {
                            showEmoteParticle(false);
                        }
                    }
                    return ActionResult.SUCCESS;
                }
                if (item == Items.ENDER_EYE && getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
                    this.setVariant(OwlVariant.INTERDIMENSIONAL_OWL);
                    this.spawnTeleportParticles(true);
                    return ActionResult.SUCCESS;
                }
//                if (!isInAir() && isOwner(player)) {
//                    if (!getWorld().isClient) {
//                        setSitting(!isSitting());
//                    }
//                    return ActionResult.success(getWorld().isClient);
//                }
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
            getWorld().playSoundFromEntity(
                    null, this,
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, getSoundCategory(),
                    2.0f, 1.0f
            );
            return true;
        }
        if (!owlHand.isEmpty() && hand == Hand.MAIN_HAND && playerHand.isEmpty()) {
            equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            getWorld().playSoundFromEntity(
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
            if (isLeashed() && getHoldingEntity() instanceof LeashKnotEntity knot) {
                leashedTime += 1;
                if (leashedTime > LEASH_TIME_BEFORE_HOME) {
                    var currentHome = getHome();
                    if (currentHome == null || !currentHome.getPos().equals(knot.getBlockPos()) || !currentHome.getDimension().equals(getWorld().getRegistryKey())) {
                        setHome(GlobalPos.create(getWorld().getRegistryKey(), knot.getBlockPos()));
                        playHappySound();
                        getWorld().sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
                    }
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

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isOf(Items.RABBIT);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        var owl = ModEntities.OWL.create(world);
        if (owl == null) return null;

        owl.setVariant(random.nextBoolean() ? getVariant() : ((OwlEntity) entity).getVariant());
        owl.setHome(GlobalPos.create(world.getRegistryKey(), entity.getBlockPos()));
        return owl;
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

    public boolean tryStartDelivery(GlobalPos destination) {
        if (!isDelivering()) {
            deliveryNavigation.setSource(DeliveryNavigation.entityPos(this));
            deliveryNavigation.setDestination(destination);
            deliveryNavigation.setState(DeliveryNavigation.State.DELIVERING);
            setSitting(false);

            return true;
        }
        return false;
    }

    public boolean tryStartDelivery(Entity target) {
        if (!isDelivering()) {
            deliveryNavigation.setSource(DeliveryNavigation.entityPos(this));
            deliveryNavigation.setDestinationEntityUUID(target.getUuid());
            deliveryNavigation.setDestination(DeliveryNavigation.entityPos(target));
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

            if (getHome() != null && (this.getVariant() == OwlVariant.INTERDIMENSIONAL_OWL ||
                    getHome().getDimension().equals(getWorld().getRegistryKey()))) {
                deliveryNavigation.setDestination(getHome());
                deliveryNavigation.setDestinationEntityUUID(null);
            } else if (deliveryNavigation.getSource().isPresent()) {
                deliveryNavigation.setDestination(deliveryNavigation.getSource().get());
                deliveryNavigation.setDestinationEntityUUID(null);
            } else if (getOwner() != null && (this.getVariant() == OwlVariant.INTERDIMENSIONAL_OWL ||
                    getOwner().getWorld().getRegistryKey().equals(getWorld().getRegistryKey()))) {
                deliveryNavigation.setDestination(DeliveryNavigation.entityPos(getOwner()));
                deliveryNavigation.setDestinationEntityUUID(getOwner().getUuid());
            }

            deliveryNavigation.setSource(DeliveryNavigation.entityPos(this));
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
        return target.damage(getDamageSources().mobAttack(this), (float) getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
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
            getWorld().playSoundFromEntity(
                    null, this,
                    ModSoundEvents.ENTITY_OWL_EAT, getSoundCategory(), 1.0f,
                    1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f
            );
        }
    }

    private void flapWings() {
        flapSpeed += (float) (isOnGround() || hasVehicle() ? -1 : 4) * 0.3f;
        flapSpeed = MathHelper.clamp(flapSpeed, 0.0f, 1.0f);
        if (!isOnGround() && flapping < 1.0f) {
            flapping = 1.0f;
        }
        flapping *= 0.9f;
        Vec3d vec3d = getVelocity();
        if (!isOnGround() && vec3d.y < 0.0) {
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
        dataTracker.startTracking(FROM_BUCKET, false);
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
        nbt.putBoolean("FromBucket", isFromBucket());
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
        if (nbt.contains("FromBucket", NbtElement.NUMBER_TYPE)) {
            setFromBucket(nbt.getBoolean("FromBucket"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, "fly/idle", 1, state -> {
                    var moving = state.isMoving() || !isOnGround();

                    if (isSongPlaying() && !moving) {
                        return state.setAndContinue(DANCE_ANIMATION);
                    }

                    if (isInSittingPose()) {
                        return state.setAndContinue(SIT);
                    }

                    return state.setAndContinue(moving ? FLY : IDLE);
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

    public static boolean canSpawn(EntityType<OwlEntity> entityType, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        return world.getBlockState(pos.down()).isIn(BlockTags.PARROTS_SPAWNABLE_ON) && isLightLevelValidForNaturalSpawn(world, pos);
    }

    @Override
    public boolean isFromBucket() {
        return dataTracker.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        dataTracker.set(FROM_BUCKET, fromBucket);
    }

    @Override
    public void copyDataToStack(ItemStack stack) {
        var nbt = stack.getOrCreateNbt();
        var entityTag = nbt.getCompound("EntityTag");
        writeCustomDataToNbt(entityTag);
        nbt.put("EntityTag", entityTag);
        if (hasCustomName()) {
            stack.setCustomName(getCustomName());
        }
    }

    @Override
    public void copyDataFromNbt(NbtCompound nbt) {
        readCustomDataFromNbt(nbt.getCompound("EntityTag"));
    }

    @Override
    public ItemStack getBucketItem() {
        return ModItems.OWL_BUCKET.getDefaultStack();
    }

    @Override
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }
}
