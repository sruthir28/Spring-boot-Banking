package com.example.paul.unit;

import com.example.paul.utils.AccountInput;
import com.example.paul.utils.CreateAccountInput;
import com.example.paul.utils.InputValidator;
import com.example.paul.utils.TransactionInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class InputValidatorTest {

    private static AccountInput accountInput(String sortCode, String accountNumber) {
        var input = new AccountInput();
        input.setSortCode(sortCode);
        input.setAccountNumber(accountNumber);
        return input;
    }

    private static TransactionInput transactionInput(AccountInput source, AccountInput target) {
        var input = new TransactionInput();
        input.setSourceAccount(source);
        input.setTargetAccount(target);
        input.setAmount(10);
        return input;
    }

    @Test
    void isSearchCriteriaValid_acceptsWellFormedSortCodeAndAccountNumber() {
        assertThat(InputValidator.isSearchCriteriaValid(accountInput("53-68-92", "78901234"))).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "53-68,78901234",
            "536892,78901234",
            "53-68-9a,78901234",
            "53-68-92,7890123",
            "53-68-92,789012345",
            "53-68-92,7890123a",
            "' 53-68-92',78901234",
            "53-68-92,'78901234 '"
    })
    void isSearchCriteriaValid_rejectsMalformedInput(String sortCode, String accountNumber) {
        assertThat(InputValidator.isSearchCriteriaValid(accountInput(sortCode, accountNumber))).isFalse();
    }

    @Test
    void isAccountNoValid_acceptsEightDigits() {
        assertThat(InputValidator.isAccountNoValid("78901234")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1234567", "123456789", "1234567a", "12 345678", "78901234\n1"})
    void isAccountNoValid_rejectsAnythingElse(String accountNo) {
        assertThat(InputValidator.isAccountNoValid(accountNo)).isFalse();
    }

    @Test
    void isCreateAccountCriteriaValid_acceptsNonBlankNames() {
        var input = new CreateAccountInput();
        input.setBankName("Some Bank");
        input.setOwnerName("John");

        assertThat(InputValidator.isCreateAccountCriteriaValid(input)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "'',John",
            "'   ',John",
            "Some Bank,''",
            "Some Bank,'  '",
            "'',''"
    })
    void isCreateAccountCriteriaValid_rejectsBlankNames(String bankName, String ownerName) {
        var input = new CreateAccountInput();
        input.setBankName(bankName);
        input.setOwnerName(ownerName);

        assertThat(InputValidator.isCreateAccountCriteriaValid(input)).isFalse();
    }

    @Test
    void isSearchTransactionValid_acceptsDistinctValidAccounts() {
        var input = transactionInput(
                accountInput("53-68-92", "78901234"),
                accountInput("67-41-18", "48573590"));

        assertThat(InputValidator.isSearchTransactionValid(input)).isTrue();
    }

    @Test
    void isSearchTransactionValid_rejectsInvalidSourceAccount() {
        var input = transactionInput(
                accountInput("53-68", "78901234"),
                accountInput("67-41-18", "48573590"));

        assertThat(InputValidator.isSearchTransactionValid(input)).isFalse();
    }

    @Test
    void isSearchTransactionValid_rejectsInvalidTargetAccount() {
        var input = transactionInput(
                accountInput("53-68-92", "78901234"),
                accountInput("67-41-18", "4857359"));

        assertThat(InputValidator.isSearchTransactionValid(input)).isFalse();
    }

    @Test
    void isSearchTransactionValid_rejectsSameSourceAndTarget() {
        var input = transactionInput(
                accountInput("53-68-92", "78901234"),
                accountInput("53-68-92", "78901234"));

        assertThat(InputValidator.isSearchTransactionValid(input)).isFalse();
    }
}
