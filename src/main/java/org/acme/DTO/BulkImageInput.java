package org.acme.DTO;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.List;
import java.util.UUID;

public class BulkImageInput {
    // ID untuk mengelompokkan gambar (misalnya, ID produk atau ID album)
    @RestForm("id")
    @PartType(MediaType.TEXT_PLAIN)
    public UUID id;

    // Nama field 'images' ini harus sesuai dengan nama form-data field yang dikirim
    // dari client.
    @RestForm("image")
    public List<FileUpload> image;
}
