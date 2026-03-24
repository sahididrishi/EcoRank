package dev.ecorank.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecorank")
public class EcoRankProperties {

    private Jwt jwt = new Jwt();
    private Plugin plugin = new Plugin();
    private Stripe stripe = new Stripe();
    private PayPal paypal = new PayPal();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public Stripe getStripe() {
        return stripe;
    }

    public void setStripe(Stripe stripe) {
        this.stripe = stripe;
    }

    public PayPal getPaypal() {
        return paypal;
    }

    public void setPaypal(PayPal paypal) {
        this.paypal = paypal;
    }

    public static class Jwt {
        private String secret;
        private int accessTokenExpiryMinutes = 15;
        private int refreshTokenExpiryDays = 7;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getAccessTokenExpiryMinutes() {
            return accessTokenExpiryMinutes;
        }

        public void setAccessTokenExpiryMinutes(int accessTokenExpiryMinutes) {
            this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        }

        public int getRefreshTokenExpiryDays() {
            return refreshTokenExpiryDays;
        }

        public void setRefreshTokenExpiryDays(int refreshTokenExpiryDays) {
            this.refreshTokenExpiryDays = refreshTokenExpiryDays;
        }
    }

    public static class Plugin {
        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Stripe {
        private String secretKey;
        private String webhookSecret;

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }

    public static class PayPal {
        private String clientId;
        private String clientSecret;
        private String mode = "sandbox";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }
}
