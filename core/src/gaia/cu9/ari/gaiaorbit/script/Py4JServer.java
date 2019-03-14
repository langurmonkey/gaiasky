package gaia.cu9.ari.gaiaorbit.script;

import gaia.cu9.ari.gaiaorbit.util.Logger;
import py4j.DefaultGatewayServerListener;
import py4j.GatewayServer;
import py4j.Py4JServerConnection;

public class Py4JServer {
    private static final Logger.Log logger = Logger.getLogger(Py4JServer.class);

    private static GatewayServer gatewayServer;

    public static void initialize(){
        gatewayServer = new GatewayServer(EventScriptingInterface.instance());
        gatewayServer.addListener(new DefaultGatewayServerListener(){

            @Override
            public void connectionStarted(Py4JServerConnection gatewayConnection) {
                logger.info("Connection started");
            }

            @Override
            public void connectionStopped(Py4JServerConnection gatewayConnection) {
                logger.info("Connection stopped");
            }

            @Override
            public void serverPostShutdown() {
                logger.info("Post shutdown");
            }

            @Override
            public void serverPreShutdown() {
                logger.info("Pre shutdown");
            }

            @Override
            public void serverStarted() {
                logger.info("Server started");
            }

            @Override
            public void serverStopped() {
                logger.info("Server stopped");
            }
        });
        gatewayServer.start();
    }

    public static void dispose(){
        if(gatewayServer != null)
            gatewayServer.shutdown();
    }
}
