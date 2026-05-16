package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Task 5 – Mocks &amp; Stubs (Chapter 6)
 *
 * <p>Target class: {@link OrderProcessor}
 *
 * <p>Use Mockito to mock {@link InventoryService} and {@link PaymentGateway},
 * then test {@link OrderProcessor#process(String, ShoppingCart)} in isolation.
 *
 * <h3>Required scenarios (at least 4)</h3>
 * <ol>
 *   <li><b>Happy path</b> — inventory available, payment succeeds → non-null {@link Order} returned.</li>
 *   <li><b>Inventory failure</b> — {@code isAvailable()} returns {@code false} for at least one item
 *       → method returns {@code null} AND {@code charge()} is <em>never</em> called.</li>
 *   <li><b>Payment failure</b> — inventory OK, {@code charge()} returns {@code false}
 *       → method returns {@code null}.</li>
 *   <li><b>Partial quantity</b> — define the expected behaviour when only some items
 *       pass the inventory check, and write a test for it.</li>
 * </ol>
 *
 * <h3>Verification</h3>
 * Use {@code verify(paymentGateway, never()).charge(...)} to assert that
 * payment is never attempted when inventory is insufficient.
 *
 * <h3>Reflection (add to your report)</h3>
 * Answer: What does mocking allow you to test that you could not test otherwise?
 * What does it prevent you from testing? When is mocking a bad idea?
 */
@ExtendWith(MockitoExtension.class)
class OrderProcessorMockTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private OrderProcessor orderProcessor;

    private ShoppingCart cart;
    private Product widget;

    @BeforeEach
    void setUp() {
        cart   = new ShoppingCart();
        widget = new Product("P001", "Widget", 25.0, 100);
    }

    @Test
    void process_inventoryAvailableAndPaymentSucceeds_returnsOrder() {
        cart.addItem(widget, 2);

        when(inventoryService.isAvailable(widget, 2)).thenReturn(true);
        when(paymentGateway.charge("customer-1", 50.0)).thenReturn(true);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo("customer-1");
        assertThat(order.getTotal()).isCloseTo(50.0, within(0.0001));
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getProduct()).isEqualTo(widget);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);

        verify(inventoryService).isAvailable(widget, 2);
        verify(paymentGateway).charge("customer-1", 50.0);
    }

    @Test
    void process_inventoryUnavailable_returnsNullAndNeverChargesPayment() {
        cart.addItem(widget, 2);

        when(inventoryService.isAvailable(widget, 2)).thenReturn(false);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNull();
        verify(inventoryService).isAvailable(widget, 2);
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    void process_paymentFails_returnsNull() {
        cart.addItem(widget, 2);

        when(inventoryService.isAvailable(widget, 2)).thenReturn(true);
        when(paymentGateway.charge("customer-1", 50.0)).thenReturn(false);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNull();
        verify(inventoryService).isAvailable(widget, 2);
        verify(paymentGateway).charge("customer-1", 50.0);
    }

    @Test
    void process_partialQuantityUnavailable_returnsNullAndNeverChargesPayment() {
        cart.addItem(widget, 5);

        /*
         * Partial fulfillment is not supported by OrderProcessor because
         * InventoryService only returns boolean availability for the full requested quantity.
         */
        when(inventoryService.isAvailable(widget, 5)).thenReturn(false);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNull();
        verify(inventoryService).isAvailable(widget, 5);
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    void process_multipleItemsSecondUnavailable_returnsNullAndNeverChargesPayment() {
        Product gadget = new Product("P002", "Gadget", 10.0, 50);
        cart.addItem(widget, 2);
        cart.addItem(gadget, 1);

        when(inventoryService.isAvailable(widget, 2)).thenReturn(true);
        when(inventoryService.isAvailable(gadget, 1)).thenReturn(false);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNull();
        verify(inventoryService).isAvailable(widget, 2);
        verify(inventoryService).isAvailable(gadget, 1);
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    void process_emptyCart_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> orderProcessor.process("customer-1", cart))
                .isInstanceOf(IllegalArgumentException.class);

        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    void process_nullCart_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> orderProcessor.process("customer-1", null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    void process_blankCustomerId_throwsIllegalArgumentException() {
        cart.addItem(widget, 1);

        assertThatThrownBy(() -> orderProcessor.process("   ", cart))
                .isInstanceOf(IllegalArgumentException.class);

        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }
}