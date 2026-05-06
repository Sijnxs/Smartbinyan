package com.example.smartbinyan;

import android.content.Context;
import android.widget.Toast;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    // ✅ Replace these with your Gmail and App Password
    private static final String SENDER_EMAIL = "carlluisssss@gmail.com";
    private static final String SENDER_PASSWORD = "eciakumnmdonykmd"; // your 16-character app password (no spaces)

    public static void sendEmail(Context context, String recipientEmail, String subject, String messageBody) {
        new Thread(() -> {
            try {
                // ✅ Gmail SMTP Configuration — using TLS
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587"); // TLS port

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                // ✅ Create email
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "SmartBinyan"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject(subject);
                message.setText(messageBody);

                // ✅ Send email
                Transport.send(message);

                new android.os.Handler(context.getMainLooper()).post(() ->
                        Toast.makeText(context, "✅ Verification code sent to " + recipientEmail, Toast.LENGTH_SHORT).show());

            } catch (MessagingException | java.io.UnsupportedEncodingException e) {
                e.printStackTrace();
                new android.os.Handler(context.getMainLooper()).post(() ->
                        Toast.makeText(context, "❌ Failed to send email: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
