package com.mallmini.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private boolean devLoginEnabled = false;
    private Payment payment = new Payment();
    private Shipping shipping = new Shipping();
    private Order order = new Order();
    private Storage storage = new Storage();
    private AdminInit adminInit = new AdminInit();
    private Cors cors = new Cors();
    private Wechat wechat = new Wechat();
    private Stock stock = new Stock();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "";
        private long expireMinutes = 1440;
    }

    @Getter
    @Setter
    public static class Payment {
        private boolean mockEnabled = false;
    }

    @Getter
    @Setter
    public static class Shipping {
        private BigDecimal fee = new BigDecimal("10.00");
        private BigDecimal freeThreshold = new BigDecimal("99.00");
    }

    @Getter
    @Setter
    public static class Order {
        private int cancelMinutes = 30;
        private boolean autoCancelEnabled = true;
        private boolean autoConfirmEnabled = true;
        private int autoConfirmDays = 7;
    }

    @Getter
    @Setter
    public static class Storage {
        private String localDir = "./uploads";
    }

    @Getter
    @Setter
    public static class AdminInit {
        private String username = "admin";
        private String password = "";
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Wechat {
        private String appId = "";
        private String appSecret = "";
    }

    @Getter
    @Setter
    public static class Stock {
        private int alertThreshold = 10;
    }
}
