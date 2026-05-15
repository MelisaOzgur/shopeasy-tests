package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 2 – Structural Testing &amp; Code Coverage (Chapter 3)
 *
 * <p>Target class: {@link ShoppingCart}
 *
 * <h3>Workflow</h3>
 * <ol>
 *   <li>Write an initial test suite based on the specification (Javadoc of ShoppingCart).</li>
 *   <li>Run {@code mvn test} to generate the JaCoCo report:
 *       <pre>  target/site/jacoco/index.html</pre></li>
 *   <li>Open the report, navigate to {@code ShoppingCart}, and identify uncovered branches.</li>
 *   <li>Add tests specifically to cover those branches until branch coverage &gt;= 80%.</li>
 *   <li>Take a screenshot of the final JaCoCo summary and put it in {@code report/jacoco-screenshot.png}.</li>
 * </ol>
 *
 * <h3>Branches to think about</h3>
 * <ul>
 *   <li>{@code addItem}: product already in cart vs. new product</li>
 *   <li>{@code removeItem}: product found vs. not found in cart</li>
 *   <li>{@code updateQuantity}: product found vs. not found, quantity valid vs. invalid</li>
 *   <li>{@code applyDiscount}: zero discount, positive discount</li>
 *   <li>{@code total}: empty cart vs. non-empty cart</li>
 * </ul>
 *
 * <h3>Bonus (PIT Mutation Testing)</h3>
 * Run: {@code mvn org.pitest:pitest-maven:mutationCoverage}
 * <br>Examine the HTML report in {@code target/pit-reports/}. Find two surviving mutants,
 * explain why each survived, and describe a test that would kill it. Add this analysis
 * to your reflection report.
 */
class ShoppingCartStructuralTest {

    private ShoppingCart cart;
    private Product apple;
    private Product banana;

    @BeforeEach
    void setUp() {
        cart   = new ShoppingCart();
        apple  = new Product("P001", "Apple",  1.50, 100);
        banana = new Product("P002", "Banana", 0.80, 50);
    }

    /** Structural path: empty cart, total loop body is not executed. */
    @Test
    void newCartStartsEmptyWithZeroTotal() {
        assertThat(cart.itemCount()).isEqualTo(0);
        assertThat(cart.total()).isCloseTo(0.0, within(0.0001));
        assertThat(cart.getItems()).isEmpty();
    }

    /** Branch: addItem adds a new product when no matching cart line exists. */
    @Test
    void addItemNewProductAddsLineAndUpdatesTotal() {
        cart.addItem(apple, 2);

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.total()).isCloseTo(3.0, within(0.0001));
        assertThat(cart.getItems().get(0).getProduct()).isEqualTo(apple);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    /** Branch: addItem finds an existing product and combines quantities. */
    @Test
    void addItemExistingProductCombinesQuantities() {
        cart.addItem(apple, 2);
        cart.addItem(apple, 3);

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.total()).isCloseTo(7.5, within(0.0001));
    }

    /** Branch: addItem sees a non-matching product and creates a second cart line. */
    @Test
    void addItemDifferentProductCreatesSecondLine() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 4);

        assertThat(cart.itemCount()).isEqualTo(2);
        assertThat(cart.total()).isCloseTo(6.2, within(0.0001));
        assertThat(cart.getItems())
                .extracting(item -> item.getProduct().getId())
                .containsExactly("P001", "P002");
    }

    /** Branch: removeItem predicate matches an existing product and removes it. */
    @Test
    void removeItemExistingProductRemovesOnlyMatchingItem() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 4);

        cart.removeItem("P001");

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getProduct()).isEqualTo(banana);
        assertThat(cart.total()).isCloseTo(3.2, within(0.0001));
    }

    /** Branch: removeItem predicate does not match and leaves the cart unchanged. */
    @Test
    void removeItemUnknownProductLeavesCartUnchanged() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 4);

        cart.removeItem("UNKNOWN");

        assertThat(cart.itemCount()).isEqualTo(2);
        assertThat(cart.total()).isCloseTo(6.2, within(0.0001));
    }

    /** Branch: updateQuantity receives valid quantity and finds the product. */
    @Test
    void updateQuantityExistingProductChangesQuantityAndTotal() {
        cart.addItem(apple, 2);

        cart.updateQuantity("P001", 5);

        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.total()).isCloseTo(7.5, within(0.0001));
    }

    /** Structural path: updateQuantity skips a non-matching line before updating a later match. */
    @Test
    void updateQuantityProductAfterNonMatchingItemStillUpdatesCorrectLine() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 4);

        cart.updateQuantity("P002", 6);

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getItems().get(1).getQuantity()).isEqualTo(6);
        assertThat(cart.total()).isCloseTo(7.8, within(0.0001));
    }

    /** Branch: updateQuantity valid quantity but unknown product reaches the not-found exception path. */
    @Test
    void updateQuantityUnknownProductThrowsException() {
        cart.addItem(apple, 2);

        assertThatThrownBy(() -> cart.updateQuantity("UNKNOWN", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    /** Branch: updateQuantity rejects the lower invalid boundary, quantity zero. */
    @Test
    void updateQuantityZeroQuantityThrowsException() {
        cart.addItem(apple, 2);

        assertThatThrownBy(() -> cart.updateQuantity("P001", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be > 0");
    }

    /** Branch: updateQuantity rejects negative quantities before searching the cart. */
    @Test
    void updateQuantityNegativeQuantityThrowsException() {
        cart.addItem(apple, 2);

        assertThatThrownBy(() -> cart.updateQuantity("P001", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be > 0");
    }

    /** Structural path: applyDiscount with 0% returns the raw total. */
    @Test
    void applyDiscountZeroPercentReturnsRawTotal() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 4);

        double discounted = cart.applyDiscount(0.0);

        assertThat(discounted).isCloseTo(6.2, within(0.0001));
    }

    /** Structural path: applyDiscount with a positive percentage returns a reduced total. */
    @Test
    void applyDiscountPositivePercentReturnsDiscountedTotal() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 4);

        double discounted = cart.applyDiscount(10.0);

        assertThat(discounted).isCloseTo(5.58, within(0.0001));
    }

    /** Structural path: applyDiscount calculates a return value but does not mutate the cart total. */
    @Test
    void applyDiscountDoesNotPersistBetweenCalls() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 4);

        double discounted = cart.applyDiscount(10.0);

        assertThat(discounted).isCloseTo(5.58, within(0.0001));
        assertThat(cart.total()).isCloseTo(6.2, within(0.0001));
        assertThat(cart.applyDiscount(10.0)).isCloseTo(5.58, within(0.0001));
    }

    /** Structural path: clear removes all cart lines after the cart has items. */
    @Test
    void clearRemovesAllItems() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 4);

        cart.clear();

        assertThat(cart.itemCount()).isEqualTo(0);
        assertThat(cart.total()).isCloseTo(0.0, within(0.0001));
        assertThat(cart.getItems()).isEmpty();
    }

    /** Structural path: getItems exposes a read-only view of the cart lines. */
    @Test
    void getItemsReturnsUnmodifiableView() {
        cart.addItem(apple, 2);

        assertThatThrownBy(() -> cart.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(cart.itemCount()).isEqualTo(1);
    }

    /** Structural path: total loop sums more than one cart item subtotal. */
    @Test
    void totalWithMultipleItemsSumsAllSubtotals() {
        cart.addItem(apple, 3);
        cart.addItem(banana, 5);

        assertThat(cart.total()).isCloseTo(8.5, within(0.0001));
    }
}