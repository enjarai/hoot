package nl.enjarai.hoot.entity.ai;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

public class DeliveryNavigation {
    public static final Codec<DeliveryNavigation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("source").forGetter(DeliveryNavigation::getSource),
            BlockPos.CODEC.fieldOf("destination").forGetter(DeliveryNavigation::getDestination),
            State.CODEC.fieldOf("state").forGetter(DeliveryNavigation::getState)
    ).apply(instance, DeliveryNavigation::new));

    private BlockPos source;
    private BlockPos destination;
    private State state = State.IDLE;

    public DeliveryNavigation() {
    }

    public DeliveryNavigation(BlockPos source, BlockPos destination, State state) {
        this.source = source;
        this.destination = destination;
        this.state = state;
    }

    public BlockPos getSource() {
        return source;
    }

    public void setSource(BlockPos source) {
        this.source = source;
    }

    public BlockPos getDestination() {
        return destination;
    }

    public void setDestination(BlockPos destination) {
        this.destination = destination;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public enum State {
        IDLE,
        DELIVERING,
        RETURNING;

        public static final Codec<State> CODEC = Codec.STRING.comapFlatMap(State::fromString, State::toString);

        public static DataResult<State> fromString(String string) {
            return switch (string) {
                case "idle" -> DataResult.success(IDLE);
                case "delivering" -> DataResult.success(DELIVERING);
                case "returning" -> DataResult.success(RETURNING);
                default -> DataResult.error("Unknown state: " + string);
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case IDLE -> "idle";
                case DELIVERING -> "delivering";
                case RETURNING -> "returning";
            };
        }
    }
}
