package org.acme.DTO;

import java.util.UUID;

public class ImageResponse {
    public UUID id;
    public String imageName;

    public ImageResponse(UUID id, String imageName) {
        this.id = id;
        this.imageName = imageName;
    }
}
