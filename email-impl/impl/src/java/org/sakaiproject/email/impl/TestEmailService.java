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

import java.util.List;
import java.util.logging.Logger;

import javax.mail.internet.InternetAddress;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.email.api.EmailService;

/**
 * <p>
 * BasicEmailService implements the EmailService.
 * </p>
 */
public class TestEmailService implements EmailService
{
	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies and their setter methods Note: keep these in sync with the BasidEmailService, to make switching between them easier -ggolden
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** Dependency: logging service. */
	protected Logger m_logger = null;

	/**
	 * Dependency: logging service.
	 * 
	 * @param service
	 *        The logging service.
	 */
	public void setLogger(Logger service)
	{
		m_logger = service;
	}

	/** Dependency: ServerConfigurationService. */
	protected ServerConfigurationService m_serverConfigurationService = null;

	/**
	 * Dependency: ServerConfigurationService.
	 * 
	 * @param service
	 *        The ServerConfigurationService.
	 */
	public void setServerConfigurationService(ServerConfigurationService service)
	{
		m_serverConfigurationService = service;
	}

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
		m_logger.info(this + ".init()");
	}

	/**
	 * Final cleanup.
	 */
	public void destroy()
	{
		m_logger.info(this + ".destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Work interface methods: org.sakai.service.email.EmailService
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected String listToStr(List list)
	{
		if (list == null) return "";
		return arrayToStr(list.toArray());
	}

	protected String arrayToStr(Object[] array)
	{
		StringBuffer buf = new StringBuffer();
		if (array != null)
		{
			buf.append("[");
			for (int i = 0; i < array.length; i++)
			{
				if (i != 0) buf.append(", ");
				buf.append(array[i].toString());
			}
			buf.append("]");
		}
		else
		{
			buf.append("");
		}

		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendMail(InternetAddress from, InternetAddress[] to, String subject, String content, InternetAddress[] headerTo,
			InternetAddress[] replyTo, List additionalHeaders)
	{
		m_logger.info(this + "sendMail: from: " + from + " to: " + arrayToStr(to) + " subject: " + subject + " headerTo: "
				+ arrayToStr(headerTo) + " replyTo: " + arrayToStr(replyTo) + " content: " + content + " additionalHeaders: "
				+ listToStr(additionalHeaders));
	}

	/**
	 * {@inheritDoc}
	 */
	public void send(String fromStr, String toStr, String subject, String content, String headerToStr, String replyToStr,
			List additionalHeaders)
	{
		m_logger.info(this + "send: from: " + fromStr + " to: " + toStr + " subject: " + subject + " headerTo: " + headerToStr
				+ " replyTo: " + replyToStr + " content: " + content + " additionalHeaders: " + listToStr(additionalHeaders));
	}
}
