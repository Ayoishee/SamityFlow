package com.samityflow;

import com.samityflow.pattern.chain.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalChainTest {
    private ApprovalHandler chain() {
        ApprovalHandler first = new EligibilityHandler();
        first.setNext(new GuaranteeHandler()).setNext(new OfficerApprovalHandler()).setNext(new ManagerApprovalHandler());
        return first;
    }

    @Test
    void completeRequestPassesEveryHandler() {
        assertDoesNotThrow(() -> chain().handle(new ApprovalRequest(true, 2, 2, true, true)));
    }

    @Test
    void chainStopsWhenGuaranteesAreMissing() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> chain().handle(new ApprovalRequest(true, 1, 2, true, true)));
        assertEquals("Required guarantees are not collected", error.getMessage());
    }
}
