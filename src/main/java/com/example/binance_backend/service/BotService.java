package com.example.binance_backend.service;

import com.example.binance_backend.model.*;
import com.example.binance_backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class BotService {

    private static final Logger logger = LoggerFactory.getLogger(BotService.class);

    private final BotSettingsRepository botSettingsRepo;
    private final BotStateRepository botStateRepo;
    private final BotStateHistoryRepository botStateHistoryRepo;
    private final BotTradeRepository botTradeRepo;
    private final UserRepository userRepo;
    private final UserCredentialsRepository userCredentialsRepo;
    private final BinanceClient binanceClient;

    @Value("${bot.simulation:true}")
    private boolean simulationMode;

    public BotService(
            BotSettingsRepository botSettingsRepo,
            BotStateRepository botStateRepo,
            BotStateHistoryRepository botStateHistoryRepo,
            BotTradeRepository botTradeRepo,
            UserRepository userRepo,
            UserCredentialsRepository userCredentialsRepo,
            BinanceClient binanceClient
    ) {
        this.botSettingsRepo = botSettingsRepo;
        this.botStateRepo = botStateRepo;
        this.botStateHistoryRepo = botStateHistoryRepo;
        this.botTradeRepo = botTradeRepo;
        this.userRepo = userRepo;
        this.userCredentialsRepo = userCredentialsRepo;
        this.binanceClient = binanceClient;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void runAllActiveBots() {
        logger.info("-----------------------------------------------------");
        logger.info("Inicio do ciclo do bot (30s) - Modo: {}", simulationMode ? "SIMULACAO" : "PRODUCAO");

        List<BotState> activeStates = botStateRepo.findAllByIsActiveTrue();
        if (activeStates.isEmpty()) {
            logger.info("Nenhum bot ativo no momento.");
        }

        for (BotState state : activeStates) {
            User user = state.getUser();
            Optional<BotSettings> maybeSettings = botSettingsRepo.findByUser(user);
            if (maybeSettings.isEmpty()) {
                logger.warn("User {} tem bot ativo mas sem configuracoes definidas.", user.getId());
                continue;
            }
            BotSettings settings = maybeSettings.get();

            try {
                executeBotCycle(user, state, settings);
            } catch (ResponseStatusException e) {
                logger.error("Erro ao processar bot do user {}: {} (HTTP {})",
                        user.getId(), e.getReason(), e.getStatusCode().value());
            } catch (Exception e) {
                logger.error("Erro inesperado ao processar bot do user {}: {}",
                        user.getId(), e.getMessage());
            }
        }

        logger.info("Fim do ciclo do bot.");
        logger.info("-----------------------------------------------------");
    }

    private void executeBotCycle(User user, BotState state, BotSettings settings) {
        String symbol = settings.getTradingPair();
        String interval = "5m";
        int limit = 50;

        List<BigDecimal> closes;
        BigDecimal lastPrice;

        logger.info("[Modo: {}] user: {} | Par: {}", simulationMode ? "SIMULACAO" : "PRODUCAO", user.getId(), symbol);

        if (simulationMode) {
            logger.info("A obter candles (simulado, sem chamada a API)...");
            closes = simulateCloses(user.getId(), limit);
            lastPrice = closes.get(closes.size() - 1);
        } else {
            List<BinanceClient.Candle> candles = binanceClient.getKlines(symbol, interval, limit);
            if (candles.size() < 20) {
                logger.warn("Dados insuficientes ({} candles) para {}, user {}.", candles.size(), symbol, user.getId());
                return;
            }
            closes = new ArrayList<>();
            for (BinanceClient.Candle c : candles) {
                closes.add(c.close);
            }
            lastPrice = closes.get(closes.size() - 1);
        }

        int rsiPeriod = settings.getRsiThreshold() != null ? settings.getRsiThreshold() : 14;
        BigDecimal rsi = calculateRSI(closes, rsiPeriod);
        MACDResult macdResult = calculateMACD(closes, 12, 26, 9);
        BollingerResult bb = calculateBollingerBands(closes, 20, 2.0);

        Optional<UserCredentials> credOpt = userCredentialsRepo.findByUser(user);
        if (credOpt.isEmpty()) {
            logger.warn("user {} nao tem credenciais Binance associadas.", user.getId());
            return;
        }
        UserCredentials creds = credOpt.get();

        Optional<BotTrade> maybeOpenTrade = botTradeRepo.findOpenTradeByUserAndSymbol(user.getId(), symbol);

        if (maybeOpenTrade.isPresent()) {
            logger.info("Trade em aberto detectado.");
        } else {
            logger.info("Sem trade ativo.");
        }

        logger.info("Indicadores:");
        logger.info("- RSI: {}", rsi.setScale(2, RoundingMode.HALF_UP));
        logger.info("- MACD.hist: {}", macdResult.histogram.setScale(3, RoundingMode.HALF_UP));
        logger.info("- BB.lower: {} | BB.upper: {}", bb.lowerBand.setScale(2, RoundingMode.HALF_UP), bb.upperBand.setScale(2, RoundingMode.HALF_UP));
        logger.info("- Ultimo preco: {}", lastPrice.setScale(2, RoundingMode.HALF_UP));

        if (maybeOpenTrade.isPresent()) {
            processOpenTrade(maybeOpenTrade.get(), settings, lastPrice, rsi, macdResult, bb,
                    creds.getEncryptedApiKey(), creds.getEncryptedSecretKey());
        } else {
            boolean rsiBuySignal = settings.isRsiEnabled() &&
                    rsi.compareTo(BigDecimal.valueOf(settings.getRsiThreshold())) < 0;
            boolean macdBuySignal = settings.isMacdEnabled() &&
                    macdResult.histogram.compareTo(BigDecimal.ZERO) > 0;
            boolean bbBuySignal = settings.isMovingAvgEnabled() &&
                    lastPrice.compareTo(bb.lowerBand) < 0;

            logger.info("Sinais de compra:");
            logger.info("- RSI ({}) < limite ({})? {}", rsi.setScale(2, RoundingMode.HALF_UP), settings.getRsiThreshold(), rsiBuySignal);
            logger.info("- MACD positivo? {}", macdBuySignal);
            logger.info("- Preco < BB.lower? {}", bbBuySignal);

            if (rsiBuySignal || macdBuySignal || bbBuySignal) {
                if (simulationMode) {
                    logger.info("Resultado: ordem NAO enviada (simulacao ativa)");
                } else {
                    placeOrderAccordingToType(user, settings, lastPrice,
                            creds.getEncryptedApiKey(), creds.getEncryptedSecretKey());
                }
            } else {
                logger.info("Resultado: nenhum sinal valido - sem ordem de compra.");
            }
        }
    }

    private void processOpenTrade(
            BotTrade openTrade,
            BotSettings settings,
            BigDecimal lastPrice,
            BigDecimal rsi,
            MACDResult macdResult,
            BollingerResult bb,
            String encApiKey,
            String encSecretKey
    ) {
        String symbol = openTrade.getSymbol();
        BigDecimal entry = openTrade.getPrice();
        BigDecimal amount = openTrade.getAmount();

        BigDecimal slPercent = settings.getStopLossPerc().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal tpPercent = settings.getTakeProfitPerc().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal stopLossPrice = entry.multiply(BigDecimal.ONE.subtract(slPercent));
        BigDecimal takeProfitPrice = entry.multiply(BigDecimal.ONE.add(tpPercent));

        if (lastPrice.compareTo(stopLossPrice) <= 0) {
            logger.info("Stop-loss atingido para {}. Ultimo preco: {} <= SL: {}", symbol, lastPrice.setScale(4,RoundingMode.HALF_UP), stopLossPrice.setScale(4,RoundingMode.HALF_UP));
            if (simulationMode) {
                logger.info("Simulacao ativa: venda por stop-loss NAO executada.");
            } else {
                closeTrade(openTrade, lastPrice, "STOP_LOSS", encApiKey, encSecretKey);
            }
            return;
        } else if (lastPrice.compareTo(takeProfitPrice) >= 0) {
            logger.info("Take-profit atingido para {}. Ultimo preco: {} >= TP: {}", symbol, lastPrice.setScale(4,RoundingMode.HALF_UP), takeProfitPrice.setScale(4,RoundingMode.HALF_UP));
            if (simulationMode) {
                logger.info("Simulacao ativa: venda por take-profit NAO executada.");
            } else {
                closeTrade(openTrade, lastPrice, "TAKE_PROFIT", encApiKey, encSecretKey);
            }
            return;
        }

        boolean rsiSell = settings.isRsiEnabled() && rsi.compareTo(BigDecimal.valueOf(70)) > 0;
        boolean macdSell = settings.isMacdEnabled() && macdResult.histogram.compareTo(BigDecimal.ZERO) < 0;
        boolean bbSell = settings.isMovingAvgEnabled() && lastPrice.compareTo(bb.upperBand) > 0;

        logger.info("Sinais de venda:");
        logger.info("- RSI ({}) > 70? {}", rsi.setScale(2,RoundingMode.HALF_UP), rsiSell);
        logger.info("- MACD negativo? {}", macdSell);
        logger.info("- Preco > BB.upper? {}", bbSell);

        if (rsiSell || macdSell || bbSell) {
            if (simulationMode) {
                logger.info("Simulacao ativa: venda por sinal tecnico NAO executada.");
            } else {
                closeTrade(openTrade, lastPrice, "INDICATOR_SELL", encApiKey, encSecretKey);
            }
        } else {
            logger.info("Trade mantido. Preco entrada: {} | Ultimo preco: {}", entry.setScale(4,RoundingMode.HALF_UP), lastPrice.setScale(4,RoundingMode.HALF_UP));
        }
    }

    private void closeTrade(
            BotTrade openTrade,
            BigDecimal exitPrice,
            String reason,
            String encApiKey,
            String encSecretKey
    ) {
        String symbol = openTrade.getSymbol();
        BigDecimal amount = openTrade.getAmount();

        try {
            BinanceClient.BinanceOrderResponse response = binanceClient.placeOrder(
                    encApiKey, encSecretKey, symbol, "SELL", "MARKET", amount, null, null);
            exitPrice = response.price;
            logger.info("Venda executada para {} a {} (motivo: {})", symbol, exitPrice.setScale(4,RoundingMode.HALF_UP), reason);
        } catch (Exception e) {
            logger.error("Erro ao fechar trade para {}: {}", symbol, e.getMessage());
            return;
        }

        BigDecimal entryPrice = openTrade.getPrice();
        openTrade.setExecutedAt(OffsetDateTime.now());
        openTrade.setPrice(exitPrice);
        openTrade.setStatus("CLOSED");
        BigDecimal profit = exitPrice.subtract(entryPrice).multiply(amount);
        openTrade.setProfitEstimate(profit.setScale(8, RoundingMode.HALF_UP));
        botTradeRepo.save(openTrade);

        logger.info("Trade encerrado: lucro estimado = {}", profit.setScale(8,RoundingMode.HALF_UP));
    }

    private void placeOrderAccordingToType(
            User user,
            BotSettings settings,
            BigDecimal lastPrice,
            String encApiKey,
            String encSecretKey
    ) {
        String symbol = settings.getTradingPair();
        BigDecimal quantity = settings.getTradeAmount();
        String orderType = settings.getOrderType();
        BigDecimal limitPrice = settings.getLimitPrice();
        BigDecimal stopPrice = settings.getStopPrice();
        BigDecimal trailingDelta = settings.getTrailingDelta();

        String binanceType;
        switch (orderType.toUpperCase(Locale.ROOT)) {
            case "MARKET": binanceType = "MARKET"; break;
            case "LIMIT": binanceType = "LIMIT"; break;
            case "STOP-LIMIT": binanceType = "STOP_LOSS_LIMIT"; break;
            case "LIMIT MAKER": binanceType = "LIMIT_MAKER"; break;
            case "TRAILING STOP": binanceType = "TRAILING_STOP_MARKET"; break;
            default: binanceType = "MARKET"; break;
        }

        BigDecimal priceParam = null;
        BigDecimal stopPriceParam = null;

        if (binanceType.equals("LIMIT") || binanceType.equals("LIMIT_MAKER")) {
            if (limitPrice == null) {
                logger.error("Limite de preco ausente para ordem LIMIT.");
                return;
            }
            priceParam = limitPrice;
        }

        if (binanceType.equals("STOP_LOSS_LIMIT")) {
            if (stopPrice == null || limitPrice == null) {
                logger.error("Faltam valores para ordem STOP-LIMIT (stopPrice e/ou limitPrice).");
                return;
            }
            priceParam = limitPrice;
            stopPriceParam = stopPrice;
        }

        if (binanceType.equals("TRAILING_STOP_MARKET")) {
            if (trailingDelta == null) {
                logger.error("TrailingDelta nao definido para TRAILING STOP.");
                return;
            }
            stopPriceParam = trailingDelta;
        }

        logger.info("Enviando ordem BUY:");
        logger.info("- Tipo: {}", binanceType);
        logger.info("- Quantidade: {}", quantity);
        logger.info("- Preco: {}", priceParam != null ? priceParam.setScale(4,RoundingMode.HALF_UP) : "-");
        logger.info("- StopPrice: {}", stopPriceParam != null ? stopPriceParam.setScale(4,RoundingMode.HALF_UP) : "-");

        try {
            BinanceClient.BinanceOrderResponse response = binanceClient.placeOrder(
                    encApiKey, encSecretKey, symbol, "BUY", binanceType, quantity, priceParam, stopPriceParam);

            BigDecimal execPrice = response.price;
            logger.info("Ordem BUY executada: preco = {} | OrderID = {}", execPrice.setScale(4,RoundingMode.HALF_UP), response.orderId);

            BotTrade newTrade = new BotTrade();
            newTrade.setUser(user);
            newTrade.setSymbol(symbol);
            newTrade.setSide("buy");
            newTrade.setAmount(quantity);
            newTrade.setPrice(execPrice);
            newTrade.setStatus("OPEN");
            botTradeRepo.save(newTrade);

            logger.info("Novo trade aberto: ID={} | Entrada: {} | Quantidade: {}",
                    newTrade.getId(), execPrice.setScale(4,RoundingMode.HALF_UP), quantity.setScale(6,RoundingMode.HALF_UP));

        } catch (Exception e) {
            logger.error("Erro ao executar ordem BUY: {}", e.getMessage());
        }
    }

        private List<BigDecimal> simulateCloses(UUID userId, int limit) {
        List<BigDecimal> closes = new ArrayList<>();
        BigDecimal price0 = BigDecimal.valueOf(100.00);
        Random rnd = new Random(userId.hashCode() + System.currentTimeMillis());
        for (int i = 0; i < limit; i++) {
            BigDecimal delta = BigDecimal.valueOf(rnd.nextDouble() * 0.02 - 0.01);
            price0 = price0.multiply(BigDecimal.ONE.add(delta));
            closes.add(price0);
        }
        return closes;
    }

    private BigDecimal calculateRSI(List<BigDecimal> closes, int period) {
        int n = closes.size();
        if (n <= period) {
            return BigDecimal.ZERO;
        }
        BigDecimal gainSum = BigDecimal.ZERO;
        BigDecimal lossSum = BigDecimal.ZERO;
        for (int i = n - period; i < n; i++) {
            BigDecimal change = closes.get(i).subtract(closes.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gainSum = gainSum.add(change);
            } else {
                lossSum = lossSum.add(change.abs());
            }
        }
        BigDecimal avgGain = gainSum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal avgLoss = lossSum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP)
        );
    }

    private MACDResult calculateMACD(List<BigDecimal> closes, int fastPeriod, int slowPeriod, int signalPeriod) {
        BigDecimal emaFast = calculateEMA(closes, fastPeriod);
        BigDecimal emaSlow = calculateEMA(closes, slowPeriod);
        BigDecimal macdLine = emaFast.subtract(emaSlow);
        List<BigDecimal> diffList = new ArrayList<>();
        for (int i = 0; i < closes.size(); i++) {
            List<BigDecimal> sub = closes.subList(0, i + 1);
            diffList.add(calculateEMA(sub, fastPeriod).subtract(calculateEMA(sub, slowPeriod)));
        }
        int sz = diffList.size();
        BigDecimal signalLine = calculateEMA(diffList.subList(sz - signalPeriod, sz), signalPeriod);
        BigDecimal histogram = macdLine.subtract(signalLine);
        return new MACDResult(macdLine, signalLine, histogram);
    }

    private BigDecimal calculateEMA(List<BigDecimal> closes, int period) {
        int n = closes.size();
        if (n < period) {
            return closes.get(n - 1);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(closes.get(i));
        }
        BigDecimal sma = sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal ema = sma;
        BigDecimal k = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(period + 1), 8, RoundingMode.HALF_UP);
        for (int i = period; i < n; i++) {
            ema = closes.get(i).multiply(k).add(ema.multiply(BigDecimal.ONE.subtract(k)));
        }
        return ema;
    }

    private BollingerResult calculateBollingerBands(List<BigDecimal> closes, int period, double multiplier) {
        int n = closes.size();
        if (n < period) {
            return new BollingerResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = n - period; i < n; i++) {
            sum = sum.add(closes.get(i));
        }
        BigDecimal sma = sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal variance = BigDecimal.ZERO;
        for (int i = n - period; i < n; i++) {
            BigDecimal diff = closes.get(i).subtract(sma);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal stdDev = sqrt(variance, 8);
        BigDecimal upper = sma.add(stdDev.multiply(BigDecimal.valueOf(multiplier)));
        BigDecimal lower = sma.subtract(stdDev.multiply(BigDecimal.valueOf(multiplier)));
        return new BollingerResult(sma, upper, lower);
    }

    private BigDecimal sqrt(BigDecimal value, int scale) {
        BigDecimal x0 = BigDecimal.ZERO;
        BigDecimal x1 = new BigDecimal(Math.sqrt(value.doubleValue()));
        while (!x0.equals(x1)) {
            x0 = x1;
            x1 = value.divide(x0, scale, RoundingMode.HALF_UP)
                    .add(x0)
                    .divide(BigDecimal.valueOf(2), scale, RoundingMode.HALF_UP);
        }
        return x1;
    }

    private static class MACDResult {
        public final BigDecimal macdLine;
        public final BigDecimal signalLine;
        public final BigDecimal histogram;
        public MACDResult(BigDecimal macdLine, BigDecimal signalLine, BigDecimal histogram) {
            this.macdLine = macdLine;
            this.signalLine = signalLine;
            this.histogram = histogram;
        }
    }

    private static class BollingerResult {
        public final BigDecimal middleBand;
        public final BigDecimal upperBand;
        public final BigDecimal lowerBand;
        public BollingerResult(BigDecimal middle, BigDecimal upper, BigDecimal lower) {
            this.middleBand = middle;
            this.upperBand = upper;
            this.lowerBand = lower;
        }
    }
}

