package com.example.binance_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class BinanceClient {

    // WebClient para chamadas públicas
    private final WebClient publicClient;

    // Base URL da API da Binance
    private static final String BASE_URL = "https://api.binance.com";

    public BinanceClient() {
        this.publicClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

   
    public List<Candle> getKlines(String symbol, String interval, int limit) {
        String path = "/api/v3/klines"
                + "?symbol=" + symbol
                + "&interval=" + interval
                + "&limit=" + limit;

        JsonNode[] raw = publicClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(JsonNode[].class)
                .block();

        List<Candle> candles = new ArrayList<>();
        if (raw != null) {
            for (JsonNode arr : raw) {
                long openTime = arr.get(0).asLong();
                BigDecimal open  = new BigDecimal(arr.get(1).asText());
                BigDecimal high  = new BigDecimal(arr.get(2).asText());
                BigDecimal low   = new BigDecimal(arr.get(3).asText());
                BigDecimal close = new BigDecimal(arr.get(4).asText());
                candles.add(new Candle(openTime, open, high, low, close));
            }
        }
        return candles;
    }

    
    public String fetchFreeBalance(String encryptedApiKey, String encryptedSecretKey, String asset) {
        // 1) Descriptografa as credenciais
        String apiKey    = decrypt(encryptedApiKey);
        String secretKey = decrypt(encryptedSecretKey);

        // 2) Prepara timestamp e parâmetros para /api/v3/account
        long timestamp = Instant.now().toEpochMilli();
        Map<String, String> params = new TreeMap<>();
        params.put("timestamp", String.valueOf(timestamp));

        String queryString = buildQueryString(params);
        String signature = hmacSHA256(queryString, secretKey);
        queryString += "&signature=" + signature;

        // 3) Cria WebClient privado com header X-MBX-APIKEY
        WebClient privateClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("X-MBX-APIKEY", apiKey)
                .build();

        // 4) Faz GET para /api/v3/account para obter balanços
        JsonNode resp = privateClient.get()
                .uri("/api/v3/account?" + queryString)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        // 5) Itera sobre "balances" para encontrar o asset desejado
        if (resp != null && resp.has("balances")) {
            for (JsonNode b : resp.get("balances")) {
                if (asset.equalsIgnoreCase(b.get("asset").asText())) {
                    return b.get("free").asText();
                }
            }
        }
        return "0";
    }

    public BinanceOrderResponse placeOrder(
            String encryptedApiKey,
            String encryptedSecretKey,
            String symbol,
            String side,
            String type,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal stopPrice
    ) {
        // 1) Descriptografa credenciais
        String apiKey    = decrypt(encryptedApiKey);
        String secretKey = decrypt(encryptedSecretKey);

        // 2) Prepara timestamp e parâmetros obrigatórios para /api/v3/order
        long timestamp = Instant.now().toEpochMilli();
        Map<String, String> params = new TreeMap<>();
        params.put("symbol", symbol);
        params.put("side", side);
        params.put("type", type);
        params.put("quantity", quantity.stripTrailingZeros().toPlainString());
        if (price != null) {
            params.put("price", price.stripTrailingZeros().toPlainString());
        }
        if (stopPrice != null) {
            params.put("stopPrice", stopPrice.stripTrailingZeros().toPlainString());
        }
        params.put("timestamp", String.valueOf(timestamp));

        String queryString = buildQueryString(params);
        String signature = hmacSHA256(queryString, secretKey);
        queryString += "&signature=" + signature;

        // 3) Cria WebClient privado com X-MBX-APIKEY
        WebClient privateClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("X-MBX-APIKEY", apiKey)
                .build();

        // 4) Dispara POST para /api/v3/order
        BinanceOrderResponse resp = privateClient.post()
                .uri("/api/v3/order?" + queryString)
                .retrieve()
                .bodyToMono(BinanceOrderResponse.class)
                .block();

        return resp;
    }
    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    private String hmacSHA256(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes());
            // converte em hex
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar HMACSHA256", e);
        }
    }

    /**
     * Descriptografa uma string cifrada. 
     */
    private String decrypt(String encrypted) {
        // TODO: implementar a lógica de descriptografia (AES, RSA, KMS, etc.)
        return encrypted;
    }

    public static class Candle {
        public long openTime;
        public BigDecimal open, high, low, close;
        public Candle(long openTime, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
            this.openTime = openTime;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
        }
    }

    public static class BinanceOrderResponse {
        public String symbol;
        public String orderId;
        public String status;
        public String side;
        public String type;
        public BigDecimal executedQty;
        public BigDecimal cummulativeQuoteQty;
        public BigDecimal price;
        
    }
}
