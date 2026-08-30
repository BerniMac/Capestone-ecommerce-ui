package Capstone.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import Capstone.model.Inventory;
import Capstone.model.Product;
import Capstone.model.Review;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import static java.awt.AWTEventMulticaster.add;

public class CatalogPanel extends JPanel {
    private JTable productTable;
    private DefaultTableModel productTableModel;
    private List<ProductRow> productRows = new ArrayList<>();


    public CatalogPanel() {

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));


        add(createProductTable(), BorderLayout.CENTER);

        loadProductData();

    }

    private JScrollPane createCenterPanel() {

        return createProductTable();

    }

    private JScrollPane createProductTable() {

        String[] columns = {

                "ID",
                "Name",
                "Description",
                "Price",
                "Stock"

        };

        productTableModel = new DefaultTableModel(columns,0){

            @Override
            public boolean isCellEditable(int row, int column){

                return false;

            }

        };

        productTable = new JTable(productTableModel);

        productTable.setRowHeight(28);

        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return new JScrollPane(productTable);

    }


    private void loadProductData() {

        SwingWorker<List<ProductRow>, Void> worker = new SwingWorker<>() {

            @Override
            protected List<ProductRow> doInBackground() throws Exception {

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:8080/api/products"))
                                .GET()
                                .build();

                HttpResponse<String> response =
                        client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());

                List<Product> products =
                        mapper.readValue(
                                response.body(),
                                new TypeReference<List<Product>>() {
                                });

                List<ProductRow> rows = new ArrayList<>();

                for (Product product : products) {

                    Inventory inventory = loadInventory(product.getProductId());

                    rows.add(new ProductRow(
                            product,
                            inventory.getStockQuantity()));

                }

                return rows;
            }

            @Override
            protected void done() {

                try {

                    productRows = get();

                    productTableModel.setRowCount(0);

                    for (ProductRow row : productRows) {

                        Product product = row.getProduct();

                        productTableModel.addRow(new Object[]{

                                product.getProductId(),
                                product.getProductName(),
                                product.getDescription(),
                                product.getCurrentPrice(),
                                row.getStockQuantity()

                        });

                    }

                } catch (Exception e) {

                    JOptionPane.showMessageDialog(
                            CatalogPanel.this,
                            e.getMessage());

                }

            }

        };

        worker.execute();
    }

    private void viewReviews() {

        int selectedRow = productTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product.");
            return;
        }

        //String productId = productRows.get(selectedRow)
          //      .getProduct()
            //    .getProductId();

        ProductRow selectedProductRow =
                productRows.get(selectedRow);

        Product selectedProduct =
                selectedProductRow.getProduct();

        String productId =
                selectedProduct.getProductId();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {

            private String reviewText = "";

            @Override
            protected Void doInBackground() throws Exception {

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8080/api/reviews/product/" + productId))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());

                List<Review> reviews = mapper.readValue(
                        response.body(),
                        new TypeReference<List<Review>>() {});

                if (reviews.isEmpty()) {
                    reviewText = "No reviews available.";
                } else {

                    StringBuilder sb = new StringBuilder();

                    for (Review review : reviews) {

                        sb.append("Rating: ")
                                .append(review.getRating())
                                .append("/5\n");

                        sb.append("Comment: ")
                                .append(review.getComment())
                                .append("\n");

                        sb.append("Date: ")
                                .append(review.getReviewDate())
                                .append("\n");

                        sb.append("---------------------------------\n");
                    }

                    reviewText = sb.toString();
                }

                return null;
            }

            @Override
            protected void done() {

                JOptionPane.showMessageDialog(
                        CatalogPanel.this,
                        reviewText,
                        "Product Reviews",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };

        worker.execute();
    }
    private Inventory loadInventory(String productId) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:8080/api/inventory/product/" + productId))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        return mapper.readValue(response.body(), Inventory.class);
    }
    private static class ProductRow {

        private final Product product;
        private final int stockQuantity;

        public ProductRow(Product product, int stockQuantity) {
            this.product = product;
            this.stockQuantity = stockQuantity;
        }

        public Product getProduct() {
            return product;
        }

        public int getStockQuantity() {
            return stockQuantity;
        }
    }
}
