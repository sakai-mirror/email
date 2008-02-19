package org.sakaiproject.email.api;

/**
 * Common content types (primary + subtype) used when sending email messages.  The most commonly
 * used are:<br/>
 * TEXT_PLAIN - for plain, unformated text only
 * TEXT_HTML - for html formatted text
 * 
 * @author <a href="mailto:carl.hall@et.gatech.edu">Carl Hall</a>
 */
public interface ContentType
{
	// plain message with no formatting
	String TEXT_PLAIN = "text/plain";

	// html formatted message
	String TEXT_HTML = "text/html";

	// richtext formatted message
	String TEXT_RICH = "text/richtext";

//	// used mainly for envelope of message to specify differing body parts
//	String MULTI_MIXED = "multipart/mixed";
//
//	// defer mime detection to client
//	String MULTI_ALT = "multipart/alternative";
//
//	// for parts intended to be viewed simultaneously
//	String MULTI_PARALLEL = "multipart/parallel";
//
//	// a digest of multiple messages
//	String MULTI_DIGEST = "multipart/digest";
}