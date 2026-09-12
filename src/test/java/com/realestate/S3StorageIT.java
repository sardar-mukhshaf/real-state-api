package com.realestate;

import static org.assertj.core.api.Assertions.*;

import com.realestate.uploads.infrastructure.storage.S3FileStorage;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class S3StorageIT {
    @Test
    void sdkUploadsSignsReadsAndDeletesThroughS3Protocol() throws Exception {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is required for the S3 protocol integration test");
        String oldKey = System.getProperty("aws.accessKeyId"),
                oldSecret = System.getProperty("aws.secretAccessKey");
        System.setProperty("aws.accessKeyId", "test-access");
        System.setProperty("aws.secretAccessKey", "test-secret");
        try (var container =
                new GenericContainer<>("adobe/s3mock:5.1.0")
                        .withEnv("COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS", "real-estate")
                        .withExposedPorts(9090)
                        .waitingFor(
                                Wait.forHttp("/real-estate")
                                        .forPort(9090)
                                        .forStatusCode(200)
                                        .withStartupTimeout(Duration.ofMinutes(2)))) {
            container.start();
            String endpoint = "http://" + container.getHost() + ":" + container.getMappedPort(9090);
            var storage = new S3FileStorage("real-estate", "us-east-1", endpoint, endpoint);
            try {
                storage.put(
                        "uploads/example",
                        "test object".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "text/plain");
                String url = storage.downloadUrl("uploads/example", true);
                assertThat(url).contains("X-Amz-Signature");
                var http = HttpClient.newHttpClient();
                var response =
                        http.send(
                                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                                HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).isEqualTo("test object");
                storage.delete("uploads/example");
                assertThat(
                                http.send(
                                                HttpRequest.newBuilder(URI.create(url))
                                                        .GET()
                                                        .build(),
                                                HttpResponse.BodyHandlers.ofString())
                                        .statusCode())
                        .isEqualTo(404);
            } finally {
                storage.close();
            }
        } finally {
            if (oldKey == null) System.clearProperty("aws.accessKeyId");
            else System.setProperty("aws.accessKeyId", oldKey);
            if (oldSecret == null) System.clearProperty("aws.secretAccessKey");
            else System.setProperty("aws.secretAccessKey", oldSecret);
        }
    }
}
