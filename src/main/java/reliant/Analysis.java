package reliant;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

    // Do GitHub timestamps ke beech kitne second
    public static double durationSeconds(String start, String end) {
        return Duration.between(Instant.parse(start), Instant.parse(end)).toMillis() / 1000.0;
    }

    // Numbers ki list ka average (khali list ho toh 0)
    public static double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }
}
