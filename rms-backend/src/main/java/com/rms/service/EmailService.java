package com.rms.service;

import com.rms.entity.Invoice;
import com.rms.entity.Lease;
import com.rms.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final TimeTravelService timeTravelService;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.base.url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine,
                        TimeTravelService timeTravelService) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.timeTravelService = timeTravelService;
    }

    public void sendInvoiceGeneratedEmail(User tenant, Invoice invoice, Lease lease) {
        if (tenant == null || tenant.getEmail() == null || tenant.getEmail().isEmpty()) {
            log.warn("Cannot send invoice email: tenant or email is null");
            return;
        }
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
            context.setVariable("year", timeTravelService.getCurrentDate().getYear());

            String htmlContent = templateEngine.process("email/invoice-generated", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "📄 New Invoice: " + invoice.getInvoiceNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            log.error("Failed to send invoice email", e);
        }
    }

    public void sendRentDueEmail(User tenant, Lease lease) {
        try {
            int dueDay = lease.getDueDay() != null ? lease.getDueDay() : 1;
            LocalDate today = timeTravelService.getCurrentDate();
            LocalDate dueDate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));

            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("spaceUnit", lease.getSpace().getUnitNumber());
            context.setVariable("amount", lease.getMonthlyRent());
            context.setVariable("dueDate", dueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("paymentUrl", baseUrl + "/payments");
            context.setVariable("year", timeTravelService.getCurrentDate().getYear());

            String htmlContent = templateEngine.process("email/rent-due", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "🚨 Rent Due Today: " + lease.getSpace().getUnitNumber(),
                    htmlContent
            );
            log.info("Rent due email sent to: {}", tenant.getEmail());
        } catch (Exception e) {
            log.error("Failed to send rent due email", e);
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
            context.setVariable("year", timeTravelService.getCurrentDate().getYear());

            String htmlContent = templateEngine.process("email/payment-approved", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "✅ Payment Approved: " + invoice.getInvoiceNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            log.error("Failed to send payment approved email", e);
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
            context.setVariable("year", timeTravelService.getCurrentDate().getYear());

            String htmlContent = templateEngine.process("email/payment-confirmed", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "📄 Payment Submitted: " + invoice.getInvoiceNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            log.error("Failed to send payment confirmation email", e);
        }
    }

    public void sendOverdueEmail(User tenant, Invoice invoice) {
        try {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                    invoice.getDueDate(),
                    timeTravelService.getCurrentDate()
            );

            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
            context.setVariable("amount", invoice.getTotalAmount());
            context.setVariable("dueDate", invoice.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("daysOverdue", daysOverdue);
            context.setVariable("penaltyAmount", invoice.getPenaltyAmount());
            context.setVariable("invoiceUrl", baseUrl + "/payments");
            context.setVariable("year", timeTravelService.getCurrentDate().getYear());

            String htmlContent = templateEngine.process("email/overdue", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "⚠️ URGENT: Invoice " + invoice.getInvoiceNumber() + " is OVERDUE",
                    htmlContent
            );
        } catch (Exception e) {
            log.error("Failed to send overdue email", e);
        }
    }

    public void sendTestEmail() {
        try {
            String testEmail = "eyobyaregal1111@gmail.com";

            Context context = new Context();
            context.setVariable("message", "This is a test email from RMS!");
            context.setVariable("year", timeTravelService.getCurrentDate().getYear());

            String htmlContent = templateEngine.process("email/test", context);
            sendHtmlEmail(
                    testEmail,
                    "🧪 Test Email from RMS",
                    htmlContent
            );
            log.info("Test email sent to: {}", testEmail);
        } catch (Exception e) {
            log.error("Test email failed", e);
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
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                    timeTravelService.getCurrentDate(), invoice.getDueDate());
            context.setVariable("daysLeft", Math.max(daysLeft, 0));
            context.setVariable("invoiceUrl", baseUrl + "/payments");
            context.setVariable("year", timeTravelService.getCurrentDate().getYear());

            String htmlContent = templateEngine.process("email/payment-reminder", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "🔔 Payment Reminder: " + invoice.getInvoiceNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            log.error("Failed to send reminder email", e);
        }
    }

    public void sendWelcomeEmail(User tenant, String username, String password) {
        try {
            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("username", username);
            context.setVariable("password", password);
            context.setVariable("loginUrl", baseUrl + "/login");
            context.setVariable("year", timeTravelService.getCurrentDate().getYear());

            String htmlContent = templateEngine.process("email/welcome", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "🎉 Welcome to RMS - Your Rental Management Portal",
                    htmlContent
            );
        } catch (Exception e) {
            log.error("Failed to send welcome email", e);
        }
    }

    public void sendPaymentConfirmedEmail(User tenant, Invoice invoice) {
        try {
            Context context = new Context();
            context.setVariable("tenantName", tenant.getName());
            context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
            context.setVariable("amount", invoice.getTotalAmount());
            context.setVariable("paymentDate", invoice.getPaymentDate() != null ?
                    invoice.getPaymentDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) :
                    timeTravelService.getCurrentDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("paymentMethod", invoice.getPaymentMethod() != null ?
                    invoice.getPaymentMethod() : "Bank Transfer");
            context.setVariable("invoiceUrl", baseUrl + "/payments");
            context.setVariable("year", timeTravelService.getCurrentDate().getYear());

            String htmlContent = templateEngine.process("email/payment-confirmed", context);
            sendHtmlEmail(
                    tenant.getEmail(),
                    "✅ Payment Confirmed: " + invoice.getInvoiceNumber(),
                    htmlContent
            );
        } catch (Exception e) {
            log.error("Failed to send payment confirmed email", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (to == null || to.isEmpty()) {
            log.warn("No email address provided");
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
            log.info("Email sent to: {} - {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
        }
    }
}