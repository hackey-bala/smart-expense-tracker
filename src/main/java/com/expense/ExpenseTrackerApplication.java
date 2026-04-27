package com.expense;

import com.expense.service.*;
import com.expense.ui.MainFrame;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.swing.*;

@SpringBootApplication
@EnableScheduling
public class ExpenseTrackerApplication {

    public static void main(String[] args) {
        // MUST set headless=false BEFORE anything AWT/Swing touches the toolkit
        System.setProperty("java.awt.headless", "false");

        // Start Spring (web layer, DB, services — no Swing here)
        ConfigurableApplicationContext context =
            SpringApplication.run(ExpenseTrackerApplication.class, args);

        // Pull services from context — they are safe plain Spring beans
        TransactionService    txService     = context.getBean(TransactionService.class);
        ReportService         reportService = context.getBean(ReportService.class);
        FraudDetectionService fraudService  = context.getBean(FraudDetectionService.class);
        PdfExportService      pdfService    = context.getBean(PdfExportService.class);

        // Construct and show the Swing UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(txService, reportService, fraudService, pdfService);
            frame.setVisible(true);
        });
    }
}
