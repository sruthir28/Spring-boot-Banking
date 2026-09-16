package com.example.paul.unit;

import com.example.paul.constants.constants;
import com.example.paul.controllers.AccountRestController;
import com.example.paul.models.Account;
import com.example.paul.services.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AccountRestController.class)
class AccountRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AccountService accountService;

    @Test
    void givenMissingInput_whenCheckingBalance_thenVerifyBadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void givenInvalidInput_whenCheckingBalance_thenVerifyBadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/accounts")
                .content("{\"sortCode\": \"53-68\",\"accountNumber\": \"78934\"}")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void givenNoAccountForInput_whenCheckingBalance_thenVerifyNoAccountFound() throws Exception {
        given(accountService.getAccount("53-68-92", "78901234")).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/accounts")
                .content("{\"sortCode\": \"53-68-92\",\"accountNumber\": \"78901234\"}")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(constants.NO_ACCOUNT_FOUND));
    }

    @Test
    void givenAccountDetails_whenCheckingBalance_thenVerifyOk() throws Exception {
        given(accountService.getAccount("53-68-92", "78901234")).willReturn(
                new Account(1L, "53-68-92", "78901234", 10.1, "Some Bank", "John"));

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/accounts")
                .content("{\"sortCode\": \"53-68-92\",\"accountNumber\": \"78901234\"}")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.sortCode").value("53-68-92"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.accountNumber").value("78901234"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.currentBalance").value(10.1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.bankName").value("Some Bank"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ownerName").value("John"));
    }
}
