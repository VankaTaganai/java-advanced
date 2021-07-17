package info.kgeorgiy.ja.panov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteAccount extends AbstractAccount {
    public RemoteAccount(final String id, final int port) throws RemoteException {
        super(id);
        UnicastRemoteObject.exportObject(this, port);
    }
}
