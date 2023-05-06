package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;



import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class SpeechToText {

    private final UploadToBucket uploadToBucket;
    private static URI POST_SYNCHRONOUS;
    private final String YANDEX_API_KEY = "<your yandex-api-key>";
    public SpeechToText()
    {
        try {
            POST_SYNCHRONOUS = new URIBuilder("https://stt.api.cloud.yandex.net/speech/v1/stt:recognize").addParameter("topic", "general:rc")
                    .addParameter("lang", "ru-RU").build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        uploadToBucket = new UploadToBucket();
    }


    public String synchronousRecognition(InputStream voiceStream)
    {
        HttpPost httpPost = new HttpPost(POST_SYNCHRONOUS);
        httpPost.setHeader("Authorization", "Api-Key " + YANDEX_API_KEY);
        try {
            httpPost.setEntity(new ByteArrayEntity(voiceStream.readAllBytes()));
            voiceStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try(CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(httpPost))
        {
            String responseData = EntityUtils.toString(response.getEntity());
            JsonObject jsonObject = new Gson().fromJson(responseData, JsonObject.class);
            return jsonObject.has("result") ? jsonObject.get("result").getAsString():null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public String asynchronousRecognition(InputStream voiceStream)
    {
        JsonObject jsonObject;
        StringBuilder stringBuilder = new StringBuilder();
        String fileLink = uploadToBucket.getLinkForFileOnYandexCloud(voiceStream, String.valueOf(voiceStream.toString()));
        String id = getFileIdOnServer(fileLink);
        if(id==null) return null;
        HttpGet request = new HttpGet("https://operation.api.cloud.yandex.net/operations/"+id);
        request.setHeader("Authorization", "Api-Key " + YANDEX_API_KEY);
        try(CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        ) {
            do
            {
                Thread.sleep(2500);
                CloseableHttpResponse response = closeableHttpClient.execute(request);
                String responseData = EntityUtils.toString(response.getEntity());
                jsonObject = new Gson().fromJson(responseData, JsonObject.class);
            }
            while(!jsonObject.get("done").getAsBoolean());

            JsonArray text = jsonObject
                    .getAsJsonObject("response")
                    .getAsJsonArray("chunks");

            text.forEach(x-> stringBuilder.append(x.getAsJsonObject()
                    .getAsJsonArray("alternatives")
                    .get(0).getAsJsonObject()
                    .get("text")
                    .getAsString() + " "));

            return stringBuilder.toString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private String createJson(String fileLink) {
        CONFIG config = new CONFIG();
        config.setSpecification(new Specification("ru-RU", "true"));
        Audio audio = new Audio(fileLink);
        REQUEST request = new REQUEST(config, audio);
        Gson gson = new Gson();
        return gson.toJson(request);
    }

    private String getFileIdOnServer(String fileLink) {
        StringEntity entity = new StringEntity(createJson(fileLink), ContentType.APPLICATION_JSON);
        HttpPost request = new HttpPost("https://transcribe.api.cloud.yandex.net/speech/stt/v2/longRunningRecognize");
        request.setHeader("Authorization", "Api-Key " + YANDEX_API_KEY);
        request.setEntity(entity);
        try(CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = httpClient.execute(request)) {
            String result = EntityUtils.toString(response.getEntity());
            JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
            return jsonObject.get("id").getAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static class Audio {
        private String uri;
        public Audio(String uri) {
            this.uri = uri;
        }
    }
    public static class CONFIG {
        private Specification specification;
        public Specification getSpecification() {
            return specification;
        }
        public void setSpecification(Specification specification) {
            this.specification = specification;
        }
    }
    public static class REQUEST {
        private CONFIG config;
        private Audio audio;
        public REQUEST(CONFIG config, Audio audio) {
            this.config = config;
            this.audio = audio;
        }

    }
    public static class Specification {
        private String languageCode;
        private String literature_text;
        public Specification(String languageCode, String literature_text) {
            this.languageCode = languageCode;
            this.literature_text = literature_text;
        }
    }

}
