package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.GroupUnit;
import com.samityflow.model.LoanProduct;
import com.samityflow.model.Member;
import com.samityflow.model.Samity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ManagementCrudTest {
    @TempDir Path temporaryDirectory;
    private Database database;

    @BeforeEach
    void setUp() {
        database = new Database("jdbc:sqlite:" + temporaryDirectory.resolve("crud.db"));
        database.initialize();
    }

    @Test
    void samityGroupAndMemberCanBeCreatedUpdatedAndDeleted() {
        SamityRepository samities = new SamityRepository(database);
        GroupUnitRepository groups = new GroupUnitRepository(database);
        MemberRepository members = new MemberRepository(database);

        Samity samity = samities.save("New Samity", "Monday");
        GroupUnit group = groups.save(samity.id(), "Group One");
        Member member = members.save(group.id(), "Amina", "01711111111");

        samities.update(samity.id(), "Updated Samity", "Tuesday");
        groups.update(group.id(), samity.id(), "Updated Group");
        members.update(member.id(), group.id(), "Updated Member", "01811111111", false, true);

        assertEquals("Updated Samity", samities.findAll().get(0).name());
        assertEquals("Updated Group", groups.findAll().get(0).name());
        assertEquals("Updated Member", members.findById(member.id()).orElseThrow().name());
        assertFalse(members.findById(member.id()).orElseThrow().eligible());

        members.delete(member.id());
        groups.delete(group.id());
        samities.delete(samity.id());
        assertTrue(members.findById(member.id()).isEmpty());
        assertTrue(groups.findAll().isEmpty());
        assertTrue(samities.findAll().isEmpty());
    }

    @Test
    void loanProductCanBeCreatedUpdatedAndDeleted() {
        LoanProductRepository products = new LoanProductRepository(database);
        LoanProduct saved = products.save(new LoanProduct(0, "Starter", "BUSINESS", 10,
                "NORMAL", 2, 10, 1, 50));
        LoanProduct changed = new LoanProduct(saved.id(), "Starter Plus", "EMERGENCY", 8,
                "GRACE", 1, 12, 2, 75);

        products.update(changed);
        assertEquals(changed, products.findById(saved.id()).orElseThrow());
        products.delete(saved.id());
        assertTrue(products.findById(saved.id()).isEmpty());
    }

    @Test
    void invalidManagementInputIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SamityRepository(database).save("", "Monday"));
        assertThrows(IllegalArgumentException.class,
                () -> new GroupUnitRepository(database).save(0, "Group"));
        assertThrows(IllegalArgumentException.class,
                () -> new MemberRepository(database).save(1, "Member", ""));
        assertThrows(IllegalArgumentException.class, () -> new LoanProductRepository(database).save(
                new LoanProduct(0, "Broken", "BUSINESS", 10, "NORMAL", 2, 0, 1, 50)));
    }
}
