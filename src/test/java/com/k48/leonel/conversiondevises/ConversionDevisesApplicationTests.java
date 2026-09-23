package com.k48.leonel.conversiondevises;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifie que le contexte Spring demarre avec une configuration minimale
 * (cle API factice : aucun appel externe n est effectue au demarrage).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "EXCHANGE_RATE_API_KEY=cle-de-test",
        "EXCHANGE_RATE_API_BASE_URL=http://localhost:9099",
        "EXCHANGE_RATE_CACHE_MINUTES=60",
        "SERVER_PORT=0"
})
class ConversionDevisesApplicationTests {

    @Test
    void leContexteSeCharge() {
    }
}
