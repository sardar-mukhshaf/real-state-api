package com.realestate.uploads.application;

public interface FileStorage {
    void put(String key, byte[] content, String contentType);

    void delete(String key);

    String downloadUrl(String key, boolean attachment);
}
