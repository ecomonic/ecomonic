package net.yellowstrawberry.ecomonic.api.event;

/**
 * Represents an economic event in the system.
 * <p>
 * This interface defines the structure for events that can occur within the economic system,
 * providing methods to retrieve the event's name and description.
 * </p>
 *
 * @since 0.0.1
 * @version 0.0.1
 */
public interface EcomonicEvent {
    /**
     * Returns the name of the event.
     *
     * @return the name of the event
     */
    String getName();

    /**
     * Returns the description of the event.
     *
     * @return the description of the event
     */
    String getDescription();
}