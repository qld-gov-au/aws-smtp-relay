package com.loopingz;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.server.SMTPServer;

public abstract class SmtpRelay implements SimpleMessageListener {
  protected DeliveryDetails deliveryDetails;
  protected static SmtpRelay singleton;
  protected SMTPServer smtpServer;

  SmtpRelay(DeliveryDetails deliveryDetails) {
    this.deliveryDetails = deliveryDetails;
  }

  public static void init(SmtpRelay relay) throws UnknownHostException {
    singleton = relay;
    reload();
  }

  @Override
  public boolean accept(String from, String recipient) {
    return true;
  }

  public static void reload(DeliveryDetails deliveryDetails) throws UnknownHostException {
    if (singleton == null) {
      throw new RuntimeException("SMTP Server singleton does not exist");
    }
    singleton.deliveryDetails = deliveryDetails;
    reload();
  }

  public static void reload() throws UnknownHostException {
    if (singleton == null) {
      throw new RuntimeException("SMTP Server singleton does not exist");
    }
    if (singleton.smtpServer != null) {
      singleton.smtpServer.stop();
    }
    singleton.run();
  }

  void run() throws UnknownHostException {
    SMTPServer.Builder builder = new SMTPServer.Builder();
    builder.bindAddress(InetAddress.getByName(deliveryDetails.getBindAddress())).port(deliveryDetails.getPort())
        .simpleMessageListener(this);
    smtpServer = builder.build();
    smtpServer.start();
  }

}
