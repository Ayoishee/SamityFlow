package com.samityflow;

import com.samityflow.database.Database;
import com.samityflow.model.LoanApplication;
import com.samityflow.repository.GroupUnitRepository;
import com.samityflow.repository.GuaranteeRepository;
import com.samityflow.repository.LoanApplicationRepository;
import com.samityflow.repository.LoanProductRepository;
import com.samityflow.repository.MemberRepository;
import com.samityflow.repository.SamityRepository;
import com.samityflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseRepositoryTest {

    @TempDir
    Path tempDirectory;

    private Database database;
    private UserRepository userRepository;
    private SamityRepository samityRepository;
    private GroupUnitRepository groupRepository;
    private MemberRepository memberRepository;
    private LoanProductRepository productRepository;
    private LoanApplicationRepository applicationRepository;
    private GuaranteeRepository guaranteeRepository;

    @BeforeEach
    void setUp() {
        Path databaseFile = tempDirectory.resolve("test.db");
        database = new Database("jdbc:sqlite:" + databaseFile);

        database.initialize();
        database.seed();

        userRepository = new UserRepository(database);
        samityRepository = new SamityRepository(database);
        groupRepository = new GroupUnitRepository(database);
        memberRepository = new MemberRepository(database);
        productRepository = new LoanProductRepository(database);
        applicationRepository = new LoanApplicationRepository(database);
        guaranteeRepository = new GuaranteeRepository(database);
    }

    @Test
    void seederCreatesSampleData() {
        assertEquals(2, userRepository.findAll().size());
        assertEquals(1, samityRepository.findAll().size());
        assertEquals(1, groupRepository.findAll().size());
        assertEquals(3, memberRepository.findAll().size());
        assertEquals(3, productRepository.findAll().size());
    }

    @Test
    void applicationRepositoryCanSaveAndReadAnApplication() {
        LoanApplication application = applicationRepository.saveNew(
                1,
                1,
                10_000,
                "Buy seeds"
        );

        LoanApplication savedApplication = applicationRepository
                .findById(application.getId())
                .orElseThrow();

        assertEquals(1, savedApplication.getMemberId());
        assertEquals(1, savedApplication.getProductId());
        assertEquals(10_000, savedApplication.getAmount());
        assertEquals("Buy seeds", savedApplication.getPurpose());
    }

    @Test
    void databasePreventsDuplicateGuarantees() {
        LoanApplication application = applicationRepository.saveNew(
                1,
                1,
                10_000,
                "Buy seeds"
        );

        guaranteeRepository.save(application.getId(), 2);

        assertEquals(
                1,
                guaranteeRepository.countForApplication(application.getId())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> guaranteeRepository.save(application.getId(), 2)
        );
    }
}
