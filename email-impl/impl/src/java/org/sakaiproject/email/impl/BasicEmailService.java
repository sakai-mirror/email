/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.email.impl;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.email.api.EmailService;

/**
 * <p>
 * BasicEmailService implements the EmailService.
 * </p>
 */
public abstract class BasicEmailService implements EmailService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(BasicEmailService.class);

	protected static final String POSTMASTER = "postmaster";

	protected static final String SMTP_HOST = "mail.smtp.host";

	protected static final String SMTP_PORT = "mail.smtp.port";

	protected static final String SMTP_FROM = "mail.smtp.from";

	protected static final String CONTENT_TYPE = "text/plain";

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies  Note: keep these in sync with the TestEmailService, to make switching between them easier -ggolden
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @return the ServerConfigurationService collaborator.
	 */
	protected abstract ServerConfigurationService serverConfigurationService();

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Configuration  Note: keep these in sync with the TestEmailService, to make switching between them easier -ggolden
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** Configuration: smtp server to use. */
	protected String m_smtp = null;

	/**
	 * Configuration: smtp server to use.
	 * 
	 * @param value
	 *        The smtp server string.
	 */
	public void setSmtp(String value)
	{
		m_smtp = value;
	}

	/** Configuration: smtp server port to use. */
	protected String m_smtpPort = null;

	/**
	 * Configuration: smtp server port to use.
	 * 
	 * @param value
	 *        The smtp server port string.
	 */
	public void setSmtpPort(String value)
	{
		m_smtpPort = value;
	}

	/** Configuration: optional smtp mail envelope return address. */
	protected String m_smtpFrom = null;

	/**
	 * Configuration: smtp mail envelope return address.
	 * 
	 * @param value
	 *        The smtp mail from address string.
	 */
	public void setSmtpFrom(String value)
	{
		m_smtpFrom = value;
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		// if no m_mailfrom set, set to the postmaster
		if (m_smtpFrom == null)
		{
			m_smtpFrom = POSTMASTER + "@" + serverConfigurationService().getServerName();
		}

		// promote these to the system properties, to keep others (James) from messing with them
		if (m_smtp != null) System.setProperty(SMTP_HOST, m_smtp);
		if (m_smtpPort != null) System.setProperty(SMTP_PORT, m_smtpPort);
		System.setProperty(SMTP_FROM, m_smtpFrom);

		M_log.info("init(): smtp: " + m_smtp + ((m_smtpPort != null) ? (":" + m_smtpPort) : "") + " bounces to: " + m_smtpFrom);
	}

	/**
	 * Final cleanup.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Work interface methods: org.sakai.service.email.EmailService
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * {@inheritDoc}
	 */
	public void sendMail(InternetAddress from, InternetAddress[] to, String subject, String content, InternetAddress[] headerTo,
			InternetAddress[] replyTo, List additionalHeaders)
	{
		if (m_smtp == null)
		{
			M_log.warn("sendMail: smtp not set");
			return;
		}

		if (from == null)
		{
			M_log.warn("sendMail: null from");
			return;
		}

		if (to == null)
		{
			M_log.warn("sendMail: null to");
			return;
		}

		if (content == null)
		{
			M_log.warn("sendMail: null content");
			return;
		}

		Properties props = new Properties();

		// set the server host
		props.put(SMTP_HOST, m_smtp);

		// set the port, if specified
		if (m_smtpPort != null)
		{
			props.put(SMTP_PORT, m_smtpPort);
		}

		// set the mail envelope return address
		props.put(SMTP_FROM, m_smtpFrom);

		Session session = Session.getDefaultInstance(props, null);

		if (M_log.isInfoEnabled())
		{
			StringBuffer buf = new StringBuffer();
			buf.append("Email.sendMail: from: ");
			buf.append(from);
			buf.append(" subject: ");
			buf.append(subject);
			buf.append(" to:");
			for (int i = 0; i < to.length; i++)
			{
				buf.append(" ");
				buf.append(to[i]);
			}
			if (headerTo != null)
			{
				buf.append(" headerTo:");
				for (int i = 0; i < headerTo.length; i++)
				{
					buf.append(" ");
					buf.append(headerTo[i]);
				}
			}
			M_log.info(buf.toString());
		}

		try
		{
			// see if we have a message-id in the additional headers
			String mid = null;
			if (additionalHeaders != null)
			{
				Iterator i = additionalHeaders.iterator();
				while (i.hasNext())
				{
					String header = (String) i.next();
					if (header.toLowerCase().startsWith("message-id: "))
					{
						mid = header.substring(12);
					}
				}
			}

			// use the special extension that can set the id
			MimeMessage msg = new MyMessage(session, mid);

			// the FULL content-type header, for example:
			// Content-Type: text/plain; charset=windows-1252; format=flowed
			String contentType = null;

			// the character set, for example, windows-1252 or UTF-8
			String charset = null;

			// set the additional headers on the message
			// but treat Content-Type specially as we need to check the charset
			// and we already dealt with the message id
			if (additionalHeaders != null)
			{
				Iterator i = additionalHeaders.iterator();
				while (i.hasNext())
				{
					String header = (String) i.next();
					if (header.toLowerCase().startsWith("content-type: "))
					{
						contentType = header;
					}
					else if (!header.toLowerCase().startsWith("message-id: "))
					{
						msg.addHeaderLine(header);
					}
				}
			}

			// date
			if (msg.getHeader("Date") == null)
			{
				msg.setSentDate(new Date(System.currentTimeMillis()));
			}

			msg.setFrom(from);

			if (msg.getHeader("To") == null)
			{
				if (headerTo != null)
				{
					msg.setRecipients(Message.RecipientType.TO, headerTo);
				}
			}

			if ((subject != null) && (msg.getHeader("Subject") == null))
			{
				msg.setSubject(subject);
			}

			if ((replyTo != null) && (msg.getHeader("Reply-To") == null))
			{
				msg.setReplyTo(replyTo);
			}

			// figure out what charset encoding to use
			//
			// first try to use the charset from the forwarded
			// Content-Type header (if there is one).
			// if that charset doesn't work, try a couple others.
			if (contentType != null)
			{
				// try and extract the charset from the Content-Type header
				int charsetStart = contentType.toLowerCase().indexOf("charset=");
				if (charsetStart != -1)
				{
					int charsetEnd = contentType.indexOf(";", charsetStart);
					if (charsetEnd == -1) charsetEnd = contentType.length();
					charset = contentType.substring(charsetStart + "charset=".length(), charsetEnd).trim();
				}
			}

			if (charset != null && canUseCharset(content, charset))
			{
				// use the charset from the Content-Type header
			}
			else if (canUseCharset(content, "ISO-8859-1"))
			{
				if (contentType != null && charset != null) contentType = contentType.replaceAll(charset, "ISO-8859-1");
				charset = "ISO-8859-1";
			}
			else if (canUseCharset(content, "windows-1252"))
			{
				if (contentType != null && charset != null) contentType = contentType.replaceAll(charset, "windows-1252");
				charset = "windows-1252";
			}
			else
			{
				// catch-all - UTF-8 should be able to handle anything
				if (contentType != null && charset != null) contentType = contentType.replaceAll(charset, "UTF-8");
				charset = "UTF-8";
			}

			// fill in the body of the message
			msg.setText(content, charset);

			// if we have a full Content-Type header, set it NOW
			// (after setting the body of the message so that format=flowed is preserved)
			if (contentType != null)
			{
				msg.addHeaderLine(contentType);
			}

			Transport.send(msg, to);
		}
		catch (MessagingException e)
		{
			// System.out.println(e);
		}
	}

	/** Returns true if the given content String can be encoded in the given charset */
	protected static boolean canUseCharset(String content, String charsetName)
	{
		try
		{
			return Charset.forName(charsetName).newEncoder().canEncode(content);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void send(String fromStr, String toStr, String subject, String content, String headerToStr, String replyToStr,
			List additionalHeaders)
	{
		if (fromStr == null)
		{
			M_log.warn("send: null fromStr");
			return;
		}

		if (toStr == null)
		{
			M_log.warn("send: null toStr");
			return;
		}

		if (content == null)
		{
			M_log.warn("send: null content");
			return;
		}

		try
		{
			InternetAddress from = new InternetAddress(fromStr);

			StringTokenizer tokens = new StringTokenizer(toStr, ", ");
			InternetAddress[] to = new InternetAddress[tokens.countTokens()];

			int i = 0;
			while (tokens.hasMoreTokens())
			{
				String next = (String) tokens.nextToken();
				to[i] = new InternetAddress(next);

				i++;
			} // cycle through and collect all of the Internet addresses from the list.

			InternetAddress[] headerTo = null;
			if (headerToStr != null)
			{
				headerTo = new InternetAddress[1];
				headerTo[0] = new InternetAddress(headerToStr);
			}

			InternetAddress[] replyTo = null;
			if (replyToStr != null)
			{
				replyTo = new InternetAddress[1];
				replyTo[0] = new InternetAddress(replyToStr);
			}

			sendMail(from, to, subject, content, headerTo, replyTo, additionalHeaders);

		}
		catch (AddressException e)
		{
			M_log.warn("send: " + e);
		}
	}

	// inspired by http://java.sun.com/products/javamail/FAQ.html#msgid
	protected class MyMessage extends MimeMessage
	{
		protected String m_id = null;

		public MyMessage(Session session, String id)
		{
			super(session);
			m_id = id;
		}

		protected void updateHeaders() throws MessagingException
		{
			super.updateHeaders();
			if (m_id != null)
			{
				setHeader("Message-Id", m_id);
			}
		}
	}
}
