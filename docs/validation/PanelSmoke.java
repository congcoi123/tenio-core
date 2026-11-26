package com.tenio.core.admin;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class PanelSmoke {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("tenio-panel-smoke");
        var history = new PerformanceHistory(dir, false);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 60; i++) {
            history.record(
                    new PerformanceHistory.Sample(
                            now - (60 - i) * 15_000L,
                            history.bootId(),
                            i / 8,
                            1,
                            i / 7,
                            i,
                            1_000_000,
                            5,
                            i * 100,
                            i * 200,
                            0,
                            0,
                            0)
            );
        }
        history.close();
        var loaded = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var ref = new PerformancePanel[1];
            SwingUtilities.invokeAndWait(
                    () -> {
                        ref[0] = new PerformancePanel(
                                history,
                                action -> executor.execute(() -> {
                                    if (SwingUtilities.isEventDispatchThread()) {
                                        throw new AssertionError("Archive I/O on EDT");
                                    }
                                    try {
                                        action.run();
                                    } finally {
                                        loaded.countDown();
                                    }
                                })
                        );
                        var controls = (JPanel) ref[0].getComponent(0);
                        for (var child : controls.getComponents()) {
                            if (child instanceof JButton button) {
                                button.doClick();
                            }
                        }
                    }
            );
            if (!loaded.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("History load stalled");
            }
            SwingUtilities.invokeAndWait(() -> {
                        var panel = ref[0];
                        panel.setSize(800, 420);
                        panel.doLayout();
                        for (var child : panel.getComponents()) {
                            if (child instanceof java.awt.Container container) {
                                container.doLayout();
                            }
                        }
                        var image = new BufferedImage(800, 420, BufferedImage.TYPE_INT_RGB);
                        var graphics = image.createGraphics();
                        panel.paint(graphics);
                        graphics.dispose();
                        try {
                            ImageIO.write(image, "png", Path.of(args[0]).toFile());
                        } catch (Exception failure) {
                            throw new RuntimeException(failure);
                        }
                    }
            );
        }
        System.out.println("Offscreen history panel loaded and painted; archive I/O ran off EDT");
    }
}
