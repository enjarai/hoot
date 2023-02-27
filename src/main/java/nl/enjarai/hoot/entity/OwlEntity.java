package nl.enjarai.hoot.entity;

import com.mojang.serialization.Dynamic;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.VariantHolder;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import nl.enjarai.hoot.registry.ModRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class OwlEntity extends TameableEntity implements GeoEntity, VariantHolder<OwlVariant> {
    private static final TrackedData<OwlVariant> VARIANT = 
            DataTracker.registerData(OwlEntity.class, ModRegistries.OWL_VARIANT_DATA);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    protected OwlEntity(EntityType<? extends OwlEntity> entityType, World world) {
        super(entityType, world);
    }

    protected Brain.Profile<OwlEntity> createBrainProfile() {
        return Brain.createProfile(OwlBrain.MEMORY_MODULES, OwlBrain.SENSORS);
    }

    @Override
    protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        return OwlBrain.create(this.createBrainProfile().deserialize(dynamic));
    }

    @SuppressWarnings("unchecked")
    public Brain<OwlEntity> getBrain() {
        return (Brain<OwlEntity>) super.getBrain();
    }

    public static DefaultAttributeContainer.Builder createOwlAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0f)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6f)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2f);
    }

    @Override
    public void tickMovement() {
        super.tickMovement();

        if (!this.world.isClient && this.isAlive() && this.age % 10 == 0) {
            this.heal(1.0f);
        }
    }

    @Override
    public void travel(Vec3d movementInput) {
        super.travel(movementInput);

        var flying = false;
        if (flying) {
            if (this.canMoveVoluntarily() || this.isLogicalSideForUpdatingMovement()) {
                if (this.isTouchingWater()) {
                    this.updateVelocity(0.02f, movementInput);
                    this.move(MovementType.SELF, this.getVelocity());
                    this.setVelocity(this.getVelocity().multiply(0.8f));
                } else if (this.isInLava()) {
                    this.updateVelocity(0.02f, movementInput);
                    this.move(MovementType.SELF, this.getVelocity());
                    this.setVelocity(this.getVelocity().multiply(0.5));
                } else {
                    float f = 0.91f;
                    if (this.onGround) {
                        f = this.world.getBlockState(new BlockPos(this.getX(), this.getY() - 1.0, this.getZ())).getBlock().getSlipperiness() * 0.91f;
                    }
                    float g = 0.16277137f / (f * f * f);
                    f = 0.91f;
                    if (this.onGround) {
                        f = this.world.getBlockState(new BlockPos(this.getX(), this.getY() - 1.0, this.getZ())).getBlock().getSlipperiness() * 0.91f;
                    }
                    this.updateVelocity(this.onGround ? 0.1f * g : this.getMovementSpeed(), movementInput);
                    this.move(MovementType.SELF, this.getVelocity());
                    this.setVelocity(this.getVelocity().multiply(0.91f));
                }
            }
        }
    }

    @Override
    protected void mobTick() {
        world.getProfiler().push("owlBrain");
        getBrain().tick((ServerWorld) world, this);
        world.getProfiler().swap("owlActivityUpdate");
        OwlBrain.updateActivity(this);
        world.getProfiler().pop();
        super.mobTick();
    }

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
    public void setVariant(OwlVariant catVariant) {
        dataTracker.set(VARIANT, catVariant);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(VARIANT, ModRegistries.OWL_VARIANT.getOrThrow(OwlVariant.WOOD_OWL));
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("variant", ModRegistries.OWL_VARIANT.getId(getVariant()).toString());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        OwlVariant variant = ModRegistries.OWL_VARIANT.get(Identifier.tryParse(nbt.getString("variant")));
        if (variant != null) {
            setVariant(variant);
        }
    }

    private <E extends GeoAnimatable> PlayState handleAnimationState(AnimationState<E> event) {
        if (event.isMoving() || !onGround) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("move.fly"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("misc.idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
//                DefaultAnimations.genericFlyIdleController(this)
                new AnimationController<>(this, "controller", 0, this::handleAnimationState)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
