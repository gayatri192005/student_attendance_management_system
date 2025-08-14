package service;

import model.Student;
import java.util.ArrayList;
import java.util.List;

public class StudentService {
    private List<Student> students;

    public StudentService() {
        this.students = new ArrayList<>();
    }

    // ➕ Add Student
    public void addStudent(Student student) {
        students.add(student);
    }

    // 📜 Get All Students
    public List<Student> getAllStudents() {
        return students;
    }

    // ✏ Update Student by Roll Number
    public boolean updateStudent(int rollNumber, String newName, double newMarks) {
        for (Student student : students) {
            if (student.getRollNumber() == rollNumber) {
                student.setName(newName);
                student.setMarks(newMarks);
                return true;
            }
        }
        return false;
    }

    // ❌ Delete Student by Roll Number
    public boolean deleteStudent(int rollNumber) {
        return students.removeIf(student -> student.getRollNumber() == rollNumber);
    }

    // 🔍 Search Student by Roll Number
    public Student searchStudent(int rollNumber) {
        for (Student student : students) {
            if (student.getRollNumber() == rollNumber) {
                return student;
            }
        }
        return null;
    }
}
