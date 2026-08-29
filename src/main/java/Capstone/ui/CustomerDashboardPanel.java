package Capstone.ui;

import Capstone.model.Address;
import Capstone.model.Customer;
import Capstone.model.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import static java.awt.AWTEventMulticaster.add;

public class CustomerDashboardPanel extends JPanel {
    private JLabel nameLabel;
    private JLabel emailLabel;
    private JLabel phoneLabel;

    private JTable addressTable;
    private JTable notificationTable;

    private DefaultTableModel addressModel;
    private DefaultTableModel notificationModel;
    private static final String CUSTOMER_ID =
            "cf3ebaa5-896a-48ea-ad05-ea60843e4a2c";

    public CustomerDashboardPanel() {

        setLayout(new BorderLayout(10,10));

        setBorder(new EmptyBorder(15,15,15,15));

        add(createProfilePanel(), BorderLayout.NORTH);

        add(createCenterPanel(), BorderLayout.CENTER);

        loadCustomer();



    }

    private JPanel createProfilePanel(){

        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(
                BorderFactory.createTitledBorder("Customer Profile"));

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;

        nameLabel = new JLabel();
        emailLabel = new JLabel();
        phoneLabel = new JLabel();

        addProfileRow(panel, gbc,0,"Name:",nameLabel);
        addProfileRow(panel, gbc,1,"Email:",emailLabel);
        addProfileRow(panel, gbc,2,"Phone:",phoneLabel);

        return panel;

    }

    private JSplitPane createCenterPanel(){

        JSplitPane splitPane = new JSplitPane();

        splitPane.setResizeWeight(0.5);

        splitPane.setLeftComponent(createAddressPanel());

        splitPane.setRightComponent(createNotificationPanel());

        return splitPane;

    }

    private JScrollPane createAddressPanel(){

        String[] columns = {

                "Street",
                "City",
                "State",
                "Zip Code",
                "Country"

        };

        addressModel = new DefaultTableModel(columns,0){

            @Override
            public boolean isCellEditable(int row,int column){

                return false;

            }

        };

        addressTable = new JTable(addressModel);

        addressTable.setRowHeight(28);

        JScrollPane scrollPane = new JScrollPane(addressTable);

        scrollPane.setBorder(
                BorderFactory.createTitledBorder("Saved Addresses"));

        return scrollPane;

    }

    private JScrollPane createNotificationPanel(){

        String[] columns = {
                "Notification",
                "Date",
                "Status"
        };

        notificationModel = new DefaultTableModel(columns,0){

            @Override
            public boolean isCellEditable(int row,int column){

                return false;

            }

        };

        notificationTable = new JTable(notificationModel);

        notificationTable.setRowHeight(28);

        JScrollPane scrollPane = new JScrollPane(notificationTable);

        scrollPane.setBorder(
                BorderFactory.createTitledBorder("Notifications"));

        return scrollPane;

    }

    private void addProfileRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label,
            JLabel value){

        gbc.gridx = 0;
        gbc.gridy = row;

        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;

        panel.add(value, gbc);

    }


    private void loadCustomer() {

        new SwingWorker<Customer, Void>() {

            @Override
            protected Customer doInBackground() throws Exception {

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8080/api/customers/" + CUSTOMER_ID))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(request,
                                HttpResponse.BodyHandlers.ofString());
                System.out.println("Status: " + response.statusCode());
                System.out.println("Body: " + response.body());
                if (response.statusCode() != 200) {
                    return null;
                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());

                return mapper.readValue(response.body(), Customer.class);
            }

            @Override
            protected void done() {

                try {

                    Customer customer = get();

                    if (customer == null) {

                        JOptionPane.showMessageDialog(
                                CustomerDashboardPanel.this,
                                "Customer not found.");

                        return;
                    }

                    nameLabel.setText(customer.getName());
                    emailLabel.setText(customer.getEmail());
                    phoneLabel.setText(customer.getPhone());

                    loadAddresses(customer.getCustomerId());
                    loadNotifications(customer.getCustomerId());


                }
                catch (Exception ex) {

                    JOptionPane.showMessageDialog(
                            CustomerDashboardPanel.this,
                            ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);

                }

            }

        }.execute();

    }


    private void loadAddresses(String customerId) {

        new SwingWorker<List<Address>, Void>() {

            @Override
            protected List<Address> doInBackground() throws Exception {

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8080/api/addresses/customer/" + customerId))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {

                    throw new RuntimeException(
                            "Failed to load addresses. HTTP "
                                    + response.statusCode());

                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());

                return mapper.readValue(
                        response.body(),
                        new TypeReference<List<Address>>() {});
            }

            @Override
            protected void done() {

                try {

                    List<Address> addresses = get();

                    addressModel.setRowCount(0);

                    for (Address address : addresses) {

                        addressModel.addRow(new Object[]{

                                address.getStreetAddress(),
                                address.getCity(),
                                address.getState(),
                                address.getPostalCode(),
                                address.getCountry()

                        });

                    }

                } catch (Exception ex) {

                    JOptionPane.showMessageDialog(
                            CustomerDashboardPanel.this,
                            ex.getMessage(),
                            "Address Error",
                            JOptionPane.ERROR_MESSAGE);

                }

            }

        }.execute();

    }


    private void loadNotifications(String customerId) {

        new SwingWorker<List<Notification>, Void>() {

            @Override
            protected List<Notification> doInBackground() throws Exception {

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8080/api/notifications/customer/" + customerId))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());
                System.out.println("Status: " + response.statusCode());
                System.out.println("Body: " + response.body());
                if (response.statusCode() != 200) {

                    throw new RuntimeException(
                            "Failed to load notifications. HTTP "
                                    + response.statusCode());

                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());

                return mapper.readValue(
                        response.body(),
                        new TypeReference<List<Notification>>() {});
            }

            @Override
            protected void done() {

                try {

                    List<Notification> notifications = get();

                    System.out.println("Notifications: " + notifications.size());

                    notificationModel.setRowCount(0);

                    for (Notification notification : notifications) {

                        System.out.println(notification.getMessage());

                        notificationModel.addRow(new Object[]{

                                notification.getMessage(),
                                notification.getNotificationDate(),
                                notification.getStatus()


                        });

                    }

                    System.out.println("Rows = " + notificationModel.getRowCount());

                } catch (Exception ex) {

                    ex.printStackTrace();

                    JOptionPane.showMessageDialog(
                            CustomerDashboardPanel.this,
                            ex.getMessage(),
                            "Notification Error",
                            JOptionPane.ERROR_MESSAGE);

                }

            }

        }.execute();

    }
    private void displayNotifications(List<Notification> notifications) {

        DefaultTableModel model =
                (DefaultTableModel) notificationTable.getModel();

        model.setRowCount(0);

        for (Notification notification : notifications) {
            System.out.println(
                    notification.getMessage() + " | " +
                            notification.getStatus());
            model.addRow(new Object[]{

                    notification.getMessage(),
                    notification.getNotificationDate(),
                    notification.getStatus()

            });
            System.out.println(notificationModel.getRowCount());
        }
    }
}
