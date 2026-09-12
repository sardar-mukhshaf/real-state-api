package com.realestate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.realestate.shared.application.*;
import com.realestate.uploads.application.FileStorage;
import com.realestate.users.application.*;
import com.realestate.users.domain.UserRole;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.*;

class PostgresApplicationIT {
    private static final String EXTERNAL = System.getProperty("test.database.url");
    private static PostgreSQLContainer postgres;
    private static ConfigurableApplicationContext context;
    private static int port;
    private static ObjectMapper json;
    private static UserService users;
    private static JdbcTemplate jdbc;
    private static final RateLimiter rateLimiter = mock(RateLimiter.class);
    private static final FileStorage storage = mock(FileStorage.class);

    @TestConfiguration(proxyBeanMethods = false)
    static class Overrides {
        @Bean
        @Primary
        RateLimiter testRateLimiter() {
            return rateLimiter;
        }

        @Bean
        @Primary
        FileStorage testFileStorage() {
            return storage;
        }
    }

    @BeforeAll
    static void startApplication() {
        if (EXTERNAL == null) {
            postgres = new PostgreSQLContainer("postgres:18.3");
            postgres.start();
        }
        context =
                new SpringApplicationBuilder(RealEstateApplication.class, Overrides.class)
                        .run(
                                "--server.port=0",
                                "--management.server.port=0",
                                "--spring.datasource.url="
                                        + (EXTERNAL == null ? postgres.getJdbcUrl() : EXTERNAL),
                                "--spring.datasource.username="
                                        + (EXTERNAL == null
                                                ? postgres.getUsername()
                                                : System.getProperty(
                                                        "test.database.user", "migration_test")),
                                "--spring.datasource.password="
                                        + (EXTERNAL == null
                                                ? postgres.getPassword()
                                                : System.getProperty("test.database.password", "")),
                                "--app.jwt.secret=integration-test-only-secret-with-64-random-looking-characters-1234",
                                "--app.bootstrap.email=",
                                "--app.bootstrap.password=",
                                "--app.storage.deletion-interval-ms=3600000",
                                "--logging.structured.format.console=",
                                "--logging.level.root=WARN");
        port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        json = context.getBean(ObjectMapper.class);
        users = context.getBean(UserService.class);
        jdbc = context.getBean(JdbcTemplate.class);
    }

    @AfterAll
    static void stopApplication() {
        if (context != null) context.close();
        if (postgres != null) postgres.stop();
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private String admin;

    record Reply(int status, JsonNode body) {
        JsonNode data() {
            return body.path("data");
        }
    }

    @BeforeEach
    void setup() throws Exception {
        reset(storage);
        reset(rateLimiter);
        when(storage.downloadUrl(anyString(), anyBoolean()))
                .thenAnswer(inv -> "https://storage.invalid/" + inv.getArgument(0));
        String email = "admin-" + UUID.randomUUID() + "@test.invalid";
        users.create("Test", "Administrator", email, "TestPassword123!", UserRole.ADMIN);
        admin =
                request(
                                "POST",
                                "/api/auth/login",
                                Data.map("email", email, "password", "TestPassword123!"),
                                null,
                                "device-test-123",
                                null)
                        .data()
                        .path("token")
                        .asText();
        assertThat(admin).isNotEmpty();
    }

    private Reply request(
            String method, String path, Object body, String token, String device, String key)
            throws Exception {
        var builder =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Content-Type", "application/json")
                        .method(
                                method,
                                body == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(
                                                json.writeValueAsString(body)));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (device != null) builder.header("x-device-id", device);
        if (key != null) builder.header("Idempotency-Key", key);
        var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), json.readTree(response.body()));
    }

    private String createUser(String role) throws Exception {
        var response =
                request(
                        "POST",
                        "/api/user/" + role + "/create",
                        Data.map(
                                "first_name",
                                "Test",
                                "last_name",
                                role,
                                "email",
                                UUID.randomUUID() + "@test.invalid",
                                "password",
                                "TestPassword123!",
                                "type",
                                role.toUpperCase(Locale.ROOT)),
                        admin,
                        null,
                        null);
        assertThat(response.status()).as(response.body().toString()).isEqualTo(201);
        assertThat(response.data().has("password")).isFalse();
        return response.data().path("id").asText();
    }

    private String createProperty(String landlord) throws Exception {
        var result =
                request(
                        "POST",
                        "/api/property/create",
                        Data.map(
                                "name",
                                "London House",
                                "size",
                                80,
                                "price",
                                "100000.25",
                                "landlord_id",
                                landlord,
                                "house_number",
                                "42",
                                "building_name",
                                "Oak",
                                "street",
                                "Baker Street",
                                "town",
                                "Westminster",
                                "city",
                                "London",
                                "postal_code",
                                "NW1"),
                        admin,
                        null,
                        null);
        assertThat(result.status()).as(result.body().toString()).isEqualTo(201);
        return result.data().path("id").asText();
    }

    @Test
    void propertyCrudLocationSearchAndSafePublicViews() throws Exception {
        String landlord = createUser("landlord"), property = createProperty(landlord);
        var update =
                request(
                        "PUT",
                        "/api/property/update/" + property,
                        Map.of("price", "123456.78"),
                        admin,
                        null,
                        null);
        assertThat(update.status()).as(update.body().toString()).isEqualTo(200);
        assertThat(update.data().path("name").asText()).isEqualTo("London House");
        var publicView = request("GET", "/api/property/single/" + property, null, null, null, null);
        assertThat(publicView.status()).isEqualTo(200);
        assertThat(publicView.data().path("landlord").has("email")).isFalse();
        assertThat(publicView.data().path("property_document").size()).isZero();
        var search =
                request(
                        "GET",
                        "/api/property/all?location=Baker%20London&size=10",
                        null,
                        null,
                        null,
                        null);
        assertThat(search.status()).as(search.body().toString()).isEqualTo(200);
        assertThat(search.body().path("pagination").path("totalElements").asLong()).isPositive();
        assertThat(
                        request("GET", "/api/property/all?sort=price;DROP", null, null, null, null)
                                .status())
                .isEqualTo(400);
        assertThat(
                        request(
                                        "PUT",
                                        "/api/property/update/" + property,
                                        Collections.singletonMap("name", null),
                                        admin,
                                        null,
                                        null)
                                .status())
                .isEqualTo(400);
        assertThat(
                        request(
                                        "DELETE",
                                        "/api/property/delete/" + property,
                                        null,
                                        admin,
                                        null,
                                        null)
                                .status())
                .isEqualTo(200);
        assertThat(
                        request("GET", "/api/property/single/" + property, null, null, null, null)
                                .status())
                .isEqualTo(404);
    }

    @Test
    void rentAccountingIsAtomicIdempotentAndProtectsPaidPayments() throws Exception {
        String landlord = createUser("landlord"),
                tenant = createUser("tenant"),
                property = createProperty(landlord);
        var contract =
                request(
                        "POST",
                        "/api/contract/create",
                        Data.map(
                                "property_id",
                                property,
                                "tenant_id",
                                tenant,
                                "start_date",
                                "2026-01-01",
                                "end_date",
                                "2026-12-31",
                                "mng_fee_percentage",
                                10,
                                "rent_per_month",
                                1000),
                        admin,
                        null,
                        null);
        assertThat(contract.status()).as(contract.body().toString()).isEqualTo(201);
        var command =
                Data.map(
                        "property_id",
                        property,
                        "tenant_id",
                        tenant,
                        "tenancy_contract_id",
                        contract.data().path("id").asText(),
                        "type",
                        "RENT",
                        "amount",
                        1000,
                        "description",
                        "Rent - first month",
                        "is_VAT",
                        true,
                        "transaction_date",
                        "2026-09-01",
                        "transaction_month",
                        9,
                        "start_date",
                        "2026-09-01",
                        "end_date",
                        "2026-09-30");
        String key = UUID.randomUUID().toString();
        var rent = request("POST", "/api/rent/create", command, admin, null, key);
        assertThat(rent.status()).as(rent.body().toString()).isEqualTo(201);
        String rentId = rent.data().path("id").asText();
        var again = request("POST", "/api/rent/create", command, admin, null, key);
        assertThat(again.data().path("id").asText()).isEqualTo(rentId);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM \"Transaction\" WHERE property_id=?",
                                Integer.class,
                                property))
                .isEqualTo(3);
        var changed = new LinkedHashMap<>(command);
        changed.put("amount", 1200);
        assertThat(request("POST", "/api/rent/create", changed, admin, null, key).status())
                .isEqualTo(409);
        assertThat(
                        request(
                                        "PUT",
                                        "/api/rent/update/" + rentId,
                                        Map.of("amount", 2000),
                                        admin,
                                        null,
                                        null)
                                .status())
                .isEqualTo(200);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT amount FROM \"Transaction\" WHERE source_rent_id=? AND type='PAYMENT'",
                                java.math.BigDecimal.class,
                                rentId))
                .isEqualByComparingTo("1800");
        String payment =
                jdbc.queryForObject(
                        "SELECT id FROM \"Transaction\" WHERE source_rent_id=? AND type='PAYMENT'",
                        String.class,
                        rentId);
        assertThat(
                        request(
                                        "PUT",
                                        "/api/landlord-payment/update/payment-status/" + payment,
                                        Map.of("status", "PAID"),
                                        admin,
                                        null,
                                        null)
                                .status())
                .isEqualTo(200);
        assertThat(
                        request(
                                        "PUT",
                                        "/api/rent/update/" + rentId,
                                        Map.of("amount", 3000),
                                        admin,
                                        null,
                                        null)
                                .status())
                .isEqualTo(409);
        var monthly =
                request(
                        "POST",
                        "/api/report/monthly",
                        Data.map("property_id", property, "month", 9, "year", 2026),
                        admin,
                        null,
                        null);
        assertThat(monthly.status()).as(monthly.body().toString()).isEqualTo(200);
        assertThat(monthly.data().path("income").path("totals").path("gross").decimalValue())
                .isEqualByComparingTo("2000");
        assertThat(
                        monthly.data()
                                .path("income")
                                .path("byMonth")
                                .get(0)
                                .path("description")
                                .asText())
                .isEqualTo("Rent - first month");
        var wrong = new LinkedHashMap<>(command);
        wrong.put("tenant_id", landlord);
        assertThat(
                        request(
                                        "POST",
                                        "/api/rent/create",
                                        wrong,
                                        admin,
                                        null,
                                        UUID.randomUUID().toString())
                                .status())
                .isEqualTo(400);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM \"Transaction\" WHERE property_id=?",
                                Integer.class,
                                property))
                .isEqualTo(3);
    }

    @Test
    void refreshReplayLogoutAndPasswordChangeRevokeSessions() throws Exception {
        String email = UUID.randomUUID() + "@test.invalid";
        var registered =
                request(
                        "POST",
                        "/api/auth/register",
                        Data.map(
                                "first_name",
                                "Normal",
                                "last_name",
                                "User",
                                "email",
                                email,
                                "password",
                                "TestPassword123!",
                                "type",
                                "USER"),
                        null,
                        "device-user-123",
                        null);
        assertThat(registered.status()).as(registered.body().toString()).isEqualTo(201);
        String access = registered.data().path("token").asText(),
                refresh = registered.data().path("refresh_token").asText();
        assertThat(request("GET", "/api/user/landlord/all", null, access, null, null).status())
                .isEqualTo(403);
        assertThat(
                        request(
                                        "POST",
                                        "/api/auth/refresh",
                                        Map.of("refresh_token", refresh),
                                        null,
                                        "wrong-device-123",
                                        null)
                                .status())
                .isEqualTo(401);
        var rotated =
                request(
                        "POST",
                        "/api/auth/refresh",
                        Map.of("refresh_token", refresh),
                        null,
                        "device-user-123",
                        null);
        assertThat(rotated.status()).as(rotated.body().toString()).isEqualTo(200);
        assertThat(
                        request(
                                        "POST",
                                        "/api/auth/refresh",
                                        Map.of("refresh_token", refresh),
                                        null,
                                        "device-user-123",
                                        null)
                                .status())
                .isEqualTo(401);
        assertThat(
                        request(
                                        "POST",
                                        "/api/auth/logout",
                                        null,
                                        rotated.data().path("token").asText(),
                                        null,
                                        null)
                                .status())
                .isEqualTo(401);
        var login =
                request(
                        "POST",
                        "/api/auth/login",
                        Map.of("email", email, "password", "TestPassword123!"),
                        null,
                        "device-user-123",
                        null);
        String nextAccess = login.data().path("token").asText();
        assertThat(
                        request(
                                        "POST",
                                        "/api/auth/change-password",
                                        Data.map(
                                                "current_password",
                                                "TestPassword123!",
                                                "new_password",
                                                "NewPassword456!"),
                                        nextAccess,
                                        null,
                                        null)
                                .status())
                .isEqualTo(200);
        assertThat(request("POST", "/api/auth/logout", null, nextAccess, null, null).status())
                .isEqualTo(401);
        assertThat(
                        request(
                                        "POST",
                                        "/api/auth/refresh",
                                        Map.of(
                                                "refresh_token",
                                                login.data().path("refresh_token").asText()),
                                        null,
                                        "device-user-123",
                                        null)
                                .status())
                .isEqualTo(401);
        var fresh =
                request(
                        "POST",
                        "/api/auth/login",
                        Map.of("email", email, "password", "NewPassword456!"),
                        null,
                        "device-user-123",
                        null);
        String freshToken = fresh.data().path("token").asText();
        assertThat(request("POST", "/api/auth/logout", null, freshToken, null, null).status())
                .isEqualTo(200);
        assertThat(request("POST", "/api/auth/logout", null, freshToken, null, null).status())
                .isEqualTo(401);
    }

    @Test
    void concurrentRefreshOnlyRotatesOnceAndReplayRevokesTheFamily() throws Exception {
        var login =
                request(
                        "POST",
                        "/api/auth/register",
                        Data.map(
                                "first_name",
                                "Race",
                                "last_name",
                                "User",
                                "email",
                                UUID.randomUUID() + "@test.invalid",
                                "password",
                                "TestPassword123!"),
                        null,
                        "race-device-123",
                        null);
        String refresh = login.data().path("refresh_token").asText();
        var gate = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Reply> task =
                    () -> {
                        gate.await();
                        return request(
                                "POST",
                                "/api/auth/refresh",
                                Map.of("refresh_token", refresh),
                                null,
                                "race-device-123",
                                null);
                    };
            var a = executor.submit(task);
            var b = executor.submit(task);
            gate.countDown();
            assertThat(List.of(a.get().status(), b.get().status()))
                    .containsExactlyInAnyOrder(200, 401);
        }
    }

    @Test
    void securityValidationAndRateLimitFailuresUseSafeErrors() throws Exception {
        assertThat(
                        request(
                                        "POST",
                                        "/api/auth/register",
                                        Data.map(
                                                "first_name",
                                                "Bad",
                                                "last_name",
                                                "Role",
                                                "email",
                                                "bad@test.invalid",
                                                "password",
                                                "TestPassword123!",
                                                "type",
                                                "ADMIN"),
                                        null,
                                        "device-test-123",
                                        null)
                                .status())
                .isEqualTo(403);
        assertThat(request("GET", "/api/user/landlord/all", null, null, null, null).status())
                .isEqualTo(401);
        assertThat(
                        request(
                                        "POST",
                                        "/api/auth/login",
                                        Map.of("email", "invalid", "password", "x"),
                                        null,
                                        "device-test-123",
                                        null)
                                .status())
                .isEqualTo(400);
        doThrow(new com.realestate.shared.domain.RateLimitException(17))
                .when(rateLimiter)
                .check(eq("GLOBAL"), anyString());
        var limited = request("GET", "/api/property/all", null, null, null, null);
        assertThat(limited.status()).isEqualTo(429);
        assertThat(limited.body().path("code").asText()).isEqualTo("RATE_LIMIT");
        reset(rateLimiter);
    }

    @Test
    void multipartUploadSurvivesMetadataEditAndCascadeQueuesCleanup() throws Exception {
        String property = createProperty(createUser("landlord"));
        String boundary = "test-boundary-123";
        byte[] header =
                ("--"
                                + boundary
                                + "\r\nContent-Disposition: form-data; name=\"images\"; filename=\"house.png\"\r\nContent-Type: image/png\r\n\r\n")
                        .getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] content = new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 1, 2};
        byte[] footer =
                ("\r\n--" + boundary + "--\r\n")
                        .getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var bytes = new java.io.ByteArrayOutputStream();
        bytes.write(header);
        bytes.write(content);
        bytes.write(footer);
        var response =
                http.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                "http://127.0.0.1:"
                                                        + port
                                                        + "/api/property/images/"
                                                        + property))
                                .header("Authorization", "Bearer " + admin)
                                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray()))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        String fileId =
                json.readTree(response.body())
                        .path("data")
                        .path("property_image")
                        .get(0)
                        .path("file_id")
                        .asText();
        assertThat(
                        request(
                                        "PUT",
                                        "/api/property/update/" + property,
                                        Map.of("name", "Renamed"),
                                        admin,
                                        null,
                                        null)
                                .data()
                                .path("property_image")
                                .size())
                .isEqualTo(1);
        assertThat(
                        request(
                                        "DELETE",
                                        "/api/property/delete/" + property,
                                        null,
                                        admin,
                                        null,
                                        null)
                                .status())
                .isEqualTo(200);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM \"File\" WHERE id=?", Integer.class, fileId))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM \"StorageDeletion\" WHERE object_key=?",
                                Integer.class,
                                "uploads/" + fileId))
                .isEqualTo(1);
        context.getBean(com.realestate.uploads.infrastructure.storage.StorageDeletionWorker.class)
                .deletePending();
        verify(storage).delete("uploads/" + fileId);
    }

    @Test
    void failedSecondUploadRollsBackMetadataAndCleansBothObjectKeys() {
        int before = jdbc.queryForObject("SELECT count(*) FROM \"File\"", Integer.class);
        doNothing()
                .doThrow(
                        new com.realestate.shared.domain.BusinessException(
                                com.realestate.shared.domain.BusinessException.Kind.UNAVAILABLE,
                                "Storage unavailable"))
                .when(storage)
                .put(anyString(), any(byte[].class), anyString());
        var upload =
                new com.realestate.uploads.domain.Upload(
                        "file.pdf",
                        "application/pdf",
                        "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        assertThatThrownBy(
                        () ->
                                context.getBean(
                                                com.realestate.uploads.application.UploadService
                                                        .class)
                                        .upload(List.of(upload, upload)))
                .isInstanceOf(com.realestate.shared.domain.BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM \"File\"", Integer.class))
                .isEqualTo(before);
        verify(storage, times(2)).delete(anyString());
    }

    @Test
    void basicCrudHeadersManagementIsolationAndAdminPasswordRevocation() throws Exception {
        assertThat(request("GET", "/api/property/all", null, null, null, null).status())
                .isEqualTo(200);
        var company =
                request(
                        "POST",
                        "/api/company/create",
                        Map.of("name", "Test Company", "vat_reg_no", "VAT-123"),
                        admin,
                        null,
                        null);
        assertThat(company.status()).as(company.body().toString()).isEqualTo(201);
        String companyId = company.data().path("id").asText();
        assertThat(
                        request(
                                        "PUT",
                                        "/api/company/update/" + companyId,
                                        Map.of("name", "Updated"),
                                        admin,
                                        null,
                                        null)
                                .data()
                                .path("vat_reg_no")
                                .asText())
                .isEqualTo("VAT-123");
        assertThat(
                        request("GET", "/api/company/single/" + companyId, null, admin, null, null)
                                .status())
                .isEqualTo(200);
        assertThat(
                        request(
                                        "DELETE",
                                        "/api/company/delete/" + companyId,
                                        null,
                                        admin,
                                        null,
                                        null)
                                .status())
                .isEqualTo(200);
        var address =
                request(
                        "POST",
                        "/api/address/create",
                        Data.map(
                                "house_number",
                                "1",
                                "street",
                                "Road",
                                "town",
                                "Town",
                                "city",
                                "City",
                                "postal_code",
                                "AB1"),
                        admin,
                        null,
                        null);
        assertThat(address.status()).as(address.body().toString()).isEqualTo(201);
        assertThat(address.data().path("notes").asText()).isEmpty();
        assertThat(
                        request(
                                        "DELETE",
                                        "/api/address/delete/" + address.data().path("id").asText(),
                                        null,
                                        admin,
                                        null,
                                        null)
                                .status())
                .isEqualTo(200);
        String tenant = createUser("tenant");
        String email = users.get(tenant).email();
        var session =
                request(
                        "POST",
                        "/api/auth/login",
                        Map.of("email", email, "password", "TestPassword123!"),
                        null,
                        "device-test-123",
                        null);
        assertThat(
                        request(
                                        "PUT",
                                        "/api/user/tenant/update/" + tenant,
                                        Map.of("password", "NewPassword456!"),
                                        admin,
                                        null,
                                        null)
                                .status())
                .isEqualTo(200);
        assertThat(
                        request(
                                        "GET",
                                        "/api/property/tenant",
                                        null,
                                        session.data().path("token").asText(),
                                        null,
                                        null)
                                .status())
                .isEqualTo(401);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM \"RefreshSession\" WHERE user_id=? AND revoked_at IS NULL",
                                Integer.class,
                                tenant))
                .isZero();
        var response =
                http.send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/health"))
                                .header("X-Request-ID", "test-correlation-123")
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.headers().firstValue("X-Request-ID")).contains("test-correlation-123");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        int managementPort =
                context.getEnvironment()
                        .getRequiredProperty("local.management.port", Integer.class);
        var live =
                http.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                "http://127.0.0.1:"
                                                        + managementPort
                                                        + "/actuator/health/liveness"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(live.statusCode()).as(live.body()).isEqualTo(200);
        assertThat(request("GET", "/actuator/prometheus", null, admin, null, null).status())
                .isEqualTo(403);
    }
}
