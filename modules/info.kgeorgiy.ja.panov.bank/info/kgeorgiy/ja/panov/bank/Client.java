package info.kgeorgiy.ja.panov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.Objects;

public final class Client {
    public static void main(final String... args) throws RemoteException {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong arguments format=[name surname passport subId diffAmount]");
            return;
        }

        final int diffAmount;
        try {
            diffAmount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("Difference of invoice amount should be integer=[" + e.getMessage() + "]");
            return;
        }

        final Bank bank;
        try {
            bank = (Bank) LocateRegistry.getRegistry().lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        }

        final String name = args[0];
        final String surname = args[1];
        final String passport = args[2];
        final String subId = args[3];

        final Person person = bank.createPersonIfAbsent(name, surname, passport);
        final Account account = bank.createAccount(subId, person);
        account.setAmount(account.getAmount() + diffAmount);
        System.out.println("Amount after transaction: " + account.getAmount());
    }
}
