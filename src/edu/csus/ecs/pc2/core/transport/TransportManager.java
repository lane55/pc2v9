package edu.csus.ecs.pc2.core.transport;

import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import java.io.Serializable;
import java.security.PublicKey;

import edu.csus.ecs.pc2.core.log.Log;
import edu.csus.ecs.pc2.core.transport.crypto.Crypto;
import edu.csus.ecs.pc2.core.transport.crypto.CryptoException;

/**
 * Transport Manager Class used to control all communication between modules.
 * <P>
 * Each TransportManager is established as either a {@link TransportManager.tmTypes#CLIENT CLIENT} or a
 * {@link TransportManager.tmTypes#SERVER  SERVER} type connector.
 * <P>
 * A {@link TransportManager.tmTypes#SERVER SERVER} will start a listener on the input port and wait for connections from
 * {@link TransportManager.tmTypes#CLIENT CLIENT} TransportManagers.
 * <P>
 * A {@link TransportManager.tmTypes#CLIENT CLIENT} will contact a {@link TransportManager.tmTypes#SERVER SERVER} and establish a
 * connection. Note that a PC<sup>2</sup> server module can also be a {@link TransportManager.tmTypes#CLIENT CLIENT} if it joins a
 * contest.
 * <P>
 * To instanciate a Server:
 * <ol>
 * <li> Use the server {@link #TransportManager(Log, ITwoToOne)} constructor
 * <li> Start the transport listening using {@link #accecptConnections(int)}
 * </ol>
 * <P>
 * To instanciate a Client:
 * <ol>
 * <li> Use the client {@link #TransportManager(Log, String, int, IBtoA)} constructor
 * <li> Contact the server using {@link #connectToMyServer()}
 * </ol>
 * Needless to say the port numbers should be identical. 
 * 
 * @author pc2@ecs.csus.edu
 * 
 */
// $HeadURL: http://pc2.ecs.csus.edu/repos/v9wip/trunk/src/edu/csus/ecs/pc2/core/transport/TransportManager.java$
public class TransportManager implements ITransportManager {
    public static final String SVN_ID = "$Id$";

    /**
     * The type/kind of transport manager.
     * 
     * This identifies the type of an instance of TransportManager.
     * 
     * @author pc2@ecs.csus.edu
     */

    public enum tmTypes {
        /**
         * Always contacts a server.
         * 
         * This is a client, it may be a client (like Team or Admin) or another server which is contacting a remoteServer.
         */
        CLIENT,
        /**
         * Creates a server (listener).
         * 
         * This is a Server, it will create a listener thread for incoming communication packets/requests.
         */
        SERVER
    };
    
    /**
     * Default connection/host port.
     */
    public static final String DEFAULT_PC2_PORT = "50002";

    private static final String PUBLIC_KEY = "PUBLIC_KEY";

    private String myServerIP = "";

    private int myServerPort = 0;

    private Log log = null;

    private ConnectionHandler myConnection = null;

    private Crypto encryptionKeys = null;

    private ConnectionHandlerThreadList connectionHandlerThreadList = null;

    private ConnectionHandlerList serversConnectionHandlerList = null;

    private tmTypes tmType;

    private ITwoToOne appServerCallBack = null;

    private IBtoA appClientCallBack = null;

    /**
     * Server Constructor.
     * 
     * @param log
     * @param appCallBack
     */
    public TransportManager(Log log, ITwoToOne appCallBack) {
        super();
        setEncrytionKeys(new Crypto());
        setConnectionHandlerThreadList(new ConnectionHandlerThreadList());
        setTmType(tmTypes.SERVER);
        setAppServerCallBack(appCallBack);
        setLog(log);
        setServersConnectionHandlerList(new ConnectionHandlerList());
    }

    /**
     * Client Constructor.
     * 
     * @param log
     * @param serverIP
     * @param port
     * @param appCallBack
     */
    public TransportManager(Log log, String serverIP, int port, IBtoA appCallBack) {
        super();
        // constructor for a client
        setMyServerPort(port);
        setMyServerIP(serverIP);
        setEncrytionKeys(new Crypto());
        setTmType(tmTypes.CLIENT);
        setAppClientCallBack(appCallBack);
        setLog(log);
    }

    /**
     * Used by Client to connect to it's server
     * 
     * @throws TransportException
     */
    public void connectToMyServer() throws TransportException {
        getLog().info("Connecting to " + getMyServerIP() + ":" + getMyServerPort());

        ConnectionHandler connectionHandler;
        try {
            connectionHandler = new ConnectionHandler(getLog(), getMyServerIP(), getMyServerPort(), this, getTmType());
            setMyConnection(connectionHandler);

        } catch (TransportException e) {
            getLog().info("Could not ConnectToMyServer()");
            throw new TransportException(e.getMessage());
        }
    }

    /**
     * Used by Servers to connect to other Servers
     * 
     * @param serverIP
     * @param port
     * @return the handler ID
     * @throws TransportException
     */
    public ConnectionHandlerID connectToServer(String serverIP, int port) throws TransportException {
        getLog().info("Connecting to " + serverIP + ":" + port);

        ConnectionHandler connectionHandler;
        try {
            connectionHandler = new ConnectionHandler(getLog(), serverIP, port, this, getTmType());
            return connectionHandler.getConnectionHandlerClientThread().getMyConnectionID();
        } catch (TransportException e) {
            getLog().info("Could not connect to server @ " + serverIP + ":" + port);
            throw new TransportException(e.getMessage());
        }
    }

    /**
     * Servers use this class to initiate listening on a specific port
     * 
     * @param listeningPort
     */
    public void accecptConnections(int listeningPort) throws TransportException {
        getLog().info("accecptConnections on port:" + listeningPort);

        try {
            ConnectionHandler connectionHandler = new ConnectionHandler(getLog(), listeningPort, this);
            getServersConnectionHandlerList().add(connectionHandler.getConnectionHandlerID(), connectionHandler);

            new Thread(connectionHandler).start();

        } catch (Exception e) {
            getLog().info("Could not Accept connections on Port:" + listeningPort);
            throw new TransportException(e.getMessage());
        }
    }

    /**
     * Returns the internal MyServerIP (used by a client)
     */
    private String getMyServerIP() {
        return myServerIP;
    }

    /**
     * Sets MyServerIP to the passed in parameter (used by a client)
     * 
     * @param myServerIP
     */
    private void setMyServerIP(String myServerIP) {
        this.myServerIP = myServerIP;
    }

    /**
     * Returns MyServerPort (used be a client)
     */
    private int getMyServerPort() {
        return myServerPort;
    }

    /**
     * Sets MyServerPort to the passed in parameter (used by client)
     * 
     * @param myServerPort
     */
    private void setMyServerPort(int myServerPort) {
        this.myServerPort = myServerPort;
    }

    /**
     * Returns the Log
     */
    private Log getLog() {
        return log;
    }

    /**
     * Returns encryptionKeys
     */
    private Crypto getEncrytionKeys() {
        return encryptionKeys;
    }

    /**
     * Sets encryptionsKeys to the passed in parameter
     * 
     * @param encrytionKeys
     */
    private void setEncrytionKeys(Crypto encrytionKeys) {
        this.encryptionKeys = encrytionKeys;
    }

    /**
     * Method used to generate a TransportWrapper packet for transmitting the initial Public key exchange
     * 
     * @return the packet
     */
    public TransportWrapper getPublicKeyPacket() {
        getLog().info("Generating Unencrypted Public Key Packet");
        TransportWrapper packet = new TransportWrapper(PUBLIC_KEY, getEncrytionKeys().getPublicKey());
        return packet;
    }

    /**
     * Method called by the lower level ConnectionHandler to pass up the received SealedObject and ConnectionHandlerID to identify
     * the sender. This method decrypts the packet using the local copy of the SecretKey and then makes the appropriate Application
     * callback.
     * 
     * @param transportPacket
     * @param connectionHandlerID
     */
    public void receive(SealedObject transportPacket, ConnectionHandlerID connectionHandlerID) {
        getLog().fine("public void receive(SealedObject TransportPacket, ConnectionHandlerID connectionHandlerID)");
        Serializable incomingMsg = null;

        try {
            incomingMsg = getEncrytionKeys().decrypt(transportPacket, connectionHandlerID.getSecretKey());
        } catch (CryptoException e) {
            e.printStackTrace();
            getLog().info("Could not decypt Packet!");
        }
        if (incomingMsg != null) {
            if (getTmType() == tmTypes.SERVER) {
                final Serializable fIncomingMsg = incomingMsg;
                final ConnectionHandlerID fConnectionHandlerID = connectionHandlerID;
                new Thread(new Runnable() {
                    public void run() {
                        getAppServerCallBack().receiveObject(fIncomingMsg, fConnectionHandlerID);
                    }
                }).start();
            } else {
                final Serializable fIncomingMsg = incomingMsg;
                new Thread(new Runnable() {
                    public void run() {
                        getAppClientCallBack().receiveObject(fIncomingMsg);
                    }
                }).start();
            }
        } else {
            // incoming message was not decrypted successfully
            getLog().info("Failed to Decrypt incoming message from: " + connectionHandlerID);
        }
    }

    /**
     * Method called by the lower level ConnectionHandler to notify the TransportManager that a connection was dropped. This method
     * in turn notifies the Application
     * 
     */
    public void connectionDropped(ConnectionHandlerID myConnectionID) {
        getLog().info("connectionDropped(ConnectionHandlerID myConnectionID) ");

        // UnRegister incoming connection with the Application
        if (getTmType() == tmTypes.SERVER) {
            ConnectionHandlerThread connectionHandlerThread = getConnectionHandlerThreadList().get(myConnectionID);
            if (connectionHandlerThread != null) {
                connectionHandlerThread.shutdownConnection();
                connectionHandlerThread = null;
            }
            getConnectionHandlerThreadList().remove(myConnectionID);
            getAppServerCallBack().connectionDropped(myConnectionID);
        } else {
            ConnectionHandlerClientThread connectionHandlerClientThread = getMyConnection().getConnectionHandlerClientThread();
            if (connectionHandlerClientThread != null) {
                connectionHandlerClientThread.shutdownConnection();
                connectionHandlerClientThread = null;
            }
            getAppClientCallBack().connectionDropped();
        }
    }

    /**
     * Method called by the lower level ConnectionHandler to notify the TransportManager that a connection was dropped. This method
     * in turn notifies the Application
     * 
     */
    public void unregisterConnection(ConnectionHandlerID myConnectionID) {
        getLog().info("unregisterConnection(ConnectionHandlerID myConnectionID) ");

        // UnRegister incoming connection with the Application
        if (getTmType() == tmTypes.SERVER) {
            ConnectionHandlerThread connectionHandlerThread = getConnectionHandlerThreadList().get(myConnectionID);
            connectionHandlerThread.setStillListening(false);
            connectionHandlerThread.shutdownConnection();
            connectionHandlerThread = null;
            getConnectionHandlerThreadList().remove(myConnectionID);
            getAppServerCallBack().connectionDropped(myConnectionID);
        } else {
            getMyConnection().getConnectionHandlerClientThread().setStillListening(false);
            getMyConnection().getConnectionHandlerClientThread().shutdownConnection();
            getMyConnection().setConnectionHandlerClientThread(null);
            getAppClientCallBack().connectionDropped();
        }
    }

    /**
     * Method called by the lower ConnectionHandler to notify the application of an incoming connection.
     * 
     * @param myConnectionID
     * @param thread
     */
    public void registerIncomingConnectionRequest(ConnectionHandlerID myConnectionID, ConnectionHandlerThread thread) {
        getLog().info("registerIncomingConnectionRequest(myConnectionID, thread) ");
        getConnectionHandlerThreadList().add(myConnectionID, thread);
        // Register incoming connection with Server
        getAppServerCallBack().connectionEstablished(myConnectionID);
    }

    /**
     * Returns the connectionHandlerThreadList
     */
    private ConnectionHandlerThreadList getConnectionHandlerThreadList() {
        return connectionHandlerThreadList;
    }

    /**
     * Sets the connectionHandlerThreadList to the passed in parameter
     * 
     * @param connectionHandlerList
     */
    private void setConnectionHandlerThreadList(ConnectionHandlerThreadList connectionHandlerList) {
        this.connectionHandlerThreadList = connectionHandlerList;
    }

    /**
     * Returns the myConnection
     */
    private ConnectionHandler getMyConnection() {
        return myConnection;
    }

    /**
     * Sets myConnection to the passed in parameter
     * 
     * @param myConnection
     */
    private void setMyConnection(ConnectionHandler myConnection) {
        this.myConnection = myConnection;
    }

    /**
     * Public Interface method for Client to send Object to their Server. This method is invoked by the application which passes in
     * a Serilizable Object to be sent to the Server.
     * 
     */
    public void send(Serializable msgObj) throws TransportException {

        final Serializable fMsgObj = msgObj;

        new Thread(new Runnable() {
            public void run() {
                getLog().info("send(Serializable)");

                int busyWait = 0;
                // Wait for connectionHandler to be ready
                while (!getMyConnection().getConnectionHandlerClientThread().getMyConnectionID().isReadyToCommunicate()) {
                    // TODO: Change to Monitor rather than busy wait.
                    busyWait++;
                }

                SecretKey secretKey = getMyConnection().getConnectionHandlerClientThread().getMyConnectionID().getSecretKey();
                SealedObject sealedObject = null;

                try {
                    sealedObject = getEncrytionKeys().encrypt(fMsgObj, secretKey);
                } catch (CryptoException e) {

                    // could not do this because we are not on a thread.
                    // throw new TransportException(e.getMessage());
                    //
                    // Application will have to handle the connectionError
                    // correctly

                    getAppClientCallBack().connectionError(fMsgObj, null, e.getMessage());
                }

                try {
                    getMyConnection().send(sealedObject);
                } catch (Exception e) {
                    // could not do this because we are not on a thread.
                    // throw new TransportException(e.getMessage());
                    //
                    // Application will have to handle the connectionError
                    // correctly

                    getAppClientCallBack().connectionError(fMsgObj, null, e.getMessage());
                }

            }
        }).start();
    }

    /**
     * Public Internet method for Server to send to clients. This method is invoked with two parameters, a Serilizable Object and a
     * connectionHandlerID. The TransportManager encrypts the Object using the specific connectionHandler SecretKey. Then it finds
     * the appropriate thread and invokes the Send method on that Thread.
     * 
     * @throws TransportException
     * 
     */
    public void send(Serializable msgObj, ConnectionHandlerID connectionHandlerID) throws TransportException {
        final Serializable fMsgObj = msgObj;
        final ConnectionHandlerID fConnectionHandlerID = connectionHandlerID;

        new Thread(new Runnable() {
            public void run() {

                getLog().info("send(Serializable, ConnectionHandlerID) to " + fConnectionHandlerID);

                int busyWait = 0;
                // Wait for connectionHandler to be ready
                while (!fConnectionHandlerID.isReadyToCommunicate()) {
                    // intentional empty
                    // TODO: Change to Monitor rather than Busywait
                    busyWait++;
                }

                SecretKey secretKey = fConnectionHandlerID.getSecretKey();
                SealedObject sealedObject = null;
                try {
                    sealedObject = getEncrytionKeys().encrypt(fMsgObj, secretKey);
                } catch (CryptoException e) {
                    // could not do this because we are not on a thread.
                    // throw new TransportException(e.getMessage());
                    //
                    // Application will have to handle the connectionError
                    // correctly

                    getAppClientCallBack().connectionError(fMsgObj, fConnectionHandlerID, e.getMessage());
                }
                try {
                    getConnectionHandlerThreadList().get(fConnectionHandlerID).send(sealedObject);
                } catch (Exception e) {
                    // could not do this because we are not on a thread.
                    // throw new TransportException(e.getMessage());
                    //
                    // Application will have to handle the connectionError
                    // correctly

                    getAppClientCallBack().connectionError(fMsgObj, fConnectionHandlerID, e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Receive Unencrypted packet from otherModule & Generate SecretKey for this connection.
     * 
     * @param wrapper
     * @param myConnectionID
     */
    protected void receiveUnencrypted(TransportWrapper wrapper, ConnectionHandlerID myConnectionID) {
        getLog().info("receiveUnencrypted(TransportWrapper, ConnectionHandlerID)");
        getLog().info(myConnectionID.toString());
        getLog().info("");

        PublicKey pk = (PublicKey) wrapper.get(PUBLIC_KEY);
        SecretKey tmpKey = getEncrytionKeys().generateSecretKey(pk, getEncrytionKeys().getPrivateKey());
        getEncrytionKeys().setSecretKey(tmpKey);

        myConnectionID.setSecretKey(tmpKey);
        myConnectionID.setReadyToCommunicate(true);

        getLog().info("Made a secret key " + tmpKey.toString());
    }

    /**
     * Returns the tmType
     * 
     * @return the tmType
     */
    protected tmTypes getTmType() {
        return tmType;
    }

    /**
     * Sets the tmType to the passed in parameter. The Server Constructor sets this variable to tmTypes.SERVER and the Client
     * Constructor sets this variable to tmTypes.CLIENT.
     * 
     * @param tmType
     */
    protected void setTmType(tmTypes tmType) {
        this.tmType = tmType;
    }

    /**
     * Returns the Server Application Callback
     */
    private ITwoToOne getAppServerCallBack() {
        return appServerCallBack;
    }

    /**
     * Sets the Server Application callback to the passed in parameter
     * 
     * @param appCallBack
     */
    private void setAppServerCallBack(ITwoToOne appCallBack) {
        this.appServerCallBack = appCallBack;
    }

    /**
     * Returns the Client Application Callback
     */
    private IBtoA getAppClientCallBack() {
        return appClientCallBack;
    }

    /**
     * Sets the Client Applcation callback tot he passed in parameter
     * 
     * @param appClientCallBack
     */
    private void setAppClientCallBack(IBtoA appClientCallBack) {
        this.appClientCallBack = appClientCallBack;
    }

    /**
     * Sets the log to the passed in parameter
     * 
     * @param log
     */
    private void setLog(Log log) {
        this.log = log;
    }

    /**
     * Returns the serversConnectionHandlerList
     */
    private ConnectionHandlerList getServersConnectionHandlerList() {
        return serversConnectionHandlerList;
    }

    /**
     * Sets the serversConnectionHandlerList to the passed in parameter
     * 
     * @param serversConnectionHandlerList
     */
    private void setServersConnectionHandlerList(ConnectionHandlerList serversConnectionHandlerList) {
        this.serversConnectionHandlerList = serversConnectionHandlerList;
    }

}
