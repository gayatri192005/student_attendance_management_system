package ui;

import model.Student;
import service.StudentService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class UpdateStudentDialog extends JDialog {
    private JTextField nameField, marksField;

    public UpdateStudentDialog(JFrame parent, StudentService studentService, int rollNumber) {
        super(parent, "✏ Update Student", true);
        setSize(300, 200);
        setLayout(new GridLayout(3, 2, 10, 10));
        setLocationRelativeTo(parent);

        Student student = studentService.searchStudent(rollNumber);
        if (student == null) {
            JOptionPane.showMessageDialog(this, "Student Not Found!", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        add(new JLabel("Name:"));
        nameField = new JTextField(student.getName());
        add(nameField);

        add(new JLabel("Marks:"));
        marksField = new JTextField(String.valueOf(student.getMarks()));
        add(marksField);

        JButton updateBtn = new JButton("Update");
        updateBtn.addActionListener((ActionEvent e) -> {
            try {
                String newName = nameField.getText();
                double newMarks = Double.parseDouble(marksField.getText());
                if (studentService.updateStudent(rollNumber, newName, newMarks)) {
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Update Failed!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid Input!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(updateBtn);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        add(cancelBtn);

        setVisible(true);
    }
}
