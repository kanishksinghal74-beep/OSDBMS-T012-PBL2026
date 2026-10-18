package com.smartwarehouse;

import javax.swing.*;

public class Main {
    public static void main(String[] a) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {}

            new LoginFrame().setVisible(true);
        });
    }
}