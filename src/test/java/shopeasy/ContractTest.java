package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 3 – Design by Contract (Chapter 4)
 *
 * <p>This task has two parts:
 *
 * <h3>Part A – Add contracts to production code</h3>
 * Open {@link ShoppingCart} and {@link PriceCalculator} and add {@code assert}
 * statements for the pre-conditions and post-conditions described in their Javadoc.
 * Note: assertions are enabled via {@code -ea} in Maven Surefire (already configured
 * in {@code pom.xml}).
 *
 * <p>Contracts to implement:
 * <ul>
 *   <li><b>ShoppingCart.addItem</b>: pre — {@code product != null}, {@code quantity > 0};
 *       post — {@code itemCount()} increased or product quantity updated.</li>
 *   <li><b>ShoppingCart.applyDiscount</b>: pre — {@code 0 <= discountRate <= 100};
 *       post — result &lt;= {@code total()} when {@code discountRate > 0}.</li>
 *   <li><b>PriceCalculator.calculate</b>: pre — {@code basePrice >= 0},
 *       {@code 0 <= discountRate <= 100}, {@code 0 <= taxRate <= 100};
 *       post — result {@code >= 0}.</li>
 *   <li><b>ShoppingCart invariant</b>: {@code total() >= 0} after any operation.</li>
 * </ul>
 *
 * <h3>Part B – Write contract tests</h3>
 * Write tests below that:
 * <ol>
 *   <li>Verify contracts hold for valid inputs (positive tests).</li>
 *   <li>Verify contracts are violated ({@code AssertionError}) for invalid inputs (negative tests).</li>
 * </ol>
 *
 * <p>Use {@code assertThatThrownBy(...).isInstanceOf(AssertionError.class)} to test violations.
 */
class ContractTest {

    private ShoppingCart cart;
    private PriceCalculator calculator;
    private Product product;

    @BeforeEach
    void setUp() {
        cart       = new ShoppingCart();
        calculator = new PriceCalculator();
        product    = new Product("P001", "Widget", 10.0, 50);
    }

    @Test
    void addItem_validInput_shouldNotThrowAndUpdateCart() {
        assertThatCode(() -> cart.addItem(product, 3)).doesNotThrowAnyException();

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(cart.total()).isCloseTo(30.0, within(0.0001));
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void addItem_existingProduct_shouldIncreaseQuantityAndPreserveInvariant() {
        cart.addItem(product, 2);

        assertThatCode(() -> cart.addItem(product, 3)).doesNotThrowAnyException();

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.total()).isCloseTo(50.0, within(0.0001));
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void applyDiscount_zeroRate_shouldBeValid() {
        cart.addItem(product, 2);

        double result = cart.applyDiscount(0.0);

        assertThat(result).isCloseTo(20.0, within(0.0001));
        assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void applyDiscount_positiveRate_shouldReturnDiscountedNonNegativeResult() {
        cart.addItem(product, 4);

        double result = cart.applyDiscount(25.0);

        assertThat(result).isCloseTo(30.0, within(0.0001));
        assertThat(result).isLessThanOrEqualTo(cart.total());
        assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void applyDiscount_hundredRate_shouldReturnZero() {
        cart.addItem(product, 4);

        double result = cart.applyDiscount(100.0);

        assertThat(result).isCloseTo(0.0, within(0.0001));
        assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void calculate_validValues_shouldReturnExpectedNonNegativeResult() {
        double result = calculator.calculate(100.0, 10.0, 20.0);

        assertThat(result).isCloseTo(108.0, within(0.0001));
        assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void calculate_zeroBoundaryValues_shouldHoldContracts() {
        double result = calculator.calculate(0.0, 0.0, 0.0);

        assertThat(result).isCloseTo(0.0, within(0.0001));
        assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void calculate_upperBoundaryValues_shouldHoldContracts() {
        double result = calculator.calculate(100.0, 100.0, 100.0);

        assertThat(result).isCloseTo(0.0, within(0.0001));
        assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void updateQuantity_validInput_shouldPreserveInvariant() {
        cart.addItem(product, 2);

        cart.updateQuantity("P001", 5);

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.total()).isCloseTo(50.0, within(0.0001));
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void clear_shouldPreserveInvariant() {
        cart.addItem(product, 2);

        cart.clear();

        assertThat(cart.itemCount()).isEqualTo(0);
        assertThat(cart.total()).isCloseTo(0.0, within(0.0001));
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void addItem_nullProduct_shouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(null, 1))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void addItem_zeroQuantity_shouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(product, 0))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void addItem_negativeQuantity_shouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(product, -1))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void applyDiscount_negativeRate_shouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.applyDiscount(-1.0))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void applyDiscount_rateGreaterThanHundred_shouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.applyDiscount(101.0))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void calculate_negativeBasePrice_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(-100.0, 10.0, 20.0))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void calculate_negativeDiscount_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100.0, -10.0, 20.0))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void calculate_discountGreaterThanHundred_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100.0, 101.0, 20.0))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void calculate_negativeTax_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100.0, 10.0, -1.0))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void calculate_taxGreaterThanHundred_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100.0, 10.0, 101.0))
                .isInstanceOf(AssertionError.class);
    }
}