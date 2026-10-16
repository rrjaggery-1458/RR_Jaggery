package com.rrjaggery.contact;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.mail.host=smtp.test.example",
        "spring.mail.properties.mail.smtp.auth=false",
        "app.contact.from=website@rrjaggery.com",
        "app.contact.recipient=rrjaggery@gmail.com",
        "management.health.mail.enabled=false"
})
@AutoConfigureMockMvc
class ContactInquiryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void acceptsPublicInquiryAndSetsSubmitterAsReplyTo() throws Exception {
        when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rohan Kumar",
                                  "email": "rohan@example.com",
                                  "message": "I would like to know about bulk jaggery pricing."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contact inquiry sent successfully."));

        var message = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(message.capture());
        org.junit.jupiter.api.Assertions.assertEquals("rrjaggery@gmail.com",
                message.getValue().getRecipients(jakarta.mail.Message.RecipientType.TO)[0].toString());
        org.junit.jupiter.api.Assertions.assertEquals("rohan@example.com",
                message.getValue().getReplyTo()[0].toString());
    }

    @Test
    void rejectsInvalidContactFieldsWithoutSendingMail() throws Exception {
        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "email": "not-an-email",
                                  "phone": "abc",
                                  "message": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(mailSender);
    }

    @Test
    void reportsSmtpFailureInsteadOfReturningSuccess() throws Exception {
        when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rohan Kumar",
                                  "email": "rohan@example.com",
                                  "phone": "+91 98765-43210",
                                  "message": "I would like to know about bulk jaggery pricing."
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value(
                        "Contact email delivery failed because the configured SMTP service could not accept the message. Please try again later."));
    }
}
