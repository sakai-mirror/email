package org.sakaiproject.email.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds an attachment for an email message.  The attachment will be included with the message.
 * 
 * TODO: Make available for attachments to be stored in CHS.
 * 
 * @author <a href="mailto:carl.hall@et.gatech.edu">Carl Hall</a>
 */
public class Attachment
{
	/**
	 * files to associated to this attachment
	 */
	private File file;

	/**
	 * the collection Id of where to store attachments. If not set, attachments will be embedded on
	 * message.
	 */
//	private String storeLocation;

	/**
	 * Whether to show a copyright alert for this attachment. Only applies to attachments that are
	 * stored in content hosting
	 */
//	private Properties properties;

	public Attachment(File file)
	{
		this.file = file;
	}

	public Attachment(String filename)
	{
		this.file = new File(filename);
	}

//	public Attachment(File file, String storeLocation)
//	{
//		this.file = file;
//		this.storeLocation = storeLocation;
//	}

//	public Attachment(File file, String storeLocation, Properties properties)
//	{
//		this.file = file;
//		this.storeLocation = storeLocation;
//		this.properties = properties;
//	}

	/**
	 * Get the file associated to this attachment
	 * 
	 * @return
	 */
	public File getFile()
	{
		return file;
	}

	/**
	 * Set the file associated to this attachment
	 * 
	 * @param file
	 */
	public void setFile(File file)
	{
		this.file = file;
	}

	/**
	 * Gets the location where to store attachments. Only used if isEmbedAttachments() == true.
	 * 
	 * @return The location to a resource store.
	 */
//	public String getStoreLocation()
//	{
//		return storeLocation;
//	}

	/**
	 * Set the location of where to store attachments. If this is null or unset, attachments will be
	 * embedded on the message and not placed in a content repository.
	 * 
	 * @param storeLocation
	 */
//	public void setStoreLocation(String storeLocation)
//	{
//		if (storeLocation != null && storeLocation.trim().length() == 0)
//			this.storeLocation = null;
//		else
//			this.storeLocation = storeLocation;
//	}

//	public Properties getProperties()
//	{
//		return properties;
//	}

//	public void setProperties(Properties properties)
//	{
//		this.properties = properties;
//	}

	public static List<Attachment> toAttachment(List<File> files)
	{
		ArrayList<Attachment> attachments = null;
		if (files != null)
		{
			attachments = new ArrayList<Attachment>();
			for (File f : files)
			{
				attachments.add(new Attachment(f));
			}
		}
		return attachments;
	}
}