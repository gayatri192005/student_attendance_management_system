// File: StudentManagementApp.java
// A single-file, fully-runnable Java Swing app for a Student Management System
// Features: Add/Edit/Delete, Search (live), Sort, Realtime stats, CSV persistence, Input validation
// Compile: javac StudentManagementApp.java
// Run:     java StudentManagementApp
/* StudentManagementSystem/
??? src/
?   ??? model/
?   ?   ??? Student.java          # Student entity class
?   ??? service/
?   ?   ??? StudentService.java   # CRUD logic
?   ??? ui/
?   ?   ??? MainUI.java           # Swing UI (main frame)
?   ?   ??? AddStudentDialog.java # Add student popup
?   ?   ??? UpdateStudentDialog.java # Update popup
?   ??? App.java                  # Main launcher
??? README.md
??? LICENSE
*/

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class StudentManagementApp {

    // ---------- Model ----------
    public static class Student {
        private String name;
        private int rollNo;        // unique
        private String className;  // e.g., "10-A"
        private double marks;      // 0..100
        private String phone;      // optional
        private String email;      // optional

        public Student(String name, int rollNo, String className, double marks, String phone, String email) {
            this.name = name;
            this.rollNo = rollNo;
            this.className = className;
            this.marks = marks;
            this.phone = phone;
            this.email = email;
        }

        public String getName() { return name; }
        public int getRollNo() { return rollNo; }
        public String getClassName() { return className; }
        public double getMarks() { return marks; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }

        public void setName(String name) { this.name = name; }
        public void setRollNo(int rollNo) { this.rollNo = rollNo; }
        public void setClassName(String className) { this.className = className; }
        public void setMarks(double marks) { this.marks = marks; }
        public void setPhone(String phone) { this.phone = phone; }
        public void setEmail(String email) { this.email = email; }
    }

    // ---------- Service (in-memory store + validation + stats) ----------
    public static class StudentService {
        private final Map<Integer, Student> byRoll = new LinkedHashMap<>();

        public synchronized boolean exists(int rollNo) {
            return byRoll.containsKey(rollNo);
        }

        public synchronized void add(Student s) {
            if (exists(s.getRollNo())) {
                throw new IllegalArgumentException("Roll No already exists: " + s.getRollNo());
            }
            validate(s);
            byRoll.put(s.getRollNo(), s);
        }

        public synchronized void update(int originalRoll, Student updated) {
            if (!exists(originalRoll)) throw new IllegalArgumentException("Original roll not found.");
            if (updated.getRollNo() != originalRoll && exists(updated.getRollNo())) {
                throw new IllegalArgumentException("New Roll No already exists: " + updated.getRollNo());
            }
            validate(updated);
            // If roll changes, remove old key
            if (updated.getRollNo() != originalRoll) {
                byRoll.remove(originalRoll);
            }
            byRoll.put(updated.getRollNo(), updated);
        }

        public synchronized void delete(int rollNo) {
            byRoll.remove(rollNo);
        }

        public synchronized List<Student> list() {
            return new ArrayList<>(byRoll.values());
        }

        public synchronized void clear() {
            byRoll.clear();
        }

        private void validate(Student s) {
            if (s.getName() == null || s.getName().trim().isEmpty()) throw new IllegalArgumentException("Name required.");
            if (s.getRollNo() <= 0) throw new IllegalArgumentException("Roll No must be > 0.");
            if (s.getClassName() == null || s.getClassName().trim().isEmpty()) throw new IllegalArgumentException("Class required.");
            if (s.getMarks() < 0 || s.getMarks() > 100) throw new IllegalArgumentException("Marks must be between 0 and 100.");
            if (s.getEmail() != null && !s.getEmail().trim().isEmpty() && !s.getEmail().contains("@"))
                throw new IllegalArgumentException("Invalid email.");
            if (s.getPhone() != null && !s.getPhone().trim().isEmpty()) {
                String p = s.getPhone().replaceAll("\\D", "");
                if (p.length() < 7) throw new IllegalArgumentException("Invalid phone.");
            }
        }

        // ---- Stats ----
        public synchronized int totalCount() { return byRoll.size(); }
        public synchronized double averageMarks() {
            if (byRoll.isEmpty()) return 0;
            double sum = 0;
            for (Student s : byRoll.values()) sum += s.getMarks();
            return sum / byRoll.size();
        }
        public synchronized double highestMarks() {
            double max = 0;
            for (Student s : byRoll.values()) max = Math.max(max, s.getMarks());
            return max;
        }
        public synchronized double lowestMarks() {
            if (byRoll.isEmpty()) return 0;
            double min = 101;
            for (Student s : byRoll.values()) min = Math.min(min, s.getMarks());
            return (min == 101) ? 0 : min;
        }
        public synchronized double passRate(double passThreshold) {
            if (byRoll.isEmpty()) return 0;
            int pass = 0;
            for (Student s : byRoll.values()) if (s.getMarks() >= passThreshold) pass++;
            return (pass * 100.0) / byRoll.size();
        }
    }

    // ---------- Table Model ----------
    public static class StudentTableModel extends AbstractTableModel {
        private final String[] cols = {"Roll No", "Name", "Class", "Marks", "Phone", "Email"};
        private final Class<?>[] types = {Integer.class, String.class, String.class, Double.class, String.class, String.class};
        private final List<Student> data;

        public StudentTableModel(List<Student> backing) {
            this.data = backing;
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }
        @Override public Class<?> getColumnClass(int columnIndex) { return types[columnIndex]; }
        @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Student s = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return s.getRollNo();
                case 1: return s.getName();
                case 2: return s.getClassName();
                case 3: return s.getMarks();
                case 4: return s.getPhone();
                case 5: return s.getEmail();
                default: return null;
            }
        }

        public Student getAt(int row) { return data.get(row); }
        public void refresh() {
            fireTableDataChanged();
        }
    }

    // ---------- CSV Persistence ----------
    public static class CSVStorage {
        private final Path file;

        public CSVStorage(String filename) {
            this.file = Paths.get(filename);
        }

        public void save(List<Student> students) throws IOException {
            List<String> out = new ArrayList<>();
            out.add("rollNo,name,class,marks,phone,email"); // header
            for (Student s : students) {
                out.add(String.format("%d,%s,%s,%.2f,%s,%s",
                        s.getRollNo(),
                        escape(s.getName()),
                        escape(s.getClassName()),
                        s.getMarks(),
                        escape(nvl(s.getPhone())),
                        escape(nvl(s.getEmail()))));
            }
            Files.write(file, out, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        public List<Student> load() throws IOException {
            List<Student> list = new ArrayList<>();
            if (!Files.exists(file)) return list;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            boolean headerSkipped = false;
            for (String line : lines) {
                if (!headerSkipped) { headerSkipped = true; continue; }
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCSV(line);
                // Expected 6 fields
                int roll = Integer.parseInt(parts[0].trim());
                String name = unescape(parts[1]);
                String cls = unescape(parts[2]);
                double marks = Double.parseDouble(parts[3].trim());
                String phone = parts.length > 4 ? unescape(parts[4]) : "";
                String email = parts.length > 5 ? unescape(parts[5]) : "";
                list.add(new Student(name, roll, cls, marks, phone, email));
            }
            return list;
        }

        private static String nvl(String s) { return s == null ? "" : s; }

        private static String escape(String s) {
            if (s == null) return "";
            boolean quote = s.contains(",") || s.contains("\"") || s.contains("\n");
            String val = s.replace("\"", "\"\"");
            return quote ? "\"" + val + "\"" : val;
        }

        private static String unescape(String s) {
            s = s.trim();
            if (s.startsWith("\"") && s.endsWith("\"")) {
                s = s.substring(1, s.length() - 1).replace("\"\"", "\"");
            }
            return s;
        }

        private static String[] parseCSV(String line) {
            List<String> tokens = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            boolean inQuotes = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '\"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    tokens.add(sb.toString().trim());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
            tokens.add(sb.toString().trim());
            return tokens.toArray(new String[0]);
        }
    }

    // ---------- Dialog for Add/Edit ----------
    public static class StudentFormDialog extends JDialog {
        private final JTextField tfName = new JTextField();
        private final JTextField tfRoll = new JTextField();
        private final JTextField tfClass = new JTextField();
        private final JSpinner spMarks = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.5));
        private final JTextField tfPhone = new JTextField();
        private final JTextField tfEmail = new JTextField();
        private boolean confirmed = false;

        public StudentFormDialog(Window owner, String title, Student existing) {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout(10,10));
            JPanel form = new JPanel(new GridBagLayout());
            form.setBorder(new EmptyBorder(10, 10, 10, 10));
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(6,6,6,6);
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1;

            int r = 0;
            addRow(form, gc, r++, "Name *", tfName);
            addRow(form, gc, r++, "Roll No *", tfRoll);
            addRow(form, gc, r++, "Class *", tfClass);
            addRow(form, gc, r++, "Marks *", spMarks);
            addRow(form, gc, r++, "Phone", tfPhone);
            addRow(form, gc, r++, "Email", tfEmail);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnCancel = new JButton("Cancel");
            JButton btnSave = new JButton("Save");
            actions.add(btnCancel);
            actions.add(btnSave);

            add(form, BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);

            btnCancel.addActionListener(e -> dispose());
            btnSave.addActionListener(e -> {
                confirmed = true;
                dispose();
            });

            if (existing != null) {
                tfName.setText(existing.getName());
                tfRoll.setText(String.valueOf(existing.getRollNo()));
                tfClass.setText(existing.getClassName());
                spMarks.setValue(existing.getMarks());
                tfPhone.setText(existing.getPhone());
                tfEmail.setText(existing.getEmail());
            }

            pack();
            setMinimumSize(new Dimension(420, getHeight()));
            setLocationRelativeTo(owner);
        }

        private void addRow(JPanel p, GridBagConstraints gc, int row, String label, JComponent comp) {
            gc.gridx = 0; gc.gridy = row; gc.weightx = 0; p.add(new JLabel(label), gc);
            gc.gridx = 1; gc.gridy = row; gc.weightx = 1; p.add(comp, gc);
        }

        public boolean isConfirmed() { return confirmed; }

        public Student getStudentOrNull() {
            try {
                String name = tfName.getText().trim();
                int roll = Integer.parseInt(tfRoll.getText().trim());
                String cls = tfClass.getText().trim();
                double marks = ((Number) spMarks.getValue()).doubleValue();
                String phone = tfPhone.getText().trim();
                String email = tfEmail.getText().trim();
                return new Student(name, roll, cls, marks, phone, email);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please fill valid data. " + ex.getMessage(),
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
    }

    // ---------- Main Frame (UI) ----------
    public static class MainFrame extends JFrame {
        private final StudentService service = new StudentService();
        private final StudentTableModel model = new StudentTableModel(service.list());
        private final JTable table = new JTable(model);
        private final TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        private final JTextField tfSearch = new JTextField();
        private final JLabel lblTotal = new JLabel("0");
        private final JLabel lblAvg = new JLabel("0.00");
        private final JLabel lblHigh = new JLabel("0.00");
        private final JLabel lblLow = new JLabel("0.00");
        private final JLabel lblPass = new JLabel("0.00%");
        private final DecimalFormat df2 = new DecimalFormat("#0.00");
        private final CSVStorage storage = new CSVStorage("students.csv");

        public MainFrame() {
            super("? Student Management System — Java Swing (Realtime)");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout(12,12));
            getRootPane().setBorder(new EmptyBorder(10,10,10,10));

            // Top: Search + Buttons
            JPanel top = new JPanel(new BorderLayout(10,10));
            JPanel searchPanel = new JPanel(new BorderLayout(6,6));
            searchPanel.add(new JLabel("? Search (Name/Roll/Class/Email): "), BorderLayout.WEST);
            searchPanel.add(tfSearch, BorderLayout.CENTER);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            JButton btnAdd = new JButton("? Add");
            JButton btnEdit = new JButton("? Edit");
            JButton btnDelete = new JButton("? Delete");
            JButton btnClear = new JButton("? Clear All");
            JButton btnReload = new JButton("? Reload CSV");
            JButton btnSave = new JButton("? Save CSV");
            btns.add(btnAdd); btns.add(btnEdit); btns.add(btnDelete);
            btns.add(btnClear); btns.add(btnReload); btns.add(btnSave);

            top.add(searchPanel, BorderLayout.CENTER);
            top.add(btns, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            // Center: Table
            table.setRowSorter(sorter);
            table.setFillsViewportHeight(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setRowHeight(24);
            ((DefaultRowSorter<?, ?>) sorter).setSortsOnUpdates(true);
            JScrollPane sp = new JScrollPane(table);
            add(sp, BorderLayout.CENTER);

            // Bottom: Stats
            JPanel stats = new JPanel(new GridLayout(1, 5, 10, 10));
            stats.setBorder(new EmptyBorder(6,0,0,0));
            stats.add(card("Total", lblTotal));
            stats.add(card("Average", lblAvg));
            stats.add(card("Highest", lblHigh));
            stats.add(card("Lowest", lblLow));
            stats.add(card("Pass Rate (>=40)", lblPass));
            add(stats, BorderLayout.SOUTH);

            // Listeners
            tfSearch.getDocument().addDocumentListener(new DocumentListener() {
                private void updateFilter() {
                    String text = tfSearch.getText().trim().toLowerCase();
                    if (text.isEmpty()) {
                        sorter.setRowFilter(null);
                    } else {
                        sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                            @Override
                            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                                for (int i = 0; i < entry.getValueCount(); i++) {
                                    Object v = entry.getValue(i);
                                    if (v != null && v.toString().toLowerCase().contains(text)) return true;
                                }
                                return false;
                            }
                        });
                    }
                    updateStatsRealtime();
                }
                @Override public void insertUpdate(DocumentEvent e) { updateFilter(); }
                @Override public void removeUpdate(DocumentEvent e) { updateFilter(); }
                @Override public void changedUpdate(DocumentEvent e) { updateFilter(); }
            });

            btnAdd.addActionListener(e -> onAdd());
            btnEdit.addActionListener(e -> onEdit());
            btnDelete.addActionListener(e -> onDelete());
            btnClear.addActionListener(e -> onClearAll());
            btnReload.addActionListener(e -> onReload());
            btnSave.addActionListener(e -> onSave());

            // Load CSV on start
            onReloadIfExists();

            // Sample data if empty
            if (service.totalCount() == 0) {
                seedSample();
            }

            updateStatsRealtime();
            setMinimumSize(new Dimension(1000, 600));
            setLocationRelativeTo(null);

            // Save on window close
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    try { storage.save(service.list()); } catch (Exception ignored) {}
                }
            });
        }

        private JPanel card(String title, JLabel value) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220,220,220)),
                    new EmptyBorder(10,10,10,10)
            ));
            JLabel t = new JLabel(title);
            t.setFont(t.getFont().deriveFont(Font.BOLD, 13f));
            value.setFont(value.getFont().deriveFont(Font.BOLD, 18f));
            value.setHorizontalAlignment(SwingConstants.RIGHT);
            p.add(t, BorderLayout.WEST);
            p.add(value, BorderLayout.EAST);
            return p;
        }

        private void onAdd() {
            StudentFormDialog dlg = new StudentFormDialog(this, "Add Student", null);
            dlg.setVisible(true);
            if (dlg.isConfirmed()) {
                Student s = dlg.getStudentOrNull();
                if (s != null) {
                    try {
                        service.add(s);
                        model.refresh();
                        updateStatsRealtime();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage(), "Add Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // If invalid, reopen for correction
                    onAdd();
                }
            }
        }

        private void onEdit() {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Select a student to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            Student current = model.getAt(modelRow);
            int originalRoll = current.getRollNo();

            StudentFormDialog dlg = new StudentFormDialog(this, "Edit Student", current);
            dlg.setVisible(true);
            if (dlg.isConfirmed()) {
                Student s = dlg.getStudentOrNull();
                if (s != null) {
                    try {
                        service.update(originalRoll, s);
                        model.refresh();
                        updateStatsRealtime();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    onEdit();
                }
            }
        }

        private void onDelete() {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Select a student to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            Student s = model.getAt(modelRow);
            int c = JOptionPane.showConfirmDialog(this,
                    "Delete student: " + s.getName() + " (Roll " + s.getRollNo() + ")?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                service.delete(s.getRollNo());
                model.refresh();
                updateStatsRealtime();
            }
        }

        private void onClearAll() {
            if (service.totalCount() == 0) return;
            int c = JOptionPane.showConfirmDialog(this,
                    "This will remove ALL students. Continue?",
                    "Confirm Clear", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                service.clear();
                model.refresh();
                updateStatsRealtime();
            }
        }

        private void onReload() {
            try {
                List<Student> loaded = storage.load();
                service.clear();
                for (Student s : loaded) service.add(s);
                model.refresh();
                updateStatsRealtime();
                JOptionPane.showMessageDialog(this, "CSV reloaded successfully.", "Reload", JOptionPane.INFORMATION_MESSAGE);
            } catch (FileNotFoundException fnf) {
                JOptionPane.showMessageDialog(this, "No CSV found to reload.", "Reload", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Reload failed: " + ex.getMessage(), "Reload Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void onReloadIfExists() {
            try {
                if (Files.exists(Paths.get("students.csv"))) {
                    List<Student> loaded = storage.load();
                    service.clear();
                    for (Student s : loaded) service.add(s);
                    model.refresh();
                }
            } catch (Exception ignored) { }
        }

        private void onSave() {
            try {
                storage.save(service.list());
                JOptionPane.showMessageDialog(this, "Saved to students.csv", "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void seedSample() {
            try {
                service.add(new Student("Gayatri", 101, "10-A", 88.5, "9876543210", "gayatri@example.com"));
                service.add(new Student("Rahul", 102, "10-A", 72.0, "9876500000", "rahul@example.com"));
                service.add(new Student("Ananya", 103, "10-B", 95.0, "9876511111", "ananya@example.com"));
                service.add(new Student("Ishaan", 104, "10-B", 39.5, "9876522222", "ishaan@example.com"));
                service.add(new Student("Meera", 105, "10-C", 64.0, "9876533333", "meera@example.com"));
            } catch (Exception ignored) {}
            model.refresh();
        }

        private void updateStatsRealtime() {
            // Stats consider the CURRENT filtered view for a "realtime projection"
            // and also update with overall if desired. Here we show filtered-view stats.
            int rows = table.getRowCount();
            lblTotal.setText(String.valueOf(rows));

            double sum = 0, hi = 0, lo = 101;
            int pass = 0;
            for (int i = 0; i < rows; i++) {
                int modelRow = table.convertRowIndexToModel(i);
                double m = (double) model.getValueAt(modelRow, 3);
                sum += m;
                hi = Math.max(hi, m);
                lo = Math.min(lo, m);
                if (m >= 40.0) pass++;
            }
            double avg = rows == 0 ? 0 : sum / rows;
            if (lo == 101) lo = 0;

            lblAvg.setText(df2.format(avg));
            lblHigh.setText(df2.format(hi));
            lblLow.setText(df2.format(lo));
            lblPass.setText(df2.format(rows == 0 ? 0 : (pass * 100.0 / rows)) + "%");
        }
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}
