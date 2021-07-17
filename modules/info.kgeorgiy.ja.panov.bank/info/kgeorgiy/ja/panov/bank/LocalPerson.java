package info.kgeorgiy.ja.panov.bank;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LocalPerson extends AbstractPerson implements Serializable {
    public LocalPerson(String name, String surname, String passport) {
        super(name, surname, passport, new HashMap<>());
    }

    public LocalPerson(String name, String surname, String passport, Map<String, Account> accounts) {
        super(name, surname, passport, accounts);
    }
}
