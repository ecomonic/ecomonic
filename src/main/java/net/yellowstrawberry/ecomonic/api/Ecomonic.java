package net.yellowstrawberry.ecomonic.api;

import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.api.listener.AccountListener;
import net.yellowstrawberry.ecomonic.api.listener.EcomonicListener;

import java.util.List;
import java.util.UUID;

public interface Ecomonic {
    String getVersion();

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

    void addEventListener(EcomonicListener listener);
    void removeEventListener(EcomonicListener listener);

    void addAccountListener(long id, AccountListener listener);
    void removeAccountListener(long id, AccountListener listener);
}
