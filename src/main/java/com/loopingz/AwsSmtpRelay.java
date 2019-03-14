package com.loopingz;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class AwsSmtpRelay implements SimpleMessageListener {

    private static CommandLine cmd;
    private static DeliveryDetails deliveryDetails;

    AwsSmtpRelay() {

    }

    public boolean accept(String from, String to) {
        return true;
    }

    public void deliver(String from, String to, InputStream inputStream) throws IOException {
        //split if override turned on to be a normal smtp relay
        AmazonSimpleEmailService client;
        if (cmd.hasOption("r")) {
            client = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(cmd.getOptionValue("r")).build();
        } else {
            client = AmazonSimpleEmailServiceClientBuilder.standard().build();
        }
        byte[] msg = IOUtils.toByteArray(inputStream);
        RawMessage rawMessage =
                new RawMessage(ByteBuffer.wrap(msg));
        SendRawEmailRequest rawEmailRequest =
                new SendRawEmailRequest(rawMessage).withSource(from)
                                                   .withDestinations(to);
        if (cmd.hasOption("a")) {
            rawEmailRequest = rawEmailRequest.withSourceArn(cmd.getOptionValue("a"));
        }
        if (cmd.hasOption("c")) {
            rawEmailRequest = rawEmailRequest.withConfigurationSetName(cmd.getOptionValue("c"));
        }
        try {
            client.sendRawEmail(rawEmailRequest);
        } catch (AmazonSimpleEmailServiceException e) {
            if(e.getErrorType() == ErrorType.Client) {
                // If it's a client error, return a permanent error
                throw new RejectException(e.getMessage());
            } else {
                throw new RejectException(451, e.getMessage());
            }
        }
    }

    //smtp deliver if override turned on

    void run() throws UnknownHostException {

        String bindAddress = cmd.hasOption("b") ? cmd.getOptionValue("b") : "127.0.0.1";

        SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(this));
        smtpServer.setBindAddress(InetAddress.getByName(bindAddress));
        if (cmd.hasOption("p")) {
            smtpServer.setPort(Integer.parseInt(cmd.getOptionValue("p")));
        } else {
            smtpServer.setPort(10025);
        }

        smtpServer.start();
    }

    public static void main(String[] args) throws UnknownHostException {
        Options options = new Options();
        options.addOption("ssm", "ssmEnable", false, "Use SSM to get configuration \r\n"
                + "${ssmPrefix}/region \r\n"
                + "${ssmPrefix}/configuration \r\n"
                + "${ssmPrefix}/sourceArn \r\n"
                + "${ssmPrefix}/smtpOverride \r\n"
                + "${ssmPrefix}/smtpHost \r\n"
                + "${ssmPrefix}/smtpPort \r\n"
                + "${ssmPrefix}/smtpUsername \r\n"
                + "${ssmPrefix}/smtpPassword \r\n");
        options.addOption("ssmP", "ssmPrefix", true, "SSM prefix to find variables default is /smtpRelay");

        options.addOption("p", "port", true, "Port number to listen to");
        options.addOption("b", "bindAddress", true, "Address to listen to");
        options.addOption("r", "region", true, "AWS region to use");
        options.addOption("c", "configuration", true, "AWS SES configuration to use");
        options.addOption("a", "sourceArn", true, "AWS ARN of the sending authorization policy");


        options.addOption("smtpO", "smtpOverride", true, "Not use SES but set SMTP variables true/false");
        options.addOption("smtpH", "smtpHost", true, "SMTP variable Host");
        options.addOption("smtpP", "smtpPort", true, "SMTP variable Port");
        options.addOption("smtpU", "smtpUsername", true, "SMTP variable Username");
        options.addOption("smtpPW", "smtpPassword", true, "SMTP variable password");

        options.addOption("h", "help", false, "Display this help");
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                // Should display version here
                formatter.printHelp("aws-smtp-relay", options);
                return;
            }

            //get configuration
            if (cmd.hasOption("ssm") ){
                deliveryDetails = new SsmConfigCollection(cmd, deliveryDetails).getConfig();
            } else {
                getCmdConfig();
            }

            //select sender
            if (deliveryDetails.isSmtpOverride()) {
                BasicSmtpRelay server = new BasicSmtpRelay(deliveryDetails);
                server.run();
            } else {
                AwsSmtpRelay server = new AwsSmtpRelay();
                server.run();
            }

        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static void getCmdConfig() {
        if (cmd.hasOption("r")) {
            deliveryDetails.setRegion(cmd.getOptionValue("r"));
        }
        if (cmd.hasOption("a")) {
            deliveryDetails.setSourceArn(cmd.getOptionValue("a"));
        }
        if (cmd.hasOption("c")) {
            deliveryDetails.setConfiguration(cmd.getOptionValue("c"));
        }
        if (cmd.hasOption("smtpO")) {
            deliveryDetails.setSmtpOverride(cmd.getOptionValue("smtpO"));
        }
        if (deliveryDetails.isSmtpOverride()) {
            if (cmd.hasOption("smtpH")) {
                deliveryDetails.setSmtpHost(cmd.getOptionValue("smtpH"));
            }
            if (cmd.hasOption("smtpP")) {
                deliveryDetails.setSmtpPort(cmd.getOptionValue("smtpP"));
            }
            if (cmd.hasOption("smtpU")) {
                deliveryDetails.setSmtpUsername(cmd.getOptionValue("smtpU"));
            }
            if (cmd.hasOption("smtpPW")) {
                deliveryDetails.setSmtpPassword(cmd.getOptionValue("smtpPW"));
            }
        }
    }
}
