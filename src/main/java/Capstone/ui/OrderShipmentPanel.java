package Capstone.ui;

import Capstone.model.Invoice;
import Capstone.model.Order;
import Capstone.model.OrderItem;
import Capstone.model.Shipment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import static java.awt.AWTEventMulticaster.add;

public class OrderShipmentPanel extends JPanel {
    private JTable ordersTable;
    private JTable orderItemsTable;

    private DefaultTableModel ordersModel;
    private DefaultTableModel orderItemsModel;

    private JLabel subtotalLabel;
    private JLabel vatLabel;
    private JLabel shippingLabel;
    private JLabel totalLabel;

    private JLabel shipmentStatusLabel;
    private JLabel trackingNumberLabel;
    private JLabel estimatedDeliveryLabel;
    private final String customerId;
    public OrderShipmentPanel(String customerId) {

        this.customerId = customerId;

        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(15,15,15,15));

        JLabel title = new JLabel("Orders & Shipment Tracking");
        title.setFont(title.getFont().deriveFont(Font.BOLD,24f));

        add(title, BorderLayout.NORTH);
        add(createMainSplitPane(), BorderLayout.CENTER);

        loadOrders();
    }

    private JSplitPane createMainSplitPane() {

        JSplitPane splitPane = new JSplitPane();

        splitPane.setResizeWeight(0.35);

        splitPane.setLeftComponent(createOrdersPanel());
        splitPane.setRightComponent(createDetailsPanel());

        return splitPane;

    }

    private JScrollPane createOrdersPanel() {

        String[] columns = {
                "Order ID",
                "Date",
                "Total",
                "Status"
        };

        ordersModel = new DefaultTableModel(columns,0){

            @Override
            public boolean isCellEditable(int row,int column){
                return false;
            }

        };

        ordersTable = new JTable(ordersModel);

        ordersTable.setRowHeight(28);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ordersTable.getSelectionModel().addListSelectionListener(e -> {

            if(!e.getValueIsAdjusting()){

                loadSelectedOrder();

            }

        });

        JScrollPane scroll = new JScrollPane(ordersTable);

        scroll.setBorder(
                BorderFactory.createTitledBorder("Orders"));

        return scroll;

    }

    private JPanel createDetailsPanel() {

        JPanel panel = new JPanel(new BorderLayout(10,10));

        panel.add(createItemsTable(), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1,2,10,10));

        bottom.add(createInvoicePanel());
        bottom.add(createShipmentPanel());

        panel.add(bottom, BorderLayout.SOUTH);

        return panel;

    }

    private JScrollPane createItemsTable() {

        String[] columns = {

                "Product",
                "Quantity",
                "Unit Price",
                "Subtotal"

        };

        orderItemsModel = new DefaultTableModel(columns,0){

            @Override
            public boolean isCellEditable(int row,int column){

                return false;

            }

        };

        orderItemsTable = new JTable(orderItemsModel);

        orderItemsTable.setRowHeight(28);

        JScrollPane scroll = new JScrollPane(orderItemsTable);

        scroll.setBorder(
                BorderFactory.createTitledBorder("Order Items"));

        return scroll;

    }

    private JPanel createInvoicePanel() {

        JPanel panel = new JPanel(new GridLayout(4,2,5,10));

        panel.setBorder(
                BorderFactory.createTitledBorder("Invoice"));

        subtotalLabel = new JLabel();
        vatLabel = new JLabel();
        shippingLabel = new JLabel();
        totalLabel = new JLabel();

        panel.add(new JLabel("Subtotal"));
        panel.add(subtotalLabel);

        panel.add(new JLabel("VAT"));
        panel.add(vatLabel);

        panel.add(new JLabel("Shipping"));
        panel.add(shippingLabel);

        panel.add(new JLabel("Total"));
        panel.add(totalLabel);

        return panel;

    }

    private JPanel createShipmentPanel() {

        JPanel panel = new JPanel(new GridLayout(3,2,5,10));

        panel.setBorder(
                BorderFactory.createTitledBorder("Shipment"));

        shipmentStatusLabel = new JLabel();
        trackingNumberLabel = new JLabel();
        estimatedDeliveryLabel = new JLabel();

        panel.add(new JLabel("Status"));
        panel.add(shipmentStatusLabel);

        panel.add(new JLabel("Tracking"));
        panel.add(trackingNumberLabel);

        panel.add(new JLabel("Estimated"));
        panel.add(estimatedDeliveryLabel);

        return panel;

    }


    private void loadOrders() {

        SwingWorker<List<Order>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected List<Order> doInBackground() throws Exception {

                        HttpClient client = HttpClient.newHttpClient();

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(
                                        "http://localhost:8080/api/orders/customer/"
                                                + customerId))
                                .GET()
                                .build();

                        HttpResponse<String> response =
                                client.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() != 200) {
                            throw new RuntimeException(
                                    "Failed to load orders. HTTP status: "
                                            + response.statusCode());
                        }

                        ObjectMapper mapper = new ObjectMapper();
                        mapper.findAndRegisterModules();

                        return mapper.readValue(
                                response.body(),
                                new TypeReference<List<Order>>() {});
                    }

                    @Override
                    protected void done() {

                        try {

                            List<Order> orders = get();

                            ordersModel.setRowCount(0);

                            DateTimeFormatter formatter =
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd");

                            for (Order order : orders) {

                                String orderDate = "";

                                if (order.getOrderDate() != null) {
                                    orderDate =
                                            order.getOrderDate()
                                                    .format(formatter);
                                }

                                ordersModel.addRow(new Object[]{
                                        order.getOrderId(),
                                        orderDate,
                                        String.format(
                                                "R %.2f",
                                                order.getTotalAmount()),
                                        "ORDERED"
                                });
                            }

                            if (ordersModel.getRowCount() > 0) {

                                ordersTable.setRowSelectionInterval(0, 0);

                                loadSelectedOrder();
                            }

                        } catch (Exception e) {

                            JOptionPane.showMessageDialog(
                                    OrderShipmentPanel.this,
                                    "Failed to load orders:\n"
                                            + e.getMessage(),
                                    "Order Loading Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                };

        worker.execute();
    }



    private void loadSelectedOrder() {

        int selectedRow = ordersTable.getSelectedRow();

        if (selectedRow == -1) {
            return;
        }

        String orderId =
                ordersModel.getValueAt(selectedRow, 0).toString();

        SwingWorker<Order, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Order doInBackground() throws Exception {

                        HttpClient client =
                                HttpClient.newHttpClient();

                        HttpRequest request =
                                HttpRequest.newBuilder()
                                        .uri(URI.create(
                                                "http://localhost:8080/api/orders/"
                                                        + orderId))
                                        .GET()
                                        .build();

                        HttpResponse<String> response =
                                client.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() != 200) {

                            throw new RuntimeException(
                                    "Failed to load order. HTTP status: "
                                            + response.statusCode());
                        }

                        ObjectMapper mapper =
                                new ObjectMapper();

                        mapper.findAndRegisterModules();

                        return mapper.readValue(
                                response.body(),
                                Order.class);
                    }

                    @Override
                    protected void done() {

                        try {

                            Order order = get();

                            loadOrderDetails(order);

                            loadOrderItems(order.getOrderId());

                            loadInvoice(order.getOrderId());

                            loadShipment(order.getOrderId());

                        } catch (Exception e) {

                            JOptionPane.showMessageDialog(
                                    OrderShipmentPanel.this,
                                    "Failed to load order details:\n"
                                            + e.getMessage(),
                                    "Order Details Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void loadOrderItems(String orderId) {

        SwingWorker<List<OrderItem>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected List<OrderItem> doInBackground()
                            throws Exception {

                        HttpClient client =
                                HttpClient.newHttpClient();

                        HttpRequest request =
                                HttpRequest.newBuilder()
                                        .uri(URI.create(
                                                "http://localhost:8080/api/order-items/order/"
                                                        + orderId))
                                        .GET()
                                        .build();

                        HttpResponse<String> response =
                                client.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() != 200) {

                            throw new RuntimeException(
                                    "Failed to load order items. HTTP status: "
                                            + response.statusCode());
                        }

                        ObjectMapper mapper =
                                new ObjectMapper();

                        mapper.findAndRegisterModules();

                        return mapper.readValue(
                                response.body(),
                                new TypeReference<List<OrderItem>>() {});
                    }

                    @Override
                    protected void done() {

                        try {

                            List<OrderItem> items = get();

                            orderItemsModel.setRowCount(0);

                            for (OrderItem item : items) {

                                String productName = "";

                                if (item.getProduct() != null) {
                                    productName =
                                            item.getProduct().getProductName();
                                }

                                double subtotal =
                                        item.getQuantity()
                                                * item.getPriceAtPurchase();

                                orderItemsModel.addRow(
                                        new Object[]{
                                                productName,
                                                item.getQuantity(),
                                                String.format(
                                                        "R %.2f",
                                                        item.getPriceAtPurchase()),
                                                String.format(
                                                        "R %.2f",
                                                        subtotal)
                                        });
                            }

                        } catch (Exception e) {

                            JOptionPane.showMessageDialog(
                                    OrderShipmentPanel.this,
                                    "Failed to load order items:\n"
                                            + e.getMessage(),
                                    "Order Items Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void loadInvoice(String orderId) {

        SwingWorker<Invoice, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Invoice doInBackground()
                            throws Exception {

                        HttpClient client =
                                HttpClient.newHttpClient();

                        HttpRequest request =
                                HttpRequest.newBuilder()
                                        .uri(URI.create(
                                                "http://localhost:8080/api/invoices/order/"
                                                        + orderId))
                                        .GET()
                                        .build();

                        HttpResponse<String> response =
                                client.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 404) {
                            return null;
                        }

                        if (response.statusCode() != 200) {

                            throw new RuntimeException(
                                    "Failed to load invoice. HTTP status: "
                                            + response.statusCode());
                        }

                        ObjectMapper mapper =
                                new ObjectMapper();

                        mapper.findAndRegisterModules();

                        return mapper.readValue(
                                response.body(),
                                Invoice.class);
                    }

                    @Override
                    protected void done() {

                        try {

                            Invoice invoice = get();

                            if (invoice == null) {

                                vatLabel.setText("Not available");

                                totalLabel.setText(
                                        String.format(
                                                "R %.2f",
                                                0.00));

                                return;
                            }

                            loadInvoiceDetails(invoice);

                        } catch (Exception e) {

                            JOptionPane.showMessageDialog(
                                    OrderShipmentPanel.this,
                                    "Failed to load invoice:\n"
                                            + e.getMessage(),
                                    "Invoice Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void loadShipment(String orderId) {

        SwingWorker<Shipment, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Shipment doInBackground()
                            throws Exception {

                        HttpClient client =
                                HttpClient.newHttpClient();

                        HttpRequest request =
                                HttpRequest.newBuilder()
                                        .uri(URI.create(
                                                "http://localhost:8080/api/shipments/order/"
                                                        + orderId))
                                        .GET()
                                        .build();

                        HttpResponse<String> response =
                                client.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString());

                        // No shipment created for this order yet
                        if (response.statusCode() == 404) {
                            return null;
                        }

                        if (response.statusCode() != 200) {

                            throw new RuntimeException(
                                    "Failed to load shipment. HTTP status: "
                                            + response.statusCode());
                        }

                        ObjectMapper mapper =
                                new ObjectMapper();

                        mapper.findAndRegisterModules();

                        return mapper.readValue(
                                response.body(),
                                Shipment.class);
                    }

                    @Override
                    protected void done() {

                        try {

                            Shipment shipment = get();

                            if (shipment == null) {

                                shipmentStatusLabel.setText(
                                        "Not available");

                                trackingNumberLabel.setText(
                                        "Not available");

                                estimatedDeliveryLabel.setText(
                                        "Not available");

                                return;
                            }

                            loadShipmentDetails(shipment);

                        } catch (Exception e) {

                            JOptionPane.showMessageDialog(
                                    OrderShipmentPanel.this,
                                    "Failed to load shipment:\n"
                                            + e.getMessage(),
                                    "Shipment Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void loadOrderDetails(Order order) {

        orderItemsModel.setRowCount(0);

        subtotalLabel.setText(
                String.format(
                        "R %.2f",
                        order.getTotalAmount()));

        // Invoice will provide the actual VAT and total
        vatLabel.setText("Loading...");

        // Shipment/backend integration will provide shipping later
        shippingLabel.setText("Not available");

        totalLabel.setText("Loading...");

        // Shipment information will be loaded asynchronously
        shipmentStatusLabel.setText("Loading...");

        trackingNumberLabel.setText("Loading...");

        estimatedDeliveryLabel.setText("Loading...");
    }

    private void loadInvoiceDetails(Invoice invoice) {

        vatLabel.setText(
                String.format(
                        "R %.2f",
                        invoice.getTaxAmount()));

        totalLabel.setText(
                String.format(
                        "R %.2f",
                        invoice.getTotalAmount()));
    }

    private void loadShipmentDetails(Shipment shipment) {

        shipmentStatusLabel.setText(
                shipment.getStatus() != null
                        ? shipment.getStatus()
                        : "Not available"
        );

        trackingNumberLabel.setText(
                shipment.getShipmentId() != null
                        ? shipment.getShipmentId()
                        : "Not available"
        );

        if (shipment.getDeliveryDate() != null) {

            estimatedDeliveryLabel.setText(
                    shipment.getDeliveryDate().toString()
            );

        } else {

            estimatedDeliveryLabel.setText(
                    "Not available"
            );
        }
    }

}
