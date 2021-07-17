package info.kgeorgiy.ja.panov.bank;

import java.util.concurrent.ConcurrentHashMap;

public class RemotePerson extends AbstractPerson {
    public RemotePerson(String name, String surname, String passport) {
        super(name, surname, passport, new ConcurrentHashMap<>());
    }
}
