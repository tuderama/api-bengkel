package org.acme.DTO;

import java.util.UUID;

public class ImageResponse {
    public UUID id;
    public String url;

    public ImageResponse(UUID id, String url) {
        this.id = id;
        this.url = url;
    }
}
