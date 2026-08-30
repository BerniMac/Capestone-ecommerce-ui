package Capstone.ui;

import Capstone.model.Product;
import Capstone.model.Review;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import static java.awt.AWTEventMulticaster.add;

public class ReviewPanel extends JPanel {
    private JComboBox<String> productComboBox;
    private JComboBox<Integer> ratingComboBox;

    private JTextArea commentArea;

    private JTable reviewTable;
    private DefaultTableModel reviewModel;
    private final String customerId = "cf3ebaa5-896a-48ea-ad05-ea60843e4a2c";

    private final Map<String, String> productIds = new HashMap<>();
    public ReviewPanel() {

        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(15,15,15,15));

        add(createReviewForm(), BorderLayout.NORTH);
        add(createReviewTable(), BorderLayout.CENTER);

        loadProducts();
        loadReviews();

    }

    private JPanel createReviewForm() {

        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(
                BorderFactory.createTitledBorder("Submit Product Review"));

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        productComboBox = new JComboBox<>();
        ratingComboBox = new JComboBox<>();

        for(int i=1;i<=5;i++){

            ratingComboBox.addItem(i);

        }

        commentArea = new JTextArea(5,25);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);

        addRow(panel,gbc,0,"Product",productComboBox);
        addRow(panel,gbc,1,"Rating",ratingComboBox);

        gbc.gridx=0;
        gbc.gridy=2;

        panel.add(new JLabel("Comment"),gbc);

        gbc.gridx=1;

        panel.add(new JScrollPane(commentArea),gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton submitButton = new JButton("Submit Review");


        submitButton.addActionListener(e -> submitReview());

        buttons.add(submitButton);

        gbc.gridx=1;
        gbc.gridy=3;

        panel.add(buttons,gbc);

        return panel;

    }

    private JScrollPane createReviewTable(){

        String[] columns={

                "Product",
                "Rating",
                "Comment",
                "Date"

        };

        reviewModel=new DefaultTableModel(columns,0){

            @Override
            public boolean isCellEditable(int row,int column){

                return false;

            }

        };

        reviewTable=new JTable(reviewModel);

        reviewTable.setRowHeight(28);

        JScrollPane scroll=new JScrollPane(reviewTable);

        scroll.setBorder(
                BorderFactory.createTitledBorder("Previous Reviews"));

        return scroll;

    }

    private void addRow(JPanel panel,
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

    private void loadProducts() {

        SwingWorker<Void, Void> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/products"))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException(
                            "Failed to load products. HTTP status: "
                                    + response.statusCode());
                }

                ObjectMapper mapper = new ObjectMapper();

                List<Product> products =
                        mapper.readValue(
                                response.body(),
                                new TypeReference<List<Product>>() {});

                SwingUtilities.invokeLater(() -> {

                    productComboBox.removeAllItems();
                    productIds.clear();

                    for (Product product : products) {

                        productComboBox.addItem(
                                product.getProductName());

                        productIds.put(
                                product.getProductName(),
                                product.getProductId());
                    }

                });

                return null;
            }

            @Override
            protected void done() {

                try {
                    get();

                } catch (Exception e) {

                    JOptionPane.showMessageDialog(
                            ReviewPanel.this,
                            "Unable to load products:\n"
                                    + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void loadReviews() {

        SwingWorker<List<Review>, Void> worker = new SwingWorker<>() {

            @Override
            protected List<Review> doInBackground() throws Exception {

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(URI.create(
                                        "http://localhost:8080/api/reviews/customer/"
                                                + customerId))
                                .GET()
                                .build();

                HttpResponse<String> response =
                        client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {

                    throw new RuntimeException(
                            "Failed to load reviews. HTTP status: "
                                    + response.statusCode());
                }

                ObjectMapper mapper = new ObjectMapper();

                mapper.registerModule(new JavaTimeModule());

                return mapper.readValue(
                        response.body(),
                        new TypeReference<List<Review>>() {
                        });
            }

            @Override
            protected void done() {

                try {

                    List<Review> reviews = get();

                    reviewModel.setRowCount(0);

                    for (Review review : reviews) {

                        String productName = "Unknown Product";

                        if (review.getProduct() != null) {
                            productName = review.getProduct().getProductName();
                        }

                        String reviewDate = "";

                        if (review.getReviewDate() != null) {
                            reviewDate =
                                    review.getReviewDate()
                                            .toLocalDate()
                                            .toString();
                        }

                        reviewModel.addRow(new Object[]{

                                productName,
                                review.getRating(),
                                review.getComment(),
                                reviewDate

                        });
                    }

                } catch (Exception e) {

                    JOptionPane.showMessageDialog(
                            ReviewPanel.this,
                            "Unable to load reviews:\n"
                                    + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void submitReview() {

        String selectedProduct =
                (String) productComboBox.getSelectedItem();

        Integer selectedRating =
                (Integer) ratingComboBox.getSelectedItem();

        String comment =
                commentArea.getText().trim();

        if (selectedProduct == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a product.");
            return;
        }

        if (selectedRating == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a rating.");
            return;
        }

        if (comment.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a comment.");
            return;
        }

        String productId = productIds.get(selectedProduct);

        if (productId == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to determine the selected product ID.");
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {

                ObjectMapper mapper = new ObjectMapper();

                Map<String, Object> requestBody =
                        new HashMap<>();

                requestBody.put(
                        "customer",
                        Map.of("customerId", customerId));

                requestBody.put(
                        "product",
                        Map.of("productId", productId));

                requestBody.put(
                        "rating",
                        selectedRating);

                requestBody.put(
                        "comment",
                        comment);

                String json =
                        mapper.writeValueAsString(requestBody);

                HttpClient client =
                        HttpClient.newHttpClient();

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(URI.create(
                                        "http://localhost:8080/api/reviews"))
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
                            "Failed to submit review. HTTP status: "
                                    + response.statusCode()
                                    + "\n"
                                    + response.body());
                }

                return null;
            }

            @Override
            protected void done() {

                try {

                    get();

                    JOptionPane.showMessageDialog(
                            ReviewPanel.this,
                            "Review submitted successfully.");


                    loadReviews();

                } catch (Exception e) {

                    JOptionPane.showMessageDialog(
                            ReviewPanel.this,
                            "Unable to submit review:\n"
                                    + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }


}
