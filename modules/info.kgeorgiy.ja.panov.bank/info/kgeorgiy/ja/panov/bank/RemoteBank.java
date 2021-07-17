package info.kgeorgiy.ja.panov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) throws RemoteException {
        this.port = port;
        UnicastRemoteObject.exportObject(this, port);
    }

    @Override
    public Account createAccount(final String subId, final Person person) throws RemoteException {
        final Person bankPerson = createPersonIfAbsent(person.getName(), person.getSurname(), person.getPassport());
        final String key = person.getPassport() + ":" + subId;
        final Account account = createAccount(key);

        bankPerson.addAccount(subId, account);
        return account;
    }

    private Account createAccount(final String id) throws RemoteException {
        final Account account = new RemoteAccount(id, port);
        if (accounts.putIfAbsent(id, account) == null) {
            return account;
        } else {
            return getAccount(id);
        }
    }

    @Override
    public Person createPerson(final String name, final String surname, final String passport) throws RemoteException {
        final Person person = new RemotePerson(name, surname, passport);
        if (persons.putIfAbsent(passport, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePerson(passport);
        }
    }

    @Override
    public Account getAccount(final String id) {
        return accounts.get(id);
    }

    @Override
    public Person getLocalPerson(final String passport) throws RemoteException {
        Person person = persons.get(passport);
        if (Objects.isNull(person)) {
            return person;
        }

        final Map<String, Account> personsAccounts = person.getAccounts();
        final Map<String, Account> accountsCopy = new HashMap<>();
        for (final String key : personsAccounts.keySet()) {
            final Account account = personsAccounts.get(key);
            accountsCopy.put(key, new LocalAccount(account.getId(), account.getAmount()));
        }

        return new LocalPerson(person.getName(), person.getSurname(), person.getPassport(), accountsCopy);
    }

    @Override
    public Person getRemotePerson(final String passport) {
        return persons.get(passport);
    }
}
