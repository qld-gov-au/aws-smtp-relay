package com.loopingz;


import com.amazonaws.util.StringUtils;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

public class BasicSmtpRelay implements SimpleMessageListener {

    private DeliveryDetails deliveryDetails;
    private Properties props;
    private boolean authRequest;

    BasicSmtpRelay(DeliveryDetails deliveryDetails) {
        this.deliveryDetails = deliveryDetails;

        props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", deliveryDetails.getSmtpHost());
        props.put("mail.smtp.port",  deliveryDetails.getSmtpPort());

        if (!StringUtils.isNullOrEmpty(deliveryDetails.getSmtpUsername()) && !StringUtils.isNullOrEmpty(deliveryDetails.getSmtpPassword())){
            props.put("mail.smtp.auth", "true");
            authRequest = true;
        } else {
            props.put("mail.smtp.auth", "false");
            authRequest = false;
        }
    }

    public boolean accept(String from, String to) {
        return true;
    }

    public void deliver(String from, String to, InputStream inputStream) {

        try {
            Session session = getSession();
            Message msg = new MimeMessage(session, inputStream);
        msg.setFrom();
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setFrom(new InternetAddress(from));

        Transport.send(msg);
        } catch(SendFailedException ex) {
            throw new RejectException(451, ex.getMessage());
        } catch (MessagingException e) {
            throw new RejectException(e.getMessage());
        }
    }

    private Session getSession() {
        if (authRequest) {
            return Session.getDefaultInstance(props,
                    new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(deliveryDetails.getSmtpUsername(), deliveryDetails.getSmtpPassword());
                        }
                    });
        }
        return Session.getDefaultInstance(props);
    }

    void run() throws UnknownHostException {
        SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(this));
        smtpServer.setBindAddress(InetAddress.getByName(deliveryDetails.getBindAddress()));
        smtpServer.setPort(deliveryDetails.getPort());
        smtpServer.start();
    }
}
