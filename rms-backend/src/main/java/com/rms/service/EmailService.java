package com.rms.service;

import com.rms.entity.Invoice;
import com.rms.entity.Lease;
import com.rms.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.base.url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendInvoiceGeneratedEmail(User tenant, Invoice invoice, Lease lease) {
        try {
            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
            context.setVariable("amount", invoice.getTotalAmount());
            context.setVariable("dueDate", invoice.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("rentAmount", invoice.getRentAmount());
            context.setVariable("penaltyAmount", invoice.getPenaltyAmount());
            context.setVariable("unitNumber", lease.getSpace().getUnitNumber());
            context.setVariable("spaceCategory", lease.getSpace().getCategory());
            context.setVariable("monthlyRent", lease.getMonthlyRent());
            context.setVariable("invoiceUrl", baseUrl + "/payments");
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/invoice-generated", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "📄 New Invoice: " + invoice.getInvoiceNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send invoice email: " + e.getMessage());
        }
    }

    public void sendPaymentApprovedEmail(User tenant, Invoice invoice) {
        try {
            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
            context.setVariable("amount", invoice.getTotalAmount());
            context.setVariable("paymentDate", invoice.getPaymentDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("paymentMethod", invoice.getPaymentMethod());
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/payment-approved", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "✅ Payment Approved: " + invoice.getInvoiceNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send payment approved email: " + e.getMessage());
        }
    }

    public void sendPaymentConfirmationEmail(User tenant, Invoice invoice) {
        try {
            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
            context.setVariable("amount", invoice.getTotalAmount());
            context.setVariable("paymentDate", invoice.getPaymentDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("paymentMethod", invoice.getPaymentMethod());
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/payment-confirmed", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "📄 Payment Submitted: " + invoice.getInvoiceNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send payment confirmation email: " + e.getMessage());
        }
    }

    public void sendOverdueEmail(User tenant, Invoice invoice) {
        try {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                    invoice.getDueDate(),
                    java.time.LocalDate.now()
            );

            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
            context.setVariable("amount", invoice.getTotalAmount());
            context.setVariable("dueDate", invoice.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("daysOverdue", daysOverdue);
            context.setVariable("penaltyAmount", invoice.getPenaltyAmount());
            context.setVariable("invoiceUrl", baseUrl + "/payments");
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/overdue", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "⚠️ URGENT: Invoice " + invoice.getInvoiceNumber() + " is OVERDUE",
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send overdue email: " + e.getMessage());
        }
    }

    public void sendTestEmail() {
        try {
            String testEmail = "eyobyaregal1111@gmail.com";

            Context context = new Context();
            context.setVariable("message", "This is a test email from RMS!");
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/test", context);
            sendHtmlEmail(
                    testEmail,
                    "🧪 Test Email from RMS",
                    htmlContent
            );
            System.out.println("✅ Test email sent to: " + testEmail);
        } catch (Exception e) {
            System.err.println("❌ Test email failed: " + e.getMessage());
            throw e;
        }
    }

    public void sendPaymentReminderEmail(User tenant, Invoice invoice) {
        try {
            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
            context.setVariable("amount", invoice.getTotalAmount());
            context.setVariable("dueDate", invoice.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("daysLeft", 3);
            context.setVariable("invoiceUrl", baseUrl + "/payments");
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/payment-reminder", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "🔔 Payment Reminder: " + invoice.getInvoiceNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send reminder email: " + e.getMessage());
        }
    }

    public void sendWelcomeEmail(User tenant, String username, String password) {
        try {
            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("username", username);
            context.setVariable("password", password);
            context.setVariable("loginUrl", baseUrl + "/login");
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/welcome", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "🎉 Welcome to RMS - Your Rental Management Portal",
                    htmlContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (to == null || to.isEmpty()) {
            System.err.println("No email address provided");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("✅ Email sent to: " + to + " - " + subject);
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}