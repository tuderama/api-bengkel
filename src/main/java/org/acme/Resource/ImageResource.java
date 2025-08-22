package org.acme.Resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.acme.DTO.DeleteImageResponse;
import org.acme.DTO.ImageInput;
import org.acme.DTO.ImageResponse;
import org.acme.Entity.Database;
import org.acme.Storage.FileStorageService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Path("/images")
@ApplicationScoped
public class ImageResource {
    @Inject
    FileStorageService fileStorageService;
    @ConfigProperty(name = "upload.directory")
    String uploadDir;

    @GET
    @Path("/file/{fileName}")
    public Response serveImage(@PathParam("fileName") String fileName) {
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
            String newFileName = fileStorageService.store(fileUpload);
            String filePath = Paths.get(newFileName).toString();

            Database image = new Database();
            image.fileName = newFileName;
            image.originalFileName = fileUpload.fileName();
            image.filePath = filePath;
            image.fileType = fileUpload.contentType();
            image.persist();

            return Response.status(Response.Status.CREATED).entity(new ImageResponse(image.id, image.filePath))
                    .build();
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

            String newFileName = fileStorageService.store(fileUpload);
            String filePath = Paths.get(newFileName).toString();

            image.fileName = newFileName;
            image.originalFileName = fileUpload.fileName();
            image.filePath = filePath;
            image.fileType = fileUpload.contentType();
            image.persist();

            return Response.ok(new ImageResponse(image.id, image.filePath)).build();
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
