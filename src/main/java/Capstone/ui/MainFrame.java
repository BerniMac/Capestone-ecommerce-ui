package Capstone.ui;

import javax.swing.*;
import java.awt.*;

import static java.awt.AWTEventMulticaster.add;
public class MainFrame extends JFrame {

    private final JTabbedPane tabbedPane;
    private final String customerId;

    public MainFrame() {

        super("legendary sprays");

        this.customerId = "cf3ebaa5-896a-48ea-ad05-ea60843e4a2c";

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();

        tabbedPane.addTab(" Product Catalog", new CatalogPanel());
        tabbedPane.addTab(" Customer Dashboard", new CustomerDashboardPanel());
        tabbedPane.addTab(" Orders", new OrderShipmentPanel(customerId));
        tabbedPane.addTab(" Reviews", new ReviewPanel());

        add(createToolbar(), BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JToolBar createToolbar() {

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);


        toolBar.addSeparator();

        JButton checkoutButton = new JButton("Checkout");

        checkoutButton.addActionListener(e -> {

            CheckoutDialog dialog =
                    new CheckoutDialog(this, customerId);

            dialog.setVisible(true);
        });

        toolBar.add(checkoutButton);

        return toolBar;

    }


}
