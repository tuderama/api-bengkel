package org.acme.DTO;

import java.util.UUID;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.ws.rs.core.MediaType;

public class ImageInput {
    @RestForm("id")
    @PartType(MediaType.TEXT_PLAIN)
    public UUID id;

    @RestForm("image")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public FileUpload image;
}
