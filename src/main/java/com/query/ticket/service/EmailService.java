package com.query.ticket.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ── Password Reset ────────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String to, String name, String resetLink) {
        send(to, "Reset your QueryDesk password",
                buildHtml("Password Reset", "Reset your password",
                        "Hi <strong>" + name + "</strong>, we received a request to reset your password.",
                        "This link expires in 15 minutes.",
                        "Reset Password", resetLink,
                        "If you didn't request this, you can safely ignore this email.", "#c8f04a"));
    }

    // ── Ticket Notifications ──────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendTicketCreatedEmail(String to, String name, String ticketId, String title) {
        send(to, "Your ticket has been created — QueryDesk",
                buildHtml("Ticket Created", "Ticket submitted successfully",
                        "Hi <strong>" + name + "</strong>, your support ticket has been received.",
                        title, "View Ticket", frontendUrl + "/tickets/" + ticketId,
                        "You'll receive updates as the status changes.", "#c8f04a"));
    }

    @Async("emailTaskExecutor")
    public void sendTicketAssignedEmail(String to, String agentName, String ticketId,
                                        String title, String assignedBy) {
        send(to, "New ticket assigned to you — QueryDesk",
                buildHtml("Ticket Assigned", "You have a new assignment",
                        "Hi <strong>" + agentName + "</strong>, a ticket has been assigned to you by <strong>"
                                + assignedBy + "</strong>.",
                        title, "View Ticket", frontendUrl + "/tickets/" + ticketId,
                        "Please update the ticket status as you progress.", "#c8f04a"));
    }

    @Async("emailTaskExecutor")
    public void sendTicketStatusChangedEmail(String to, String name, String ticketId,
                                             String title, String newStatus) {
        String color = switch (newStatus) {
            case "RESOLVED" -> "#4ade80";
            case "ESCALATED" -> "#f87171";
            case "CLOSED" -> "#6b7280";
            default -> "#c8f04a";
        };
        send(to, "Ticket status updated — QueryDesk",
                buildHtml("Status Updated",
                        "Your ticket status changed to " + newStatus.replace("_", " "),
                        "Hi <strong>" + name + "</strong>, the status of your ticket has been updated.",
                        "Status: <strong style=\"color:" + color + "\">"
                                + newStatus.replace("_", " ") + "</strong>",
                        "View Ticket", frontendUrl + "/tickets/" + ticketId, "", color));
    }

    @Async("emailTaskExecutor")
    public void sendTicketEscalatedEmail(String to, String name, String ticketId, String title) {
        send(to, "Your ticket has been escalated — QueryDesk",
                buildHtml("Ticket Escalated", "Your ticket has been escalated",
                        "Hi <strong>" + name
                                + "</strong>, your ticket has been escalated to a higher priority.",
                        title, "View Ticket", frontendUrl + "/tickets/" + ticketId,
                        "Our senior team is now reviewing your case.", "#f87171"));
    }

    // ── SLA Notification ──────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendSlaBreachedEmail(String to, String name, String ticketId,
                                     String title, String priority) {
        send(to, "⚠️ SLA Breached — Immediate Action Required",
                buildHtml("SLA Breached", "A ticket has breached its SLA deadline",
                        "Hi <strong>" + name + "</strong>, the following ticket has exceeded its SLA "
                                + "deadline and requires <strong>immediate attention</strong>.",
                        "Ticket: <strong>" + title + "</strong><br/>"
                                + "Priority: <strong style=\"color:#f87171\">" + priority + "</strong>",
                        "View Ticket", frontendUrl + "/tickets/" + ticketId,
                        "This ticket has been automatically escalated by the system.", "#f87171"));
    }

    // ── User Notifications ────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendUserCreatedEmail(String to, String name, String tempPassword) {
        send(to, "Welcome to QueryDesk — Your account is ready",
                buildHtml("Welcome", "Your account has been created",
                        "Hi <strong>" + name
                                + "</strong>, an administrator has created an account for you.",
                        "Temporary password: <strong>" + tempPassword + "</strong>",
                        "Login Now", frontendUrl + "/login",
                        "Please change your password after your first login.", "#c8f04a"));
    }

    @Async("emailTaskExecutor")
    public void sendRoleChangedEmail(String to, String name, String newRole) {
        send(to, "Your role has been updated — QueryDesk",
                buildHtml("Role Updated", "Your account role has changed",
                        "Hi <strong>" + name + "</strong>, your role on QueryDesk has been updated.",
                        "New role: <strong>" + newRole + "</strong>",
                        "Go to Dashboard", frontendUrl + "/dashboard",
                        "Your access level has changed. Please re-login if you notice any issues.",
                        "#c8f04a"));
    }

    @Async("emailTaskExecutor")
    public void sendAccountStatusEmail(String to, String name, boolean enabled) {
        String action = enabled ? "enabled" : "disabled";
        String color = enabled ? "#4ade80" : "#f87171";
        send(to, "Your account has been " + action + " — QueryDesk",
                buildHtml("Account " + (enabled ? "Enabled" : "Disabled"),
                        "Your account status changed",
                        "Hi <strong>" + name + "</strong>, your QueryDesk account has been <strong>"
                                + action + "</strong>.",
                        enabled ? "You can now login and access the platform."
                                : "Contact your administrator if you think this is a mistake.",
                        enabled ? "Login Now" : "Contact Support",
                        enabled ? frontendUrl + "/login" : "mailto:support@querydesk.com",
                        "", color));
    }

    // ── Core send ─────────────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent → {} | {}", to, subject);
        } catch (MessagingException e) {
            log.error("Email failed → {} | {}: {}", to, subject, e.getMessage());
        }
    }

    // ── HTML builder ──────────────────────────────────────────────────────────

    private String buildHtml(String badge, String heading, String body,
                              String highlight, String btnText, String btnUrl,
                              String footer, String accent) {
        return """
                <!DOCTYPE html><html>
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#0a0a0f;font-family:'Helvetica Neue',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0a0a0f;padding:40px 20px;">
                    <tr><td align="center">
                      <table width="520" cellpadding="0" cellspacing="0"
                             style="background:#12121f;border-radius:20px;border:1px solid #1e1e2e;overflow:hidden;">
                        <tr><td style="padding:28px 36px 20px;border-bottom:1px solid #1e1e2e;">
                          <table cellpadding="0" cellspacing="0"><tr>
                            <td style="background:%s;width:34px;height:34px;border-radius:8px;
                                       text-align:center;vertical-align:middle;">
                              <span style="font-size:17px;font-weight:900;color:#0a0a0f;">Q</span>
                            </td>
                            <td style="padding-left:10px;">
                              <span style="font-size:15px;font-weight:700;color:#fff;">QueryDesk</span>
                            </td>
                          </tr></table>
                        </td></tr>
                        <tr><td style="padding:28px 36px;">
                          <p style="color:#8888aa;font-size:11px;text-transform:uppercase;
                                    letter-spacing:1px;margin:0 0 6px;">%s</p>
                          <h1 style="color:#fff;font-size:20px;font-weight:700;
                                     margin:0 0 14px;letter-spacing:-0.5px;">%s</h1>
                          <p style="color:#8888aa;font-size:14px;line-height:1.7;margin:0 0 20px;">%s</p>
                          <div style="background:#0a0a0f;border-radius:10px;padding:12px 16px;
                                      margin:0 0 20px;border-left:3px solid %s;">
                            <p style="color:#fff;font-size:14px;margin:0;">%s</p>
                          </div>
                          <table cellpadding="0" cellspacing="0" style="margin:0 0 20px;">
                            <tr><td style="background:%s;border-radius:10px;">
                              <a href="%s" style="display:inline-block;padding:11px 26px;
                                                  color:#0a0a0f;font-size:14px;font-weight:700;
                                                  text-decoration:none;">%s →</a>
                            </td></tr>
                          </table>
                          %s
                        </td></tr>
                        <tr><td style="padding:16px 36px;border-top:1px solid #1e1e2e;">
                          <p style="color:#3a3a5a;font-size:11px;margin:0;text-align:center;">
                            © 2026 QueryDesk · Automated notification
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body></html>
                """.formatted(accent, badge, heading, body, accent, highlight,
                accent, btnUrl, btnText,
                footer.isBlank() ? "" : "<p style=\"color:#5a5a7a;font-size:12px;\">"
                        + footer + "</p>");
    }

    @Async("emailTaskExecutor")
    public void sendOtpEmail(String to, String name, String otp) {
        send(to, "Verify your QueryDesk account — OTP inside",
                buildHtml(
                        "Email Verification",
                        "Verify your email address",
                        "Hi <strong>" + name + "</strong>, thank you for registering on QueryDesk. "
                                + "Use the OTP below to verify your email address.",
                        "<span style=\"font-size:28px;font-weight:900;letter-spacing:8px;color:#c8f04a;\">"
                                + otp + "</span>",
                        "Go to Verification",
                        (frontendUrl + "/verify-otp"),
                        "This OTP expires in <strong>10 minutes</strong>. "
                                + "If you didn't register, please ignore this email.",
                        "#c8f04a"
                ));
    }
}