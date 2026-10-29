package com.scouting.scouting.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Club deportivo. No duplica DNI, correo ni telefono personales del
 * responsable; esa vinculacion se deriva de {@link UserOrganization} en SCP-004.
 * El pais se fija a PE para este alcance.
 */
@Entity
@Table(
        name = "clubs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_clubs_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_clubs_ruc", columnNames = "ruc")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codigo visible generado por el servidor, por ejemplo CLU-000001. */
    @Column(name = "code", nullable = false, length = 12, updatable = false)
    private String code;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Texto libre en esta fase; no es un enum. */
    @Column(name = "type", nullable = false, length = 100)
    private String type;

    @Column(name = "ruc", nullable = false, length = 11)
    private String ruc;

    @Column(name = "foundation_year", nullable = false)
    private Integer foundationYear;

    @Column(name = "logo_url", nullable = false, length = 500)
    private String logoUrl;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    /** Pais fijo PE; no se acepta desde el request. */
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    /** Codigo UBIGEO de distrito (6 digitos) del catalogo local INEI. */
    @Column(name = "ubigeo_code", nullable = false, length = 6)
    private String ubigeoCode;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Column(name = "institutional_email", nullable = false, length = 150)
    private String institutionalEmail;

    @Column(name = "institutional_phone", nullable = false, length = 30)
    private String institutionalPhone;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "tiktok_url", length = 500)
    private String tiktokUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 30)
    private OperationalStatus operationalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
