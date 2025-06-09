package net.yellowstrawberry.ecomonic.api.account;

import java.util.UUID;

/**
 * Represents a player account in the economic system.
 * <p>
 * This class extends the {@link Account} class and is associated with a specific player identified by their UUID.
 * It provides methods to manage the player's financial account, including depositing and withdrawing money.
 * </p>
 *
 * @since 0.0.1
 * @version 0.0.1
 */
public class PlayerAccount extends Account {

    private final UUID owner;

    public PlayerAccount(UUID owner, Long id) {
        super(id);
        this.owner = owner;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }
}
