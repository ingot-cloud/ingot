package com.ingot.framework.oss.minio.endpoint;

import com.ingot.framework.oss.minio.common.MinioItem;
import com.ingot.framework.oss.minio.service.MinioService;
import io.minio.StatObjectResponse;
import io.minio.messages.Bucket;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description  : MinioEndpoint.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 14:44.</p>
 */
@RestController
@AllArgsConstructor
@ConditionalOnProperty(name = "ingot.minio.endpoint.enable", havingValue = "true")
@RequestMapping("${ingot.minio.endpoint.name:/minio}")
public class MinioEndpoint {
    private final MinioService minioService;


    /**
     * Bucket Endpoints
     */
    @PostMapping("/bucket/{bucketName}")
    public Bucket createBucket(@PathVariable String bucketName) {
        minioService.createBucket(bucketName);
        return minioService.getBucket(bucketName).orElseThrow();
    }

    @GetMapping("/bucket")
    public List<Bucket> getBuckets() {
        return minioService.getAllBuckets();
    }

    @GetMapping("/bucket/{bucketName}")
    public Bucket getBucket(@PathVariable String bucketName) {
        return minioService.getBucket(bucketName).orElseThrow(() -> new IllegalArgumentException("Bucket Name not found!"));
    }

    @DeleteMapping("/bucket/{bucketName}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteBucket(@PathVariable String bucketName) {
        minioService.removeBucket(bucketName);
    }

    /**
     * Object Endpoints
     */
    @PostMapping("/object/{bucketName}")
    public StatObjectResponse createObject(@RequestBody MultipartFile object, @PathVariable String bucketName) {
        try {
            String name = object.getOriginalFilename();
            minioService.putObject(bucketName, name, object.getInputStream(), object.getSize(), object.getContentType());
            return minioService.getObjectInfo(bucketName, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/object/{bucketName}/{objectName}")
    public StatObjectResponse createObject(@RequestBody MultipartFile object, @PathVariable String bucketName, @PathVariable String objectName) {
        try {
            minioService.putObject(bucketName, objectName, object.getInputStream(), object.getSize(), object.getContentType());
            return minioService.getObjectInfo(bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/object/{bucketName}/{objectName}")
    public List<MinioItem> filterObject(@PathVariable String bucketName, @PathVariable String objectName) {
        return minioService.getAllObjectsByPrefix(bucketName, objectName, true);
    }

    @GetMapping("/object/{bucketName}/{objectName}/{expires}")
    public Map<String, Object> getObject(@PathVariable String bucketName, @PathVariable String objectName, @PathVariable Integer expires) {
        Map<String, Object> responseBody = new HashMap<>(8);
        // Put Object info
        responseBody.put("bucket", bucketName);
        responseBody.put("object", objectName);
        responseBody.put("url", minioService.getObjectURL(bucketName, objectName, expires, TimeUnit.MINUTES));
        responseBody.put("expires", expires);
        return responseBody;
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping("/object/{bucketName}/{objectName}/")
    public void deleteObject(@PathVariable String bucketName, @PathVariable String objectName) {
        try {
            minioService.removeObject(bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
