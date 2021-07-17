package info.kgeorgiy.ja.panov.bank;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class BankTests {
    private static final int BANK_PORT = 8080;
    private static final String NAME_REGISTRY = "//localhost/bank";
    private static final String NAME = "Leha";
    private static final String SURNAME = "Тагил";
    private static final String PASSPORT = "হ্যালো স্লামালিকুম";
    private static final String SUB_ID = "DEAD_BEEF";
    private static final int AMOUNT = 15;
    private static final int THREADS = 20;
    private static final int ACTIONS_IN_THREAD = 20;
    private static final int AWAIT_TIME_MILLISECONDS = 10_000;

    private static Bank bank;
    private static Registry registry;

    private void assertEqualsPerson(Person a, Person b) throws RemoteException {
        Assert.assertEquals(a.getName(), b.getName());
        Assert.assertEquals(a.getSurname(), b.getSurname());
        Assert.assertEquals(a.getPassport(), b.getPassport());
    }

    private void assertEqualsAccount(Account a, Account b) throws RemoteException {
        Assert.assertEquals(a.getId(), b.getId());
        Assert.assertEquals(a.getAmount(), b.getAmount());
    }

    private Account createDefaultAccount() throws RemoteException {
        Person person = bank.createPerson(NAME, SURNAME, PASSPORT);
        return bank.createAccount(SUB_ID, person);
    }

    @BeforeClass
    public static void beforeClass() throws RemoteException {
        registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    }

    @Before
    public void beforeTest() throws RemoteException {
        registry.rebind(NAME_REGISTRY, new RemoteBank(BANK_PORT));
        try {
            bank = (Bank) registry.lookup(NAME_REGISTRY);
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
        }
    }

    @Test
    public void test_personNotFound() throws RemoteException {
        Assert.assertNull(bank.getLocalPerson(PASSPORT));
        Assert.assertNull(bank.getRemotePerson(PASSPORT));
    }

    @Test
    public void test_createPerson() throws RemoteException {
        Person person = bank.createPerson(NAME, SURNAME, PASSPORT);
        assertEqualsPerson(person, bank.getRemotePerson(PASSPORT));
        assertEqualsPerson(person, bank.getLocalPerson(PASSPORT));
    }

    @Test
    public void test_createAccount() throws RemoteException {
        Person person = bank.createPerson(NAME, SURNAME, PASSPORT);
        Account account = bank.createAccount(SUB_ID, person);

        assertEqualsAccount(account, bank.getAccount(PASSPORT + ":" + SUB_ID));
    }

    @Test
    public void test_amountSet() throws RemoteException {
        Person person = bank.createPerson(NAME, SURNAME, PASSPORT);
        Account account = bank.createAccount(SUB_ID, person);
        account.setAmount(AMOUNT);

        Assert.assertEquals(AMOUNT, account.getAmount());
    }

    @Test
    public void test_localAccountAmountSet() throws RemoteException {
        Person person = bank.createPerson(NAME, SURNAME, PASSPORT);
        Account account = bank.createAccount(SUB_ID, person);
        Person localPerson = bank.getLocalPerson(person.getPassport());
        Account localAccount = localPerson.getAccount(SUB_ID);

        localAccount.setAmount(AMOUNT);
        Account remoteAccount = bank.getAccount(account.getId());

        Assert.assertEquals(localAccount.getId(), remoteAccount.getId());
        Assert.assertNotEquals(localAccount.getAmount(), remoteAccount.getAmount());
    }

    @Test
    public void test_remoteAccountAmountSet() throws RemoteException {
        Person person = bank.createPerson(NAME, SURNAME, PASSPORT);
        bank.createAccount(SUB_ID, person);
        Account localAccount = bank.getLocalPerson(person.getPassport()).getAccount(SUB_ID);
        Person remotePerson = bank.getRemotePerson(person.getPassport());
        Account remoteAccount = remotePerson.getAccount(SUB_ID);

        remoteAccount.setAmount(AMOUNT);

        Assert.assertEquals(localAccount.getId(), remoteAccount.getId());
        Assert.assertNotEquals(localAccount.getAmount(), remoteAccount.getAmount());
        Assert.assertEquals(localAccount.getAmount(), 0);
        Assert.assertEquals(remoteAccount.getAmount(), AMOUNT);
    }

    @Test
    public void test_createManyPersons() throws RemoteException {
        for (int personId = 0; personId < 100; personId++) {
            final String curPassport = PASSPORT + personId;
            Person person = bank.createPerson(NAME, SURNAME, curPassport);
            assertEqualsPerson(person, bank.getRemotePerson(curPassport));
            assertEqualsPerson(person, bank.getLocalPerson(curPassport));
        }
    }

    @Test
    public void test_manyTransaction() throws RemoteException {
        Account account = createDefaultAccount();
        for (int transaction = 0; transaction < 100; transaction++) {
            account.setAmount(transaction);
            Assert.assertEquals(account.getAmount(), transaction);
        }
    }

    private interface RunnableRemote {
        void run(int threadNum, int actinoNum) throws RemoteException;
    }

    private void parallelTest(final int threads, final int actionInThread, final RunnableRemote task) throws RemoteException {
        final AtomicReference<RemoteException> atomicException = new AtomicReference<>(null);
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach(threadNum -> {
            pool.submit(() -> {
                for (int j = 0; j < actionInThread; j++) {
                    try {
                        task.run(threadNum, j);
                    } catch (RemoteException e) {
                        if (!atomicException.compareAndSet(null, e)) {
                            atomicException.get().addSuppressed(e);
                        }
                    }
                }
            });
        });

        pool.shutdown();
        try {
            if (!pool.awaitTermination(AWAIT_TIME_MILLISECONDS, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }

        final RemoteException exception = atomicException.get();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test_parallelAdd() throws RemoteException {
        final Account account = createDefaultAccount();
        parallelTest(THREADS, ACTIONS_IN_THREAD, (a, b) -> account.addAmount(AMOUNT));

        Assert.assertEquals(account.getAmount(), THREADS * ACTIONS_IN_THREAD * AMOUNT);
    }

    @Test
    public void test_parallelCreatePersons() throws RemoteException {
        parallelTest(THREADS, ACTIONS_IN_THREAD, (threadNum, actionNum) -> {
            final String name = Integer.toString(threadNum);
            final String surname = Integer.toString(actionNum);
            bank.createPerson(name, surname, name + "_" + surname);
        });

        for (int threadNum = 0; threadNum < THREADS; threadNum++) {
            for (int actionNum = 0; actionNum < ACTIONS_IN_THREAD; actionNum++) {
                final String name = Integer.toString(threadNum);
                final String surname = Integer.toString(threadNum);
                final Person person = bank.getRemotePerson(name + "_" + surname);
                Assert.assertNotNull(person);
                Assert.assertEquals(name, person.getName());
                Assert.assertEquals(surname, person.getSurname());
            }
        }
    }

    @Test
    public void test_client() throws RemoteException {
        Client.main(NAME, SURNAME, PASSPORT, SUB_ID, Integer.toString(AMOUNT));
        Person person = bank.getRemotePerson(PASSPORT);
        Account account = person.getAccount(SUB_ID);
        Assert.assertEquals(AMOUNT, account.getAmount());
        Assert.assertEquals(account.getId(), PASSPORT + ":" + SUB_ID);
        Assert.assertEquals(person.getName(), NAME);
        Assert.assertEquals(person.getSurname(), SURNAME);
        Assert.assertEquals(person.getPassport(), PASSPORT);
    }

    public static void main(String[] args) {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new TextListener(System.out));
        junit.run(BankTests.class);
        System.exit(0);
    }
}
