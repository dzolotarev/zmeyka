package com.dzolotarev.zmeyka;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Окно приложения. */
public class GameFrame extends JFrame {

    public GameFrame(GamePanel panel) {
        super(Version.ARTIFACT + " " + Version.NUMBER);
        setContentPane(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                panel.requestFocusInWindow();
            }
        });
    }

    public static void show(GamePanel panel) {
        SwingUtilities.invokeLater(() -> new GameFrame(panel).setVisible(true));
    }
}
