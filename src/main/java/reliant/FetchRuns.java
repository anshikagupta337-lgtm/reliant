package reliant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FetchRuns {

    static HttpClient client = HttpClient.newHttpClient();
    static ObjectMapper mapper = new ObjectMapper();

    static JsonNode getJson(String url, String token) throws Exception {
        Thread.sleep(1500);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "reliant")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.out.println("Error aaya: " + response.statusCode());
            System.out.println(response.body());
            return null;
        }
        return mapper.readTree(response.body());
    }

    static void analyse(String repo, String token) throws Exception {
        System.out.println("=== " + repo + " ===");
        int total = 0;
        int failures = 0;
        int reruns = 0;
        int approvals = 0;
        int retryFlaky = 0;
        Map<String, Set<String>> byCommit = new HashMap<>();

        for (int page = 1; page <= 5; page++) {
            String url = "https://api.github.com/repos/" + repo + "/actions/runs"
                    + "?per_page=100&status=completed&created=2026-10-01..2026-10-07&page=" + page;
            JsonNode root = getJson(url, token);
            if (root == null) {
                break;
            }
            JsonNode runs = root.get("workflow_runs");
            if (runs.size() == 0) {
                break;
            }
            for (JsonNode run : runs) {
                total++;
                String conclusion = run.get("conclusion").asText();
                if (conclusion.equals("failure")) {
                    failures++;
                }
                if (conclusion.equals("success") || conclusion.equals("failure")) {
                    String key = run.get("head_sha").asText().substring(0, 7) + " | "
                            + run.get("name").asText() + " | " + run.get("event").asText();
                    byCommit.computeIfAbsent(key, k -> new HashSet<>()).add(conclusion);
                }
                int attempt = run.get("run_attempt").asInt();
                if (attempt > 1) {
                    reruns++;
                    String id = run.get("id").asText();
                    JsonNode first = getJson(
                            "https://api.github.com/repos/" + repo + "/actions/runs/" + id + "/attempts/1", token);
                    String firstResult = (first == null) ? "unknown" : first.get("conclusion").asText();
                    if (firstResult.equals("action_required")) {
                        approvals++;
                    } else if (firstResult.equals("failure") && conclusion.equals("success")) {
                        retryFlaky++;
                        System.out.println("  FLAKY (re-run): " + run.get("name").asText() + " | "
                                + run.get("html_url").asText());
                    }
                }
            }
        }

        int mixed = 0;
        for (Map.Entry<String, Set<String>> e : byCommit.entrySet()) {
            if (e.getValue().size() == 2) {
                mixed++;
                if (mixed <= 10) {
                    System.out.println("  FLAKY (same commit, success + failure): " + e.getKey());
                }
            }
        }

        System.out.println("Total runs: " + total);
        System.out.println("Failed runs: " + failures);
        System.out.println("Re-runs: " + reruns + " (approval waits: " + approvals + ")");
        System.out.println("Flaky via re-run: " + retryFlaky);
        System.out.println("Flaky via same commit: " + mixed);
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isEmpty()) {
            System.out.println("GITHUB_TOKEN set nahi hai. Pehle terminal me token set karo.");
            return;
        }
        String[] repos = {"apache/superset", "home-assistant/core", "grafana/grafana"};
        for (String repo : repos) {
            analyse(repo, token);
        }
    }
}
