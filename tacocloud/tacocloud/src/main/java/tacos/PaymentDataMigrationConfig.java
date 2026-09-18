package tacos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition
    .ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tacos.data.migration.SensitivePaymentDataMigration;

@Configuration
@ConditionalOnProperty(prefix = "tacocloud.migrations.payment-data", name = "enabled", havingValue = "true")
public class PaymentDataMigrationConfig {
    private static final Logger log = LoggerFactory.getLogger(PaymentDataMigrationConfig.class);

    @Bean
    public ApplicationRunner paymentMigration(SensitivePaymentDataMigration migration) {

        return arguments -> {
        SensitivePaymentDataMigration
            .MigrationResult result =
                migration.migrate().block();

        if (result != null) {
            log.warn(
                "Scrub completed: "
                    + "ordersModified={}, "
                    + "paymentMethodsModified={}",
                result.getModifiedOrders(),
                result.getModifiedPaymentMethods());
        }
    };
  }
}
