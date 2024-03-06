package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// Класс для работы с API Честного знака
class CrptApi {
    private final Semaphore semaphore; // Семафор для управления лимитом запросов
    private final HttpClient httpClient; // HTTP клиент для отправки запросов
    private final int requestLimit; // Ограничение на количество запросов
    private final long duration; // Количество времени ограничения на количество запросов
    private final TimeUnit timeUnit; // Единица времени ограничения на количество запросов

    // Конструктор класса CrptApi
    public CrptApi(TimeUnit timeUnit, long duration, int requestLimit) {
        semaphore = new Semaphore(requestLimit);
        this.duration = duration;
        this.timeUnit = timeUnit;
        httpClient = HttpClient.newBuilder().build();
        this.requestLimit = requestLimit;
        updateLimit();
    }

    // Метод для обновления лимита запросов в указанном времени
    private void updateLimit() {
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(timeUnit.toMillis(duration));
                    semaphore.release(requestLimit);
                }
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }).start(); // Запуск потока
    }

    // Метод для создания документа
    public void createDocument(Document document, String signature) throws JsonProcessingException {
        try {
            semaphore.acquire();
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonDocument = objectMapper.writeValueAsString(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(System.out::println);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// Класс для представления документа
@Data
@AllArgsConstructor
class Document {
    private Description description;
    private String doc_id;
    private String doc_status;
    private String doc_type;
    private boolean importRequest;
    private String owner_inn;
    private String participant_inn;
    private String producer_inn;
    private String production_date;
    private String production_type;
    private List<Product> products;
    private String reg_date;
    private String reg_number;
}

// Класс для описания документа
@Data
@AllArgsConstructor
class Description {
    private String participantInn;
}

// Класс для представления продукта
@Data
@AllArgsConstructor
class Product {
    private String certificate_document;
    private String certificate_document_date;
    private String certificate_document_number;
    private String owner_inn;
    private String producer_inn;
    private String production_date;
    private String tnved_code;
    private String uit_code;
    private String uitu_code;
}

public class App {
    public static void main(String[] args) throws JsonProcessingException {
        // Создаем экземпляр CrptApi
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10, 5);

        // Создаем пример документа и подписи
        Document sampleDocument = new Document(
                new Description("sampleInn"),
                "sample",
                "sample",
                "sample",
                true,
                "sample",
                "sample",
                "sample",
                "sample",
                "sample",
                List.of(new Product(
                        "sample",
                        "sample",
                        "sample",
                        "sample",
                        "sample",
                        "sample",
                        "sample",
                        "sample",
                        "sample"
                )),
                "sample",
                "sample"
        );
        String sampleSignature = "sampleSignature";

        // Вызываем метод для создания документа
        crptApi.createDocument(sampleDocument, sampleSignature);
    }
}
