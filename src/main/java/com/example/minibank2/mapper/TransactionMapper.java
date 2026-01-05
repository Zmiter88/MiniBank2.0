package com.example.minibank2.mapper;

import com.example.minibank2.dto.TransactionResponse;
import com.example.minibank2.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAccountId(transaction.getAccount().getId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setDateTime(transaction.getDateTime());
        return response;
    }
}
