package antafes.sfmanager.gui;

import antafes.sfmanager.util.Timer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class Splash extends JWindow
{
    private final int min = 0, max = 100;
    private boolean show = true;
    final JProgressBar progressBar = new JProgressBar(min, max);

    public Splash(final URL imgPath, final int showFor, final JFrame frame)
    {
        final Timer timer = new Timer(showFor);
        Thread wRunner = new Thread(() -> {
            timer.start();
            while (show && timer.getCounter() <= showFor) {
                Splash.this.setVisible(true);
            }
            Splash.this.setVisible(false);
            Splash.this.dispose();
            frame.setVisible(true);
        });

        final Runnable pbRunner = () -> {
            for (int i = min; i <= max; i++) {
                try {
                    Thread.sleep(showFor / max);
                } catch (InterruptedException ignored) {
                }
                progressBar.setValue(i);
            }
        };

        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());
        ImageIcon icon = new ImageIcon(imgPath);
        this.setSize(icon.getIconWidth(), icon.getIconHeight());
        contentPane.add(new JLabel(icon, JLabel.CENTER), BorderLayout.CENTER);
        contentPane.add(progressBar, BorderLayout.SOUTH);

        this.setBackground(Color.WHITE);
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                show = false;
                setVisible(false);
                dispose();
            }
        });
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        new Thread(wRunner).start();
        new Thread(pbRunner).start();
    }
}
