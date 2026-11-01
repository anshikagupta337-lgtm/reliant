# Reliant

**Find flaky tests and slow builds in GitHub Actions, and put a ₹ figure on the waste.**

## Problem → Solution → Result

- **Problem:** CI wastes engineer time. Teams re-run failed pipelines and wait, and nobody measures the cost.
- **Solution:** Reliant reads a public repo's GitHub Actions history, separates real retries from approval waits, and converts flaky re-runs into engineer-hours and ₹.
- **Result:** Across 5 public repos (1 to 7 October 2026, newest 500 finished runs each), one confirmed flaky run was found in grafana/grafana, costing about ₹200 of engineer time (estimate).

## Findings

| Repo | Failed runs | Re-runs | Flaky via re-run | Flaky via same commit |
|---|---|---|---|---|
| pytorch/pytorch | 38 | 13 (all approval waits) | 0 | not checked |
| apache/airflow | 71 | 2 | 0 | not checked |
| apache/superset | 6 | 0 | 0 | 1 (to verify) |
| home-assistant/core | 45 | 1 | 0 | 0 |
| grafana/grafana | 16 | 2 | 1 | 1 (to verify) |

**Verified example:** grafana/grafana, Integration Tests, pull request #134447, run 37688620618. Attempt 1 failed after 25m 8s with one error (shard Sqlite Enterprise 2/16 exited with code 1, the other shards passed). The re-run on the same commit passed in 15m 11s. At 1 engineer and ₹800/hour, the re-run time is about ₹200.

**A mistake worth knowing about:** looking only at the latest attempt, pytorch seemed to have 6 to 8 flaky candidates. Checking attempt 1 showed they were all `action_required`, meaning a maintainer had to approve the run. Those are not retries. Reliant now checks attempt 1 of every re-run, and a test covers this case.

## How it works

```
GitHub REST API  ->  FetchRuns.java  ->  Analysis.java  ->  dashboard
 (workflow runs)     (fetch + attempt 1    (flaky check,      (numbers, chart,
                      lookup per re-run)    cost in rupees)    cost sliders)
```

- A run is counted as flaky via re-run when attempt 1 = `failure` and the latest attempt = `success`.
- `action_required` on attempt 1 is counted as an approval wait, not a retry.
- Cost = flaky failures x minutes lost x engineers waiting x hourly rate.

## Run it

1. Create a fine-grained GitHub token (public repositories, read-only).
2. Double-click `run.bat`, paste the token when asked (it is hidden and never saved to a file).
3. Tests: `mvn clean test`

Never put the token in a file or commit it.

## Limitations

- One week, newest 500 finished runs per repo. Not the full history.
- Failures are counted per workflow run, not per individual test.
- Same commit with a success and a failure can also come from infrastructure problems, manual re-runs or separate pushes. Treat it as a strong signal, not proof.
- Run duration is not measured automatically yet, so minutes lost is an assumption.
- ₹ figures are estimates based on assumed pay and time. They are not exact savings.
- GitHub keeps logs for a limited time, so old data may be missing.

## Tech

Java 21, Maven, Jackson, JUnit 5, GitHub REST API.