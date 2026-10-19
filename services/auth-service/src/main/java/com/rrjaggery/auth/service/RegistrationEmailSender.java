package com.rrjaggery.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationEmailSender {

  private static final Logger log = LoggerFactory.getLogger(RegistrationEmailSender.class);

  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final String from;

  public RegistrationEmailSender(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      @Value("${app.mail.from:${spring.mail.username:}}") String from) {
    this.mailSenderProvider = mailSenderProvider;
    this.from = from;
  }

  public void sendRegistrationCode(String email, String code, int expiryMinutes) {
    String subject = "Verify your RR Jaggery account: " + code;
    String greeting = "Welcome to RR Jaggery,";
    String heading = "Verify Your Email Address";
    String intro = "Thank you for creating an account with <strong>RR Jaggery</strong>. Use the 6-digit verification code below to verify your email and activate your account:";
    String note = "If you did not attempt to create an account, you can safely disregard this email.";

    sendHtmlEmail(email, subject, heading, greeting, intro, code, expiryMinutes, note);
  }

  public void sendPasswordResetOtp(String email, String userName, String code, int expiryMinutes) {
    String subject = "RR Jaggery — Password Reset OTP: " + code;
    String greeting = StringUtils.hasText(userName) ? "Hello " + userName + "," : "Hello,";
    String heading = "Password Reset Request";
    String intro = "We received a request to reset your password for your <strong>RR Jaggery</strong> account. Use the 6-digit One-Time Password (OTP) below to complete your password reset:";
    String note = "If you did not request a password reset, you can safely ignore this email. Your account credentials will remain unchanged.";

    sendHtmlEmail(email, subject, heading, greeting, intro, code, expiryMinutes, note);
  }

  private void sendHtmlEmail(
      String toEmail,
      String subject,
      String heading,
      String greeting,
      String intro,
      String code,
      int expiryMinutes,
      String note) {

    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null || !StringUtils.hasText(from)) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email delivery is not configured");
    }

    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(toEmail);
      helper.setSubject(subject);

      ClassPathResource logoResource = new ClassPathResource("static/rr-logo.png");
      boolean hasLogo = logoResource.exists();

      String html = buildEmailHtml(heading, greeting, intro, code, expiryMinutes, note, hasLogo);
      helper.setText(html, true);

      if (hasLogo) {
        try {
          helper.addInline("rrLogo", logoResource, "image/png");
        } catch (Exception ex) {
          log.warn("Could not attach inline logo to email: {}", ex.getMessage());
        }
      }

      mailSender.send(mimeMessage);
      log.info("Branded email successfully sent to {}", toEmail);
    } catch (MessagingException | RuntimeException ex) {
      log.error("Failed to send email to {}: {}", toEmail, ex.getMessage());
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Unable to deliver email", ex);
    }
  }

  private String buildEmailHtml(
      String heading,
      String greeting,
      String intro,
      String code,
      int expiryMinutes,
      String note,
      boolean hasLogo) {

    String logoHtml = hasLogo
        ? """
            <div style="text-align: center; margin-bottom: 12px;">
              <img src="cid:rrLogo" width="96" height="96" alt="RR Jaggery Logo" style="display: inline-block; width: 96px; height: 96px; border-radius: 50%; border: 3px solid #FEF3C7; box-shadow: 0 4px 14px rgba(0,0,0,0.25); object-fit: cover;" />
            </div>
            """
        : """
            <div style="display: inline-block; width: 68px; height: 68px; background: #ECFDF5; border: 2px solid #FEF3C7; border-radius: 50%; line-height: 68px; font-size: 32px; box-shadow: 0 2px 8px rgba(0,0,0,0.2); margin-bottom: 12px;">
              &#127855;
            </div>
            """;

    String template = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>RR Jaggery</title>
        </head>
        <body style="margin: 0; padding: 0; background-color: #f2f8f3; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; color: #1f2937;">
          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #f2f8f3; padding: 32px 12px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%" style="max-width: 580px; background-color: #ffffff; border-radius: 18px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(6, 78, 59, 0.12), 0 8px 10px -6px rgba(6, 78, 59, 0.08); border: 1px solid #d1fae5;">

                  <!-- Header: Emerald Gradient with Gold Accents -->
                  <tr>
                    <td style="background: linear-gradient(135deg, #064e3b 0%, #047857 60%, #059669 100%); padding: 32px 24px 26px 24px; text-align: center;">
                      {{LOGO_HTML}}
                      <h1 style="margin: 0; font-size: 26px; color: #ffffff; letter-spacing: 1.5px; font-weight: 800; text-transform: uppercase;">
                        RR JAGGERY
                      </h1>
                      <p style="margin: 6px 0 0 0; color: #a7f3d0; font-size: 13px; font-weight: 500; letter-spacing: 0.5px;">
                        100% Natural &bull; All Kinds of Organic Jaggery
                      </p>
                    </td>
                  </tr>
                  <!-- Golden Accent Trim -->
                  <tr>
                    <td style="height: 4px; background: linear-gradient(90deg, #F59E0B 0%, #D97706 50%, #F59E0B 100%); font-size: 0; line-height: 0;">&nbsp;</td>
                  </tr>

                  <!-- Body Content -->
                  <tr>
                    <td style="padding: 34px 30px 24px 30px;">
                      <h2 style="margin: 0 0 16px 0; font-size: 21px; color: #064e3b; font-weight: 700;">
                        {{HEADING}}
                      </h2>
                      <p style="margin: 0 0 14px 0; font-size: 15px; line-height: 1.6; color: #374151;">
                        {{GREETING}}
                      </p>
                      <p style="margin: 0 0 24px 0; font-size: 15px; line-height: 1.6; color: #374151;">
                        {{INTRO}}
                      </p>

                      <!-- Highlighted OTP Box -->
                      <div style="text-align: center; margin: 26px 0; padding: 22px 16px; background-color: #ecfdf5; border: 2px dashed #059669; border-radius: 14px;">
                        <span style="font-size: 38px; font-weight: 800; letter-spacing: 12px; color: #064e3b; display: inline-block; padding-left: 12px; font-family: 'Courier New', Courier, monospace;">
                          {{CODE}}
                        </span>
                        <div style="margin-top: 10px; font-size: 12.5px; color: #047857; font-weight: 600;">
                          &#9201; Valid for {{EXPIRY}} minutes
                        </div>
                      </div>

                      <!-- Security Notice -->
                      <div style="margin: 22px 0 16px 0; padding: 12px 14px; background-color: #fffbeb; border-left: 4px solid #f59e0b; border-radius: 6px;">
                        <p style="margin: 0; font-size: 13px; line-height: 1.5; color: #92400e;">
                          <strong>Security Reminder:</strong> Never share this verification code with anyone. RR Jaggery representatives will never ask for your OTP.
                        </p>
                      </div>

                      <p style="margin: 0; font-size: 13px; line-height: 1.5; color: #6b7280;">
                        {{NOTE}}
                      </p>
                    </td>
                  </tr>

                  <!-- Divider -->
                  <tr>
                    <td style="padding: 0 30px;">
                      <div style="border-top: 1px solid #e5e7eb;"></div>
                    </td>
                  </tr>

                  <!-- Footer: Real RR Jaggery Business Data from Website -->
                  <tr>
                    <td style="padding: 24px 28px 28px 28px; background-color: #f7fbf8; text-align: center;">
                      <p style="margin: 0 0 6px 0; font-size: 14px; font-weight: 700; color: #064e3b;">
                        RR Jaggery &bull; Jaggery Manufacturing &amp; Wholesale
                      </p>
                      <p style="margin: 0 0 4px 0; font-size: 12px; color: #4b5563; line-height: 1.5;">
                        &#128205; Vallurupallem, Thotlavalluru, Andhra Pradesh &ndash; 521163
                      </p>
                      <p style="margin: 0 0 4px 0; font-size: 12px; color: #4b5563; line-height: 1.5;">
                        &#128222; +91 9948533315 &nbsp;|&nbsp; &#9993;&#65039; <a href="mailto:rrjaggery@gmail.com" style="color: #047857; text-decoration: none; font-weight: 600;">rrjaggery@gmail.com</a>
                      </p>
                      <p style="margin: 0 0 10px 0; font-size: 12px; color: #6b7280; line-height: 1.5;">
                        &#128345; Support Hours: 10:00 AM &ndash; 6:00 PM (Mon&ndash;Sat) &nbsp;|&nbsp; Serving Vijayawada, Guntur &amp; beyond
                      </p>
                      <p style="margin: 0; font-size: 11px; color: #9ca3af; font-weight: 500;">
                        100% Naturally Processed &bull; Pure Sugarcane &bull; Zero Added Chemicals
                      </p>
                    </td>
                  </tr>

                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """;

    return template
        .replace("{{LOGO_HTML}}", logoHtml)
        .replace("{{HEADING}}", heading)
        .replace("{{GREETING}}", greeting)
        .replace("{{INTRO}}", intro)
        .replace("{{CODE}}", code)
        .replace("{{EXPIRY}}", String.valueOf(expiryMinutes))
        .replace("{{NOTE}}", note);
  }
}
