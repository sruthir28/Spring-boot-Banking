package com.example.paul.unit;

import com.example.paul.constants.ACTION;
import com.example.paul.models.Account;
import com.example.paul.models.Transaction;
import com.example.paul.repositories.AccountRepository;
import com.example.paul.repositories.TransactionRepository;
import com.example.paul.services.TransactionService;
import com.example.paul.utils.AccountInput;
import com.example.paul.utils.TransactionInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class TransactionServiceTest {

    @TestConfiguration
    static class TransactionServiceTestContextConfiguration {

        @Bean
        public TransactionService transactionService() {
            return new TransactionService();
        }
    }

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = new Account(1L, "53-68-92", "78901234", 458.1, "Some Bank", "John");
        targetAccount = new Account(2L, "67-41-18", "48573590", 64.9, "Some Other Bank", "Major");

        when(accountRepository.findBySortCodeAndAccountNumber("53-68-92", "78901234"))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findBySortCodeAndAccountNumber("67-41-18", "48573590"))
                .thenReturn(Optional.of(targetAccount));
    }

    @Test
    void whenTransactionDetails_thenTransferShouldBeDenied() {
        var sourceAccount = new AccountInput();
        sourceAccount.setSortCode("53-68-92");
        sourceAccount.setAccountNumber("78901234");

        var targetAccount = new AccountInput();
        targetAccount.setSortCode("67-41-18");
        targetAccount.setAccountNumber("48573590");

        var input = new TransactionInput();
        input.setSourceAccount(sourceAccount);
        input.setTargetAccount(targetAccount);
        input.setAmount(50);
        input.setReference("My reference");

        boolean isComplete = transactionService.makeTransfer(input);

        assertThat(isComplete).isTrue();
    }

    @Test
    void whenTransactionDetailsAndAmountTooLarge_thenTransferShouldBeDenied() {
        var sourceAccount = new AccountInput();
        sourceAccount.setSortCode("53-68-92");
        sourceAccount.setAccountNumber("78901234");

        var targetAccount = new AccountInput();
        targetAccount.setSortCode("67-41-18");
        targetAccount.setAccountNumber("48573590");

        var input = new TransactionInput();
        input.setSourceAccount(sourceAccount);
        input.setTargetAccount(targetAccount);
        input.setAmount(10000);
        input.setReference("My reference");

        boolean isComplete = transactionService.makeTransfer(input);

        assertThat(isComplete).isFalse();
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void whenTransferSucceeds_thenSourceDebitedAndTargetCredited() {
        boolean isComplete = transactionService.makeTransfer(buildInput("53-68-92", "78901234", "67-41-18", "48573590", 50));

        assertThat(isComplete).isTrue();

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(captor.capture());
        List<Account> saved = captor.getAllValues();

        assertThat(saved.get(0).getId()).isEqualTo(1L);
        assertThat(saved.get(0).getCurrentBalance()).isCloseTo(408.1, within(0.0001));
        assertThat(saved.get(1).getId()).isEqualTo(2L);
        assertThat(saved.get(1).getCurrentBalance()).isCloseTo(114.9, within(0.0001));

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void whenAmountEqualsBalance_thenTransferSucceeds() {
        boolean isComplete = transactionService.makeTransfer(buildInput("53-68-92", "78901234", "67-41-18", "48573590", 458.1));

        assertThat(isComplete).isTrue();
        assertThat(sourceAccount.getCurrentBalance()).isCloseTo(0.0, within(0.0001));
        assertThat(targetAccount.getCurrentBalance()).isCloseTo(523.0, within(0.0001));
    }

    @Test
    void isAmountAvailable_boundary() {
        assertThat(transactionService.isAmountAvailable(100.0, 100.0)).isTrue();
        assertThat(transactionService.isAmountAvailable(100.01, 100.0)).isFalse();
        assertThat(transactionService.isAmountAvailable(50.0, 100.0)).isTrue();
    }

    @Test
    void updateAccountBalance_depositIncreasesBalance() {
        var account = new Account(3L, "11-22-33", "12345678", 100.0, "Bank", "Owner");

        transactionService.updateAccountBalance(account, 25.5, ACTION.DEPOSIT);

        assertThat(account.getCurrentBalance()).isCloseTo(125.5, within(0.0001));
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void whenSourceAccountMissing_thenTransferDenied() {
        when(accountRepository.findBySortCodeAndAccountNumber("00-00-00", "00000000"))
                .thenReturn(Optional.empty());

        boolean isComplete = transactionService.makeTransfer(buildInput("00-00-00", "00000000", "67-41-18", "48573590", 10));

        assertThat(isComplete).isFalse();
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void whenTargetAccountMissing_thenTransferDenied() {
        when(accountRepository.findBySortCodeAndAccountNumber("00-00-00", "00000000"))
                .thenReturn(Optional.empty());

        boolean isComplete = transactionService.makeTransfer(buildInput("53-68-92", "78901234", "00-00-00", "00000000", 10));

        assertThat(isComplete).isFalse();
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    private static TransactionInput buildInput(String srcSort, String srcNum, String tgtSort, String tgtNum, double amount) {
        var source = new AccountInput();
        source.setSortCode(srcSort);
        source.setAccountNumber(srcNum);

        var target = new AccountInput();
        target.setSortCode(tgtSort);
        target.setAccountNumber(tgtNum);

        var input = new TransactionInput();
        input.setSourceAccount(source);
        input.setTargetAccount(target);
        input.setAmount(amount);
        input.setReference("My reference");
        return input;
    }
}
