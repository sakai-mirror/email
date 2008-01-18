package org.sakaiproject.email.api;

public interface ContentTypes
{
	// plain message with no formatting
	String TEXT = "text/plain";

	// html formatted message
	String HTML = "text/html";

	// used mainly for envelope of message to specify differing body parts
	String MIXED = "multipart/mixed";

	// defer mime detection to client
	String ALT = "multipart/alternative";
}