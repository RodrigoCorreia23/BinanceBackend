package com.example.binance_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * BinanceClient: encapsula chamadas à API pública e privada da Binance,
 * recebendo as chaves do user dinamicamente (criptografadas no banco de dados).
 *
 * - getKlines(...)       → busca candles (klines) públicos.
 * - fetchFreeBalance(...)→ recupera saldo "free" de um asset (ex: USDT) usando credenciais do user.
 * - placeOrder(...)      → envia ordens privadas (MARKET, LIMIT, STOP_LOSS_LIMIT, TRAILING_STOP_MARKET, LIMIT_MAKER)
 *                         usando HMAC-SHA256 para assinatura.
 */
@Service
public class BinanceClient {

    // WebClient para chamadas públicas (exchangeInfo, klines, etc.)
    private final WebClient publicClient;

    // Base URL da API da Binance (tanto para público quanto para privado)
    private static final String BASE_URL = "https://api.binance.com";

    public BinanceClient() {
        this.publicClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    // =====================================
    // 1) BUSCA DE CANDLES (KLINES) PÚBLICOS
    // =====================================
    /**
     * Retorna uma lista de Candle (openTime, open, high, low, close) para um dado symbol,
     * no intervalo especificado (ex: "5m", "15m", "1h") e limite de candles.
     *
     * @param symbol   par de trading (ex: "BTCUSDT")
     * @param interval intervalo (ex: "5m")
     * @param limit    número de candles a retornar (ex: 50)
     * @return lista de Candle contendo timestamps e preços
     */
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

    // ============================================================
    // 2) BUSCA DE SALDO "FREE" DE UM ASSET (USDT, BTC, ETC.) PRIVADO
    // ============================================================
    /**
     * Recupera o saldo "free" (não usado em ordens) de um determinado asset
     * (por exemplo, "USDT") para o user cujas credenciais foram armazenadas.
     *
     * @param encryptedApiKey    chave da API cifrada no banco
     * @param encryptedSecretKey secret da API cifrada no banco
     * @param asset              código do asset (ex: "USDT", "BTC")
     * @return string com o saldo livre (ex: "123.45678900")
     */
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

    // =====================================
    // 3) ENVIO DE ORDENS PRIVADAS (BUY/SELL) 
    // =====================================
    /**
     * Envia uma ordem (BUY ou SELL) para a Binance, usando as credenciais do user.
     * Aceita ordens do tipo:
     * - MARKET
     * - LIMIT
     * - STOP_LOSS_LIMIT
     * - TRAILING_STOP_MARKET
     * - LIMIT_MAKER
     *
     * Exemplo de uso:
     *   placeOrder(encApiKey, encSecretKey, "BTCUSDT", "BUY", "MARKET", 0.001, null, null);
     *
     * @param encryptedApiKey    chave de API cifrada no banco
     * @param encryptedSecretKey secret de API cifrada no banco
     * @param symbol             par de trading (ex: "BTCUSDT")
     * @param side               "BUY" ou "SELL"
     * @param type               "MARKET", "LIMIT", "STOP_LOSS_LIMIT", "TRAILING_STOP_MARKET", "LIMIT_MAKER"
     * @param quantity           quantidade a comprar/vender (ex: 0.001)
     * @param price              preço de limite (se aplicável; null para ordens MARKET)
     * @param stopPrice          preço de gatilho (para STOP_LOSS_LIMIT ou delta para TRAILING_STOP_MARKET)
     * @return BinanceOrderResponse com dados do orderId, price, status, etc.
     */
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

    // =========================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================
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
        // TODO: implemente sua lógica de descriptografia (AES, RSA, KMS, etc.)
        return encrypted;
    }

    // -------------------------------
    // CLASSES INTERNAS DE RESPOSTA
    // -------------------------------
    /**
     * Representa um candle (kline) retornado pela API pública.
     */
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

    /**
     * Mapeia a resposta da Binance ao criar uma ordem.
     * Contém apenas campos principais; amplie conforme necessidade.
     */
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
