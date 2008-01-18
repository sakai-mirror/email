package org.sakaiproject.email.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Attachments
{
	// files to attach to message
	private List<File> attachments;

	// where to put files in the resource store. If not set, attachments will be embedded on
	// message.
	private String storeLocation;

	/**
	 * Get the list of files contained herein
	 * 
	 * @return
	 */
	public List<File> getAttachments()
	{
		return attachments;
	}

	/**
	 * Add a file attachment.  Will not be added if file is null.
	 * 
	 * @param attachment
	 */
	public void addAttachment(File attachment)
	{
		if (attachment == null)
		{
			if (attachments == null)
				attachments = new ArrayList<File>();
			attachments.add(attachment);
		}
	}

	/**
	 * Set the list of attached files.  This will override any previous attachments.  Null accepted.
	 * @param attachments
	 */
	public void setAttachments(List<File> attachments)
	{
		this.attachments = attachments;
	}

	/**
	 * Gets the location where to store attachments.  Only used if isEmbedAttachments() == true.
	 * 
	 * @return The location to a resource store.
	 */
	public String getStoreLocation()
	{
		return storeLocation;
	}

	/**
	 * Set the location of where to store attachments.  If this is null or unset, attachments will
	 * be embedded on the message and not placed in a content repository.
	 * 
	 * @param storeLocation
	 */
	public void setStoreLocation(String storeLocation)
	{
		if (storeLocation != null && storeLocation.trim().length() == 0)
			this.storeLocation = null;
		else
			this.storeLocation = storeLocation;
	}
}