package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 1 – Specification-Based Testing (Chapter 2)
 *
 * <p>Target class: {@link PriceCalculator}
 *
 * <p>Your goal is to test {@code PriceCalculator.calculate(basePrice, discountRate, taxRate)}
 * using the domain testing technique from Chapter 2:
 * <ol>
 *   <li>Identify equivalence partitions for each input dimension.</li>
 *   <li>Identify boundary values between partitions (on-point / off-point).</li>
 *   <li>Write at least 10 meaningful test cases that cover both partitions and boundaries.</li>
 *   <li>Use {@code @ParameterizedTest} with {@code @CsvSource} for tests that share structure.</li>
 *   <li>Add a comment above each test method explaining which partition or boundary it covers.</li>
 * </ol>
 *
 * <h3>Input dimensions to consider</h3>
 * <ul>
 *   <li><b>basePrice</b>  – zero, positive, very large</li>
 *   <li><b>discountRate</b> – 0 (no discount), (0,100) typical, 100 (full discount)</li>
 *   <li><b>taxRate</b>    – 0 (no tax), (0,100) typical, 100 (100% tax)</li>
 * </ul>
 */
class PriceCalculatorSpecTest {

    private PriceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PriceCalculator();
    }

    /** Partition: zero base price — result must always be 0 regardless of discount and tax rates. */
    @Test
    void zeroBasePriceReturnsZero() {
        double result = calculator.calculate(0.0, 25.0, 18.0);

        assertThat(result).isCloseTo(0.0, within(0.0001));
    }

    /** Partition: positive base price with typical discount and tax values — verifies the main formula. */
    @ParameterizedTest(name = "base={0}, discount={1}%, tax={2}% => expected={3}")
    @CsvSource({
            "100.0, 10.0, 20.0, 108.0",
            "200.0, 25.0, 10.0, 165.0",
            "49.99, 15.0, 8.0, 45.89082"
    })
    void typicalValidValuesApplyDiscountThenTax(
            double basePrice,
            double discountRate,
            double taxRate,
            double expected
    ) {
        double result = calculator.calculate(basePrice, discountRate, taxRate);

        assertThat(result).isCloseTo(expected, within(0.0001));
    }

    /** Boundary: discountRate at lower bound 0% — no reduction is applied, only tax changes the price. */
    @Test
    void discountRateZeroMeansNoDiscount() {
        double result = calculator.calculate(100.0, 0.0, 20.0);

        assertThat(result).isCloseTo(120.0, within(0.0001));
    }

    /** Boundary: taxRate at lower bound 0% — no tax is applied, only discount changes the price. */
    @Test
    void taxRateZeroMeansNoTax() {
        double result = calculator.calculate(100.0, 30.0, 0.0);

        assertThat(result).isCloseTo(70.0, within(0.0001));
    }

    /** Boundary: discountRate at upper bound 100% — full discount wipes the price to 0. */
    @Test
    void discountRateHundredMeansFullDiscount() {
        double result = calculator.calculate(100.0, 100.0, 20.0);

        assertThat(result).isCloseTo(0.0, within(0.0001));
    }

    /** Boundary: taxRate at upper bound 100% — discounted price is doubled. */
    @Test
    void taxRateHundredMeansPriceIsDoubledAfterDiscount() {
        double result = calculator.calculate(100.0, 25.0, 100.0);

        assertThat(result).isCloseTo(150.0, within(0.0001));
    }

    /** Boundary/off-point: discountRate just above 0% — price is reduced slightly. */
    @Test
    void discountRateJustAboveZeroReducesPriceSlightly() {
        double result = calculator.calculate(100.0, 0.01, 0.0);

        assertThat(result).isCloseTo(99.99, within(0.0001));
    }

    /** Boundary/off-point: discountRate just below 100% — only a very small amount remains. */
    @Test
    void discountRateJustBelowHundredLeavesTinyPositiveAmount() {
        double result = calculator.calculate(100.0, 99.99, 0.0);

        assertThat(result).isCloseTo(0.01, within(0.0001));
    }

    /** Boundary/off-point: taxRate just above 0% — price is increased slightly. */
    @Test
    void taxRateJustAboveZeroIncreasesPriceSlightly() {
        double result = calculator.calculate(100.0, 0.0, 0.01);

        assertThat(result).isCloseTo(100.01, within(0.0001));
    }

    /** Boundary/off-point: taxRate just below 100% — price is almost doubled. */
    @Test
    void taxRateJustBelowHundredAlmostDoublesPrice() {
        double result = calculator.calculate(100.0, 0.0, 99.99);

        assertThat(result).isCloseTo(199.99, within(0.0001));
    }

    /** Partition: very large positive base price — calculation should still follow the same formula. */
    @Test
    void veryLargeBasePriceUsesSameFormula() {
        double result = calculator.calculate(1_000_000.0, 12.5, 18.0);

        assertThat(result).isCloseTo(1_032_500.0, within(0.0001));
    }

    /** Invalid partition: negative basePrice — current implementation does not reject it and returns a negative result. */
    @Test
    void negativeBasePriceDocumentsCurrentBehavior() {
        double result = calculator.calculate(-100.0, 10.0, 20.0);

        assertThat(result).isCloseTo(-108.0, within(0.0001));
    }

    /** Invalid partition: negative discountRate — current implementation treats it like a price increase. */
    @Test
    void negativeDiscountRateDocumentsCurrentBehavior() {
        double result = calculator.calculate(100.0, -10.0, 0.0);

        assertThat(result).isCloseTo(110.0, within(0.0001));
    }

    /** Invalid partition: discountRate greater than 100% — current implementation can produce a negative price. */
    @Test
    void discountRateGreaterThanHundredDocumentsCurrentBehavior() {
        double result = calculator.calculate(100.0, 150.0, 0.0);

        assertThat(result).isCloseTo(-50.0, within(0.0001));
    }

    /** Invalid partition: negative taxRate — current implementation treats it like a price reduction. */
    @Test
    void negativeTaxRateDocumentsCurrentBehavior() {
        double result = calculator.calculate(100.0, 0.0, -10.0);

        assertThat(result).isCloseTo(90.0, within(0.0001));
    }

    /** Invalid partition: taxRate greater than 100% — current implementation accepts it and increases the price heavily. */
    @Test
    void taxRateGreaterThanHundredDocumentsCurrentBehavior() {
        double result = calculator.calculate(100.0, 0.0, 150.0);

        assertThat(result).isCloseTo(250.0, within(0.0001));
    }
}