package com.example.webhook;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.HashMap;

@SpringBootApplication
public class WebhookApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(WebhookApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Application started. Sending initial POST request...");

        // Create a RestTemplate instance to send HTTP requests
        RestTemplate restTemplate = new RestTemplate();
        
        // Define the URL for the first POST request
        String generateWebhookUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        // Set the headers for the request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "raghav chandak");
        requestBody.put("regNo", "22BEC0810");
        requestBody.put("email", "raghavchandak1331@gmail.com");

        // Create the HTTP entity with headers and body
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // Send the POST request
            ResponseEntity<Map> response = restTemplate.exchange(
                    generateWebhookUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class);

            // Check if the request was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully generated webhook.");
                Map<String, String> responseBody = response.getBody();
                
                // DEBUG: Print the entire response body to see all available keys
                System.out.println("Full response body: " + responseBody);

                // FIX: Use the correct key "webhook" based on the log output
                String webhookUrl = (String) responseBody.get("webhook"); 
                String accessToken = (String) responseBody.get("accessToken");

                System.out.println("Webhook URL: " + webhookUrl);
                System.out.println("Access Token: " + accessToken);

                // Now, determine which SQL question to solve
                String regNo = requestBody.get("regNo");
                // Extract the last two digits of the registration number
                int lastTwoDigits = Integer.parseInt(regNo.substring(regNo.length() - 2));

                String sqlQuery;
                if (lastTwoDigits % 2 != 0) {
                    // Odd registration number - Question 1
                    System.out.println("Registration number is ODD. Solving Question 1.");
                    sqlQuery = "WITH monthly_sales AS ( " +
                               "    SELECT " +
                               "        DATE_TRUNC('month', sale_date) as sale_month, " +
                               "        product_category, " +
                               "        SUM(sale_amount) as monthly_total, " +
                               "        COUNT(DISTINCT customer_id) as unique_customers " +
                               "    FROM sales s " +
                               "    JOIN products p ON s.product_id = p.product_id " +
                               "    WHERE sale_date >= '2023-01-01' " +
                               "    GROUP BY DATE_TRUNC('month', sale_date), product_category " +
                               "), " +
                               "category_growth AS ( " +
                               "    SELECT " +
                               "        sale_month, " +
                               "        product_category, " +
                               "        monthly_total, " +
                               "        unique_customers, " +
                               "        LAG(monthly_total) OVER ( " +
                               "            PARTITION BY product_category " +
                               "            ORDER BY sale_month " +
                               "        ) as prev_month_total, " +
                               "        (monthly_total - LAG(monthly_total) OVER ( " +
                               "            PARTITION BY product_category " +
                               "            ORDER BY sale_month " +
                               "        )) / LAG(monthly_total) OVER ( " +
                               "            PARTITION BY product_category " +
                               "            ORDER BY sale_month " +
                               "        ) * 100 as growth_percentage " +
                               "    FROM monthly_sales " +
                               ") " +
                               "SELECT " +
                               "    sale_month, " +
                               "    product_category, " +
                               "    monthly_total, " +
                               "    unique_customers, " +
                               "    ROUND(growth_percentage, 2) as growth_percentage " +
                               "FROM category_growth " +
                               "WHERE growth_percentage IS NOT NULL " +
                               "    AND growth_percentage > 10 " +
                               "ORDER BY sale_month DESC, growth_percentage DESC;";
                } else {
                    // Even registration number - Question 2
                    System.out.println("Registration number is EVEN. Solving Question 2.");
                    sqlQuery = "SELECT " +
                               "    e1.EMP_ID, " +
                               "    e1.FIRST_NAME, " +
                               "    e1.LAST_NAME, " +
                               "    d.DEPARTMENT_NAME, " +
                               "    (SELECT COUNT(*) " +
                               "     FROM Employees e2 " +
                               "     WHERE e2.DEPARTMENT_ID = e1.DEPARTMENT_ID AND e2.DOB > e1.DOB) AS YOUNGER_EMPLOYEES_COUNT " +
                               "FROM " +
                               "    Employees e1 " +
                               "JOIN " +
                               "    Departments d ON e1.DEPARTMENT_ID = d.DEPARTMENT_ID " +
                               "ORDER BY " +
                               "    e1.EMP_ID DESC;";
                }

                // Submit the solution
                submitSolution(webhookUrl, accessToken, sqlQuery);

            } else {
                System.err.println("Failed to generate webhook. Status code: " + response.getStatusCode());
                System.err.println("Response body: " + response.getBody());
            }

        } catch (Exception e) {
            System.err.println("An error occurred during the webhook generation request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void submitSolution(String webhookUrl, String accessToken, String sqlQuery) {
        System.out.println("Submitting the final SQL query...");

        RestTemplate restTemplate = new RestTemplate();
        
        String submitUrl;
        // Use the dynamic webhook URL if it exists, otherwise fall back to the static one
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            System.out.println("Using dynamic webhook URL for submission: " + webhookUrl);
            submitUrl = webhookUrl;
        } else {
            System.out.println("Dynamic webhook URL is null. Falling back to static URL from PDF.");
            submitUrl = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
        }

        // Set the headers for the submission request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // FIX: Send the token without the "Bearer " prefix as a test
        headers.set("Authorization", accessToken);

        // Create the request body for submission
        Map<String, String> submissionBody = new HashMap<>();
        submissionBody.put("finalQuery", sqlQuery);

        // Create the HTTP entity for submission
        HttpEntity<Map<String, String>> submissionEntity = new HttpEntity<>(submissionBody, headers);

        try {
            // Send the final POST request
            ResponseEntity<String> submissionResponse = restTemplate.exchange(
                    submitUrl,
                    HttpMethod.POST,
                    submissionEntity,
                    String.class);
            
            if (submissionResponse.getStatusCode().is2xxSuccessful()) {
                System.out.println("Solution submitted successfully!");
                System.out.println("Response: " + submissionResponse.getBody());
            } else {
                System.err.println("Failed to submit solution. Status code: " + submissionResponse.getStatusCode());
                System.err.println("Response body: " + submissionResponse.getBody());
            }

        } catch (Exception e) {
            System.err.println("An error occurred during solution submission: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

