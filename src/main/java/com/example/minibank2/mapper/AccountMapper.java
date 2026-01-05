package com.example.minibank2.mapper;

import com.example.minibank2.dto.AccountResponse;
import com.example.minibank2.dto.CreateAccountResponse;
import com.example.minibank2.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toAccountResponse(Account account) {
        if (account == null) {
            return null;
        }

        AccountResponse dto = new AccountResponse();
        dto.setId(account.getId());
        dto.setOwner(account.getOwner());
        dto.setNumber(account.getNumber());
        dto.setCurrency(account.getCurrency());
        dto.setBalance(account.getBalance());
        dto.setStatus(account.getStatus());
        dto.setAccountType(account.getAccountType());
        dto.setInterestRate(account.getInterestRate());
        dto.setCreatedAt(account.getCreatedAt());
        return dto;
    }

    public CreateAccountResponse toCreateAccountResponse(Account account) {
        if (account == null) {
            return null;
        }

        CreateAccountResponse dto = new CreateAccountResponse();
        dto.setId(account.getId());
        dto.setOwner(account.getOwner());
        dto.setNumber(account.getNumber());
        dto.setCurrency(account.getCurrency());
        dto.setBalance(account.getBalance());
        dto.setStatus(account.getStatus());
        dto.setAccountType(account.getAccountType());
        dto.setInterestRate(account.getInterestRate());
        dto.setCreatedAt(account.getCreatedAt());
        return dto;
    }
}
