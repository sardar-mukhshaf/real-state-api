package com.realestate.properties.infrastructure.web;

import com.realestate.properties.application.PropertyService;
import com.realestate.shared.application.*;
import com.realestate.shared.infrastructure.web.Responses;
import com.realestate.uploads.infrastructure.web.MultipartUploads;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/property")
public class PropertyController {
    private final PropertyService properties;

    public PropertyController(PropertyService properties) {
        this.properties = properties;
    }

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> createJson(@RequestBody Map<String, Object> body) {
        var p = properties.create(new Changes(body), List.of());
        return Responses.created(properties.view(p.id(), false), "Property created successfully");
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<?> createForm(
            @RequestParam Map<String, String> fields,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        var p =
                properties.create(
                        new Changes(new HashMap<>(fields)), MultipartUploads.read(images, 5));
        return Responses.created(properties.view(p.id(), false), "Property created successfully");
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> updateJson(@PathVariable String id, @RequestBody Map<String, Object> body) {
        properties.update(id, new Changes(body), List.of());
        return Responses.ok(properties.view(id, false), "Property updated successfully");
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<?> updateForm(
            @PathVariable String id,
            @RequestParam Map<String, String> fields,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        properties.update(id, new Changes(new HashMap<>(fields)), MultipartUploads.read(images, 5));
        return Responses.ok(properties.view(id, false), "Property updated successfully");
    }

    @GetMapping("/all")
    ResponseEntity<?> all(
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "false") boolean descending) {
        return list(location, null, null, page, size, sort, descending, true);
    }

    @GetMapping("/single/{id}")
    ResponseEntity<?> get(@PathVariable String id) {
        return Responses.ok(properties.view(id, true), "Property fetched successfully");
    }

    @DeleteMapping("/delete/{id}")
    ResponseEntity<?> delete(@PathVariable String id) {
        return Responses.ok(properties.delete(id), "Property deleted successfully");
    }

    @GetMapping("/landlord")
    ResponseEntity<?> landlord(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return list(null, jwt.getSubject(), null, page, size, "id", false, false);
    }

    @GetMapping("/tenant")
    ResponseEntity<?> tenant(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return list(null, null, jwt.getSubject(), page, size, "id", false, false);
    }

    @PostMapping("/images/{id}")
    ResponseEntity<?> images(
            @PathVariable String id, @RequestPart("images") List<MultipartFile> images) {
        properties.attach(id, MultipartUploads.read(images, 5), false);
        return Responses.ok(properties.view(id, false), "Property images uploaded successfully");
    }

    @PostMapping("/documents/{id}")
    ResponseEntity<?> documents(
            @PathVariable String id, @RequestPart("documents") List<MultipartFile> documents) {
        properties.attach(id, MultipartUploads.read(documents, 5), true);
        return Responses.ok(properties.view(id, false), "Property documents uploaded successfully");
    }

    private ResponseEntity<?> list(
            String location,
            String owner,
            String tenant,
            int page,
            int size,
            String sort,
            boolean descending,
            boolean publicView) {
        var result = properties.search(location, owner, tenant, page, size, sort, descending);
        var response =
                new LinkedHashMap<>(
                        Responses.ok(
                                        properties.views(result.items(), publicView),
                                        "Properties fetched successfully")
                                .getBody());
        response.put(
                "pagination",
                Responses.map(
                        "page",
                        page,
                        "size",
                        size,
                        "totalElements",
                        result.totalElements(),
                        "totalPages",
                        result.totalPages()));
        return ResponseEntity.ok()
                .header("X-Total-Count", Long.toString(result.totalElements()))
                .body(response);
    }
}
