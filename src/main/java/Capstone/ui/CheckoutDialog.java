package Capstone.ui;

import Capstone.model.Card;
import Capstone.model.Customer;
import Capstone.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static java.awt.AWTEventMulticaster.add;

public class CheckoutDialog extends JDialog {
    private JTextField streetField;
    private JTextField cityField;
    private JTextField stateField;
    private JTextField zipField;
    private JTextField countryField;

    private JTextField cardHolderField;
    private JTextField cardTypeField;
    private JTextField cardNumberField;
    private JTextField expiryField;
    private JPasswordField cvvField;

    private JLabel subtotalLabel;
    private JLabel vatLabel;
    private JLabel shippingLabel;
    private JLabel totalLabel;

    private JLabel shipmentStatusLabel;
    private JLabel estimatedDeliveryLabel;
    private final String customerId;

    public CheckoutDialog(JFrame parent, String customerId) {

        super(parent, "Checkout", true);

        this.customerId = customerId;

        setSize(850,700);
        setLocationRelativeTo(parent);

        setLayout(new BorderLayout(15,15));

        ((JComponent)getContentPane())
                .setBorder(new EmptyBorder(15,15,15,15));

        add(createTitle(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createButtons(), BorderLayout.SOUTH);

        //loadOrderSummary();
    }

    private JLabel createTitle(){

        JLabel label = new JLabel("Checkout");

        label.setFont(
                label.getFont().deriveFont(Font.BOLD,26f));

        return label;

    }

    private JPanel createCenterPanel(){

        JPanel panel = new JPanel(new GridLayout(2,2,15,15));

        panel.add(createAddressPanel());
        panel.add(createPaymentPanel());
        panel.add(createSummaryPanel());
        panel.add(createShipmentPanel());

        return panel;

    }

    private JPanel createAddressPanel(){

        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(
                BorderFactory.createTitledBorder("Delivery Address"));

        GridBagConstraints gbc = createConstraints();

        streetField = new JTextField(18);
        cityField = new JTextField(18);
        stateField = new JTextField(18);
        zipField = new JTextField(18);
        countryField = new JTextField(18);

        addField(panel, gbc,0,"Street",streetField);
        addField(panel, gbc,1,"City",cityField);
        addField(panel, gbc,2,"State",stateField);
        addField(panel, gbc,3,"Zip Code",zipField);
        addField(panel, gbc,4,"Country",countryField);

        return panel;

    }

    private JPanel createPaymentPanel(){

        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(
                BorderFactory.createTitledBorder("Payment"));

        GridBagConstraints gbc = createConstraints();

        cardHolderField = new JTextField(18);
        cardTypeField = new JTextField(18);
        cardNumberField = new JTextField(18);
        expiryField = new JTextField(18);
        cvvField = new JPasswordField(18);

        addField(panel, gbc, 0, "Cardholder", cardHolderField);
        addField(panel, gbc, 1, "Card type", cardTypeField);
        addField(panel, gbc, 2, "Card Number", cardNumberField);
        addField(panel, gbc, 3, "Expiry", expiryField);
        addField(panel, gbc, 4, "CVV", cvvField);

        JButton payButton = new JButton("Pay");

        gbc.gridx = 1;
        gbc.gridy = 5;

        panel.add(payButton, gbc);

        payButton.addActionListener(e -> saveCard());

        return panel;

    }
    private void saveCard() {

        String cardHolderName = cardHolderField.getText();
        String cardNumber = cardNumberField.getText();
        String cardExpiry = expiryField.getText();
        String cardCVV = new String(cvvField.getPassword());

        Card card = new Card();

        card.setCardHolderName(cardHolderName);
        card.setCardType("VISA");
        card.setCardNumber(cardNumber);
        card.setCardExpiry(cardExpiry);
        card.setCardCVV(cardCVV);
        SwingWorker<String, Void> worker = new SwingWorker<>() {

            @Override
            protected String doInBackground() throws Exception {

                ObjectMapper mapper = new ObjectMapper();

                String json = mapper.writeValueAsString(card);

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8080/api/cards"))
                        .header(
                                "Content-Type",
                                "application/json")
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json))
                        .build();

                HttpResponse<String> response =
                        client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException(
                            "Failed to save card. Status: "
                                    + response.statusCode());
                }

                return response.body();
            }

            @Override
            protected void done() {

                try {

                    String response = get();

                    JOptionPane.showMessageDialog(
                            null,
                            "Card saved successfully!");

                } catch (Exception e) {

                    JOptionPane.showMessageDialog(
                            null,
                            "Error saving card: "
                                    + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private JPanel createSummaryPanel(){

        JPanel panel = new JPanel(new GridLayout(4,2,5,10));

        panel.setBorder(
                BorderFactory.createTitledBorder("Invoice Preview"));

        subtotalLabel = new JLabel("R 0.00");
        vatLabel = new JLabel("R 0.00");
        shippingLabel = new JLabel("R 0.00");
        totalLabel = new JLabel("R 0.00");

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

    private JPanel createShipmentPanel(){

        JPanel panel = new JPanel(new GridLayout(2,2,5,10));

        panel.setBorder(
                BorderFactory.createTitledBorder("Shipment"));

        shipmentStatusLabel = new JLabel("Awaiting Payment");
        estimatedDeliveryLabel = new JLabel("3-5 Business Days");

        panel.add(new JLabel("Status"));
        panel.add(shipmentStatusLabel);

        panel.add(new JLabel("Estimated"));
        panel.add(estimatedDeliveryLabel);

        return panel;

    }

    private JPanel createButtons(){

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelButton = new JButton("Cancel");
        JButton placeOrderButton = new JButton("Place Order");

        cancelButton.addActionListener(e -> dispose());

        placeOrderButton.addActionListener(e -> placeOrder());

        panel.add(cancelButton);
        panel.add(placeOrderButton);

        return panel;

    }

    private void placeOrder() {

        SwingWorker<Void, Void> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {

                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules();

                Customer customer = new Customer();
                customer.setCustomerId(customerId);

                Order order = new Order();

                order.setCustomer(customer);
                order.setOrderDate(LocalDateTime.parse(LocalDateTime.now().toString()));
                order.setTotalAmount(order.getTotalAmount());

                String json = mapper.writeValueAsString(order);

                System.out.println("Order JSON:");
                System.out.println(json);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8080/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpClient client = HttpClient.newHttpClient();

                HttpResponse<String> response =
                        client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                System.out.println(
                        "Order response status: "
                                + response.statusCode()
                );

                System.out.println(
                        "Order response body: "
                                + response.body()
                );

                if (response.statusCode() < 200
                        || response.statusCode() >= 300) {

                    throw new RuntimeException(
                            "Order creation failed: "
                                    + response.body()
                    );
                }

                return null;
            }

            @Override
            protected void done() {

                try {

                    get();

                    JOptionPane.showMessageDialog(
                            CheckoutDialog.this,
                            "Order successfully placed!"
                    );

                    dispose();

                } catch (Exception ex) {

                    JOptionPane.showMessageDialog(
                            CheckoutDialog.this,
                            ex.getMessage(),
                            "Checkout Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }


    private void addField(JPanel panel,
                          GridBagConstraints gbc,
                          int row,
                          String label,
                          JComponent component){

        gbc.gridx=0;
        gbc.gridy=row;

        panel.add(new JLabel(label),gbc);

        gbc.gridx=1;

        panel.add(component,gbc);

    }

    private GridBagConstraints createConstraints(){

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        return gbc;

    }
}
