package Capstone;


import Capstone.ui.MainFrame;

import javax.swing.*;

public class Main {
    static void main() {
       SwingUtilities.invokeLater(() -> {

           MainFrame frame = new MainFrame();
           frame.setVisible(true);

       });
    }
}
