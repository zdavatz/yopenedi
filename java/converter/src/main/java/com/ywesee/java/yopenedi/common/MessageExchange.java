package com.ywesee.java.yopenedi.common;

public interface MessageExchange<Party> {
    public Party getSender();
    public Party getRecipient();
    public String getRecipientGLN();
    public void setRecipientGLNOverride(String replaced);
}
