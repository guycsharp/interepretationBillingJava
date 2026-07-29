package Utils;

import org.junit.jupiter.api.Test;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

public class BillingLogicTest {

    // -----------------------------
    // Helper: create SQL date
    // -----------------------------
    private Date d(String iso) {
        return Date.valueOf(iso);
    }

    // ============================================================
    //  TESTS FOR calculateTotal_General
    // ============================================================

    @Test
    void testGeneral_15min_becomesHalfHour() {
        double result = BillingLogic.calculateTotal_General(5, 5, 28, 15);
        assertEquals(14, result, 0.001);
    }

    @Test
    void testGeneral_35min_becomesFullHour() {
        double result = BillingLogic.calculateTotal_General(5, 5, 28, 35);
        assertEquals(28, result, 0.001);
    }

    @Test
    void testGeneral_45min_becomesFullHour() {
        double result = BillingLogic.calculateTotal_General(5, 5, 28, 45);
        assertEquals(28, result, 0.001);
    }

    @Test
    void testGeneral_65min_becomes1h30() {
        double result = BillingLogic.calculateTotal_General(5, 5, 28, 65);
        assertEquals(42, result, 0.001); // 28 + 14
    }

    @Test
    void testGeneral_OffsetRoundsUp() {
        // leftover = 58 → minutesToNextHour = 2 → <= offsetunit+offsetBy (5+5=10)
        double result = BillingLogic.calculateTotal_General(5, 5, 28, 118);
        assertEquals(56, result, 0.001); // 2 hours
    }

    // ============================================================
    //  TESTS FOR calculateTotal_CF
    // ============================================================

    @Test
    void testCF_15min_halfHour() {
        double result = BillingLogic.calculateTotal_CF(5, 5, 28, 15);
        assertEquals(14, result, 0.001);
    }

    @Test
    void testCF_30min_halfHour() {
        double result = BillingLogic.calculateTotal_CF(5, 5, 28, 30);
        assertEquals(14, result, 0.001);
    }

    @Test
    void testCF_45min_fullHour() {
        double result = BillingLogic.calculateTotal_CF(5, 5, 28, 45);
        assertEquals(28, result, 0.001);
    }

    @Test
    void testCF_65min_oneHourHalf() {
        double result = BillingLogic.calculateTotal_CF(5, 5, 28, 65);
        assertEquals(42, result, 0.001);
    }

    // ============================================================
    //  TESTS FOR calculateTotal_PT (original)
    // ============================================================

    @Test
    void testPT_15min_halfHourRate() {
        double result = BillingLogic.calculateTotal_PT(5, 5, 28, 15, 12, d("2024-01-01"));
        assertEquals(12, result, 0.001);
    }

    @Test
    void testPT_45min_fullHour() {
        double result = BillingLogic.calculateTotal_PT(5, 5, 28, 45, 12, d("2024-01-01"));
        assertEquals(28, result, 0.001);
    }

    @Test
    void testPT_65min_oneHourHalf() {
        double result = BillingLogic.calculateTotal_PT(5, 5, 28, 65, 12, d("2024-01-01"));
        assertEquals(40, result, 0.001); // 28 + 12
    }

    // ============================================================
    //  TESTS FOR calculateTotalAmount (dispatcher)
    // ============================================================

    @Test
    void testDispatcher_PlanetTraduction_callsPT() {
        double result = BillingLogic.calculateTotalAmount(
                5, 5, 28, 15, 12, "Planet Traduction", d("2024-01-01")
        );
        assertEquals(12, result, 0.001);
    }

    @Test
    void testDispatcher_Default_callsGeneral() {
        double result = BillingLogic.calculateTotalAmount(
                5, 5, 28, 15, 12, "Other Company", d("2024-01-01")
        );
        assertEquals(14, result, 0.001);
    }
}
