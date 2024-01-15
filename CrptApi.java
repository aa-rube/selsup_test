import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient client;
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.requestSemaphore = new Semaphore(requestLimit);
        scheduleSemaphoreReleaseTask(timeUnit);
    }

    public void createDocument(String documentJson, String signature) {
        try {
            requestSemaphore.acquire();
            HttpRequest request = buildRequest(documentJson, signature);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            requestSemaphore.release();
        }
    }

    private HttpRequest buildRequest(String documentJson, String signature) {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(documentJson))
                .build();
    }

    private void scheduleSemaphoreReleaseTask(TimeUnit timeUnit) {
        Runnable task = () -> {
            try {
                requestSemaphore.acquire(requestSemaphore.availablePermits());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        long interval = timeUnit.toMillis(1);
        java.util.Timer timer = new java.util.Timer(true);
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, interval, interval);
    }
}