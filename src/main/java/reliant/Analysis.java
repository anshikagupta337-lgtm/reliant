package reliant;

public class Analysis {

    // Flaky tab, jab pehli attempt fail hui aur re-run me pass ho gayi
    public static boolean isRetryFlaky(String firstAttempt, String latest) {
        return "failure".equals(firstAttempt) && "success".equals(latest);
    }

    // action_required matlab maintainer approval ka intezaar, retry nahi
    public static boolean isApprovalWait(String firstAttempt) {
        return "action_required".equals(firstAttempt);
    }

    // Kharcha = flaky failures x minutes x engineers x hourly rate (estimate)
    public static double cost(int flakyFailures, double minutesPerFailure, int engineers, double hourlyRate) {
        return flakyFailures * minutesPerFailure / 60.0 * engineers * hourlyRate;
    }
}
