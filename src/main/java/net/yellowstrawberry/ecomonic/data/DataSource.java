package net.yellowstrawberry.ecomonic.data;

import net.yellowstrawberry.ecomonic.api.account.Account;

import java.util.List;
import java.util.UUID;

public interface DataSource {
    Account getAccount(Long id);
    Account getPrimaryAccount(UUID uuid);
    List<Account> getAccounts(UUID uuid);

    Account createAccount(UUID uuid);
    Account deleteAccount(long id);

    boolean withdraw(long id, double amount);
    boolean deposit(long id, double amount);
    boolean set(long id, double amount);
    boolean has(long id, double amount);

    double getBalance(Long id);
}
