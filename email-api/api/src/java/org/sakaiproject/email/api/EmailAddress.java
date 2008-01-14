/**
 * Value object for email address properties.  Mimics javax.mail.internet.InternetAddress without having a
 * dependency on javax.mail
 */
public class EmailAddress
{
	// holds the name of the recipient aka "Personal" part of the email address
	private String name;

	// an address that be used as an email message recipient
	private String email;

	/**
	 * Constructor for the minimum values of this class.
	 *
	 * @param email Email address of recipient
	 */
	public EmailAddress(String email) throws IllegalArgumentException
	{
		if (email == null || email.trim().size() == 0)
			throw new IllegalArgumentException("Email cannot be empty.");

		this.email = email;
	}

	/**
	 * Constructor for all values of this class.
	 *
	 * @param name Personal part of an email address.
	 * @param email Actual address of email recipient.
	 */
	public EmailAddress(String name, String email)
	{
		this(email);
		this.name = name;
	}

	/**
	 * Get the name associated to this email addressee.
	 *
	 * @return The personal part of this email address.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Get the recipient's email address.
	 *
	 * @return The email address of the recipient.
	 */
	public String getEmail()
	{
		return email;
	}
}
