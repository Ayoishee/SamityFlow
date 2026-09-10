package com.samityflow.service;

import com.samityflow.database.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SavingsPersistenceTest {
    @TempDir Path tempDirectory;
    private SavingsService service;

    @BeforeEach
    void setUp() {
        Database database = new Database("jdbc:sqlite:" + tempDirectory.resolve("savings.db"));
        database.initialize();
        database.seed();
        service = new SavingsService(database);
    }

    @Test
    void depositAndWithdrawalChangePersistentBalance() {
        service.deposit(1, 1_000, "SAV-001");
        service.withdraw(1, 350, "SAV-002");

        assertEquals(650, service.balance(1), 0.001);
    }

    @Test
    void withdrawalCannotExceedBalance() {
        service.deposit(1, 500, "SAV-001");

        assertThrows(IllegalArgumentException.class,
                () -> service.withdraw(1, 501, "SAV-002"));
        assertEquals(500, service.balance(1), 0.001);
    }

    @Test
    void duplicateSavingsReferenceIsRejected() {
        service.deposit(1, 500, "SAV-001");

        assertThrows(IllegalArgumentException.class,
                () -> service.deposit(1, 200, "SAV-001"));
    }
}
