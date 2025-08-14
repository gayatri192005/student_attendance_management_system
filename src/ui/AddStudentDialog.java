package ui;

import model.Student;
import service.StudentService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AddStudentDialog extends JDialog {
    private JTextField rollField, nameField, marksField;

    public AddStudentDialog(JFrame parent, StudentService studentService) {
        super(parent, "➕ Add Student", true);
        setSize(300, 200);
        setLayout(new GridLayout(4, 2, 10, 10));
        setLocationRelativeTo(parent);

        add(new JLabel("Roll Number:"));
        rollField = new JTextField();
        add(rollField);

        add(new JLabel("Name:"));
        nameField = new JTextField();
        add(nameField);

        add(new JLabel("Marks:"));
        marksField = new JTextField();
        add(marksField);

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener((ActionEvent e) -> {
            try {
                int roll = Integer.parseInt(rollField.getText());
                String name = nameField.getText();
                double marks = Double.parseDouble(marksField.getText());
                studentService.addStudent(new Student(roll, name, marks));
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid Input!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(addBtn);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        add(cancelBtn);

        setVisible(true);
    }
}
