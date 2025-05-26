package io.github.anikafokken.so_springboot.controllers;

// For working with JSON (Jackson)
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// For Spring Boot annotations and ResponseEntity
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

// For handling JSON file (if you're reading from a file)
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import desktop.JsonFileHandler;
import desktop.PerformanceGroup;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class SpringBootController {
    private static final Logger logger = Logger.getLogger(SpringBootController.class.getName());

    @PostMapping("/datapost")
    public ResponseEntity<String> receiveData(@RequestBody JsonNode payload) {
        ObjectMapper objectMapper = new ObjectMapper();
        // Handle the incoming data from the spreadsheet
        System.out.println("Received Data: " + payload.toString());
        // JsonFileHandler handler = new JsonFileHandler();
        JsonFileHandler handler = new JsonFileHandler();
        List<PerformanceGroup> groups = new ArrayList<>();

        // handler.writeGroupScheduleToJson(payload, "travelling_data.json");
        saveJsonToFile(payload, "travelling_data.json");
        // Process the data (e.g., store in a database or log it)
        return ResponseEntity.ok("Data received successfully! Data: " + payload.toString());

    }

    public void saveJsonToFile(JsonNode jsonNode, String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonFileHandler handler = new JsonFileHandler();

        try {
            // Read the existing JSON file
            File file = new File(filePath);
            JsonNode existingData = objectMapper.readTree(file);

            // Merge the new data with the existing data
            if (existingData.isArray() && jsonNode.isArray()) {
                // Assuming both existingData and jsonNode are arrays
                ArrayNode arrayNode = (ArrayNode) existingData;
                for (JsonNode newData : jsonNode) {
                    arrayNode.add(newData); // Merge new data into the existing array
                }
            } else if (existingData.isObject() && jsonNode.isObject()) {
                // Merge if both are objects (you can define specific merging logic here)
                ((ObjectNode) existingData).setAll((ObjectNode) jsonNode);
            }

            // Write the updated data back to the file
            objectMapper.writeValue(file, existingData);
            System.out.println("Updated JSON file: " + existingData.toString());

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error saving JSON to file.");
        }
    }

    public String testEndpoint() {
        return "Hello from backend!";
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to Schedule Optimizer!";
    }

    // Send JSON data when accessed via a GET request
    @GetMapping("/dataget")
    public ResponseEntity<String> sendData() {
        try {
            // Create a simple JSON object using Jackson ObjectMapper
            JsonFileHandler handler = new JsonFileHandler();
            ObjectMapper objectMapper = new ObjectMapper();

            File file = new File("travelling_data.json");
            if (!file.exists()) {
                return ResponseEntity.status(500).body("{\"error\": \"JSON file not found\"}");
            }
            JsonNode jsonNode = objectMapper.readTree(file);

            handler.readFromJsonFile("travelling_data.json");
            String jsonData = jsonNode.toString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Return the JSON data as a response with HTTP status 200 OK
            return ResponseEntity.ok().headers(headers).body(jsonData);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("{\"error\": \"Failed to generate JSON\"}");
        }
    }

    // TODO: stop the server when done
}
