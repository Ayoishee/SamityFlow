package com.samityflow.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionServiceTest {

    @Test
    void acceptsPositiveCollectionWithNewReference() {
        CollectionService service = new CollectionService();

        assertTrue(service.collect(500, "COL-001"));
    }

    @Test
    void rejectsZeroAndNegativeCollections() {
        CollectionService service = new CollectionService();

        assertFalse(service.collect(0, "COL-001"));
        assertFalse(service.collect(-100, "COL-002"));
        assertFalse(service.collect(100, ""));
        assertFalse(service.collect(100, null));
    }

    @Test
    void rejectsDuplicateCollectionReference() {
        CollectionService service = new CollectionService();

        assertTrue(service.collect(500, "COL-001"));
        assertFalse(service.collect(700, "COL-001"));
    }

    @Test
    void reversedReferenceCanBeUsedAgain() {
        CollectionService service = new CollectionService();

        assertTrue(service.collect(500, "COL-001"));
        assertTrue(service.reverse("COL-001"));
        assertTrue(service.collect(500, "COL-001"));
    }
}
