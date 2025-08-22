package org.acme.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "images")
public class Database extends PanacheEntityBase {
    @Id
    @GeneratedValue
    public UUID id; // Primary Key, diisi dari input

    @Column(nullable = false, unique = true)
    public String fileName; // Nama file unik yang disimpan di server

    @Column(nullable = false)
    public String originalFileName; // Nama file asli saat di-upload

    @Column(nullable = false)
    public String filePath; // Path lengkap ke file di server

    @Column(nullable = false)
    public String fileType; // Tipe konten (e.g., "image/jpeg")

    public LocalDateTime createdAt = LocalDateTime.now();
}
