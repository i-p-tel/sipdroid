package com.jstun.core.attribute;

import com.jstun.core.attribute.MessageAttributeInterface.MessageAttributeType;

public class UnknownMessageAttributeException extends MessageAttributeParsingException {
	private static final long serialVersionUID = 5375193544145543299L;
	
	private MessageAttributeType type;
	
	public UnknownMessageAttributeException(String mesg, MessageAttributeType type) {
		super(mesg);
		this.type = type;
	}
	
	public MessageAttributeType getType() {
		return type;
	}
}
