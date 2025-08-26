package org.acme.Resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.acme.DTO.BulkImageInput; // <-- IMPORT BARU
import org.acme.DTO.DeleteImageResponse;
import org.acme.DTO.ImageInput;
import org.acme.DTO.ImageResponse;
import org.acme.Entity.Database;
import org.acme.Storage.FileStorageService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList; // <-- IMPORT BARU
import java.util.List; // <-- IMPORT BARU
import java.util.Optional;
import java.util.UUID;

@Path("api/v1/images")
@ApplicationScoped
public class ImageResource {
    @Context
    UriInfo uriInfo;
    @Inject
    FileStorageService fileStorageService;
    @ConfigProperty(name = "upload.directory")
    String uploadDir;

    private String buildImageUrl(String fileName) {
        return uriInfo.getBaseUriBuilder() // -> http://localhost:8080/
                .path(ImageResource.class) // -> /images
                .path("file") // -> /file
                .path(fileName) // -> /{fileNameValue}
                .build()
                .toString();
    }

    @GET
    @Path("/file/{fileName}")
    public Response serveImage(@PathParam("fileName") String fileName) {
        // ... (kode yang sudah ada tidak berubah)
        File imageFile = Paths.get(uploadDir, fileName).toFile();
        if (!imageFile.exists()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("File tidak ditemukan di server.")
                    .build();
        }
        Optional<Database> imageMetadataOpt = Database.find("fileName", fileName).firstResultOptional();
        String contentType = imageMetadataOpt.map(db -> db.fileType).orElse("application/octet-stream");
        return Response.ok(imageFile)
                .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                .header("Content-Type", contentType)
                .build();
    }

    // --- CREATE ---
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createImage(ImageInput input) {
        // ... (kode yang sudah ada tidak berubah)
        if (input.image == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new DeleteImageResponse("Gambar wajib diisi.")).build();
        }

        // --- VALIDASI FORMAT FILE ---
        FileUpload fileUpload = input.image;
        String contentType = fileUpload.contentType();
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity(new DeleteImageResponse("Format file tidak didukung. Hanya PNG dan JPG yang diizinkan."))
                    .build();
        }
        // --- AKHIR VALIDASI ---

        try {
            String newFileName = fileStorageService.store(input.image);

            Database image = new Database();
            image.fileName = newFileName;
            image.originalFileName = input.image.fileName();
            image.filePath = Paths.get(newFileName).toString(); // filePath tetap disimpan
            image.fileType = input.image.contentType();
            image.persist();

            // ✅ Gunakan helper method untuk membuat URL lengkap
            String imageUrl = buildImageUrl(image.fileName);

            return Response.status(Response.Status.CREATED).entity(new ImageResponse(image.id, imageUrl))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new DeleteImageResponse(e.getMessage()))
                    .build();
        }
    }

    // --- BULK CREATE (METHOD BARU) ---
    @POST
    @Path("/bulk") // Path baru untuk membedakan dengan create tunggal
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createBulkImages(BulkImageInput input) {
        if (input.image == null || input.image.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new DeleteImageResponse("Tidak ada gambar yang diunggah.")).build();
        }

        List<ImageResponse> successfulUploads = new ArrayList<>();

        try {
            for (FileUpload fileUpload : input.image) {
                // ... (kode validasi format file)

                String newFileName = fileStorageService.store(fileUpload);

                Database image = new Database();
                image.fileName = newFileName;
                image.originalFileName = fileUpload.fileName();
                image.filePath = Paths.get(newFileName).toString();
                image.fileType = fileUpload.contentType();
                image.persist();

                // ✅ Gunakan helper method untuk membuat URL lengkap
                String imageUrl = buildImageUrl(image.fileName);
                successfulUploads.add(new ImageResponse(image.id, imageUrl));
            }
            return Response.status(Response.Status.CREATED).entity(successfulUploads).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new DeleteImageResponse(e.getMessage()))
                    .build();
        }
    }

    // --- UPDATE ---
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateImage(@PathParam("id") UUID id, ImageInput input) {
        // ... (kode yang sudah ada tidak berubah)
        if (input.image == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new DeleteImageResponse("Gambar wajib diisi.")).build();
        }

        // --- VALIDASI FORMAT FILE ---
        FileUpload fileUpload = input.image;
        String contentType = fileUpload.contentType();
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity(new DeleteImageResponse("Format file tidak didukung. Hanya PNG dan JPG yang diizinkan."))
                    .build();
        }
        // --- AKHIR VALIDASI ---

        Optional<Database> imageOpt = Database.findByIdOptional(id);
        if (imageOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new DeleteImageResponse("Gambar dengan ID " + id + " tidak ditemukan.")).build();
        }

        try {
            Database image = imageOpt.get();
            fileStorageService.delete(image.fileName);

            String newFileName = fileStorageService.store(input.image);

            image.fileName = newFileName;
            image.originalFileName = input.image.fileName();
            image.filePath = Paths.get(newFileName).toString();
            image.fileType = input.image.contentType();
            image.persist();

            // ✅ Gunakan helper method untuk membuat URL lengkap
            String imageUrl = buildImageUrl(image.fileName);

            return Response.ok(new ImageResponse(image.id, imageUrl)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new DeleteImageResponse(e.getMessage()))
                    .build();
        }
    }

    // --- DELETE ---
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response deleteImage(@PathParam("id") UUID id) {
        // ... (kode yang sudah ada tidak berubah)
        return Database.<Database>findByIdOptional(id)
                .map(image -> {
                    fileStorageService.delete(image.fileName);
                    image.delete();
                    return Response.ok(new DeleteImageResponse("Gambar dengan ID " + id + " berhasil dihapus."))
                            .build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new DeleteImageResponse("Gambar dengan ID " + id + " tidak ditemukan.")).build());
    }
}