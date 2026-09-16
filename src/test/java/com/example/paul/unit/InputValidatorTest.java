package com.example.paul.unit;

import com.example.paul.utils.AccountInput;
import com.example.paul.utils.CreateAccountInput;
import com.example.paul.utils.InputValidator;
import com.example.paul.utils.TransactionInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidatorTest {

    private static final String VALID_SORT_CODE = "53-68-92";
    private static final String VALID_ACCOUNT_NUMBER = "78901234";

    private static AccountInput accountInput(String sortCode, String accountNumber) {
        AccountInput input = new AccountInput();
        input.setSortCode(sortCode);
        input.setAccountNumber(accountNumber);
        return input;
    }

    private static CreateAccountInput createAccountInput(String bankName, String ownerName) {
        CreateAccountInput input = new CreateAccountInput();
        input.setBankName(bankName);
        input.setOwnerName(ownerName);
        return input;
    }

    private static TransactionInput transactionInput(AccountInput source, AccountInput target) {
        TransactionInput input = new TransactionInput();
        input.setSourceAccount(source);
        input.setTargetAccount(target);
        input.setAmount(10.0);
        return input;
    }

    // isSearchCriteriaValid

    @Test
    void givenValidSortCodeAndAccountNumber_whenIsSearchCriteriaValid_thenTrue() {
        assertTrue(InputValidator.isSearchCriteriaValid(accountInput(VALID_SORT_CODE, VALID_ACCOUNT_NUMBER)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "53-68", "536892", "53-68-9", "53-68-921", "5a-68-92", "53_68_92", "53-68-92-", " 53-68-92", "53-68-92 "})
    void givenMalformedSortCode_whenIsSearchCriteriaValid_thenFalse(String sortCode) {
        assertFalse(InputValidator.isSearchCriteriaValid(accountInput(sortCode, VALID_ACCOUNT_NUMBER)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "1234567", "123456789", "1234567a", "12-34-56-78", " 12345678", "12345678 "})
    void givenMalformedAccountNumber_whenIsSearchCriteriaValid_thenFalse(String accountNumber) {
        assertFalse(InputValidator.isSearchCriteriaValid(accountInput(VALID_SORT_CODE, accountNumber)));
    }

    // isAccountNoValid

    @ParameterizedTest
    @ValueSource(strings = {"00000000", "12345678", "99999999"})
    void givenEightDigitAccountNumber_whenIsAccountNoValid_thenTrue(String accountNo) {
        assertTrue(InputValidator.isAccountNoValid(accountNo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "1234567", "123456789", "1234567a", "abcdefgh", "1234 5678", "-12345678"})
    void givenNonEightDigitAccountNumber_whenIsAccountNoValid_thenFalse(String accountNo) {
        assertFalse(InputValidator.isAccountNoValid(accountNo));
    }

    // isCreateAccountCriteriaValid

    @Test
    void givenBankAndOwnerName_whenIsCreateAccountCriteriaValid_thenTrue() {
        assertTrue(InputValidator.isCreateAccountCriteriaValid(createAccountInput("Some Bank", "John")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void givenBlankBankName_whenIsCreateAccountCriteriaValid_thenFalse(String bankName) {
        assertFalse(InputValidator.isCreateAccountCriteriaValid(createAccountInput(bankName, "John")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void givenBlankOwnerName_whenIsCreateAccountCriteriaValid_thenFalse(String ownerName) {
        assertFalse(InputValidator.isCreateAccountCriteriaValid(createAccountInput("Some Bank", ownerName)));
    }

    @Test
    void givenBothNamesBlank_whenIsCreateAccountCriteriaValid_thenFalse() {
        assertFalse(InputValidator.isCreateAccountCriteriaValid(createAccountInput("", "")));
    }

    // isSearchTransactionValid

    @Test
    void givenValidDistinctAccounts_whenIsSearchTransactionValid_thenTrue() {
        TransactionInput input = transactionInput(
                accountInput(VALID_SORT_CODE, VALID_ACCOUNT_NUMBER),
                accountInput("65-93-37", "21956204"));
        assertTrue(InputValidator.isSearchTransactionValid(input));
    }

    @Test
    void givenInvalidSourceAccount_whenIsSearchTransactionValid_thenFalse() {
        TransactionInput input = transactionInput(
                accountInput("53-68", VALID_ACCOUNT_NUMBER),
                accountInput("65-93-37", "21956204"));
        assertFalse(InputValidator.isSearchTransactionValid(input));
    }

    @Test
    void givenInvalidTargetAccount_whenIsSearchTransactionValid_thenFalse() {
        TransactionInput input = transactionInput(
                accountInput(VALID_SORT_CODE, VALID_ACCOUNT_NUMBER),
                accountInput("65-93-37", "2195620"));
        assertFalse(InputValidator.isSearchTransactionValid(input));
    }

    @Test
    void givenSameSourceAndTarget_whenIsSearchTransactionValid_thenFalse() {
        TransactionInput input = transactionInput(
                accountInput(VALID_SORT_CODE, VALID_ACCOUNT_NUMBER),
                accountInput(VALID_SORT_CODE, VALID_ACCOUNT_NUMBER));
        assertFalse(InputValidator.isSearchTransactionValid(input));
    }

    @Test
    void givenSameSortCodeDifferentAccountNumber_whenIsSearchTransactionValid_thenTrue() {
        TransactionInput input = transactionInput(
                accountInput(VALID_SORT_CODE, VALID_ACCOUNT_NUMBER),
                accountInput(VALID_SORT_CODE, "21956204"));
        assertTrue(InputValidator.isSearchTransactionValid(input));
    }
}
