package com.realestate.uploads.infrastructure.storage;

import com.realestate.shared.domain.BusinessException;
import com.realestate.uploads.application.FileStorage;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
public class S3FileStorage implements FileStorage {
    private final S3Client client;
    private final S3Presigner signer;
    private final String bucket;

    @org.springframework.beans.factory.annotation.Autowired
    public S3FileStorage(
            com.realestate.shared.infrastructure.configuration.AppProperties properties) {
        this(
                properties.storage().bucket(),
                properties.storage().region(),
                properties.storage().endpoint(),
                properties.storage().publicEndpoint());
    }

    public S3FileStorage(String bucket, String region, String endpoint, String publicEndpoint) {
        this.bucket = bucket;
        var builder =
                S3Client.builder()
                        .region(Region.of(region))
                        .credentialsProvider(DefaultCredentialsProvider.builder().build())
                        .httpClientBuilder(
                                UrlConnectionHttpClient.builder()
                                        .connectionTimeout(Duration.ofSeconds(3))
                                        .socketTimeout(Duration.ofSeconds(10)))
                        .overrideConfiguration(
                                c ->
                                        c.apiCallTimeout(Duration.ofSeconds(30))
                                                .apiCallAttemptTimeout(Duration.ofSeconds(12))
                                                .retryStrategy(
                                                        StandardRetryStrategy.builder()
                                                                .maxAttempts(3)
                                                                .build()));
        var signing =
                S3Presigner.builder()
                        .region(Region.of(region))
                        .credentialsProvider(DefaultCredentialsProvider.builder().build());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
            signing.endpointOverride(
                            URI.create(publicEndpoint.isBlank() ? endpoint : publicEndpoint))
                    .serviceConfiguration(
                            S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        client = builder.build();
        signer = signing.build();
    }

    public void put(String key, byte[] content, String type) {
        try {
            client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(type).build(),
                    RequestBody.fromBytes(content));
        } catch (RuntimeException ex) {
            throw new BusinessException(
                    BusinessException.Kind.UNAVAILABLE, "File storage temporarily unavailable");
        }
    }

    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public String downloadUrl(String key, boolean attachment) {
        return signer.presignGetObject(
                        GetObjectPresignRequest.builder()
                                .signatureDuration(Duration.ofMinutes(5))
                                .getObjectRequest(
                                        GetObjectRequest.builder()
                                                .bucket(bucket)
                                                .key(key)
                                                .responseContentDisposition(
                                                        attachment ? "attachment" : "inline")
                                                .build())
                                .build())
                .url()
                .toExternalForm();
    }

    @PreDestroy
    public void close() {
        client.close();
        signer.close();
    }
}
