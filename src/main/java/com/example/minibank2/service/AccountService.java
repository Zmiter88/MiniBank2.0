package com.example.minibank2.service;

import com.example.minibank2.dto.AccountResponse;
import com.example.minibank2.dto.CreateAccountRequest;
import com.example.minibank2.dto.CreateAccountResponse;
import com.example.minibank2.dto.UpdateAccountRequest;
import com.example.minibank2.entity.Account;
import com.example.minibank2.entity.AccountType;
import com.example.minibank2.exception.AccountNotFoundException;
import com.example.minibank2.exception.InsufficientFundsException;
import com.example.minibank2.exception.TransferToSameAccountException;
import com.example.minibank2.exception.InvalidAmountException;
import com.example.minibank2.mapper.AccountMapper;
import com.example.minibank2.repository.AccountRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * AccountService to warstwa logiki biznesowej dla kont bankowych.
 * Odpowiada za pobieranie danych z repozytorium i wykonywanie operacji na kontach.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final NumberGeneratorService numberGeneratorService;
    private final AccountMapper accountMapper;
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    // Konstruktor z wstrzykiwaniem zależności
    public AccountService(AccountRepository accountRepository,
                          TransactionService transactionService,
                          NumberGeneratorService numberGeneratorService,
                          AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.numberGeneratorService = numberGeneratorService;
        this.accountMapper = accountMapper;
    }

    // 🔹 Metoda pomocnicza do pobrania konta lub rzucenia wyjątku
    private Account getAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id " + accountId));
    }

    // 🔹 Zwraca listę wszystkich kont
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toAccountResponse)
                .toList();
    }

    // 🔹 Tworzy nowe konto i zapisuje je w bazie
    public CreateAccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setOwner(request.getOwner());
        account.setCurrency(request.getCurrency());
        account.setAccountType(request.getAccountType());
        account.setCreatedAt(LocalDate.now());
        account.setNumber(numberGeneratorService.generateAccountNumber());
        account.setStatus("ACTIVE");
        account.setBalance(BigDecimal.ZERO);
        account.setInterestRate(request.getAccountType() == AccountType.SAVINGS ? new BigDecimal("0.02") : BigDecimal.ZERO);

        Account savedAccount = accountRepository.save(account);
        return accountMapper.toCreateAccountResponse(savedAccount);
    }

    // 🔹 Aktualizacja konta (tylko wybrane pola)
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request) {
        Account account = getAccountOrThrow(id);
        account.setOwner(request.getOwner());
        Account updatedAccount = accountRepository.save(account);
        return accountMapper.toAccountResponse(updatedAccount);
    }

    // 🔹 Usuwanie konta
    public AccountResponse deleteAccount(Long id) {
        Account account = getAccountOrThrow(id);
        accountRepository.delete(account);
        logger.info("Account with id {} has been deleted", id);
        return accountMapper.toAccountResponse(account);
    }

    // 🔹 Pobranie konta po id
    public AccountResponse findAccountById(Long id) {
        return accountMapper.toAccountResponse(getAccountOrThrow(id));
    }

    // 🔹 Pobranie kont dla właściciela
    public List<AccountResponse> findAccountsByOwner(String owner) {
        List<Account> accounts = accountRepository.findByOwner(owner);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts for owner: " + owner);
        }
        return accounts.stream().map(accountMapper::toAccountResponse).toList();
    }

    // 🔹 Konto z najwyższym saldem (Spring)
    public AccountResponse getAccountWithMaxBalanceSpring() {
        Account account = accountRepository.findTopByOrderByBalanceDesc()
                .orElseThrow(() -> new AccountNotFoundException("No accounts in database"));
        return accountMapper.toAccountResponse(account);
    }

    // 🔹 Znajdowanie konta z saldem większym niż podane
    public List<AccountResponse> getAccountsWithBalanceGreaterThan(BigDecimal amount) {
        List<Account> accounts = accountRepository.findByBalanceGreaterThan(amount);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found with balance greater than " + amount);
        }
        return accounts.stream().map(accountMapper::toAccountResponse).toList();
    }

    // 🔹 Znalezienie kont utworzonych po dacie
    public List<AccountResponse> getAccountsCreatedAfterDate(LocalDate date) {
        List<Account> accounts = accountRepository.findByCreatedAtAfter(date);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found after " + date);
        }
        return accounts.stream().map(accountMapper::toAccountResponse).toList();
    }

    // 🔹 Najstarsze konto
    public AccountResponse getTheOldestAccount() {
        Account account = accountRepository.findTopByOrderByCreatedAtAsc()
                .orElseThrow(() -> new AccountNotFoundException("No accounts found"));
        return accountMapper.toAccountResponse(account);
    }

    // 🔹 Liczba kont w danej walucie
    public Long getHowManyAccountWithCurrency(String currency) {
        return accountRepository.countByCurrency(currency);
    }

    // 🔹 Pierwsze konto z aktywnym statusem posortowane malejąco po saldzie
    public AccountResponse firstActiveAccountOrderByBalanceDesc(String status) {
        Account account = accountRepository.findTopByStatusOrderByBalanceDesc(status)
                .orElseThrow(() -> new AccountNotFoundException("No account found with status " + status));
        return accountMapper.toAccountResponse(account);
    }

    // 🔹 Konta utworzone przed określoną datą
    public List<AccountResponse> accountsCreatedBefore(LocalDate date) {
        List<Account> accounts = accountRepository.findAllByCreatedAtBefore(date);
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found before " + date);
        }
        return accounts.stream().map(accountMapper::toAccountResponse).toList();
    }

    // 🔹 Konto z najwyższym saldem w danej walucie
    public AccountResponse accountWithHighestBalanceIn(String currency) {
        Account account = accountRepository.findTopByCurrencyOrderByBalanceDesc(currency)
                .orElseThrow(() -> new AccountNotFoundException("No account found"));
        return accountMapper.toAccountResponse(account);
    }

    // 🔹 Top 3 kont z najwyższym saldem
    public List<AccountResponse> top3HighestBalanceAccounts() {
        List<Account> accounts = accountRepository.findTop3ByOrderByBalanceDesc();
        if (accounts.isEmpty()) {
            throw new AccountNotFoundException("No accounts found in database");
        }
        return accounts.stream().map(accountMapper::toAccountResponse).toList();
    }

    // 🔹 Wykonanie przelewu między kontami
    @Transactional
    public void transfer(Long senderId, Long receiverId, BigDecimal amount) {
        if (senderId.equals(receiverId)) {
            throw new TransferToSameAccountException("Nie można wykonać przelewu na to samo konto.");
        }

        Account sender = getAccountOrThrow(senderId);
        Account receiver = getAccountOrThrow(receiverId);

        sender.withdraw(amount);
        receiver.deposit(amount);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        transactionService.recordTransfer(sender, receiver, amount);
    }
    // Metoda do wpłaty kasy na konto
    @Transactional
    public AccountResponse deposit(Long accountId, BigDecimal amount) {
        Account account = getAccountOrThrow(accountId);
        account.deposit(amount);
        accountRepository.save(account);
        transactionService.recordDeposit(account, amount);
        return accountMapper.toAccountResponse(account);
    }
    // Metoda do wypłaty kasy z konta
    @Transactional
    public AccountResponse withdraw(Long accountId, BigDecimal amount) {
        Account account = getAccountOrThrow(accountId);
        account.withdraw(amount);
        accountRepository.save(account);
        transactionService.recordWithdraw(account, amount);
        return accountMapper.toAccountResponse(account);
    }
}
