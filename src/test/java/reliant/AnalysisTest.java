package reliant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class AnalysisTest {

    @Test
    void failureThenSuccessIsFlaky() {
        assertTrue(Analysis.isRetryFlaky("failure", "success"));
    }

    @Test
    void approvalWaitIsNotFlaky() {
        assertFalse(Analysis.isRetryFlaky("action_required", "success"));
        assertTrue(Analysis.isApprovalWait("action_required"));
    }

    @Test
    void failureThenFailureIsNotFlaky() {
        assertFalse(Analysis.isRetryFlaky("failure", "failure"));
    }

    @Test
    void cancelledThenSuccessIsNotFlaky() {
        assertFalse(Analysis.isRetryFlaky("cancelled", "success"));
    }

    @Test
    void costOfOneFifteenMinuteFailure() {
        assertEquals(200.0, Analysis.cost(1, 15, 1, 800), 0.001);
    }

    @Test
    void zeroFlakyMeansZeroCost() {
        assertEquals(0.0, Analysis.cost(0, 30, 5, 800), 0.001);
    }

    @Test
    void costScalesWithEngineers() {
        assertEquals(1600.0, Analysis.cost(2, 30, 2, 800), 0.001);
    }
}
