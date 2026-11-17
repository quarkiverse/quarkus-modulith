package io.quarkiverse.modulith.events.jms.deployment.fixtures.support;

import jakarta.inject.Singleton;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;

@Singleton
public class FakeConnectionFactory implements ConnectionFactory {

    @Override
    public Connection createConnection() {
        throw new UnsupportedOperationException("no broker in this test");
    }

    @Override
    public Connection createConnection(String userName, String password) {
        throw new UnsupportedOperationException("no broker in this test");
    }

    @Override
    public JMSContext createContext() {
        throw new UnsupportedOperationException("no broker in this test");
    }

    @Override
    public JMSContext createContext(String userName, String password) {
        throw new UnsupportedOperationException("no broker in this test");
    }

    @Override
    public JMSContext createContext(String userName, String password, int sessionMode) {
        throw new UnsupportedOperationException("no broker in this test");
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        throw new UnsupportedOperationException("no broker in this test");
    }
}
