package net.yellowstrawberry.ecomonic.api.account;

import java.util.UUID;

/**
 * Represents a server account (Owner-less) in the economic system.
 * <p>
 * This class extends the {@link Account} class and is associated with a server itself.
 * </p>
 *
 * @since 0.0.1
 * @version 0.0.1
 */
public class ServerAccount extends Account {
    public ServerAccount(Long id) {
        super(id);
    }

    @Override
    public UUID getOwner() {
        return null;
    }
}
