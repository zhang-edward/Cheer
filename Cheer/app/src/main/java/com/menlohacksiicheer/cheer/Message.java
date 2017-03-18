package com.menlohacksiicheer.cheer;

public class Message {
    public boolean getIncoming() {
        return incoming;
    }

    public void setIncoming(boolean bool) {
        incoming = bool;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean incoming;
    public String message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String status = null;


    public Message(boolean incoming, String message) {
        this.incoming = incoming;
        this.message = message;
    }

}
