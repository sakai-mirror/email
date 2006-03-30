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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.email.api.Digest;
import org.sakaiproject.email.api.DigestEdit;
import org.sakaiproject.email.api.DigestMessage;
import org.sakaiproject.email.api.DigestService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.entity.api.Edit;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeBreakdown;
import org.sakaiproject.time.api.TimeRange;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.BaseResourcePropertiesEdit;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.StorageUser;
import org.sakaiproject.util.Xml;
import org.sakaiproject.webapp.api.SessionBindingEvent;
import org.sakaiproject.webapp.api.SessionBindingListener;
import org.sakaiproject.webapp.api.SessionManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>
 * BaseDigestService is the base service for DigestService.
 * </p>
 */
public abstract class BaseDigestService implements DigestService, StorageUser, Runnable
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(BasicEmailService.class);

	private ResourceLoader rb = new ResourceLoader("email-impl");

	/** Storage manager for this service. */
	protected Storage m_storage = null;

	/** The initial portion of a relative access point URL. */
	protected String m_relativeAccessPoint = null;

	/** The queue of digests waiting to be added (DigestMessage). */
	protected List m_digestQueue = new Vector();

	/** The thread I run my periodic clean and report on. */
	protected Thread m_thread = null;

	/** My thread's quit flag. */
	protected boolean m_threadStop = false;

	/** How long to wait between runnable runs (ms). */
	protected static final long PERIOD = 1000;

	/** True if we are in the mode of sending out digests, false if we are waiting. */
	protected boolean m_sendDigests = true;

	/** The time period last time the sendDigests() was called. */
	protected String m_lastSendPeriod = null;

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Runnable
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Start the clean and report thread.
	 */
	protected void start()
	{
		m_threadStop = false;

		m_thread = new Thread(this, getClass().getName());
		m_thread.start();
	}

	/**
	 * Stop the clean and report thread.
	 */
	protected void stop()
	{
		if (m_thread == null) return;

		// signal the thread to stop
		m_threadStop = true;

		// wake up the thread
		m_thread.interrupt();

		m_thread = null;
	}

	/**
	 * Run the clean and report thread.
	 */
	public void run()
	{
		// loop till told to stop
		while ((!m_threadStop) && (!Thread.currentThread().isInterrupted()))
		{
			try
			{
				// process the queue of digest requests
				processQueue();

				// check for a digest mailing time
				sendDigests();
			}
			catch (Throwable e)
			{
				M_log.warn(": exception: ", e);
			}

			// take a small nap
			try
			{
				Thread.sleep(PERIOD);
			}
			catch (Throwable ignore)
			{
			}
		}
	}

	/**
	 * Attempt to process all the queued digest requests. Ones that cannot be processed now will be returned to the queue.
	 */
	protected void processQueue()
	{
		// setup a re-try queue
		List retry = new Vector();

		// grab the queue - any new stuff will be processed next time
		List queue = new Vector();
		synchronized (m_digestQueue)
		{
			queue.addAll(m_digestQueue);
			m_digestQueue.clear();
		}

		for (Iterator iQueue = queue.iterator(); iQueue.hasNext();)
		{
			DigestMessage message = (DigestMessage) iQueue.next();
			try
			{
				DigestEdit edit = edit(message.getTo());
				edit.add(message);
				commit(edit);
				// %%% could do this by pulling all for id from the queue in one commit -ggolden
			}
			catch (InUseException e)
			{
				// retry next time
				retry.add(message);
			}
		}

		// requeue the retrys
		if (retry.size() > 0)
		{
			synchronized (m_digestQueue)
			{
				m_digestQueue.addAll(retry);
			}
		}
	}

	/**
	 * If it's time, send out any digested messages. Send once daily, after a certiain time of day (local time).
	 */
	protected void sendDigests()
	{
		// compute the current period
		String curPeriod = computeRange(m_timeService.newTime()).toString();

		// if we are in a new period, start sending again
		if (!curPeriod.equals(m_lastSendPeriod))
		{
			m_sendDigests = true;

			// remember this period for next check
			m_lastSendPeriod = curPeriod;
		}

		// if we are not sending, early out
		if (!m_sendDigests) return;

		if (M_log.isDebugEnabled()) M_log.debug("checking for sending digests");

		// count send candidate digests
		int count = 0;

		// process each digest
		List digests = getDigests();
		for (Iterator iDigests = digests.iterator(); iDigests.hasNext();)
		{
			Digest digest = (Digest) iDigests.next();

			// see if this one has any prior periods
			List periods = digest.getPeriods();
			if (periods.size() == 0) continue;

			boolean found = false;
			for (Iterator iPeriods = periods.iterator(); iPeriods.hasNext();)
			{
				String period = (String) iPeriods.next();
				if (!curPeriod.equals(period))
				{
					found = true;
					break;
				}
			}
			if (!found) continue;

			// this digest is a send candidate
			count++;

			// get a lock
			DigestEdit edit = null;
			try
			{
				boolean changed = false;
				edit = edit(digest.getId());

				// process each non-current period
				for (Iterator iPeriods = edit.getPeriods().iterator(); iPeriods.hasNext();)
				{
					String period = (String) iPeriods.next();

					// process if it's not the current period
					if (!curPeriod.equals(period))
					{
						TimeRange periodRange = m_timeService.newTimeRange(period);
						Time timeInPeriod = periodRange.firstTime();

						// any messages?
						List msgs = edit.getMessages(timeInPeriod);
						if (msgs.size() > 0)
						{
							// send this one
							send(edit.getId(), msgs, periodRange);
						}

						// clear this period
						edit.clear(timeInPeriod);

						changed = true;
					}
				}

				// commit, release the lock
				if (changed)
				{
					// delete it if empty
					if (edit.getPeriods().size() == 0)
					{
						remove(edit);
					}
					else
					{
						commit(edit);
					}
					edit = null;
				}
				else
				{
					cancel(edit);
					edit = null;
				}
			}
			// if in use, missing, whatever, skip on
			catch (Throwable any)
			{
			}
			finally
			{
				if (edit != null)
				{
					cancel(edit);
					edit = null;
				}
			}

		} // for (Iterator iDigests = digests.iterator(); iDigests.hasNext();)

		// if we didn't see any send candidates, we will stop sending till next period
		if (count == 0)
		{
			m_sendDigests = false;
		}
	}

	/**
	 * Send a single digest message
	 * 
	 * @param id
	 *        The use id to send the message to.
	 * @param msgs
	 *        The List (DigestMessage) of message to digest.
	 * @param period
	 *        The time period of the digested messages.
	 */
	protected void send(String id, List msgs, TimeRange period)
	{
		// sanity check
		if (msgs.size() == 0) return;

		try
		{
			String to = m_userDirectoryService.getUser(id).getEmail();

			// if use has no email address we can't send it
			if ((to == null) || (to.length() == 0)) return;

			String from = "postmaster@" + m_serverConfigurationService.getServerName();
			String subject = m_serverConfigurationService.getString("ui.service", "Sakai") + " " + rb.getString("notif") + " "
					+ period.firstTime().toStringLocalDate();

			StringBuffer body = new StringBuffer();
			body.append(subject);
			body.append("\n\n");

			// toc
			int count = 1;
			for (Iterator iMsgs = msgs.iterator(); iMsgs.hasNext();)
			{
				DigestMessage msg = (DigestMessage) iMsgs.next();

				body.append(Integer.toString(count));
				body.append(".  ");
				body.append(msg.getSubject());
				body.append("\n");
				count++;
			}
			body.append("\n----------------------\n\n");

			// for each msg
			count = 1;
			for (Iterator iMsgs = msgs.iterator(); iMsgs.hasNext();)
			{
				DigestMessage msg = (DigestMessage) iMsgs.next();

				// repeate toc entry
				body.append(Integer.toString(count));
				body.append(".  ");
				body.append(msg.getSubject());
				body.append("\n\n");

				// message body
				body.append(msg.getBody());

				body.append("\n----------------------\n\n");
				count++;
			}

			// tag
			body.append(rb.getString("thiaut") + " " + m_serverConfigurationService.getString("ui.service", "Sakai") + " "
					+ "(" + m_serverConfigurationService.getServerUrl() + ")" + "\n"
					+ rb.getString("youcan") + "\n");

			if (M_log.isDebugEnabled()) M_log.debug(this + " sending digest email to: " + to);

			m_emailService.send(from, to, subject, body.toString(), to, null, null);
		}
		catch (Throwable any)
		{
			M_log.warn(".send: digest to: " + id + " not sent: " + any.toString());
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Abstractions, etc.
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Construct storage for this service.
	 */
	protected abstract Storage newStorage();

	/**
	 * Access the partial URL that forms the root of resource URLs.
	 * 
	 * @param relative
	 *        if true, form within the access path only (i.e. starting with /content)
	 * @return the partial URL that forms the root of resource URLs.
	 */
	protected String getAccessPoint(boolean relative)
	{
		return (relative ? "" : m_serverConfigurationService.getAccessUrl()) + m_relativeAccessPoint;
	}

	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @param id
	 *        The digest id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String digestReference(String id)
	{
		return getAccessPoint(true) + Entity.SEPARATOR + id;
	}

	/**
	 * Access the digest id extracted from a digest reference.
	 * 
	 * @param ref
	 *        The digest reference string.
	 * @return The the digest id extracted from a digest reference.
	 */
	protected String digestId(String ref)
	{
		String start = getAccessPoint(true) + Entity.SEPARATOR;
		int i = ref.indexOf(start);
		if (i == -1) return ref;
		String id = ref.substring(i + start.length());
		return id;
	}

	/**
	 * Check security permission.
	 * 
	 * @param lock
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @return true if allowd, false if not
	 */
	protected boolean unlockCheck(String lock, String resource)
	{
		if (!m_securityService.unlock(lock, resource))
		{
			return false;
		}

		return true;
	}

	/**
	 * Check security permission.
	 * 
	 * @param lock
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @exception PermissionException
	 *            Thrown if the user does not have access
	 */
	protected void unlock(String lock, String resource) throws PermissionException
	{
		if (!unlockCheck(lock, resource))
		{
			throw new PermissionException(m_sessionManager.getCurrentSessionUserId(), lock, resource);
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies and their setter methods
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** Dependency: TimeService. */
	protected TimeService m_timeService = null;

	/**
	 * Dependency: TimeService.
	 * 
	 * @param service
	 *        The TimeService.
	 */
	public void setTimeService(TimeService service)
	{
		m_timeService = service;
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

	/** Dependency: EmailService. */
	protected EmailService m_emailService = null;

	/**
	 * Dependency: EmailService.
	 * 
	 * @param service
	 *        The EmailService.
	 */
	public void setEmailService(EmailService service)
	{
		m_emailService = service;
	}

	/** Dependency: EventTrackingService. */
	protected EventTrackingService m_eventTrackingService = null;

	/**
	 * Dependency: EventTrackingService.
	 * 
	 * @param service
	 *        The EventTrackingService.
	 */
	public void setEventTrackingService(EventTrackingService service)
	{
		m_eventTrackingService = service;
	}

	/** Dependency: SecurityService. */
	protected SecurityService m_securityService = null;

	/**
	 * Dependency: SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		m_securityService = service;
	}

	/** Dependency: UserDirectoryService. */
	protected UserDirectoryService m_userDirectoryService = null;

	/**
	 * Dependency: UserDirectoryService.
	 * 
	 * @param service
	 *        The UserDirectoryService.
	 */
	public void setUserDirectoryService(UserDirectoryService service)
	{
		m_userDirectoryService = service;
	}

	/** Dependency: the session manager. */
	protected SessionManager m_sessionManager = null;

	/**
	 * Dependency - set the session manager.
	 * 
	 * @param value
	 *        The session manager.
	 */
	public void setSessionManager(SessionManager manager)
	{
		m_sessionManager = manager;
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		m_relativeAccessPoint = REFERENCE_ROOT;

		// construct storage and read
		m_storage = newStorage();
		m_storage.open();

		// setup the queue
		m_digestQueue.clear();

		start();

		M_log.info("init()");
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		stop();

		m_storage.close();
		m_storage = null;

		if (m_digestQueue.size() > 0)
		{
			M_log.warn(".shutdown: with items in digest queue"); // %%%
		}
		m_digestQueue.clear();

		M_log.info("destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * DigestService implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @inheritDoc
	 */
	public Digest getDigest(String id) throws IdUnusedException
	{
		Digest digest = findDigest(id);
		if (digest == null) throw new IdUnusedException(id);

		return digest;
	}

	/**
	 * @inheritDoc
	 */
	public List getDigests()
	{
		List digests = m_storage.getAll();

		return digests;
	}

	/**
	 * @inheritDoc
	 */
	public void digest(String to, String subject, String body)
	{
		DigestMessage message = new org.sakaiproject.email.impl.DigestMessage(to, subject, body);

		// queue this for digesting
		synchronized (m_digestQueue)
		{
			m_digestQueue.add(message);
		}
	}

	/**
	 * @inheritDoc
	 */
	public DigestEdit edit(String id) throws InUseException
	{
		// security
		// unlock(SECURE_EDIT_DIGEST, digestReference(id));

		// one add/edit at a time, please, to make sync. only one digest per user
		// TODO: I don't link sync... could just do the add and let it fail if it already exists -ggolden
		synchronized (m_storage)
		{
			// check for existance
			if (!m_storage.check(id))
			{
				try
				{
					return add(id);
				}
				catch (IdUsedException e)
				{
					M_log.warn(".edit: from the add: " + e);
				}
			}

			// ignore the cache - get the user with a lock from the info store
			DigestEdit edit = m_storage.edit(id);
			if (edit == null) throw new InUseException(id);

			((BaseDigest) edit).setEvent(SECURE_EDIT_DIGEST);

			return edit;
		}
	}

	/**
	 * @inheritDoc
	 */
	public void commit(DigestEdit edit)
	{
		// check for closed edit
		if (!edit.isActiveEdit())
		{
			try
			{
				throw new Exception();
			}
			catch (Exception e)
			{
				M_log.warn(".commit(): closed DigestEdit", e);
			}
			return;
		}

		// update the properties
		// addLiveUpdateProperties(user.getPropertiesEdit());

		// complete the edit
		m_storage.commit(edit);

		// track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(((BaseDigest) edit).getEvent(), edit.getReference(), true));

		// close the edit object
		((BaseDigest) edit).closeEdit();
	}

	/**
	 * @inheritDoc
	 */
	public void cancel(DigestEdit edit)
	{
		// check for closed edit
		if (!edit.isActiveEdit())
		{
			try
			{
				throw new Exception();
			}
			catch (Exception e)
			{
				M_log.warn(".cancel(): closed DigestEdit", e);
			}
			return;
		}

		// release the edit lock
		m_storage.cancel(edit);

		// close the edit object
		((BaseDigest) edit).closeEdit();
	}

	/**
	 * @inheritDoc
	 */
	public void remove(DigestEdit edit)
	{
		// check for closed edit
		if (!edit.isActiveEdit())
		{
			try
			{
				throw new Exception();
			}
			catch (Exception e)
			{
				M_log.warn(".remove(): closed DigestEdit", e);
			}
			return;
		}

		// complete the edit
		m_storage.remove(edit);

		// track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(SECURE_REMOVE_DIGEST, edit.getReference(), true));

		// close the edit object
		((BaseDigest) edit).closeEdit();
	}

	/**
	 * @inheritDoc
	 */
	protected BaseDigest findDigest(String id)
	{
		BaseDigest digest = (BaseDigest) m_storage.get(id);

		return digest;
	}

	/**
	 * @inheritDoc
	 */
	public DigestEdit add(String id) throws IdUsedException
	{
		// check security (throws if not permitted)
		// unlock(SECURE_ADD_DIGEST, digestReference(id));

		// one add/edit at a time, please, to make sync. only one digest per user
		synchronized (m_storage)
		{
			// reserve a user with this id from the info store - if it's in use, this will return null
			DigestEdit edit = m_storage.put(id);
			if (edit == null)
			{
				throw new IdUsedException(id);
			}

			return edit;
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Digest implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	public class BaseDigest implements DigestEdit, SessionBindingListener
	{
		/** The user id. */
		protected String m_id = null;

		/** The properties. */
		protected ResourcePropertiesEdit m_properties = null;

		/** The digest time ranges (Map TimeRange string to List of DigestMessage). */
		protected Map m_ranges = null;

		/**
		 * Construct.
		 * 
		 * @param id
		 *        The user id.
		 */
		public BaseDigest(String id)
		{
			m_id = id;

			// setup for properties
			ResourcePropertiesEdit props = new BaseResourcePropertiesEdit();
			m_properties = props;

			// setup for ranges
			m_ranges = new Hashtable();

			// if the id is not null (a new user, rather than a reconstruction)
			// and not the anon (id == "") user,
			// add the automatic (live) properties
			// %%% if ((m_id != null) && (m_id.length() > 0)) addLiveProperties(props);
		}

		/**
		 * Construct from another Digest object.
		 * 
		 * @param user
		 *        The user object to use for values.
		 */
		public BaseDigest(Digest digest)
		{
			setAll(digest);
		}

		/**
		 * Construct from information in XML.
		 * 
		 * @param el
		 *        The XML DOM Element definining the user.
		 */
		public BaseDigest(Element el)
		{
			// setup for properties
			m_properties = new BaseResourcePropertiesEdit();

			// setup for ranges
			m_ranges = new Hashtable();

			m_id = el.getAttribute("id");

			// the children (properties, messages)
			NodeList children = el.getChildNodes();
			final int length = children.getLength();
			for (int i = 0; i < length; i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element = (Element) child;

				// look for properties
				if (element.getTagName().equals("properties"))
				{
					// re-create properties
					m_properties = new BaseResourcePropertiesEdit(element);
				}

				// look for a messages
				else if (element.getTagName().equals("messages"))
				{
					String period = element.getAttribute("period");

					// find the range
					List msgs = (List) m_ranges.get(period);
					if (msgs == null)
					{
						msgs = new Vector();
						m_ranges.put(period, msgs);
					}

					// do these children for messages
					NodeList msgChildren = element.getChildNodes();
					final int msgChildrenLen = msgChildren.getLength();
					for (int m = 0; m < msgChildrenLen; m++)
					{
						Node msgChild = msgChildren.item(m);
						if (msgChild.getNodeType() != Node.ELEMENT_NODE) continue;
						Element msgChildEl = (Element) msgChild;

						if (msgChildEl.getTagName().equals("message"))
						{
							String subject = Xml.decodeAttribute(msgChildEl, "subject");
							String body = Xml.decodeAttribute(msgChildEl, "body");
							msgs.add(new org.sakaiproject.email.impl.DigestMessage(m_id, subject, body));
						}
					}
				}
			}
		}

		/**
		 * Take all values from this object.
		 * 
		 * @param user
		 *        The user object to take values from.
		 */
		protected void setAll(Digest digest)
		{
			m_id = digest.getId();

			m_properties = new BaseResourcePropertiesEdit();
			m_properties.addAll(digest.getProperties());

			m_ranges = new Hashtable();
			// %%% deep enough? -ggolden
			m_ranges.putAll(((BaseDigest) digest).m_ranges);
		}

		/**
		 * @inheritDoc
		 */
		public Element toXml(Document doc, Stack stack)
		{
			Element digest = doc.createElement("digest");

			if (stack.isEmpty())
			{
				doc.appendChild(digest);
			}
			else
			{
				((Element) stack.peek()).appendChild(digest);
			}

			stack.push(digest);

			digest.setAttribute("id", getId());

			// properties
			m_properties.toXml(doc, stack);

			// for each message range
			for (Iterator it = m_ranges.entrySet().iterator(); it.hasNext();)
			{
				Map.Entry entry = (Map.Entry) it.next();

				Element messages = doc.createElement("messages");
				digest.appendChild(messages);
				messages.setAttribute("period", (String) entry.getKey());

				// for each message
				for (Iterator iMsgs = ((List) entry.getValue()).iterator(); iMsgs.hasNext();)
				{
					DigestMessage msg = (DigestMessage) iMsgs.next();

					Element message = doc.createElement("message");
					messages.appendChild(message);
					Xml.encodeAttribute(message, "subject", msg.getSubject());
					Xml.encodeAttribute(message, "body", msg.getBody());
				}
			}

			stack.pop();

			return digest;
		}

		/**
		 * @inheritDoc
		 */
		public String getId()
		{
			if (m_id == null) return "";
			return m_id;
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl()
		{
			return getAccessPoint(false) + m_id;
		}

		/**
		 * @inheritDoc
		 */
		public String getReference()
		{
			return digestReference(m_id);
		}

		/**
		 * @inheritDoc
		 */
		public String getReference(String rootProperty)
		{
			return getReference();
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl(String rootProperty)
		{
			return getUrl();
		}

		/**
		 * @inheritDoc
		 */
		public ResourceProperties getProperties()
		{
			return m_properties;
		}

		/**
		 * @inheritDoc
		 */
		public List getMessages(Time period)
		{
			synchronized (m_ranges)
			{
				// find the range
				String range = computeRange(period).toString();
				List msgs = (List) m_ranges.get(range);

				List rv = new Vector();
				if (msgs != null)
				{
					rv.addAll(msgs);
				}

				return rv;
			}
		}

		/**
		 * @inheritDoc
		 */
		public List getPeriods()
		{
			synchronized (m_ranges)
			{
				List rv = new Vector();
				rv.addAll(m_ranges.keySet());

				return rv;
			}
		}

		/**
		 * @inheritDoc
		 */
		public boolean equals(Object obj)
		{
			if (!(obj instanceof Digest)) return false;
			return ((Digest) obj).getId().equals(getId());
		}

		/**
		 * @inheritDoc
		 */
		public int hashCode()
		{
			return getId().hashCode();
		}

		/**
		 * @inheritDoc
		 */
		public int compareTo(Object obj)
		{
			if (!(obj instanceof Digest)) throw new ClassCastException();

			// if the object are the same, say so
			if (obj == this) return 0;

			// sort based on (unique) id
			int compare = getId().compareTo(((Digest) obj).getId());

			return compare;
		}

		/******************************************************************************************************************************************************************************************************************************************************
		 * Edit implementation
		 *****************************************************************************************************************************************************************************************************************************************************/

		/** The event code for this edit. */
		protected String m_event = null;

		/** Active flag. */
		protected boolean m_active = false;

		/**
		 * @inheritDoc
		 */
		public void add(DigestMessage msg)
		{
			synchronized (m_ranges)
			{
				// find the current range
				String range = computeRange(m_timeService.newTime()).toString();
				List msgs = (List) m_ranges.get(range);
				if (msgs == null)
				{
					msgs = new Vector();
					m_ranges.put(range, msgs);
				}
				msgs.add(msg);
			}
		}

		/**
		 * @inheritDoc
		 */
		public void add(String to, String subject, String body)
		{
			DigestMessage msg = new org.sakaiproject.email.impl.DigestMessage(to, subject, body);

			synchronized (m_ranges)
			{
				// find the current range
				String range = computeRange(m_timeService.newTime()).toString();
				List msgs = (List) m_ranges.get(range);
				if (msgs == null)
				{
					msgs = new Vector();
					m_ranges.put(range, msgs);
				}
				msgs.add(msg);
			}
		}

		/**
		 * @inheritDoc
		 */
		public void clear(Time period)
		{
			synchronized (m_ranges)
			{
				// find the range
				String range = computeRange(period).toString();
				List msgs = (List) m_ranges.get(range);
				if (msgs != null)
				{
					m_ranges.remove(range);
				}
			}
		}

		/**
		 * Clean up.
		 */
		protected void finalize()
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancel(this);
			}
		}

		/**
		 * Take all values from this object.
		 * 
		 * @param user
		 *        The user object to take values from.
		 */
		protected void set(Digest digest)
		{
			setAll(digest);
		}

		/**
		 * Access the event code for this edit.
		 * 
		 * @return The event code for this edit.
		 */
		protected String getEvent()
		{
			return m_event;
		}

		/**
		 * Set the event code for this edit.
		 * 
		 * @param event
		 *        The event code for this edit.
		 */
		protected void setEvent(String event)
		{
			m_event = event;
		}

		/**
		 * @inheritDoc
		 */
		public ResourcePropertiesEdit getPropertiesEdit()
		{
			return m_properties;
		}

		/**
		 * Enable editing.
		 */
		protected void activate()
		{
			m_active = true;
		}

		/**
		 * @inheritDoc
		 */
		public boolean isActiveEdit()
		{
			return m_active;
		}

		/**
		 * Close the edit object - it cannot be used after this.
		 */
		protected void closeEdit()
		{
			m_active = false;
		}

		/******************************************************************************************************************************************************************************************************************************************************
		 * SessionBindingListener implementation
		 *****************************************************************************************************************************************************************************************************************************************************/

		/**
		 * @inheritDoc
		 */
		public void valueBound(SessionBindingEvent event)
		{
		}

		/**
		 * @inheritDoc
		 */
		public void valueUnbound(SessionBindingEvent event)
		{
			if (M_log.isDebugEnabled()) M_log.debug(this + ".valueUnbound()");

			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancel(this);
			}
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Storage
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected interface Storage
	{
		/**
		 * Open.
		 */
		public void open();

		/**
		 * Close.
		 */
		public void close();

		/**
		 * Check if a digest by this id exists.
		 * 
		 * @param id
		 *        The user id.
		 * @return true if a digest for this id exists, false if not.
		 */
		public boolean check(String id);

		/**
		 * Get the digest with this id, or null if not found.
		 * 
		 * @param id
		 *        The digest id.
		 * @return The digest with this id, or null if not found.
		 */
		public Digest get(String id);

		/**
		 * Get all digests.
		 * 
		 * @return The list of all digests.
		 */
		public List getAll();

		/**
		 * Add a new digest with this id.
		 * 
		 * @param id
		 *        The digest id.
		 * @return The locked Digest object with this id, or null if the id is in use.
		 */
		public DigestEdit put(String id);

		/**
		 * Get a lock on the digest with this id, or null if a lock cannot be gotten.
		 * 
		 * @param id
		 *        The digest id.
		 * @return The locked Digest with this id, or null if this records cannot be locked.
		 */
		public DigestEdit edit(String id);

		/**
		 * Commit the changes and release the lock.
		 * 
		 * @param user
		 *        The edit to commit.
		 */
		public void commit(DigestEdit edit);

		/**
		 * Cancel the changes and release the lock.
		 * 
		 * @param user
		 *        The edit to commit.
		 */
		public void cancel(DigestEdit edit);

		/**
		 * Remove this edit and release the lock.
		 * 
		 * @param user
		 *        The edit to remove.
		 */
		public void remove(DigestEdit edit);
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * StorageUser implementation (no container)
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @inheritDoc
	 */
	public Entity newContainer(String ref)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Entity newContainer(Element element)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Entity newContainer(Entity other)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Entity newResource(Entity container, String id, Object[] others)
	{
		return new BaseDigest(id);
	}

	/**
	 * @inheritDoc
	 */
	public Entity newResource(Entity container, Element element)
	{
		return new BaseDigest(element);
	}

	/**
	 * @inheritDoc
	 */
	public Entity newResource(Entity container, Entity other)
	{
		return new BaseDigest((Digest) other);
	}

	/**
	 * @inheritDoc
	 */
	public Edit newContainerEdit(String ref)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newContainerEdit(Element element)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newContainerEdit(Entity other)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newResourceEdit(Entity container, String id, Object[] others)
	{
		BaseDigest e = new BaseDigest(id);
		e.activate();
		return e;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newResourceEdit(Entity container, Element element)
	{
		BaseDigest e = new BaseDigest(element);
		e.activate();
		return e;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newResourceEdit(Entity container, Entity other)
	{
		BaseDigest e = new BaseDigest((Digest) other);
		e.activate();
		return e;
	}

	/**
	 * @inheritDoc
	 */
	public Object[] storageFields(Entity r)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public boolean isDraft(Entity r)
	{
		return false;
	}

	/**
	 * @inheritDoc
	 */
	public String getOwnerId(Entity r)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Time getDate(Entity r)
	{
		return null;
	}

	/**
	 * Compute a time range based on a specific time.
	 * 
	 * @return The time range that encloses the specific time.
	 */
	protected TimeRange computeRange(Time time)
	{
		// set the period to "today" (local!) from day start to next day start, not end inclusive
		TimeBreakdown brk = time.breakdownLocal();
		brk.setMs(0);
		brk.setSec(0);
		brk.setMin(0);
		brk.setHour(0);
		Time start = m_timeService.newTimeLocal(brk);
		Time end = m_timeService.newTime(start.getTime() + 24 * 60 * 60 * 1000);
		return m_timeService.newTimeRange(start, end, true, false);
	}
}
