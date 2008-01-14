package org.sakaiproject.email.api;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type safe constant for message mime types
 */
enum MimeType
{
	TEXT("text/plain"), HTML("text/html");

	private String mimeString;

	public MimeType(String mimeString)
	{
		this.mimeString = mimeString;
	}

	public String getMimeString()
	{
		return mimeString;
	}
}

/**
 * Type safe constant for recipient types
 */
enum RecipientType
{
	TO, CC, BCC
}

/**
 * Value object for sending emails. Mimics javax.mail.internet.MimeMessage without having a
 * dependency on javax.mail
 */
public class EmailMessage
{
	// who this message is from
	private EmailAddress from;

	// addressee for replies
	private EmailAddress replyTo;

	// recipients of message
	private List<EmailAddress> recipients;

	// subject of message
	private String subject;

	// body content of message
	private String body;

	// attachments to consider for message
	private List<File> attachments;

	// arbitrary headers for message
	private HashMap<String, String> headers;

	// mime type of message
	private String mimeType;

	// character set of text in message
	private String charset;

	// whether to embed attachments to message or persist to resource store
	private boolean embedAttachments;

	// where to put files in the resource store.  Only used if embedAttachments == true.
	private String embedLocation;

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
		this.from = new EmailAddress(from);
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
	public EmailAddress getReplyTo()
	{
		return replyTo;
	}

	/**
	 * Set recipient for replies.
	 * 
	 * @param email
	 *            Email string of reply to recipient.
	 */
	public void setReplyTo(String email)
	{
		replyTo = new EmailAddress(email);
	}

	/**
	 * Set recipient for replies.
	 * 
	 * @param email
	 *            {@link EmailAddress} of reply to recipient.
	 */
	public void setReplyTo(EmailAddress replyTo)
	{
		this.replyTo = replyTo;
	}

	/**
	 * Get intended recipients of this message.
	 * 
	 * @return List of {@link EmailAddress} that will receive this messagel
	 */
	public List<EmailAddress> getRecipients()
	{
		return recipients;
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
	 * @return List of {@link java.io.File} attached to this message.
	 */
	public List<File> getAttachments()
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
			attachments.add(attachment);
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
		if (attachment != null)
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
	public void setAttachments(List<File> attachments)
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
	 * Add a header to this message.
	 * 
	 * @param key
	 *            The key of the header.
	 * @param value
	 *            The value of the header.
	 */
	public void addHeader(String key, String value)
	{
		headers.put(key, value);
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
	 * @return {@link MimeType} of this message.
	 */
	public MimeType getMimeType()
	{
		return mimeType;
	}

	/**
	 * Set the mime type of this message.
	 * 
	 * @param mimeType
	 *            The mime type to use for this message.
	 */
	public void setMimeType(MimeType mimeType)
	{
		this.mimeType = mimeType;
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
	 */
	public void setCharset(String charset)
	{
		this.charset = charset;
	}

	/**
	 * Whether to embed attachments on message or to place in a resource store.
	 * 
	 * @return true if attachments are included with this message. false if attachments are place in
	 *         a resource store.
	 */
	public boolean isEmbedAttachments()
	{
		return embedAttachment;
	}

	/**
	 * Set whether to embed attachments on message or to place in a resource store.
	 * 
	 * @param embedAttachments
	 *            Flag to set embed policy. Should be true if attachments are included with this
	 *            message. false if attachments are place in a resource store.
	 */
	public void setEmbedAttachments(boolean embedAttachments, String embedLocation)
	{
		this.embedAttachments = embedAttachments;
		this.embedLocation = embedLocation;
	}
}
