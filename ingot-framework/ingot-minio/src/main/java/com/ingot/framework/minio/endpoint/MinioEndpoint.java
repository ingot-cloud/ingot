package com.ingot.framework.minio.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.minio.service.MinioService;
import com.ingot.framework.minio.common.MinioItem;
import io.minio.StatObjectResponse;
import io.minio.messages.Bucket;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    @SneakyThrows
    @PostMapping("/bucket/{bucketName}")
    public Bucket createBucket(@PathVariable String bucketName) {
        minioService.createBucket(bucketName);
        return minioService.getBucket(bucketName).get();
    }

    @SneakyThrows
    @GetMapping("/bucket")
    public List<Bucket> getBuckets() {
        return minioService.getAllBuckets();
    }

    @SneakyThrows
    @GetMapping("/bucket/{bucketName}")
    public Bucket getBucket(@PathVariable String bucketName) {
        return minioService.getBucket(bucketName).orElseThrow(() -> new IllegalArgumentException("Bucket Name not found!"));
    }

    @SneakyThrows
    @DeleteMapping("/bucket/{bucketName}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteBucket(@PathVariable String bucketName) {
        minioService.removeBucket(bucketName);
    }

    /**
     * Object Endpoints
     */
    @SneakyThrows
    @PostMapping("/object/{bucketName}")
    public StatObjectResponse createObject(@RequestBody MultipartFile object, @PathVariable String bucketName) {
        String name = object.getOriginalFilename();
        minioService.putObject(bucketName, name, object.getInputStream(), object.getSize(), object.getContentType());
        return minioService.getObjectInfo(bucketName, name);
    }

    @SneakyThrows
    @PostMapping("/object/{bucketName}/{objectName}")
    public StatObjectResponse createObject(@RequestBody MultipartFile object, @PathVariable String bucketName, @PathVariable String objectName) {
        minioService.putObject(bucketName, objectName, object.getInputStream(), object.getSize(), object.getContentType());
        return minioService.getObjectInfo(bucketName, objectName);
    }

    @SneakyThrows
    @GetMapping("/object/{bucketName}/{objectName}")
    public List<MinioItem> filterObject(@PathVariable String bucketName, @PathVariable String objectName) {
        return minioService.getAllObjectsByPrefix(bucketName, objectName, true);
    }

    @SneakyThrows
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

    @SneakyThrows
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping("/object/{bucketName}/{objectName}/")
    public void deleteObject(@PathVariable String bucketName, @PathVariable String objectName) {

        minioService.removeObject(bucketName, objectName);
    }
}
