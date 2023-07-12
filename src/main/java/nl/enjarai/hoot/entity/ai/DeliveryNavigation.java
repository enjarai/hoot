package nl.enjarai.hoot.entity.ai;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DeliveryNavigation {
    public static final Codec<DeliveryNavigation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("source").forGetter(DeliveryNavigation::getSource),
            BlockPos.CODEC.optionalFieldOf("destination").forGetter(DeliveryNavigation::getDestination),
            Codec.INT_STREAM.comapFlatMap(
                    intStream -> {
                        int[] ints = intStream.toArray();
                        return ints.length == 4 ? DataResult.success(Uuids.toUuid(ints)) :
                                DataResult.error(() -> "Invalid UUID: " + Arrays.toString(ints));
                    },
                    uuid -> IntStream.of(Uuids.toIntArray(uuid))
            ).optionalFieldOf("destination_entity_uuid").forGetter(DeliveryNavigation::getDestinationEntityUUID),
            State.CODEC.fieldOf("state").forGetter(DeliveryNavigation::getState)
    ).apply(instance, DeliveryNavigation::new));

    private BlockPos source;
    private BlockPos destination;
    private UUID destinationEntityUUID;
    private State state = State.IDLE;

    public DeliveryNavigation() {
    }

    public DeliveryNavigation(Optional<BlockPos> source, Optional<BlockPos> destination, Optional<UUID> destinationEntityUUID, State state) {
        this.source = source.orElse(null);
        this.destination = destination.orElse(null);
        this.destinationEntityUUID = destinationEntityUUID.orElse(null);
        this.state = state;
    }

    public Optional<BlockPos> getSource() {
        return Optional.ofNullable(source);
    }

    public void setSource(BlockPos source) {
        this.source = source;
    }

    public Optional<BlockPos> getDestination() {
        return Optional.ofNullable(destination);
    }

    public void setDestination(BlockPos destination) {
        this.destination = destination;
    }

    public Optional<UUID> getDestinationEntityUUID() {
        return Optional.ofNullable(destinationEntityUUID);
    }

    public void setDestinationEntityUUID(UUID destinationEntityUUID) {
        this.destinationEntityUUID = destinationEntityUUID;
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
                default -> DataResult.error(() -> "Unknown state: " + string);
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
