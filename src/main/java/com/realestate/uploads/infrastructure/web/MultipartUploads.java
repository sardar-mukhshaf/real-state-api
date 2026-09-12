package com.realestate.uploads.infrastructure.web;

import com.realestate.shared.domain.BusinessException;
import com.realestate.uploads.domain.Upload;
import java.io.IOException;
import java.util.*;
import org.springframework.web.multipart.MultipartFile;

public final class MultipartUploads {
    private MultipartUploads() {}

    public static List<Upload> read(List<MultipartFile> files, int max) {
        if (files == null) return List.of();
        if (files.size() > max) throw BusinessException.invalid("Too many files");
        var result = new ArrayList<Upload>();
        for (var file : files) {
            if (file.getSize() > 5 * 1024 * 1024)
                throw BusinessException.invalid("File exceeds 5 MB");
            try {
                result.add(
                        new Upload(
                                file.getOriginalFilename(),
                                file.getContentType(),
                                file.getBytes()));
            } catch (IOException ex) {
                throw BusinessException.invalid("Unable to read uploaded file");
            }
        }
        return result;
    }
}
