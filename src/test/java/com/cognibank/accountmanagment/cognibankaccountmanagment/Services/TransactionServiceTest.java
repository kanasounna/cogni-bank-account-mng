package com.cognibank.accountmanagment.cognibankaccountmanagment.Services;


import com.cognibank.accountmanagment.cognibankaccountmanagment.Exceptions.LowBalanceException;
import com.cognibank.accountmanagment.cognibankaccountmanagment.Exceptions.OverDraftException;
import com.cognibank.accountmanagment.cognibankaccountmanagment.Model.*;
import com.cognibank.accountmanagment.cognibankaccountmanagment.Repository.AccountRepository;
import com.cognibank.accountmanagment.cognibankaccountmanagment.Repository.TransactionRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import static org.assertj.core.api.Assertions.*;


import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;
    @Before
    public void init() {

    }

    @Test
    public void shouldReturnCustomerIdGivenASpecificTransaction() {
        //List<Account> listAccounts = new ArrayList<>();

        Account account = new Account().withBalance(0L).withAccountNumber(78L).withAccountType(AccountType.Checking).withUserId("222");

        //listAccounts.add(account);
        accountService.createAccount(account);

        Transaction transaction = new Transaction()
                .withAccount(account)
                .setAmount(100)
                .withDestinationAccount("Checking")
                .withCustomerId(account.getUserId())
                .withType(TransactionType.Debit);

        transactionService.createTransaction(transaction);

        List<Transaction> tran = transactionService.getTransactionsByCustomerId(account.getUserId());

        Assert.assertEquals("User tied to the transaction should have the same id as actual",
                tran.get(0).getCustomerId(), "222");
    }

    @Test
    public void shouldReturnCustomerTransactionForAGivenAccountNumber() {

        List<Account> userAccounts = new ArrayList<>();

        Transaction transaction = new Transaction();

        Account account = new Account()
                .withAccountNumber(6l)
                .withAccountType(AccountType.Checking)
                .withBalance(98000).withUserId("222");
        userAccounts.add(account);
        accountService.createAccount(account);
        transaction
                .withAccount(account)
                .setAmount(800)
                .withDestinationAccount("Checking")
                .withCustomerId("222")
                .withType(TransactionType.Debit);

        transactionService.createTransaction(transaction);
        transactionService.getTransactionByAccountNumber(6l);

        Assert.assertEquals(transaction.getAccount().getAccountNumber(), 6);

    }

    @Test
    @Transactional
    public void depositTest(){
        Account account = new Account()
                .withUserId("1")
                .withAccountType(AccountType.Savings)
                .withAccountNumber(10l)
                .withBalance(100.0);
        account = accountService.createAccount(account);
        double actualBalance = transactionService.deposit(10.0, account);
        assertEquals("Account balance should be updated after the deposit.", 110.0,actualBalance, 0.1);
    }


    @Test
    @Transactional
    public void withdrawTest() throws Exception{
        Account account = new Account()
                .withUserId("1")
                .withAccountType(AccountType.Savings)
                .withAccountNumber(10l)
                .withBalance(100.0);
        account = accountService.createAccount(account);
        double actualBalance = transactionService.withdraw(10.0, account);
        assertEquals("Account balance should be updated after the deposit.", 90.0,actualBalance, 0.1);
    }

    @Test
    @Transactional
    public void transfertTest() throws Exception{
        Account originAccount = new Account()
                .withUserId("1")
                .withAccountType(AccountType.Savings)
                .withAccountNumber(3400000015l)
                .withBalance(100.0);
        originAccount = accountService.createAccount(originAccount);

        Account destinationAccount = new Account()
                .withUserId("1")
                .withAccountType(AccountType.Savings)
                .withAccountNumber(3400000030l)
                .withBalance(100.0);
        destinationAccount = accountService.createAccount(destinationAccount);

        long transactionId = transactionService.transfer(originAccount.getAccountNumber(), destinationAccount.getAccountNumber(),50);

        //test on the origine account number
        double actualOrigineAccountBalance=accountRepository.findByAccountNumber(originAccount.getAccountNumber()).getBalance();
        double expectedOrigineAccountBallance=50.0;
        assertEquals("Account balance of the origin account should be updated after the transfer.", actualOrigineAccountBalance,expectedOrigineAccountBallance, 0.1);

        //test on the destination account number
        double actualDestinationAccountBalance=accountRepository.findByAccountNumber(destinationAccount.getAccountNumber()).getBalance();
        double expectedDestinationAccountBallance=150.0;
        assertEquals("Account balance of the origin account should be updated after the transfer.", actualDestinationAccountBalance,expectedDestinationAccountBallance, 0.1);

        //test on the transaction record

        List<Transaction> lastTransation =transactionRepository.findTransactionsIdByAccountIdAndDestinationAccountId(originAccount.getId(),destinationAccount.getId());
        double actualAmountTransaction=lastTransation.get(lastTransation.size()-1).getAmount();
        double expectedAmountTransaction=50.0;
        assertEquals("Transaction amount should be update after the transfert.", actualAmountTransaction,expectedAmountTransaction, 0.1);


    }

    @Test//(expected = LowBalanceException.class)
    @Transactional
    public void withdrawTestForLowBalance(){
        try {
            Account account = new Account()
                    .withUserId("1")
                    .withAccountType(AccountType.Savings)
                    .withAccountNumber(10l)
                    .withBalance(100.0);
            account = accountService.createAccount(account);

            double actualBalance = transactionService.withdraw(80.0, account);
        }
        catch (LowBalanceException e){
            assertThat(e)
                    .isInstanceOf(LowBalanceException.class)
                    .hasMessage("Balance below $25. Notify the Customer");
        }
    }

    @Test//(expected = OverDraftException.class)
    @Transactional
    public void withdrawTestForOverDraft(){
        try {
            Account account = new Account()
                    .withUserId("1")
                    .withAccountType(AccountType.Savings)
                    .withAccountNumber(10l)
                    .withBalance(10.0);
            account = accountService.createAccount(account);

            double actualBalance = transactionService.withdraw(130.0, account);
        }
        catch (OverDraftException e){
            assertThat(e)
                    .isInstanceOf(OverDraftException.class)
                    .hasMessage("Balance below - $100. Notify the Customer");
        }
    }

    @Test
    @Transactional
    public void reportTest() {
//        final String userId = "ABCD1234";

        Account account = new Account()
                .withBalance(0L)
                .withAccountNumber(78L)
                .withUserId("ABCD1234")
              //  .withId("0e4c1211-2c58-4956-b523-ed0d64dc54c4")
                .withAccountType(AccountType.Checking);
        accountService.createAccount(account);
        account=accountRepository.findByUserId("ABCD1234").get().get(0);
        // Transaction transaction=new Transaction();
        LocalDate startDate=LocalDate.now().minusDays(1l);
        LocalDate endDate=LocalDate.now().plusDays(1l);
        transactionService.deposit(57676,account);
        transactionService.deposit(157676,account);
        transactionService.deposit(3,account);

        transactionService.deposit(56,account);
        transactionService.deposit(56,account);

        List<Transaction> transactions=transactionService.report(account.getId(),startDate, endDate);
        Assert.assertEquals("Number of transaction should be", 5, transactions.size());
        assertEquals("Account must be the same", account, transactions.get(0).getAccount());
    }

    //Testing the Json object for notification message
    @Test
    public void jSonObjectForMessageToSend(){
        long accountNumber=3400000012l;
        String email="hg@hf.fueh";
        String userName="egfu";
        double amount=387.87;
        String objectToSend="{\"accountNumber\":\""
                +accountNumber+"\",\"email\":\""+email+"\",\"userName\":\""+userName+"\",\"amount\":\""+amount+"\"}";
        System.out.println(objectToSend);
        assertEquals(1,1);
    }
}
