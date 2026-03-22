import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TodoApp extends JFrame {
    private static final String DATA_FILE = System.getProperty("user.home") + "/.todo_app/tasks.json";
    private List<Task> tasks = new ArrayList<>();
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> taskList;
    private JTextField taskInput;
    private JLabel statusLabel;
    private int selectedIndex = -1;

    public TodoApp() {
        setTitle("To-Do List Manager");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(28, 10, 10));

        loadTasks();
        initUI();
        refreshTaskList();
        updateStatus();
    }

    private void initUI() {
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(104, 109, 114));
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("📋 My Tasks", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(new Color(85, 85, 85)); // Light dark background
        inputPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        taskInput = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 200)); // White translucent
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        taskInput.setOpaque(false);
        taskInput.setFont(new Font("Arial", Font.PLAIN, 12));
        taskInput.setPreferredSize(new Dimension(400, 30));
        taskInput.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        taskInput.addActionListener(e -> addTask());

        JButton addButton = new JButton("➕ Add");
        addButton.setBackground(new Color(173, 216, 230)); // Light blue
        addButton.setForeground(Color.BLACK); // Contrast
        addButton.setFont(new Font("Arial", Font.BOLD, 11));
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> addTask());

        inputPanel.add(taskInput, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.NORTH);

        // Task list with scrollbar
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        taskList = new JList<String>(listModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index > -1 && index < tasks.size()) {
                    Task task = tasks.get(index);
                    return task.isCompleted() ? "Completed task" : "Pending task";
                }
                return null;
            }
        };
        ToolTipManager.sharedInstance().registerComponent(taskList);
        taskList.setFont(new Font("Arial", Font.PLAIN, 12));
        taskList.setBackground(Color.WHITE);
        taskList.addListSelectionListener(e -> {
            selectedIndex = taskList.getSelectedIndex();
            updateStatus();
        });
        taskList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = taskList.locationToIndex(e.getPoint());
                if (index > -1 && index < tasks.size()) {
                    tasks.get(index).toggleCompleted();
                    saveTasks();
                    refreshTaskList();
                    updateStatus();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setBorder(null);
        listPanel.add(scrollPane, BorderLayout.CENTER);
        add(listPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        // Status bar
        statusLabel = new JLabel("Total: 0 | Completed: 0 | Pending: 0");
        statusLabel.setBackground(new Color(45, 112, 129));
        statusLabel.setForeground(new Color(50, 71, 86));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(75, 114, 50));
        buttonPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JButton completeBtn = createButton("✓ Complete", new Color(52, 152, 219), e -> completeTask());
        JButton editBtn = createButton("✏️ Edit", new Color(243, 156, 18), e -> editTask());
        JButton deleteBtn = createButton("🗑️ Delete", new Color(231, 76, 60), e -> deleteTask());
        JButton clearBtn = createButton("🧹 Clear Completed", new Color(149, 165, 166), e -> clearCompleted());

        buttonPanel.add(completeBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(clearBtn);

        return buttonPanel;
    }

    private JButton createButton(String text, Color bgColor, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 9));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(5, 10, 5, 10));
        btn.addActionListener(listener);
        return btn;
    }

    private void addTask() {
        String taskText = taskInput.getText().trim();
        if (taskText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a task description.", "Empty Task", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Task newTask = new Task(tasks.size() + 1, taskText, false, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        tasks.add(newTask);
        saveTasks();
        taskInput.setText("");
        refreshTaskList();
        updateStatus();
        JOptionPane.showMessageDialog(this, "Task added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void completeTask() {
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        tasks.get(selectedIndex).toggleCompleted();
        saveTasks();
        refreshTaskList();
        updateStatus();
    }

    private void editTask() {
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String currentText = tasks.get(selectedIndex).text;
        String newText = JOptionPane.showInputDialog(this, "Edit task description:", "Edit Task", JOptionPane.PLAIN_MESSAGE, null, new Object[]{}, currentText).toString();

        if (newText != null && !newText.trim().isEmpty()) {
            tasks.get(selectedIndex).text = newText.trim();
            saveTasks();
            refreshTaskList();
            updateStatus();
            JOptionPane.showMessageDialog(this, "Task updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void deleteTask() {
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this task?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            tasks.remove(selectedIndex);
            saveTasks();
            refreshTaskList();
            updateStatus();
            JOptionPane.showMessageDialog(this, "Task deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearCompleted() {
        long completedCount = tasks.stream().filter(Task::isCompleted).count();
        if (completedCount == 0) {
            JOptionPane.showMessageDialog(this, "There are no completed tasks to clear.", "No Completed Tasks", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (JOptionPane.showConfirmDialog(this, String.format("Remove %d completed task(s)?", completedCount), "Confirm Clear", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            tasks.removeIf(Task::isCompleted);
            saveTasks();
            refreshTaskList();
            updateStatus();
            JOptionPane.showMessageDialog(this, "Completed tasks cleared!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void refreshTaskList() {
        listModel.clear();
        for (Task task : tasks) {
            String displayText;
            if (task.completed) {
                displayText = "<html><strike>☒ " + task.text + "</strike></html>";
            } else {
                displayText = "<html>☐ " + task.text + "</html>";
            }
            listModel.addElement(displayText);
        }
    }

    private void updateStatus() {
        int total = tasks.size();
        long completed = tasks.stream().filter(Task::isCompleted).count();
        int pending = total - (int)completed;
        statusLabel.setText(String.format("Total: %d | Completed: %d | Pending: %d", total, completed, pending));
    }

    private void loadTasks() {
        try {
            Files.createDirectories(Paths.get(DATA_FILE).getParent());
            if (Files.exists(Paths.get(DATA_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(DATA_FILE)));
                // Simple JSON parsing - in production, use a proper JSON library like Gson
                // For now, we'll start with empty list if file exists but can't parse
                tasks = new ArrayList<>();
            }
        } catch (IOException e) {
            tasks = new ArrayList<>();
        }
    }

    private void saveTasks() {
        try {
            Files.createDirectories(Paths.get(DATA_FILE).getParent());
            // Simple JSON serialization - in production, use Gson or Jackson
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                json.append(String.format("{\n" +
                    "    \"id\": %d,\n" +
                    "    \"text\": \"%s\",\n" +
                    "    \"completed\": %s,\n" +
                    "    \"created_at\": \"%s\"\n" +
                    "}", task.id, task.text.replace("\"", "\\\""), task.completed, task.createdAt));
                if (i < tasks.size() - 1) json.append(",");
            }
            json.append("]");
            Files.write(Paths.get(DATA_FILE), json.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save tasks: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TodoApp().setVisible(true);
        });
    }

    static class Task {
        int id;
        String text;
        boolean completed;
        String createdAt;

        Task(int id, String text, boolean completed, String createdAt) {
            this.id = id;
            this.text = text;
            this.completed = completed;
            this.createdAt = createdAt;
        }

        boolean isCompleted() {
            return completed;
        }

        void toggleCompleted() {
            completed = !completed;
        }
    }
}
