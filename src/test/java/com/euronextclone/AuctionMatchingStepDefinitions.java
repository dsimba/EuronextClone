package com.euronextclone;

import com.euronextclone.ordertypes.Limit;
import com.euronextclone.ordertypes.Market;
import com.euronextclone.ordertypes.MarketToLimit;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import cucumber.annotation.en.Then;
import cucumber.table.DataTable;

import java.util.List;

import static junit.framework.Assert.assertEquals;

public class AuctionMatchingStepDefinitions {

    private final MatchingUnit matchingUnit;

    public AuctionMatchingStepDefinitions(World world) {
        this.matchingUnit = world.getMatchingUnit();
    }

    @Then("^the book looks like:$")
    public void the_book_looks_like(DataTable expectedBooks) throws Throwable {
        final List<MontageRow> rows = expectedBooks.asList(MontageRow.class);
        final List<OrderRow> expectedBids = FluentIterable.from(rows).filter(MontageRow.NON_EMPTY_BID).transform(MontageRow.TO_TEST_BID).toImmutableList();
        final List<OrderRow> expectedAsks = FluentIterable.from(rows).filter(MontageRow.NON_EMPTY_ASK).transform(MontageRow.TO_TEST_ASK).toImmutableList();

        final List<OrderRow> actualBids = FluentIterable.from(matchingUnit.getOrders(Order.OrderSide.Buy)).transform(OrderRow.FROM_ORDER).toImmutableList();
        final List<OrderRow> actualAsks = FluentIterable.from(matchingUnit.getOrders(Order.OrderSide.Sell)).transform(OrderRow.FROM_ORDER).toImmutableList();

        assertEquals(expectedBids, actualBids);
        assertEquals(expectedAsks, actualAsks);
    }

    private static class OrderRow {
        private String broker;
        private int quantity;
        private String price;

        public static final Function<? super Order, OrderRow> FROM_ORDER = new Function<Order, OrderRow>() {
            @Override
            public OrderRow apply(final Order input) {
                final OrderRow orderRow = new OrderRow();
                orderRow.setBroker(input.getBroker());
                orderRow.setPrice(input.getOrderTypeLimit().toString());
                orderRow.setQuantity(input.getQuantity());
                return orderRow;
            }
        };

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final OrderRow orderRow = (OrderRow) o;

            if (quantity != orderRow.quantity) return false;
            if (!broker.equals(orderRow.broker)) return false;
            if (!price.equals(orderRow.price)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = broker.hashCode();
            result = 31 * result + quantity;
            result = 31 * result + price.hashCode();
            return result;
        }

        public String getBroker() {
            return broker;
        }

        public void setBroker(String broker) {
            this.broker = broker;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }
    }

    private static class MontageRow {
        private String bidBroker;
        private Integer bidQuantity;
        private String bidPrice;
        private String askBroker;
        private Integer askQuantity;
        private String askPrice;

        public static final Predicate<? super MontageRow> NON_EMPTY_BID = new Predicate<MontageRow>() {
            @Override
            public boolean apply(final MontageRow input) {
                return input.bidBroker != null && !"".equals(input.bidBroker);
            }
        };

        public static final Function<? super MontageRow, OrderRow> TO_TEST_BID = new Function<MontageRow, OrderRow>() {
            @Override
            public OrderRow apply(final MontageRow input) {
                final OrderRow orderRow = new OrderRow();
                orderRow.setBroker(input.bidBroker);
                orderRow.setPrice(input.bidPrice);
                orderRow.setQuantity(input.bidQuantity);
                return orderRow;
            }
        };

        public static final Function<? super MontageRow, OrderRow> TO_TEST_ASK = new Function<MontageRow, OrderRow>() {
            @Override
            public OrderRow apply(final MontageRow input) {
                final OrderRow orderRow = new OrderRow();
                orderRow.setBroker(input.askBroker);
                orderRow.setPrice(input.askPrice);
                orderRow.setQuantity(input.askQuantity);
                return orderRow;
            }
        };

        public static final Function<? super MontageRow, Order> TO_BID = new Function<MontageRow, Order>() {
            @Override
            public Order apply(final MontageRow input) {
                final OrderTypeLimit orderTypeLimit = parseOrderPrice(input.bidPrice);
                return new Order(input.bidBroker, input.bidQuantity, orderTypeLimit, Order.OrderSide.Buy);
            }
        };
        public static final Predicate<? super MontageRow> NON_EMPTY_ASK = new Predicate<MontageRow>() {
            @Override
            public boolean apply(final MontageRow input) {
                return input.askBroker != null && !"".equals(input.askBroker);
            }
        };
        public static final Function<? super MontageRow, Order> TO_ASK = new Function<MontageRow, Order>() {
            @Override
            public Order apply(final MontageRow input) {
                final OrderTypeLimit orderTypeLimit = parseOrderPrice(input.askPrice);
                return new Order(input.askBroker, input.askQuantity, orderTypeLimit, Order.OrderSide.Sell);
            }
        };

        private static OrderTypeLimit parseOrderPrice(String price) {
            if ("MTL".equals(price)) {
                return new MarketToLimit();
            }
            if ("MO".equals(price)) {
                return new Market();
            }
            return new Limit(Double.parseDouble(price));
        }
    }
}