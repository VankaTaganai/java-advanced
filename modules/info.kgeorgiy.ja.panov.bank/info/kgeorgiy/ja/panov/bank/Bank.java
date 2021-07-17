package info.kgeorgiy.ja.panov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    Account createAccount(String subId, Person person) throws RemoteException;

    Person createPerson(String name, String surname, String passport) throws RemoteException;

    Account getAccount(String id) throws RemoteException;

    Person getLocalPerson(String passport) throws RemoteException;

    Person getRemotePerson(String passport) throws RemoteException;

    default Person createPersonIfAbsent(final String name, final String surname, final String passport)
            throws RemoteException {
        final Person person = getRemotePerson(passport);
        if (person == null) {
            return createPerson(name, surname, passport);
        } else {
            return person;
        }
    }
}
