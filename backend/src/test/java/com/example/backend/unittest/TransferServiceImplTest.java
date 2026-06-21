package com.example.backend.unittest;

import com.example.backend.dtos.TransactionResponse;
import com.example.backend.entities.Account;
import com.example.backend.entities.TransactionLog;
import com.example.backend.enums.AccountStatus;
import com.example.backend.enums.TransactionCategory;
import com.example.backend.exceptions.AccountNotFoundException;
import com.example.backend.exceptions.DuplicateTransferException;
import com.example.backend.exceptions.InvalidRedemptionException;
import com.example.backend.repositories.AccountRepository;
import com.example.backend.repositories.TransactionLogRepository;
import com.example.backend.services.AccountService;
import com.example.backend.services.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Full branch coverage for TransferServiceImpl, including:
 *  - sender/receiver lookup failures
 *  - reward point redemption validation (null/zero, invalid amount, over-available, over-cap)
 *  - account-not-active / insufficient-balance FAILED paths (with point/discount reset)
 *  - duplicate idempotency key
 *  - successful transfer with and without redemption
 *  - category resolution: null, blank, valid, invalid string
 */
class TransferServiceImplTest {

    @InjectMocks
    private TransferServiceImpl transferService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private AccountService accountService;

    private static final String FROM_ID = "ACC1111";
    private static final String TO_ID = "ACC2222";
    private static final String IDEMPOTENCY_KEY = "key123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Account activeAccount(String accountId, double balance, String holderName) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVE);
        account.setHolderName(holderName);
        return account;
    }

    // ── SENDER / RECEIVER LOOKUP ─────────────────────────────────

    @Test
    void testTransfer_senderNotFound() {
        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                transferService.transfer(FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", null));

        verify(accountRepository, never()).findByAccountId(TO_ID);
    }

    @Test
    void testTransfer_receiverNotFound() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                transferService.transfer(FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", null));
    }

    // ── REDEMPTION VALIDATION ─────────────────────────────────────

    @Test
    void testTransfer_redeemPointsNull_noRedemption() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", null);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(0, response.getPointsRedeemed());
        assertEquals(0.0, response.getDiscountAmount());
        assertEquals(100.0, response.getAmountPaid());
        verify(accountService, never()).getAvailablePoints(any());
        verify(accountService, never()).addRedeemedPoints(any(), anyInt());
    }

    @Test
    void testTransfer_redeemPointsZero_noRedemption() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", 0);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(0, response.getPointsRedeemed());
        verify(accountService, never()).getAvailablePoints(any());
    }

    @Test
    void testTransfer_redeemPointsNegative_noRedemption() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", -5);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(0, response.getPointsRedeemed());
    }

    @Test
    void testTransfer_redeemPointsRequested_amountNull_throwsInvalidRedemption() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));

        assertThrows(InvalidRedemptionException.class, () ->
                transferService.transfer(FROM_ID, TO_ID, null, IDEMPOTENCY_KEY, "RENT", "note", 5));
    }

    @Test
    void testTransfer_redeemPointsRequested_amountZeroOrNegative_throwsInvalidRedemption() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));

        assertThrows(InvalidRedemptionException.class, () ->
                transferService.transfer(FROM_ID, TO_ID, 0.0, IDEMPOTENCY_KEY, "RENT", "note", 5));
    }

    @Test
    void testTransfer_redeemPointsExceedsAvailable_throwsInvalidRedemption() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(accountService.getAvailablePoints(FROM_ID)).thenReturn(3);

        // amount=100 -> cap = 10, requesting 5 points but only 3 available
        InvalidRedemptionException ex = assertThrows(InvalidRedemptionException.class, () ->
                transferService.transfer(FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", 5));

        assertTrue(ex.getMessage().contains("3 reward points available"));
        verify(transactionLogRepository, never()).save(any());
    }

    @Test
    void testTransfer_redeemPointsExceedsCap_throwsInvalidRedemption() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        // Plenty of points available, but cap = 10% of 100 = 10
        when(accountService.getAvailablePoints(FROM_ID)).thenReturn(50);

        InvalidRedemptionException ex = assertThrows(InvalidRedemptionException.class, () ->
                transferService.transfer(FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", 20));

        assertTrue(ex.getMessage().contains("at most 10 points"));
    }

    @Test
    void testTransfer_redeemPointsWithinCapAndAvailable_success() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(accountService.getAvailablePoints(FROM_ID)).thenReturn(50);
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // amount=100, cap=10, redeem exactly 10
        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", 10);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(10, response.getPointsRedeemed());
        assertEquals(10.0, response.getDiscountAmount());
        // Sender pays amount - discount = 90; receiver still gets full 100
        assertEquals(90.0, response.getAmountPaid());
        assertEquals(410.0, fromAccount.getBalance()); // 500 - 90
        assertEquals(300.0, toAccount.getBalance());   // 200 + 100
        verify(accountService, times(1)).addRedeemedPoints(FROM_ID, 10);
    }

    // ── ACCOUNT STATUS / BALANCE / DUPLICATE (FAILED PATH) ─────────

    @Test
    void testTransfer_senderNotActive_marksFailedAndResetsRedemption() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        fromAccount.setStatus(AccountStatus.LOCKED);
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", null);

        assertEquals("FAILED", response.getStatus());
        assertEquals("Sender account is not active", response.getFailureReason());
        assertEquals(0, response.getPointsRedeemed());
        assertEquals(0.0, response.getDiscountAmount());
        assertEquals(100.0, response.getAmountPaid());
        // No balances should change
        assertEquals(500.0, fromAccount.getBalance());
        assertEquals(200.0, toAccount.getBalance());
        verify(accountRepository, never()).save(any());
        verify(accountService, never()).addRedeemedPoints(any(), anyInt());
    }

    @Test
    void testTransfer_receiverNotActive_marksFailed() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");
        toAccount.setStatus(AccountStatus.CLOSED);

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", null);

        assertEquals("FAILED", response.getStatus());
        assertEquals("Receiver account is not active", response.getFailureReason());
    }

    @Test
    void testTransfer_insufficientBalance_marksFailedAndResetsRedemption() {
        Account fromAccount = activeAccount(FROM_ID, 50.0, "Sender"); // not enough
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 1000.0, IDEMPOTENCY_KEY, "RENT", "note", null);

        assertEquals("FAILED", response.getStatus());
        assertEquals("Insufficient balance in sender account", response.getFailureReason());
        assertEquals(0, response.getPointsRedeemed());
        assertEquals(0.0, response.getDiscountAmount());
        assertEquals(1000.0, response.getAmountPaid());
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    @Test
    void testTransfer_insufficientBalance_afterRedemptionDiscount_stillFails() {
        // Balance covers the discounted amount in theory is irrelevant here —
        // verifies the validateBalance check runs against amountToDebit (post-discount),
        // and on failure points/discount are correctly reset to zero.
        Account fromAccount = activeAccount(FROM_ID, 5.0, "Sender"); // too low even after discount
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(accountService.getAvailablePoints(FROM_ID)).thenReturn(50);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", 10);

        assertEquals("FAILED", response.getStatus());
        assertEquals(0, response.getPointsRedeemed());
        assertEquals(0.0, response.getDiscountAmount());
        assertEquals(100.0, response.getAmountPaid());
        verify(accountService, never()).addRedeemedPoints(any(), anyInt());
    }

    @Test
    void testTransfer_duplicateIdempotencyKey_marksFailed() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(new TransactionLog());

        // validateIdempotency throws DuplicateTransferException which is NOT caught
        // by the try/catch (only AccountNotActiveException/InsufficientBalanceException are),
        // so it propagates up.
        assertThrows(DuplicateTransferException.class, () ->
                transferService.transfer(FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "note", null));

        verify(transactionLogRepository, never()).save(any());
    }

    // ── SUCCESS PATH DETAILS ───────────────────────────────────────

    @Test
    void testTransfer_success_noRedemption_savesLogAndReturnsResponse() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "RENT", "I am paying the rent", null);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(400.0, fromAccount.getBalance());
        assertEquals(300.0, toAccount.getBalance());
        assertEquals("RENT", response.getCategory());
        assertEquals("I am paying the rent", response.getNote());
        assertEquals("Sender", response.getFromAccountHolderName());
        assertEquals("Receiver", response.getToAccountHolderName());
        assertEquals(FROM_ID, response.getFromAccountId());
        assertEquals(TO_ID, response.getToAccountId());
        assertNotNull(response.getCreatedOn());
        assertNull(response.getFailureReason());

        verify(accountRepository, times(1)).save(fromAccount);
        verify(accountRepository, times(1)).save(toAccount);
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    // ── CATEGORY RESOLUTION BRANCHES ────────────────────────────────

    @Test
    void testTransfer_nullCategory_defaultsToOther() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, null, "note", null);

        assertEquals(TransactionCategory.OTHER.name(), response.getCategory());
    }

    @Test
    void testTransfer_blankCategory_defaultsToOther() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "   ", "note", null);

        assertEquals(TransactionCategory.OTHER.name(), response.getCategory());
    }

    @Test
    void testTransfer_invalidCategoryString_defaultsToOther() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "not-a-real-category", "note", null);

        assertEquals(TransactionCategory.OTHER.name(), response.getCategory());
    }

    @Test
    void testTransfer_validLowercaseCategory_resolvesCaseInsensitively() {
        Account fromAccount = activeAccount(FROM_ID, 500.0, "Sender");
        Account toAccount = activeAccount(TO_ID, 200.0, "Receiver");

        when(accountRepository.findByAccountId(FROM_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountId(TO_ID)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(null);
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                FROM_ID, TO_ID, 100.0, IDEMPOTENCY_KEY, "  grocery  ", "note", null);

        assertEquals(TransactionCategory.GROCERY.name(), response.getCategory());
    }
}