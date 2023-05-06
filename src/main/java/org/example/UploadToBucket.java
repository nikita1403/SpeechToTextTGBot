package org.example;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.InputStream;


public class UploadToBucket {
    private final String STATIC_SECRET_KEY = "<your yandex-static-secret-key-for-bucket>";
    private final String STATIC_ACCESS_KEY = "your yandex-static-access-ket-for-bucket";
    private final String BUCKET_NAME = "your bucket-name";
    public String getLinkForFileOnYandexCloud(InputStream inputStream, String fileName)
    {
        AWSCredentials awsCredentials = new BasicAWSCredentials(STATIC_ACCESS_KEY, STATIC_SECRET_KEY);
        AmazonS3 s3Client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withEndpointConfiguration(new AmazonS3ClientBuilder.EndpointConfiguration(
                        "storage.yandexcloud.net", "ru-central1"
                )).build();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("audio/ogg");
        s3Client.putObject(BUCKET_NAME,  fileName+".ogg", inputStream, metadata);
        if(s3Client.doesObjectExist(BUCKET_NAME, fileName+".ogg"))
        {
            s3Client.shutdown();
            return "https://storage.yandexcloud.net/" + BUCKET_NAME + "/" + fileName+".ogg";
        }
        else
        {
            s3Client.shutdown();
            return null;
        }
    }
}
