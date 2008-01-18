package org.sakaiproject.email.api;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sakaiproject.email.api.EmailAddress.RecipientType;

/**
 * Value object for sending emails. Mimics javax.mail.internet.MimeMessage without having a
 * dependency on javax.mail<br>
 * 
 * <p>Sending a message can be done by specifying recipients and/or <em>actual</em> recipients.  If
 * only recipients (to, cc, bcc) are specified, those are the people that will recieve the message
 * and will see each other listed in the to, cc and bcc fields.  If actual recipients are specified,
 * any other recipients will be ignored but will be added to the email headers appropriately.  This
 * allows for mailing to lists and hiding recipients (recipients: mylist@somedomain.edu,
 * actualRecipients: [long list of students].</p>
 * 
 * <p>The default content type for a message is {@link ContentType#TEXT}.
 * 
 * <p>The default character set for a message is UTF-8.
 * 
 * @see javax.mail.Transport#send(MimeMessage)
 * @see javax.mail.Transport#send(MimeMessage, Address[])
 * @see javax.mail.internet.InternetAddress
 */
public class EmailMessage
{
	/**
	 * Type safe constant for message mime types
	 */
	// who this message is from
	private EmailAddress from;

	// addressee for replies
	private List<EmailAddress> replyTo;

	// recipients of message
	private Map<RecipientType, List<EmailAddress>> recipients;

	// subject of message
	private String subject;

	// body content of message
	private String body;

	// attachments to consider for message
	private Attachments attachments;

	// arbitrary headers for message
	private HashMap<String, String> headers;

	// mime type of message
	private String contentType = ContentTypes.TEXT;

	// character set of text in message
	private String charset;

	// format of this message.  common value is "flowed"
	private String format;

	/**
	 * Default constructor.
	 */
	public EmailMessage()
	{
	}

	public EmailMessage(String from, String to, String subject, String body)
	{
		setFrom(from);
		addRecipient(RecipientType.TO, to);
		setSubject(subject);
		setBody(body);
	}

	/**
	 * Get the sender of this message.
	 * 
	 * @return The sender of this message.
	 */
	public EmailAddress getFrom()
	{
		return from;
	}

	/**
	 * Set the sender of this message.
	 * 
	 * @param email
	 *            Email address of sender.
	 */
	public void setFrom(String email)
	{
		this.from = new EmailAddress(email);
	}

	/**
	 * Set the sender of this message.
	 * 
	 * @param emailAddress
	 *            {@link EmailAddress} of message sender.
	 */
	public void setFrom(EmailAddress emailAddress)
	{
		this.from = emailAddress;
	}

	/**
	 * Get recipient for replies.
	 * 
	 * @return {@link EmailAddress} of reply to recipient.
	 */
	public List<EmailAddress> getReplyTo()
	{
		return replyTo;
	}

	/**
	 * Set recipient for replies.
	 * 
	 * @param email
	 *            Email string of reply to recipient.
	 */
	public void addReplyTo(EmailAddress emailAddress)
	{
		if (replyTo == null)
			replyTo = new ArrayList<EmailAddress>();
		replyTo.add(emailAddress);
	}

	/**
	 * Set recipient for replies.
	 * 
	 * @param email
	 *            {@link EmailAddress} of reply to recipient.
	 */
	public void setReplyTo(List<EmailAddress> replyTo)
	{
		this.replyTo = replyTo;
	}

	/**
	 * Get intended recipients of this message.
	 * 
	 * @return List of {@link EmailAddress} that will receive this messagel
	 */
	public Map<RecipientType, List<EmailAddress>> getRecipients()
	{
		return recipients;
	}

	/**
	 * Get recipients of this message that are associated to a certain type
	 * 
	 * @param type
	 * @return
	 * @see RecipientType
	 */
	public List<EmailAddress> getRecipients(RecipientType type)
	{
		List<EmailAddress> retval = null;
		if (recipients != null)
			recipients.get(type);
		return retval;
	}

	/**
	 * Add a recipient to this message.
	 * 
	 * @param type
	 *            How to address the recipient.
	 * @param email
	 *            Email to send to.
	 */
	public void addRecipient(RecipientType type, String email)
	{
		List<EmailAddress> addresses = recipients.get(type);
		if (addresses == null)
			addresses = new ArrayList<EmailAddress>();
		addresses.add(new EmailAddress(email));
		recipients.put(type, addresses);
	}

	/**
	 * Add a recipient to this message.
	 * 
	 * @param type
	 *            How to address the recipient.
	 * @param name
	 *            Name of recipient.
	 * @param email
	 *            Email to send to.
	 */
	public void addRecipient(RecipientType type, String name, String email)
	{
		List<EmailAddress> addresses = recipients.get(type);
		if (addresses == null)
			addresses = new ArrayList<EmailAddress>();
		addresses.add(new EmailAddress(name, email));
		recipients.put(type, addresses);
	}

	/**
	 * Add multiple recipients to this message.
	 * 
	 * @param type
	 *            How to address the recipients.
	 * @param addresses
	 *            List of {@link EmailAddress} to add to this message.
	 */
	public void addRecipients(RecipientType type, List<EmailAddress> addresses)
	{
		List<EmailAddress> currentAddresses = recipients.get(type);
		if (currentAddresses == null)
			recipients.put(type, addresses);
		else
			currentAddresses.addAll(addresses);
	}

	/**
	 * Set the recipients of this message. This will replace any existing recipients of the same
	 * type.
	 * 
	 * @param type
	 *            How to address the recipients.
	 * @param addresses
	 *            List of {@link EmailAddress} to add to this message.
	 */
	public void setRecipients(RecipientType type, List<EmailAddress> addresses)
	{
		if (addresses != null)
			recipients.put(type, addresses);
		else
			recipients.remove(type);
	}

	/**
	 * Set the recipients of this messsage. This will replace any existing recipients
	 * 
	 * @param recipients
	 */
	public void setRecipients(Map<RecipientType, List<EmailAddress>> recipients)
	{
		this.recipients = recipients;
	}

	/**
	 * Get all recipients as a flattened list. This is intended to be used for determining the
	 * recipients for an SMTP route.
	 * 
	 * @return list of recipient addresses associated to this message
	 */
	public List<EmailAddress> getAllRecipients()
	{
		List<EmailAddress> rcpts = new ArrayList<EmailAddress>();

		if (recipients.containsKey(RecipientType.TO))
			rcpts.addAll(recipients.get(RecipientType.TO));

		if (recipients.containsKey(RecipientType.CC))
			rcpts.addAll(recipients.get(RecipientType.CC));

		if (recipients.containsKey(RecipientType.BCC))
			rcpts.addAll(recipients.get(RecipientType.BCC));

		if (recipients.containsKey(RecipientType.ACTUAL))
			rcpts.addAll(recipients.get(RecipientType.ACTUAL));

		return rcpts;
	}

	/**
	 * Get the subject of this message.
	 * 
	 * @return The subject of this message. May be empty or null value.
	 */
	public String getSubject()
	{
		return subject;
	}

	/**
	 * Set the subject of this message.
	 * 
	 * @param subject
	 *            Subject for this message. Empty and null values allowed.
	 */
	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	/**
	 * Get the body content of this message.
	 * 
	 * @return The body content of this message.
	 */
	public String getBody()
	{
		return body;
	}

	/**
	 * Set the body content of this message.
	 * 
	 * @param body
	 *            The content of this message.
	 */
	public void setBody(String body)
	{
		this.body = body;
	}

	/**
	 * Get the attachments on this message
	 * 
	 * @return List of {@link Attachments} attached to this message.
	 */
	public Attachments getAttachments()
	{
		return attachments;
	}

	/**
	 * Add an attachment to this message.
	 * 
	 * @param attachment
	 *            File to attach to this message.
	 */
	public void addAttachment(File attachment)
	{
		if (attachment != null)
		{
			if (attachments == null)
				attachments = new Attachments();
			attachments.addAttachment(attachment);
		}
	}

	/**
	 * Add an attachment to this message.
	 * 
	 * @param attachmentUrl
	 *            The full URL of a resource to be attached. Must fit nicely into a
	 *            {@link java.io.File}
	 */
	public void addAttachment(String attachmentUrl)
	{
		if (attachmentUrl != null)
		{
			File f = new File(attachmentUrl);
			addAttachment(f);
		}
	}

	/**
	 * Set the attachments of this message. Will replace any existing attachments.
	 * 
	 * @param attachments
	 *            The attachments to set on this message.
	 */
	public void setAttachments(Attachments attachments)
	{
		this.attachments = attachments;
	}

	/**
	 * Get the headers of this message.
	 * 
	 * @return {@link java.util.Map} of headers set on this message.
	 */
	public Map<String, String> getHeaders()
	{
		return headers;
	}

	/**
	 * Flattens the headers down to "key: value" strings.
	 * 
	 * @return List of properly formatted headers. List will be 0 length if no headers found. Does
	 *         not return null
	 */
	public List<String> extractHeaders()
	{
		List<String> retval = new ArrayList<String>();;
		if (headers != null)
		{
			for (String key : headers.keySet())
			{
				String value = headers.get(key);
				if (key != null && value != null)
				{
					retval.add(key + ": " + value);
				}
			}
		}
		return retval;
	}

	/**
	 * Remove a header from this message.  Does nothing if header is not found.
	 * 
	 * @param key
	 */
	public void removeHeader(String key)
	{
		if (headers != null && !headers.isEmpty() && headers.containsKey(key))
			headers.remove(key);
	}

	/**
	 * Add a header to this message. If the key is found in the headers of this message, the value
	 * is appended to the previous value found and separated by a space. A key of null will not be
	 * added. If value is null, will remove any previous entries of the matching key.
	 * 
	 * @param key
	 *            The key of the header.
	 * @param value
	 *            The value of the header.
	 */
	public void setHeader(String key, String value)
	{
		if (key != null && value != null)
		{
			if (headers == null)
				headers = new HashMap<String, String>();

			headers.put(key, value);
		}
	}

	/**
	 * Set the headers of this message. Will replace any existing headers.
	 * 
	 * @param headers
	 *            The headers to use on this message.
	 */
	public void setHeaders(HashMap<String, String> headers)
	{
		this.headers = headers;
	}

	/**
	 * Get the mime type of this message.
	 * 
	 * @return {@link org.sakaiproject.email.api.ContentTypes} of this message.
	 */
	public String getContentType()
	{
		return contentType;
	}

	/**
	 * Set the mime type of this message.
	 * 
	 * @param mimeType
	 *            The mime type to use for this message.
	 * @see org.sakaiproject.email.api.ContentTypes
	 */
	public void setContentType(String mimeType)
	{
		this.contentType = mimeType;
	}

	/**
	 * Get the character set for text in this message. Used for the subject and body.
	 * 
	 * @return The character set used for this message.
	 */
	public String getCharset()
	{
		return charset;
	}

	/**
	 * Set the character set for text in this message.
	 * 
	 * @param charset
	 *            The character set used to render text in this message.
	 * @see org.sakaproject.email.api.CharsetConstants
	 */
	public void setCharset(String charset)
	{
		this.charset = charset;
	}

	public String getFormat()
	{
		return format;
	}

	public void setFormat(String format)
	{
		this.format = format;
	}
}