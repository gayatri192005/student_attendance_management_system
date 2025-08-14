package ui;

import model.Student;
import service.StudentService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainUI extends JFrame {
    private StudentService studentService;
    private JTable studentTable;
    private DefaultTableModel tableModel;

    public MainUI(StudentService studentService) {
        this.studentService = studentService;

        setTitle("📚 Student Management System");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Table
        tableModel = new DefaultTableModel(new Object[]{"Roll No", "Name", "Marks"}, 0);
        studentTable = new JTable(tableModel);
        refreshTable();

        // Buttons
        JButton addBtn = new JButton("➕ Add");
        JButton updateBtn = new JButton("✏ Update");
        JButton deleteBtn = new JButton("❌ Delete");
        JButton searchBtn = new JButton("🔍 Search");

        addBtn.addActionListener(this::handleAdd);
        updateBtn.addActionListener(this::handleUpdate);
        deleteBtn.addActionListener(this::handleDelete);
        searchBtn.addActionListener(this::handleSearch);

        JPanel btnPanel = new JPanel();
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(searchBtn);

        add(new JScrollPane(studentTable), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Student student : studentService.getAllStudents()) {
            tableModel.addRow(new Object[]{
                    student.getRollNumber(),
                    student.getName(),
                    student.getMarks()
            });
        }
    }

    private void handleAdd(ActionEvent e) 
    {
        new AddStudentDialog(this, studentService); // Custom form popup
        refreshTable(); // Table update after dialog close
    }


    private void handleUpdate(ActionEvent e)
    {
        try
        {
            int roll = Integer.parseInt(JOptionPane.showInputDialog("Enter Roll Number to Update:"));
            new UpdateStudentDialog(this, studentService, roll); // Custom dialog
            refreshTable();
        } 
        catch (Exception ex) 
        {
            JOptionPane.showMessageDialog(this, "Invalid Input!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void handleDelete(ActionEvent e) {
        try {
            int roll = Integer.parseInt(JOptionPane.showInputDialog("Enter Roll Number to Delete:"));
            if (studentService.deleteStudent(roll)) {
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Student Not Found!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid Input!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSearch(ActionEvent e) {
        try {
            int roll = Integer.parseInt(JOptionPane.showInputDialog("Enter Roll Number to Search:"));
            Student student = studentService.searchStudent(roll);
            if (student != null) {
                JOptionPane.showMessageDialog(this, student.toString(), "Student Found", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Student Not Found!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid Input!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
