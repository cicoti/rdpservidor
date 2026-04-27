package com.s4etech.desktop.session;

import java.net.InetAddress;

public class ActiveSessionContext {

    private volatile InetAddress clientAddress;
    private volatile int videoPort;
    private volatile int controlPort;
    private volatile long sessionId = -1;
    private volatile long activatedAt = 0L;

    public synchronized void activate(long sessionId, InetAddress clientAddress, int videoPort, int controlPort) {
        this.sessionId = sessionId;
        this.clientAddress = clientAddress;
        this.videoPort = videoPort;
        this.controlPort = controlPort;
        this.activatedAt = System.currentTimeMillis();
    }

    public synchronized void clear() {
        this.sessionId = -1;
        this.clientAddress = null;
        this.videoPort = 0;
        this.controlPort = 0;
        this.activatedAt = 0L;
    }

    public boolean hasActiveSession() {
        return clientAddress != null && sessionId > 0;
    }

    public InetAddress getClientAddress() {
        return clientAddress;
    }

    public int getVideoPort() {
        return videoPort;
    }

    public int getControlPort() {
        return controlPort;
    }

    public long getSessionId() {
        return sessionId;
    }

    public long getActivatedAt() {
        return activatedAt;
    }
}
