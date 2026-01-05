package com.example.minibank2.config;

import com.example.minibank2.entity.Account;
import com.example.minibank2.repository.AccountRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

//@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(AccountRepository accountRepository) {
        return args -> {
            if (accountRepository.count() == 0) { // żeby nie duplikować danych
                ObjectMapper mapper = new ObjectMapper();

                // Dodaj obsługę typów z java.time
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                TypeReference<List<Account>> typeRef = new TypeReference<>() {};
                InputStream inputStream = getClass().getResourceAsStream("/data/accounts.json");

                if (inputStream == null) {
                    System.err.println("❌ Nie znaleziono pliku accounts.json!");
                    return;
                } else {
                    System.out.println("Plik znaleziony poprawnie!");
                }

                List<Account> accounts = mapper.readValue(inputStream, typeRef);

                // Ustaw dokładność salda
                accounts.forEach(a -> a.setBalance(a.getBalance().setScale(2, BigDecimal.ROUND_HALF_UP)));

                accountRepository.saveAll(accounts);
                System.out.println("✅ Załadowano dane testowe: " + accounts.size() + " kont");
            } else {
                System.out.println("ℹ️ Dane już istnieją w bazie – pomijam inicjalizację.");
            }
        };
    }
}
