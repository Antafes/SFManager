/*
 * This file is part of SFManager.
 *
 * SFManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SFManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SFManager. If not, see <http://www.gnu.org/licenses/>.
 *
 * @package SFManager
 * @author Marian Pollzien <map@wafriv.de>
 * @copyright (c) 2026, Marian Pollzien
 * @license https://www.gnu.org/licenses/lgpl.html LGPLv3
 */

package antafes.sf.manager.gui;

import antafes.sf.manager.util.Timer;

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
