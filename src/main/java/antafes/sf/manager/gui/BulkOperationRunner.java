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

import lombok.Setter;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs long operations off the event dispatch thread while a modal progress animation is shown.
 * The result callbacks run on the event dispatch thread after the whole operation has finished.
 */
@UtilityClass
public final class BulkOperationRunner
{
    @FunctionalInterface
    public interface Work<T>
    {
        T run() throws Exception;
    }

    /**
     * Runs operations on the calling thread without a progress dialog. Intended for tests.
     */
    @Setter
    private volatile boolean synchronous;

    public <T> void run(
        Component parent,
        String message,
        Work<T> work,
        Consumer<T> onSuccess,
        Consumer<Exception> onFailure
    )
    {
        if (synchronous || GraphicsEnvironment.isHeadless()) {
            T result;
            try {
                result = work.run();
            } catch (Exception exception) {
                onFailure.accept(exception);
                return;
            }
            onSuccess.accept(result);
            return;
        }

        JDialog progressDialog = createProgressDialog(parent, message);
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception
            {
                return work.run();
            }

            @Override
            protected void done()
            {
                progressDialog.dispose();
                try {
                    onSuccess.accept(this.get());
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    onFailure.accept(cause instanceof Exception cast ? cast : exception);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(exception);
                }
            }
        };
        worker.execute();
        progressDialog.setVisible(true);
    }

    private JDialog createProgressDialog(Component parent, String message)
    {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window window) {
            owner = window;
        }
        JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        content.add(new JLabel(message), BorderLayout.NORTH);
        content.add(progressBar, BorderLayout.CENTER);
        dialog.setContentPane(content);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(280, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);

        return dialog;
    }
}
