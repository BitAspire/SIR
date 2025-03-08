package me.croabeast.sir.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a base key that has a unique name and provides a UUID based on that name.
 * <p>
 * This interface ensures that any implementing class will have a method to retrieve a name and
 * a default method to generate a UUID from that name.
 */
public interface BaseKey {

    /**
     * Gets the unique name associated with this key.
     * <p>
     * The name should be a unique identifier that can be used to distinguish this key
     * from other keys.
     *
     * @return The unique name associated with this key.
     */
    @NotNull
    String getName();

    /**
     * Generates a UUID based on the name of this key.
     * <p>
     * This default method uses {@link UUID#nameUUIDFromBytes(byte[])} to create a UUID
     * from the byte representation of the name. This ensures that the same name will always
     * produce the same UUID.
     *
     * @return A UUID generated from the name of this key.
     */
    @NotNull
    default UUID getUuid() {
        return UUID.nameUUIDFromBytes(getName().getBytes());
    }
}
