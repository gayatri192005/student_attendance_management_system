import service.StudentService;
import ui.MainUI;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        // ✅ Swing ko Event Dispatch Thread me chalana best practice hai
        SwingUtilities.invokeLater(() -> {
            StudentService studentService = new StudentService();
            MainUI ui = new MainUI(studentService);
            ui.setVisible(true);
        });
    }
}
