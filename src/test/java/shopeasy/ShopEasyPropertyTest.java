package shopeasy;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 4 – Property-Based Testing (Chapter 5)
 *
 * <p>Target classes: {@link PriceCalculator}, {@link ShoppingCart}
 *
 * <p>Using jqwik, define and test at least <strong>3 distinct properties</strong>.
 * You must use at least one custom {@code @Provide} method.
 *
 * <h3>Suggested properties (you may use these or design your own)</h3>
 * <ul>
 *   <li><b>Monotonicity</b> – For any fixed base and tax, increasing the discount
 *       rate never increases the final price.</li>
 *   <li><b>Identity</b> – A 0% discount and 0% tax returns exactly the base price.</li>
 *   <li><b>Boundedness</b> – The result is always &gt;= 0.</li>
 *   <li><b>Cart commutativity</b> – Adding product A then B yields the same total
 *       as adding B then A.</li>
 *   <li><b>Discount transitivity</b> – Applying a 10% then another 10% discount via
 *       {@code applyDiscount} is equivalent to a single call with the compounded rate
 *       (think carefully: is this actually true for this implementation?).</li>
 * </ul>
 *
 * <h3>For each property, include a comment that answers:</h3>
 * <ol>
 *   <li>What does this property mean in plain English?</li>
 *   <li>What class of bugs would this property catch?</li>
 * </ol>
 *
 * <h3>If jqwik finds a failing case</h3>
 * Do not just fix the test. Investigate the root cause and explain it in your
 * reflection report (include the counterexample jqwik printed).
 */
class ShopEasyPropertyTest {

    /**
     * Property: With 0% discount and 0% tax, the final price is exactly the base price.
     * Bug class caught: accidental tax/discount application or wrong formula constants.
     */
    @Property
    void identityWithNoDiscountAndNoTax(
            @ForAll @DoubleRange(min = 0.0, max = 10_000.0) double basePrice
    ) {
        PriceCalculator calculator = new PriceCalculator();

        double result = calculator.calculate(basePrice, 0.0, 0.0);

        assertThat(result).isCloseTo(basePrice, within(0.0001));
    }

    /**
     * Property: For the same base price and tax rate, a higher discount never increases the final price.
     * Bug class caught: discount sign errors, such as adding the discount instead of subtracting it.
     */
    @Property
    void higherDiscountNeverIncreasesFinalPrice(
            @ForAll @DoubleRange(min = 0.0, max = 10_000.0) double basePrice,
            @ForAll @DoubleRange(min = 0.0, max = 100.0) double firstDiscount,
            @ForAll @DoubleRange(min = 0.0, max = 100.0) double secondDiscount,
            @ForAll @DoubleRange(min = 0.0, max = 100.0) double taxRate
    ) {
        PriceCalculator calculator = new PriceCalculator();

        double lowerDiscount = Math.min(firstDiscount, secondDiscount);
        double higherDiscount = Math.max(firstDiscount, secondDiscount);

        double priceWithLowerDiscount = calculator.calculate(basePrice, lowerDiscount, taxRate);
        double priceWithHigherDiscount = calculator.calculate(basePrice, higherDiscount, taxRate);

        assertThat(priceWithHigherDiscount).isLessThanOrEqualTo(priceWithLowerDiscount + 0.0001);
    }

    /**
     * Property: For valid inputs, the final price is non-negative and cannot exceed double the base price.
     * Bug class caught: negative prices, excessive tax application, or formula order/sign mistakes.
     */
    @Property
    void finalPriceIsAlwaysWithinValidBounds(
            @ForAll @DoubleRange(min = 0.0, max = 10_000.0) double basePrice,
            @ForAll @DoubleRange(min = 0.0, max = 100.0) double discountRate,
            @ForAll @DoubleRange(min = 0.0, max = 100.0) double taxRate
    ) {
        PriceCalculator calculator = new PriceCalculator();

        double result = calculator.calculate(basePrice, discountRate, taxRate);

        assertThat(result).isGreaterThanOrEqualTo(0.0);
        assertThat(result).isLessThanOrEqualTo((basePrice * 2.0) + 0.0001);
    }

    /**
     * Property: Adding two distinct products in either order produces the same cart total.
     * Bug class caught: order-dependent total calculation or incorrect cart aggregation.
     */
    @Property
    void addingDistinctProductsInDifferentOrderKeepsSameTotal(
            @ForAll("distinctProductPairs") ProductPair pair,
            @ForAll @IntRange(min = 1, max = 100) int firstQuantity,
            @ForAll @IntRange(min = 1, max = 100) int secondQuantity
    ) {
        ShoppingCart firstCart = new ShoppingCart();
        firstCart.addItem(pair.first(), firstQuantity);
        firstCart.addItem(pair.second(), secondQuantity);

        ShoppingCart secondCart = new ShoppingCart();
        secondCart.addItem(pair.second(), secondQuantity);
        secondCart.addItem(pair.first(), firstQuantity);

        assertThat(firstCart.total()).isCloseTo(secondCart.total(), within(0.0001));
    }

    /**
     * Property: Applying a discount returns a discounted value but does not mutate the cart's raw total.
     * Bug class caught: accidental mutation of cart state when applying a discount.
     */
    @Property
    void applyDiscountDoesNotMutateCartTotal(
            @ForAll("validProducts") Product product,
            @ForAll @IntRange(min = 1, max = 100) int quantity,
            @ForAll @DoubleRange(min = 0.0, max = 100.0) double discountRate
    ) {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(product, quantity);

        double rawTotal = cart.total();
        double discounted = cart.applyDiscount(discountRate);

        assertThat(discounted).isGreaterThanOrEqualTo(0.0);
        assertThat(discounted).isLessThanOrEqualTo(rawTotal + 0.0001);
        assertThat(cart.total()).isCloseTo(rawTotal, within(0.0001));
    }

    @Provide
    Arbitrary<Product> validProducts() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(12),
                Arbitraries.doubles().between(0.01, 500.0),
                Arbitraries.integers().between(0, 1_000)
        ).as((name, price, stock) -> new Product("P-" + name, name, price, stock));
    }

    @Provide
    Arbitrary<ProductPair> distinctProductPairs() {
        Arbitrary<Double> prices = Arbitraries.doubles().between(0.01, 500.0);
        Arbitrary<Integer> stockQuantities = Arbitraries.integers().between(0, 1_000);

        return Combinators.combine(prices, prices, stockQuantities, stockQuantities)
                .as((firstPrice, secondPrice, firstStock, secondStock) ->
                        new ProductPair(
                                new Product("P-FIRST", "First", firstPrice, firstStock),
                                new Product("P-SECOND", "Second", secondPrice, secondStock)
                        ));
    }

    private record ProductPair(Product first, Product second) {
    }
}