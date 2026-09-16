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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
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

    private static TransactionInput transferInput(String sourceSortCode, String sourceAccountNumber,
                                                  String targetSortCode, String targetAccountNumber,
                                                  double amount) {
        var source = new AccountInput();
        source.setSortCode(sourceSortCode);
        source.setAccountNumber(sourceAccountNumber);

        var target = new AccountInput();
        target.setSortCode(targetSortCode);
        target.setAccountNumber(targetAccountNumber);

        var input = new TransactionInput();
        input.setSourceAccount(source);
        input.setTargetAccount(target);
        input.setAmount(amount);
        input.setReference("My reference");
        return input;
    }

    private static TransactionInput validTransferInput(double amount) {
        return transferInput("53-68-92", "78901234", "67-41-18", "48573590", amount);
    }

    @Test
    void whenTransactionDetails_thenTransferShouldBeDenied() {
        boolean isComplete = transactionService.makeTransfer(validTransferInput(50));

        assertThat(isComplete).isTrue();
    }

    @Test
    void whenTransactionDetailsAndAmountTooLarge_thenTransferShouldBeDenied() {
        boolean isComplete = transactionService.makeTransfer(validTransferInput(10000));

        assertThat(isComplete).isFalse();
    }

    @Test
    void whenTransferSucceeds_thenSourceIsDebitedAndTargetIsCredited() {
        transactionService.makeTransfer(validTransferInput(50));

        var captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues()).containsExactly(sourceAccount, targetAccount);
        assertThat(sourceAccount.getCurrentBalance()).isCloseTo(408.1, within(0.0001));
        assertThat(targetAccount.getCurrentBalance()).isCloseTo(114.9, within(0.0001));
    }

    @Test
    void whenTransferSucceeds_thenTransactionIsSavedOnce() {
        transactionService.makeTransfer(validTransferInput(50));

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getAmount()).isEqualTo(50);
        assertThat(saved.getSourceAccountId()).isEqualTo(1L);
        assertThat(saved.getTargetAccountId()).isEqualTo(2L);
        assertThat(saved.getTargetOwnerName()).isEqualTo("Major");
        assertThat(saved.getReference()).isEqualTo("My reference");
    }

    @Test
    void whenTransferDenied_thenNothingIsSaved() {
        boolean isComplete = transactionService.makeTransfer(validTransferInput(10000));

        assertThat(isComplete).isFalse();
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
        assertThat(sourceAccount.getCurrentBalance()).isEqualTo(458.1);
        assertThat(targetAccount.getCurrentBalance()).isEqualTo(64.9);
    }

    @Test
    void whenTransferringExactFullBalance_thenTransferIsAllowed() {
        boolean isComplete = transactionService.makeTransfer(validTransferInput(458.1));

        assertThat(isComplete).isTrue();
        assertThat(sourceAccount.getCurrentBalance()).isCloseTo(0.0, within(0.0001));
        assertThat(targetAccount.getCurrentBalance()).isCloseTo(523.0, within(0.0001));
    }

    @Test
    void whenSourceAccountMissing_thenTransferIsDenied() {
        when(accountRepository.findBySortCodeAndAccountNumber("11-11-11", "11111111"))
                .thenReturn(Optional.empty());

        boolean isComplete = transactionService.makeTransfer(
                transferInput("11-11-11", "11111111", "67-41-18", "48573590", 50));

        assertThat(isComplete).isFalse();
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void whenTargetAccountMissing_thenTransferIsDenied() {
        when(accountRepository.findBySortCodeAndAccountNumber("22-22-22", "22222222"))
                .thenReturn(Optional.empty());

        boolean isComplete = transactionService.makeTransfer(
                transferInput("53-68-92", "78901234", "22-22-22", "22222222", 50));

        assertThat(isComplete).isFalse();
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void isAmountAvailable_boundaries() {
        assertThat(transactionService.isAmountAvailable(50, 458.1)).isTrue();
        assertThat(transactionService.isAmountAvailable(458.1, 458.1)).isTrue();
        assertThat(transactionService.isAmountAvailable(458.11, 458.1)).isFalse();
        assertThat(transactionService.isAmountAvailable(0, 0)).isTrue();
    }

    @Test
    void updateAccountBalance_deposit_increasesBalanceAndSaves() {
        transactionService.updateAccountBalance(targetAccount, 35.1, ACTION.DEPOSIT);

        assertThat(targetAccount.getCurrentBalance()).isCloseTo(100.0, within(0.0001));
        verify(accountRepository).save(targetAccount);
    }

    @Test
    void updateAccountBalance_withdraw_decreasesBalanceAndSaves() {
        transactionService.updateAccountBalance(sourceAccount, 58.1, ACTION.WITHDRAW);

        assertThat(sourceAccount.getCurrentBalance()).isCloseTo(400.0, within(0.0001));
        verify(accountRepository).save(sourceAccount);
    }
}
