package com.scouting.scouting.media.service;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.media.entity.MediaKind;
import com.scouting.scouting.media.entity.MediaPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

/**
 * Acceso a los dos buckets de R2. Los clientes se crean de forma diferida para
 * que los tests y los perfiles que no usan medios no necesiten credenciales.
 */
@Service
public class MediaStorage {

    public enum Bucket {
        PUBLIC,
        PRIVATE
    }

    public record ObjectMetadata(long sizeBytes, String contentType) {
    }

    private static final long DEFAULT_TTL_SECONDS = 900;

    private final String endpoint;
    private final String region;
    private final String publicBucket;
    private final String privateBucket;
    private final String publicBaseUrl;
    private final String publicAccessKey;
    private final String publicSecretKey;
    private final String privateAccessKey;
    private final String privateSecretKey;
    private final long presignTtlSeconds;

    private volatile S3Client publicClient;
    private volatile S3Client privateClient;
    private volatile S3Presigner publicPresigner;
    private volatile S3Presigner privatePresigner;

    public MediaStorage(
            @Value("${R2_ENDPOINT:}") String endpoint,
            @Value("${R2_REGION:auto}") String region,
            @Value("${R2_PUBLIC_BUCKET:}") String publicBucket,
            @Value("${R2_PRIVATE_BUCKET:}") String privateBucket,
            @Value("${R2_PUBLIC_BASE_URL:}") String publicBaseUrl,
            @Value("${R2_PUBLIC_ACCESS_KEY_ID:}") String publicAccessKey,
            @Value("${R2_PUBLIC_SECRET_ACCESS_KEY:}") String publicSecretKey,
            @Value("${R2_PRIVATE_ACCESS_KEY_ID:}") String privateAccessKey,
            @Value("${R2_PRIVATE_SECRET_ACCESS_KEY:}") String privateSecretKey,
            @Value("${R2_PRESIGN_TTL_SECONDS:900}") long presignTtlSeconds) {
        this.endpoint = normalize(endpoint);
        this.region = blankToDefault(region, "auto");
        this.publicBucket = normalize(publicBucket);
        this.privateBucket = normalize(privateBucket);
        this.publicBaseUrl = normalizePublicBaseUrl(publicBaseUrl);
        this.publicAccessKey = normalize(publicAccessKey);
        this.publicSecretKey = normalize(publicSecretKey);
        this.privateAccessKey = normalize(privateAccessKey);
        this.privateSecretKey = normalize(privateSecretKey);
        this.presignTtlSeconds = presignTtlSeconds > 0 ? presignTtlSeconds : DEFAULT_TTL_SECONDS;
    }

    public boolean isConfigured() {
        return !endpoint.isBlank()
                && !publicBucket.isBlank()
                && !privateBucket.isBlank()
                && !publicBaseUrl.isBlank()
                && !publicAccessKey.isBlank()
                && !publicSecretKey.isBlank()
                && !privateAccessKey.isBlank()
                && !privateSecretKey.isBlank();
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw ApiException.serviceUnavailable(
                    "MEDIA_STORAGE_NOT_CONFIGURED",
                    "El almacenamiento de archivos no esta configurado.");
        }
    }

    public String bucketFor(MediaPurpose purpose) {
        return bucketFor(purpose.kind());
    }

    public String bucketFor(MediaKind kind) {
        return kind == MediaKind.IMAGE ? publicBucket : privateBucket;
    }

    public String bucketFor(Bucket bucket) {
        return bucket == Bucket.PUBLIC ? publicBucket : privateBucket;
    }

    public String buildPublicUrl(String objectKey) {
        return publicBaseUrl + "/" + objectKey.replaceAll("^/+", "");
    }

    public String presignPut(Bucket bucket, String objectKey, String contentType) {
        requireConfigured();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketFor(bucket))
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            PresignedPutObjectRequest presigned = presigner(bucket).presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofSeconds(presignTtlSeconds))
                            .putObjectRequest(request)
                            .build());
            return presigned.url().toString();
        } catch (SdkClientException ex) {
            throw ApiException.serviceUnavailable(
                    "MEDIA_STORAGE_UNAVAILABLE",
                    "No se pudo preparar la subida del archivo.");
        }
    }

    public String presignGet(Bucket bucket, String objectKey) {
        requireConfigured();
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketFor(bucket))
                    .key(objectKey)
                    .build();
            PresignedGetObjectRequest presigned = presigner(bucket).presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofSeconds(presignTtlSeconds))
                            .getObjectRequest(request)
                            .build());
            return presigned.url().toString();
        } catch (SdkClientException ex) {
            throw ApiException.serviceUnavailable(
                    "MEDIA_STORAGE_UNAVAILABLE",
                    "No se pudo preparar la reproduccion del archivo.");
        }
    }

    public ObjectMetadata head(Bucket bucket, String objectKey) {
        requireConfigured();
        try {
            HeadObjectResponse response = client(bucket).headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketFor(bucket))
                            .key(objectKey)
                            .build());
            return new ObjectMetadata(
                    response.contentLength() == null ? 0L : response.contentLength(),
                    response.contentType());
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw ApiException.badRequest(
                        "MEDIA_UPLOAD_NOT_FOUND",
                        "El archivo no se encontro en el almacenamiento.");
            }
            throw ApiException.serviceUnavailable(
                    "MEDIA_STORAGE_UNAVAILABLE",
                    "No se pudo verificar el archivo subido.");
        } catch (SdkClientException ex) {
            throw ApiException.serviceUnavailable(
                    "MEDIA_STORAGE_UNAVAILABLE",
                    "No se pudo verificar el archivo subido.");
        }
    }

    private S3Client client(Bucket bucket) {
        if (bucket == Bucket.PUBLIC) {
            S3Client result = publicClient;
            if (result == null) {
                synchronized (this) {
                    result = publicClient;
                    if (result == null) {
                        publicClient = result = buildClient(publicAccessKey, publicSecretKey);
                    }
                }
            }
            return result;
        }
        S3Client result = privateClient;
        if (result == null) {
            synchronized (this) {
                result = privateClient;
                if (result == null) {
                    privateClient = result = buildClient(privateAccessKey, privateSecretKey);
                }
            }
        }
        return result;
    }

    private S3Presigner presigner(Bucket bucket) {
        if (bucket == Bucket.PUBLIC) {
            S3Presigner result = publicPresigner;
            if (result == null) {
                synchronized (this) {
                    result = publicPresigner;
                    if (result == null) {
                        publicPresigner = result = buildPresigner(publicAccessKey, publicSecretKey);
                    }
                }
            }
            return result;
        }
        S3Presigner result = privatePresigner;
        if (result == null) {
            synchronized (this) {
                result = privatePresigner;
                if (result == null) {
                    privatePresigner = result = buildPresigner(privateAccessKey, privateSecretKey);
                }
            }
        }
        return result;
    }

    private S3Client buildClient(String accessKey, String secretKey) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials(accessKey, secretKey))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private S3Presigner buildPresigner(String accessKey, String secretKey) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials(accessKey, secretKey))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private StaticCredentialsProvider credentials(String accessKey, String secretKey) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizePublicBaseUrl(String value) {
        String normalized = normalize(value).replaceAll("/+$", "");
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.startsWith("http://") || normalized.startsWith("https://")
                ? normalized
                : "https://" + normalized;
    }

    private static String blankToDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }
}
