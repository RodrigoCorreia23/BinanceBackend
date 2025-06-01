package com.example.binance_backend.service;

import okhttp3.*;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BinanceClient {
    private final OkHttpClient http = new OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public String fetchFreeBalance(String apiKey, String secretKey, String asset) {
        try {
            long ts = System.currentTimeMillis();
            String query = "timestamp=" + ts;
            String signature = hmacSha256(secretKey, query);

            Request req = new Request.Builder()
                .url("https://api.binance.com/api/v3/account?" + query + "&signature=" + signature)
                .addHeader("X-MBX-APIKEY", apiKey)
                .build();

            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    throw new IOException("Binance API error: " + resp.code());
                }
                Map<?,?> json = mapper.readValue(resp.body().string(), Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String,String>> balances = (List<Map<String,String>>) json.get("balances");
                for (var b : balances) {
                    if (asset.equals(b.get("asset"))) {
                        return b.get("free");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar Binance", e);
        }
        return "0";
    }

    private String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
