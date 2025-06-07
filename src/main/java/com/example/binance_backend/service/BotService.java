package com.example.binance_backend.service;

import com.example.binance_backend.model.BotSettings;
import com.example.binance_backend.model.BotState;
import com.example.binance_backend.model.BotTrade;
import com.example.binance_backend.model.User;
import com.example.binance_backend.model.UserCredentials;
import com.example.binance_backend.repository.BotSettingsRepository;
import com.example.binance_backend.repository.BotStateHistoryRepository;
import com.example.binance_backend.repository.BotStateRepository;
import com.example.binance_backend.repository.BotTradeRepository;
import com.example.binance_backend.repository.UserCredentialsRepository;
import com.example.binance_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

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

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void runAllActiveBots() {
        logger.info("==== Iniciando ciclo de avaliação do bot (30s) — modo: {} ====",
                simulationMode ? "SIMULAÇÃO" : "PRODUÇÃO");

        List<BotState> activeStates = botStateRepo.findAllByIsActiveTrue();
        if (activeStates.isEmpty()) {
            logger.info("Nenhum bot ativo no momento.");
        }

        for (BotState state : activeStates) {
            User user = state.getUser();
            Optional<BotSettings> maybeSettings = botSettingsRepo.findByUser(user);
            if (maybeSettings.isEmpty()) {
                logger.warn("Usuário {} possui bot ativo mas sem configurações.", user.getId());
                continue;
            }
            BotSettings settings = maybeSettings.get();

            try {
                executeBotCycle(user, state, settings);
            } catch (ResponseStatusException e) {
                logger.error("Erro ao processar bot para usuário {}: {} (HTTP {})",
                        user.getId(), e.getReason(), e.getStatusCode().value());
            } catch (Exception e) {
                logger.error("Erro inesperado ao processar bot para usuário {}: {}",
                        user.getId(), e.getMessage());
            }
        }

        logger.info("==== Fim do ciclo de avaliação do bot ====");
    }

    private void executeBotCycle(User user, BotState state, BotSettings settings) {
        String symbol = settings.getTradingPair();
        String interval = "5m";
        int limit = 50;

        List<BigDecimal> closes;
        BigDecimal lastPrice;

        if (simulationMode) {
            logger.info("[SIMULAÇÃO | Usuário: {} | {}] Buscando candles de forma simulada (não chama a API).",
                        user.getId(), symbol);
            closes = simulateCloses(user.getId(), limit);
            lastPrice = closes.get(closes.size() - 1);
        } else {
            List<BinanceClient.Candle> candles = binanceClient.getKlines(symbol, interval, limit);
            if (candles.size() < 20) {
                logger.warn("[Usuário: {} | {}] Dados insuficientes de candles (obtidos: {})",
                            user.getId(), symbol, candles.size());
                return;
            }
            closes = new ArrayList<>();
            for (BinanceClient.Candle c : candles) {
                closes.add(c.close);
            }
            lastPrice = closes.get(closes.size() - 1);
        }

        int rsiPeriod = (settings.getRsiThreshold() != null) ? settings.getRsiThreshold() : 14;
        BigDecimal rsi = calculateRSI(closes, rsiPeriod);
        MACDResult macdResult = calculateMACD(closes, 12, 26, 9);
        BollingerResult bb = calculateBollingerBands(closes, 20, 2.0);

        Optional<UserCredentials> credOpt = userCredentialsRepo.findByUser(user);
        if (credOpt.isEmpty()) {
            logger.warn("[Usuário: {} | {}] não possui credenciais Binance cadastradas.", user.getId(), symbol);
            return;
        }
        UserCredentials creds = credOpt.get();

        Optional<BotTrade> maybeOpenTrade = botTradeRepo.findOpenTradeByUserAndSymbol(user.getId(), symbol);

        if (maybeOpenTrade.isPresent()) {
            BotTrade openTrade = maybeOpenTrade.get();
            logger.info("[Usuário: {} | {}] Trade aberto: ID={} | Side={} | EntryPrice={} | Amount={}",
                    user.getId(), symbol, openTrade.getId(), openTrade.getSide(),
                    openTrade.getPrice().setScale(4, RoundingMode.HALF_UP),
                    openTrade.getAmount().setScale(6, RoundingMode.HALF_UP));
            logger.info("--- Indicadores: RSI={} | MACD.hist={} | BB.lower={} | BB.upper={} | LastPrice={}",
                    rsi.setScale(4, RoundingMode.HALF_UP),
                    macdResult.histogram.setScale(6, RoundingMode.HALF_UP),
                    bb.lowerBand.setScale(4, RoundingMode.HALF_UP),
                    bb.upperBand.setScale(4, RoundingMode.HALF_UP),
                    lastPrice.setScale(4, RoundingMode.HALF_UP)
            );
        } else {
            logger.info("[Usuário: {} | {}] Nenhum trade aberto.", user.getId(), symbol);
            logger.info("--- Indicadores: RSI={} | MACD.hist={} | BB.lower={} | BB.upper={} | LastPrice={}",
                    rsi.setScale(4, RoundingMode.HALF_UP),
                    macdResult.histogram.setScale(6, RoundingMode.HALF_UP),
                    bb.lowerBand.setScale(4, RoundingMode.HALF_UP),
                    bb.upperBand.setScale(4, RoundingMode.HALF_UP),
                    lastPrice.setScale(4, RoundingMode.HALF_UP)
            );
        }

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

            if (rsiBuySignal || macdBuySignal || bbBuySignal) {
                logger.info("[Usuário: {} | {}] Sinal de COMPRA detectado. RSI:{} < {}? {}, MACD.hist:{} > 0? {}, LastPrice:{} < BB.lower:{}? {}",
                        user.getId(), symbol,
                        rsi.setScale(4, RoundingMode.HALF_UP), settings.getRsiThreshold(), rsiBuySignal,
                        macdResult.histogram.setScale(6, RoundingMode.HALF_UP), macdBuySignal,
                        lastPrice.setScale(4, RoundingMode.HALF_UP), bb.lowerBand.setScale(4, RoundingMode.HALF_UP), bbBuySignal);

                if (simulationMode) {
                    logger.info("[SIMULAÇÃO | Usuário: {} | {}] (SIMULAÇÃO) Ordem de COMPRA NÃO enviada. Fim do ciclo.",
                            user.getId(), symbol);
                } else {
                    placeOrderAccordingToType(user, settings, lastPrice,
                            creds.getEncryptedApiKey(), creds.getEncryptedSecretKey());
                }
            } else {
                logger.info("[Usuário: {} | {}] Sem sinais de compra neste ciclo.", user.getId(), symbol);
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
        User user = openTrade.getUser();

        BigDecimal slPercent = settings.getStopLossPerc().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal tpPercent = settings.getTakeProfitPerc().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal stopLossPrice = entry.multiply(BigDecimal.ONE.subtract(slPercent));
        BigDecimal takeProfitPrice = entry.multiply(BigDecimal.ONE.add(tpPercent));

        if (lastPrice.compareTo(stopLossPrice) <= 0) {
            logger.info("[Usuário: {} | {}] Preço atingiu STOP-LOSS. LastPrice={} <= SL={}",
                    user.getId(), symbol,
                    lastPrice.setScale(4, RoundingMode.HALF_UP),
                    stopLossPrice.setScale(4, RoundingMode.HALF_UP));
            if (simulationMode) {
                logger.info("[SIMULAÇÃO | Usuário: {} | {}] (SIMULAÇÃO) Ordem de VENDA por STOP-LOSS NÃO enviada. Fim do ciclo.",
                        user.getId(), symbol);
            } else {
                closeTrade(openTrade, lastPrice, "STOP_LOSS", encApiKey, encSecretKey);
            }
            return;
        } else if (lastPrice.compareTo(takeProfitPrice) >= 0) {
            logger.info("[Usuário: {} | {}] Preço atingiu TAKE-PROFIT. LastPrice={} >= TP={}",
                    user.getId(), symbol,
                    lastPrice.setScale(4, RoundingMode.HALF_UP),
                    takeProfitPrice.setScale(4, RoundingMode.HALF_UP));
            if (simulationMode) {
                logger.info("[SIMULAÇÃO | Usuário: {} | {}] (SIMULAÇÃO) Ordem de VENDA por TAKE-PROFIT NÃO enviada. Fim do ciclo.",
                        user.getId(), symbol);
            } else {
                closeTrade(openTrade, lastPrice, "TAKE_PROFIT", encApiKey, encSecretKey);
            }
            return;
        }

        boolean rsiSellSignal = settings.isRsiEnabled() && rsi.compareTo(BigDecimal.valueOf(70)) > 0;
        boolean macdSellSignal = settings.isMacdEnabled() && macdResult.histogram.compareTo(BigDecimal.ZERO) < 0;
        boolean bbSellSignal = settings.isMovingAvgEnabled() && lastPrice.compareTo(bb.upperBand) > 0;

        if (rsiSellSignal || macdSellSignal || bbSellSignal) {
            logger.info("[Usuário: {} | {}] Sinal de VENDA detectado. RSI:{} > 70? {}, MACD.hist:{} < 0? {}, LastPrice:{} > BB.upper:{}? {}",
                    user.getId(), symbol,
                    rsi.setScale(4, RoundingMode.HALF_UP), rsiSellSignal,
                    macdResult.histogram.setScale(6, RoundingMode.HALF_UP), macdSellSignal,
                    lastPrice.setScale(4, RoundingMode.HALF_UP), bb.upperBand.setScale(4, RoundingMode.HALF_UP), bbSellSignal);

            if (simulationMode) {
                logger.info("[SIMULAÇÃO | Usuário: {} | {}] (SIMULAÇÃO) Ordem de VENDA por INDICADORES NÃO enviada. Fim do ciclo.",
                        user.getId(), symbol);
            } else {
                closeTrade(openTrade, lastPrice, "INDICATOR_SELL", encApiKey, encSecretKey);
            }
        } else {
            logger.info("[Usuário: {} | {}] Trade mantido. EntryPrice={} | LastPrice={}",
                    user.getId(), symbol,
                    entry.setScale(4, RoundingMode.HALF_UP),
                    lastPrice.setScale(4, RoundingMode.HALF_UP));
        }
    }

    private void closeTrade(
            BotTrade openTrade,
            BigDecimal exitPrice,
            String reason,
            String encApiKey,
            String encSecretKey
    ) {
        User user = openTrade.getUser();
        String symbol = openTrade.getSymbol();
        BigDecimal amount = openTrade.getAmount();

        try {
            BinanceClient.BinanceOrderResponse response = binanceClient.placeOrder(
                    encApiKey,
                    encSecretKey,
                    symbol,
                    "SELL",
                    "MARKET",
                    amount,
                    null,
                    null
            );
            exitPrice = response.price;
            logger.info("[Usuário: {} | {}] Ordem SELL executada no preço {} para fechar trade {} (motivo={})",
                    user.getId(), symbol, exitPrice.setScale(4, RoundingMode.HALF_UP), openTrade.getId(), reason);
        } catch (Exception e) {
            logger.error("[Usuário: {} | {}] Erro ao enviar ordem SELL para fechamento: {}",
                    user.getId(), symbol, e.getMessage());
            return;
        }

        BigDecimal entryPrice = openTrade.getPrice();
        openTrade.setExecutedAt(OffsetDateTime.now());
        openTrade.setPrice(exitPrice);
        openTrade.setStatus("CLOSED");
        BigDecimal profit = exitPrice.subtract(entryPrice).multiply(amount);
        openTrade.setProfitEstimate(profit.setScale(8, RoundingMode.HALF_UP));
        botTradeRepo.save(openTrade);

        logger.info("[Usuário: {} | {}] Trade {} encerrado com lucro estimado {}",
                user.getId(), symbol, openTrade.getId(), profit.setScale(8, RoundingMode.HALF_UP));
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
            case "MARKET":
                binanceType = "MARKET";
                break;
            case "LIMIT":
                binanceType = "LIMIT";
                break;
            case "STOP-LIMIT":
                binanceType = "STOP_LOSS_LIMIT";
                break;
            case "LIMIT MAKER":
                binanceType = "LIMIT_MAKER";
                break;
            case "TRAILING STOP":
                binanceType = "TRAILING_STOP_MARKET";
                break;
            default:
                binanceType = "MARKET";
                break;
        }

        BigDecimal priceParam = null;
        BigDecimal stopPriceParam = null;
        if (binanceType.equals("LIMIT") || binanceType.equals("LIMIT_MAKER")) {
            if (limitPrice == null) {
                logger.error("[Usuário: {} | {}] Faltando limitPrice para ordem LIMIT", user.getId(), symbol);
                return;
            }
            priceParam = limitPrice;
        }
        if (binanceType.equals("STOP_LOSS_LIMIT")) {
            if (stopPrice == null || limitPrice == null) {
                logger.error("[Usuário: {} | {}] Faltando stopPrice ou limitPrice para ordem STOP-LIMIT", user.getId(), symbol);
                return;
            }
            priceParam = limitPrice;
            stopPriceParam = stopPrice;
        }
        if (binanceType.equals("TRAILING_STOP_MARKET")) {
            if (trailingDelta == null) {
                logger.error("[Usuário: {} | {}] Faltando trailingDelta para ordem TRAILING STOP", user.getId(), symbol);
                return;
            }
            stopPriceParam = trailingDelta;
        }

        logger.info("[Usuário: {} | {}] Enviando ordem BUY: type={} | quantity={} | price={} | stopPrice={}",
                user.getId(), symbol, binanceType,
                quantity.setScale(6, RoundingMode.HALF_UP),
                priceParam != null ? priceParam.setScale(4, RoundingMode.HALF_UP) : "null",
                stopPriceParam != null ? stopPriceParam.setScale(4, RoundingMode.HALF_UP) : "null");

        try {
            BinanceClient.BinanceOrderResponse response = binanceClient.placeOrder(
                    encApiKey,
                    encSecretKey,
                    symbol,
                    "BUY",
                    binanceType,
                    quantity,
                    priceParam,
                    stopPriceParam
            );

            BigDecimal execPrice = response.price;
            logger.info("[Usuário: {} | {}] Ordem BUY executada no preço {}. OrderID={}",
                    user.getId(), symbol, execPrice.setScale(4, RoundingMode.HALF_UP), response.orderId);

            BotTrade newTrade = new BotTrade();
            newTrade.setUser(user);
            newTrade.setSymbol(symbol);
            newTrade.setSide("buy");
            newTrade.setAmount(quantity);
            newTrade.setPrice(execPrice);
            newTrade.setStatus("OPEN");
            botTradeRepo.save(newTrade);

            logger.info("[Usuário: {} | {}] Novo trade aberto: ID={} | EntryPrice={} | Amount={}",
                    user.getId(), symbol,
                    newTrade.getId(),
                    execPrice.setScale(4, RoundingMode.HALF_UP),
                    quantity.setScale(6, RoundingMode.HALF_UP));

        } catch (Exception e) {
            logger.error("[Usuário: {} | {}] Erro ao enviar ordem BUY: {}", user.getId(), symbol, e.getMessage());
        }
    }

    private List<BigDecimal> simulateCloses(UUID userId, int limit) {
        List<BigDecimal> closes = new ArrayList<>();
        BigDecimal price0 = BigDecimal.valueOf(100.00);
        Random rnd = new Random(userId.hashCode() + System.currentTimeMillis());
        for (int i = 0; i < limit; i++) {
            BigDecimal delta = BigDecimal.valueOf(rnd.nextDouble() * 0.02 - 0.01);
            BigDecimal next = price0.multiply(BigDecimal.ONE.add(delta));
            closes.add(next);
            price0 = next;
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

        List<BigDecimal> differences = new ArrayList<>();
        for (int i = 0; i < closes.size(); i++) {
            List<BigDecimal> sublist = closes.subList(0, i + 1);
            BigDecimal f = calculateEMA(sublist, fastPeriod);
            BigDecimal s = calculateEMA(sublist, slowPeriod);
            differences.add(f.subtract(s));
        }
        int size = differences.size();
        BigDecimal signalLine = calculateEMA(differences.subList(size - signalPeriod, size), signalPeriod);
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
        BigDecimal emaPrev = sma;
        BigDecimal k = BigDecimal.valueOf(2.0).divide(BigDecimal.valueOf(period + 1), 8, RoundingMode.HALF_UP);
        for (int i = period; i < n; i++) {
            BigDecimal price = closes.get(i);
            emaPrev = price.multiply(k).add(emaPrev.multiply(BigDecimal.ONE.subtract(k)));
        }
        return emaPrev;
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
        public BollingerResult(BigDecimal middleBand, BigDecimal upperBand, BigDecimal lowerBand) {
            this.middleBand = middleBand;
            this.upperBand = upperBand;
            this.lowerBand = lowerBand;
        }
    }
}
