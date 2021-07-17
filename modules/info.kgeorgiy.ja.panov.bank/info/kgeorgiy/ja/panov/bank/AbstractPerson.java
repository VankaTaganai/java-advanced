package info.kgeorgiy.ja.panov.bank;

import java.util.Collections;
import java.util.Map;

public abstract class AbstractPerson implements Person {
    private final String name;
    private final String surname;
    private final String passport;
    private final Map<String, Account> accounts;

    public AbstractPerson(String name, String surname, String passport, Map<String, Account> accounts) {
        this.name = name;
        this.surname = surname;
        this.passport = passport;
        this.accounts = accounts;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public String getPassport() {
        return passport;
    }

    @Override
    public Account getAccount(final String subId) {
        return accounts.get(subId);
    }

    @Override
    public Map<String, Account> getAccounts() {
        return Collections.unmodifiableMap(accounts);
    }

    @Override
    public synchronized Account addAccount(final String subId, final Account account) {
        return accounts.put(subId, account);
    }
}
