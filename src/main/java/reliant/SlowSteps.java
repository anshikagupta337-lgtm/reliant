package reliant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SlowSteps {

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

    static void addTime(Map<String, List<Double>> times, String key, JsonNode item) {
        JsonNode start = item.path("started_at");
        JsonNode end = item.path("completed_at");
        if (!start.isTextual() || !end.isTextual()) {
            return;
        }
        if (item.path("conclusion").asText("").equals("skipped")) {
            return;
        }
        double secs = Analysis.durationSeconds(start.asText(), end.asText());
        times.computeIfAbsent(key, k -> new ArrayList<>()).add(secs);
    }

    static void printTop(String title, Map<String, List<Double>> times) {
        System.out.println(title);
        List<Map.Entry<String, List<Double>>> entries = new ArrayList<>(times.entrySet());
        entries.sort((a, b) -> Double.compare(Analysis.average(b.getValue()), Analysis.average(a.getValue())));
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            Map.Entry<String, List<Double>> e = entries.get(i);
            double max = 0.0;
            for (double v : e.getValue()) {
                if (v > max) {
                    max = v;
                }
            }
            System.out.printf("%d. %s | avg %.1f min | max %.1f min | seen %d times%n",
                    i + 1, e.getKey(), Analysis.average(e.getValue()) / 60.0, max / 60.0, e.getValue().size());
        }
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        String repo = "grafana/grafana";
        System.out.println("Repo: " + repo);

        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isEmpty()) {
            System.out.println("GITHUB_TOKEN set nahi hai. Pehle token set karo.");
            return;
        }

        JsonNode root = getJson("https://api.github.com/repos/" + repo
                + "/actions/workflows/pr-test-integration.yml/runs"
                + "?per_page=15&status=completed&created=2026-10-01..2026-10-07", token);
        if (root == null) {
            return;
        }

        Map<String, List<Double>> jobTimes = new HashMap<>();
        Map<String, List<Double>> stepTimes = new HashMap<>();
        int runsRead = 0;

        for (JsonNode run : root.get("workflow_runs")) {
            JsonNode jobsRoot = getJson("https://api.github.com/repos/" + repo + "/actions/runs/"
                    + run.get("id").asText() + "/jobs?per_page=100", token);
            if (jobsRoot == null) {
                continue;
            }
            runsRead++;
            for (JsonNode job : jobsRoot.get("jobs")) {
                // "Sqlite (2/16)" aur "Sqlite (3/16)" ko ek hi job maanenge
                String jobName = job.path("name").asText().replaceAll(" \\(\\d+/\\d+\\)", "");
                addTime(jobTimes, jobName, job);
                for (JsonNode step : job.path("steps")) {
                    addTime(stepTimes, jobName + " > " + step.path("name").asText(), step);
                }
            }
        }

        System.out.println("Runs padhi: " + runsRead);
        System.out.println();
        printTop("Top 5 sabse dheere JOBS:", jobTimes);
        printTop("Top 5 sabse dheere STEPS:", stepTimes);
    }
}
