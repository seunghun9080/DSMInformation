package org.thinkinggms;

public class MessageResponseData {
    public String inputMessage;
    public String outputMessage;
    public String userId;

    public MessageResponseData(String inputMessage, String outputMessage, String userId) {
        this.inputMessage = inputMessage;
        this.outputMessage = outputMessage;
        this.userId = userId;
    }
}
