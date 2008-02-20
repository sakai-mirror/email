package org.sakaiproject.email.impl;

import com.dumbster.smtp.SimpleSmtpServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.mail.internet.InternetAddress;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.email.api.Attachment;
import org.sakaiproject.email.api.EmailAddress;
import org.sakaiproject.email.api.EmailMessage;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.email.api.EmailAddress.RecipientType;
import org.sakaiproject.test.SakaiTestBase;

public class TestEmailService extends SakaiTestBase
{
	private static Log log = LogFactory.getLog(TestEmailService.class);

	private static final boolean USE_INT_MAIL_SERVER = false;

	EmailService emailService;

	InternetAddress from;
	InternetAddress[] to;
	String subject;
	String content;
	HashMap<RecipientType, InternetAddress[]> headerToMap;
	InternetAddress[] headerTo;
	InternetAddress[] replyTo;
	ArrayList<String> additionalHeaders;
	ArrayList<Attachment> attachments;

	public static Test suite()
	{
		TestSetup setup = new TestSetup(new TestSuite(TestEmailService.class))
		{
			SimpleSmtpServer server = null;

			protected void setUp() throws Exception
			{
				if (log.isDebugEnabled())
					log.debug("starting setup");
				try
				{
					oneTimeSetup();
					if (USE_INT_MAIL_SERVER)
						server = SimpleSmtpServer.start(8025);
				}
				catch (Exception e)
				{
					log.warn(e);
				}
				if (log.isDebugEnabled())
					log.debug("finished setup");
			}

			protected void tearDown() throws Exception
			{
				if (log.isDebugEnabled())
					log.debug("tearing down");
				if (USE_INT_MAIL_SERVER && server != null)
					server.stop();
				oneTimeTearDown();
			}
		};
		return setup;
	}

	public void setUp() throws Exception
	{
		log.info("Setting up test case...");
		from = new InternetAddress("from@example.com");

		to = new InternetAddress[2];
		to[0] = new InternetAddress("to@example.com");
		to[1] = new InternetAddress("carl.hall@gmail.com");

		subject = "Super cool test subject";

		content = "Super cool test content";

		headerToMap = new HashMap<RecipientType, InternetAddress[]>();
		headerTo = new InternetAddress[1];
		// create the TO
		headerTo[0] = new InternetAddress("randomDude@example.com", "Random Dude");
		headerToMap.put(RecipientType.TO, headerTo);
		// create the CC
		headerTo[0] = new InternetAddress("otherPerson@example.com");
		headerToMap.put(RecipientType.CC, headerTo);

		replyTo = new InternetAddress[1];
		replyTo[0] = new InternetAddress("replyTo@example.com");

		additionalHeaders = new ArrayList<String>();
		additionalHeaders.add("x-testmessage-rocks: super-awesome");

		attachments = new ArrayList<Attachment>();
		attachments.add(new Attachment("/home/chall39/velocity.log"));
		attachments.add(new Attachment("/home/chall39/quotes.txt"));

		emailService = (EmailService) getService(EmailService.class.getName());
	}

	public void testSend() throws Exception
	{
		emailService.send(from.getAddress(), to[0].getAddress() + ", test2@example.com", subject, content,
				headerTo[0].getAddress(), replyTo[0].getAddress(), additionalHeaders);
	}

	public void xtestSendEmailMessage() throws Exception
	{
		// create message with from, subject, content
		EmailMessage msg = new EmailMessage(from.getAddress(), subject, content);
		// add message recipients that appear in the header
		HashMap<RecipientType, List<EmailAddress>> tos = new HashMap<RecipientType, List<EmailAddress>>();
		for (RecipientType type : headerToMap.keySet())
		{
			ArrayList<EmailAddress> addrs = new ArrayList<EmailAddress>();
			for (InternetAddress iaddr : headerToMap.get(type))
			{
				addrs.add(new EmailAddress(iaddr.getAddress(), iaddr.getPersonal()));
			}
			tos.put(type, addrs);
		}
		// add the actual recipients
		tos.put(RecipientType.ACTUAL, EmailAddress.toEmailAddress(to));
		msg.setRecipients(tos);
		// add additional headers
		msg.setHeaders(additionalHeaders);
		// add attachments
		msg.setAttachments(attachments);
		// send message
		emailService.send(msg);
	}

	public void testSendEmailMessageWithoutAttachments() throws Exception
	{
		// create message with from, subject, content
		EmailMessage msg = new EmailMessage(from.getAddress(), subject, content);
		// add message recipients that appear in the header
		HashMap<RecipientType, List<EmailAddress>> tos = new HashMap<RecipientType, List<EmailAddress>>();
		for (RecipientType type : headerToMap.keySet())
		{
			ArrayList<EmailAddress> addrs = new ArrayList<EmailAddress>();
			for (InternetAddress iaddr : headerToMap.get(type))
			{
				addrs.add(new EmailAddress(iaddr.getAddress(), iaddr.getPersonal()));
			}
			tos.put(type, addrs);
		}
		// add the actual recipients
		tos.put(RecipientType.ACTUAL, EmailAddress.toEmailAddress(to));
		msg.setRecipients(tos);
		// add additional headers
		msg.setHeaders(additionalHeaders);
		// send message
		emailService.send(msg);
	}

	public void testSendMailBasic() throws Exception
	{
		emailService.sendMail(from, to, subject, content, null, null, null, null);
	}

	public void testSendMailAllButAttachments() throws Exception
	{
		emailService.sendMail(from, to, subject, content, headerTo, replyTo, additionalHeaders);
	}

	public void testSendMailAll() throws Exception
	{
		emailService.sendMail(from, to, subject, content, headerToMap, replyTo, additionalHeaders,
				attachments);
	}
}