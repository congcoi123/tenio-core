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

import java.util.function.Consumer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Local desktop console with the operating system look and feel. */
public final class AdminWindow implements AutoCloseable {
  @FunctionalInterface
  public interface Query {
    InspectionResult run(String input) throws Exception;
  }

  private final Query query;
  @FunctionalInterface
  public interface LocalCommands {
    boolean execute(String command, Consumer<String> output);
  }

  private final LocalCommands commands;
  private final AtomicBoolean closed = new AtomicBoolean();
  private final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor(
      Thread.ofPlatform().daemon(true).name("tenio-admin-ui-query").factory());
  private JFrame frame;
  private Timer refresh;
  private JTextArea output;
  private JTextField input;
  private DefaultTableModel table;
  private Chart chart;
  private boolean busy;
  private String pending;

  public AdminWindow(String title, Query query, LocalCommands commands) {
    this.query = query;
    this.commands = commands;
    SwingUtilities.invokeLater(() -> open(title));
  }

  private void open(String title) {
    if (closed.get()) {
      return;
    }
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ReflectiveOperationException | UnsupportedLookAndFeelException unavailable) {
      // Swing's installed default remains available.
    }
    frame = new JFrame(title);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override public void windowClosed(WindowEvent event) {
        close();
      }
    });
    table = new DefaultTableModel(new Object[]{"Metric", "Value"}, 0) {
      @Override public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    chart = new Chart();
    chart.setBorder(BorderFactory.createTitledBorder("Connected players (last 60 samples)"));
    output = new JTextArea(8, 64);
    output.setEditable(false);
    input = new JTextField();
    input.setToolTipText("stats, room <id>, session <id>, network pressure, history, limits");
    input.addActionListener(event -> execute(input.getText().strip(), true));
    var tabs = new JTabbedPane();
    tabs.addTab("Statistics", new JScrollPane(new JTable(table)));
    tabs.addTab("Players", chart);
    var lower = new JPanel(new BorderLayout(4, 4));
    lower.add(new JScrollPane(output));
    lower.add(input, BorderLayout.SOUTH);
    var split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, lower);
    split.setResizeWeight(0.6);
    frame.add(split);
    append(commands == null ? "stats | room <id> | session <id> | network pressure | limits | history [id]"
        : "help | stats | room <id> | session <id> | network pressure | limits | history [id]");
    frame.setSize(new Dimension(680, 500));
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
    input.requestFocusInWindow();
    refresh = new Timer(5000, e -> {
      if ((frame.getExtendedState() & JFrame.ICONIFIED) == 0) {
        execute("stats", false);
      }
    });
    refresh.start();
    execute("stats", false);
  }

  private void execute(String command, boolean manual) {
    if (busy && manual && pending == null) {
      pending = command;
      input.setEditable(false);
    }
    if (closed.get() || busy || command.isBlank()) {
      return;
    }
    busy = true;
    if (manual) {
      input.setText("");
      append("> " + command);
    }
    worker.execute(() -> {
      try {
        boolean handled = manual && commands != null && !command.equals("server stats")
            && commands.execute(command, text -> SwingUtilities.invokeLater(() -> append(text)));
        if (!handled) {
          var result = query.run(command);
          SwingUtilities.invokeLater(() -> {
            if (closed.get()) {
              return;
            }
            if (command.equals("stats") || command.equals("server stats")) {
              table.setRowCount(0);
              for (int i = 0; i < result.labels().size(); i++) {
                table.addRow(new Object[]{result.labels().get(i), result.values().get(i)});
                if (result.labels().get(i).equals("Players")) {
                  chart.addSample(Integer.parseInt(result.values().get(i)));
                }
              }
            } else if (manual) {
              for (int i = 0; i < result.labels().size(); i++) {
                append(result.labels().get(i) + ": " + result.values().get(i));
              }
              if (result.labels().isEmpty()) {
                append("No entries");
              }
            }
          });
        }
      } catch (Exception failure) {
        SwingUtilities.invokeLater(() -> append(failure.toString()));
      } finally {
        SwingUtilities.invokeLater(() -> {
          busy = false;
          if (pending != null && !closed.get()) {
            String next = pending;
            pending = null;
            input.setEditable(true);
            execute(next, true);
          }
        });
      }
    });
  }

  private void append(String text) {
    if (!closed.get()) {
      if (output.getDocument().getLength() > 32000) {
        output.replaceRange("", 0, 16000);
      }
      output.append(text + System.lineSeparator());
      output.setCaretPosition(output.getDocument().getLength());
    }
  }

  @Override public void close() {
    if (closed.compareAndSet(false, true)) {
      worker.shutdownNow();
      SwingUtilities.invokeLater(() -> {
        if (refresh != null) {
          refresh.stop();
        }
        if (frame != null) {
          frame.dispose();
        }
      });
    }
  }

  private static final class Chart extends JPanel {
    private final ArrayDeque<Integer> samples = new ArrayDeque<>();
    void addSample(int count) {
      if (samples.size() == 60) {
        samples.removeFirst();
      }
      samples.addLast(count);
      repaint();
    }
    @Override protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      int max = samples.stream().mapToInt(Integer::intValue).max().orElse(1);
      int i = 0, previousX = 0, previousY = 0;
      for (int count : samples) {
        int x = 12 + i * (getWidth() - 24) / 59;
        int y = getHeight() - 14 - (int) (count / (double) Math.max(1, max) * (getHeight() - 48));
        if (i++ > 0) {
          g.drawLine(previousX, previousY, x, y);
        }
        previousX = x;
        previousY = y;
      }
    }
  }
}
