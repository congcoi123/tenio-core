/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.admin;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Local-only, explicit range queries. Archive I/O never runs on the Swing event thread.
 */
final class PerformancePanel extends JPanel {

    private List<PerformanceHistory.Sample> points = List.of();
    private final JComboBox<String> metric = new JComboBox<>(
            new String[]{
                    "Players",
                    "CPU %",
                    "Heap MiB",
                    "Read B/s",
                    "Written B/s",
                    "Command p95 µs",
                    "Command maximum µs"
            }
    );
    private final JComboBox<Integer> minutes = new JComboBox<>(new Integer[]{15, 60, 360, 1440, 10080});
    private final AtomicBoolean loading = new AtomicBoolean();
    private final JLabel status = new JLabel("Choose a range and Load. Points preserve CPU peaks when downsampled.");

    PerformancePanel(PerformanceHistory history, Executor worker) {
        super(new BorderLayout());
        var controls = new JPanel();
        controls.add(metric);
        controls.add(new JLabel("Minutes"));
        controls.add(minutes);
        var load = new JButton("Load");
        controls.add(load);
        add(controls, BorderLayout.NORTH);
        var plot = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                draw((Graphics2D) graphics, getWidth(), getHeight());
            }
        };
        add(plot);
        add(status, BorderLayout.SOUTH);
        metric.addActionListener(event -> plot.repaint());
        load.addActionListener(
                event -> {
                    if (!loading.compareAndSet(false, true)) {
                        return;
                    }
                    int range = (Integer) minutes.getSelectedItem();
                    load.setEnabled(false);
                    try {
                        worker.execute(() -> {
                            try {
                                var end = Instant.now();
                                var samples = history.query(end.minusSeconds(range * 60L), end, 300);
                                SwingUtilities.invokeLater(
                                        () -> {
                                            points = samples;
                                            plot.repaint();
                                            status.setText(samples.size()
                                                    + " retained points; export losses: "
                                                    + history.droppedExports());
                                        }
                                );
                            } catch (Exception failure) {
                                SwingUtilities.invokeLater(() -> status.setText("Unable to read archive: " + failure.getMessage()));
                            } finally {
                                loading.set(false);
                                SwingUtilities.invokeLater(() -> load.setEnabled(true));
                            }
                        });
                    } catch (RejectedExecutionException closed) {
                        loading.set(false);
                        load.setEnabled(true);
                    }
                });
    }

    private double value(PerformanceHistory.Sample point) {
        return switch (metric.getSelectedIndex()) {
            case 0 -> point.players();
            case 1 -> Math.max(0, point.cpuPercent());
            case 2 -> point.heapBytes() / (1024.0 * 1024);
            case 3 -> point.readBytesPerSecond();
            case 4 -> point.writtenBytesPerSecond();
            case 5 -> point.commandP95Nanos() / 1000.0;
            default -> point.commandMaximumNanos() / 1000.0;
        };
    }

    private void draw(Graphics2D g, int width, int height) {
        int left = 80,
                top = 25,
                right = Math.max(left + 1, width - 25),
                bottom = Math.max(top + 1, height - 45);
        g.setColor(Color.GRAY);
        g.drawLine(left, top, left, bottom);
        g.drawLine(left, bottom, right, bottom);
        if (points.isEmpty()) {
            g.drawString("No samples in the selected range", left + 8, top + 20);
            return;
        }
        double maximum = Math.max(1, points.stream().mapToDouble(this::value).max().orElse(1));
        long start = points.getFirst().timestamp(),
                span = Math.max(1, points.getLast().timestamp() - start);
        g.drawString(String.format(java.util.Locale.ROOT, "%,.1f", maximum), 5, top + 5);
        g.drawString("0", left - 20, bottom);
        g.drawString((String) metric.getSelectedItem(), left, 16);
        var time = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
        g.drawString(time.format(Instant.ofEpochMilli(start)), left, bottom + 20);
        g.drawString(time.format(Instant.ofEpochMilli(start + span)), Math.max(left, right - 95), bottom + 20);
        int previousX = -1, previousY = -1;
        String boot = null;
        for (var point : points) {
            int x = left + (int) ((point.timestamp() - start) * (right - left) / span);
            int y = bottom - (int) (value(point) * (bottom - top) / maximum);
            if (!point.bootId().equals(boot)) {
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(x, top, x, bottom);
            } else if (previousX >= 0) {
                g.setColor(new Color(35, 100, 175));
                g.drawLine(previousX, previousY, x, y);
            }
            previousX = x;
            previousY = y;
            boot = point.bootId();
        }
    }
}
