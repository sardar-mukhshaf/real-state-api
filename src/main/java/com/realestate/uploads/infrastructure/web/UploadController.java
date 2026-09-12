package com.realestate.uploads.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.shared.infrastructure.web.Responses;
import com.realestate.uploads.application.UploadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class UploadController {
    public record FileIds(
            @JsonProperty("imageIds") @NotEmpty @Size(max = 100) List<@NotBlank String> imageIds) {}

    private final UploadService uploads;

    public UploadController(UploadService uploads) {
        this.uploads = uploads;
    }

    @PostMapping({"", "/"})
    ResponseEntity<?> upload(@RequestPart("images") List<MultipartFile> images) {
        return Responses.created(
                uploads.upload(MultipartUploads.read(images, 10)).stream()
                        .map(uploads::view)
                        .toList(),
                "Files uploaded successfully");
    }

    @PostMapping("/get-images")
    ResponseEntity<?> getMany(@Valid @RequestBody FileIds body) {
        return Responses.ok(
                uploads.getMany(body.imageIds()).stream().map(uploads::view).toList(),
                "Files fetched successfully");
    }

    @PostMapping("/delete-images")
    ResponseEntity<?> deleteMany(@Valid @RequestBody FileIds body) {
        return Responses.ok(uploads.deleteMany(body.imageIds()), "Files deleted successfully");
    }

    @GetMapping("/property_images/{propertyId}")
    ResponseEntity<?> images(@PathVariable String propertyId) {
        return Responses.ok(uploads.propertyImages(propertyId), "Images fetched successfully");
    }

    @GetMapping("/single/{id}")
    ResponseEntity<?> single(@PathVariable String id) {
        return Responses.ok(uploads.view(uploads.get(id)), "File fetched successfully");
    }

    @DeleteMapping("/delete/{id}")
    ResponseEntity<?> delete(@PathVariable String id) {
        return Responses.ok(uploads.delete(id), "File deleted successfully");
    }
}
