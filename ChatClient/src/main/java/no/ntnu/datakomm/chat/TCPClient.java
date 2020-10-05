package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.*;
import java.util.*;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;

    public static void main(String[] args) {
        TCPClient c = new TCPClient();
         if( c.connect("datakomm.work", 1300)) {
             System.out.println("ss");
         }
         if( c.isConnectionActive()) {
             System.out.println("brr");
         }
        c.disconnect();
         if( !c.isConnectionActive()) {
             System.out.println("skskssk");
         }
    }

    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port) {
        try {
            connection = new Socket(host, port);
            toServer = new PrintWriter(connection.getOutputStream(), true);
            fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect() {
        if (isConnectionActive()) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.connection = null;
        }
        // TODO Step 4: implement this method (Maybe done)
        // Hint: remember to check if connection is active
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        return connection != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        if (isConnectionActive()) {
            toServer.println(cmd);
            return true;
        } else {
            return false;
        }
        // TODO Step 2: Implement this method (Maybe Done)
        // Hint: Remember to check if connection is active
    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        String cmd = "msg " + message;
        return sendCommand(cmd);

        // TODO Step 2: implement this method (Maybe done)
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        String cmd = "login " + username;
        sendCommand(cmd);
        // TODO Step 3: implement this method (Maybe done)
        // Hint: Reuse sendCommand() method
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        String cmd = "users";
        sendCommand(cmd);

        // TODO Step 5: implement this method (Maybe done)
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        String cmd = "privmsg " + recipient + " " + message;
        return sendCommand(cmd);
        // TODO Step 6: Implement this method (Maybe Done)
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        sendCommand("help");
        // TODO Step 8: Implement this method (Maybe done)
        // Hint: Reuse sendCommand() method
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        try {
            String response = fromServer.readLine();
            if (response == null) {
                throw new IOException("null response");
            }
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
            onDisconnect();
        }
        return null;
        // TODO Step 3: Implement this method (Maybe done)
        // TODO Step 4: If you get I/O Exception or null from the stream, it means that something has gone wrong (maybe done)
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.
    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError() {
        if (lastError != null) {
            return lastError;
        } else {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread() {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() -> {
            parseIncomingCommands();
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands() {
        while (isConnectionActive()) {
            String incomingCmd = waitServerResponse();
            if( incomingCmd == null) {
                break;
            }
            String command = "";
            String firstHandInformation = "";
            String secondHandInformation = "";

            String[] arr = incomingCmd.split(" ", 3);
            command = arr[0];

            if(arr.length>2) {
                secondHandInformation = arr[2];
                firstHandInformation = arr[1] + " " + arr[2];
            }
            else if(arr.length>1)
                firstHandInformation = arr[1];

            switch (command) {
                case "loginok":
                    onLoginResult(true,"");
                    System.out.println(command);
                    break;
                case "loginerr":
                    onLoginResult(false, firstHandInformation);
                    break;
                case "users":
                    String[] userList;
                    userList = firstHandInformation.split(" ");
                    onUsersList(userList);
                    break;
                case "msg":
                    onMsgReceived(false, arr[1], secondHandInformation);
                    break;
                case "privmsg":
                    onMsgReceived(true, arr[1], secondHandInformation);
                    break;
                case "msgok":
                    System.out.println("msgok");
                    break;
                case "msgerr":
                    onMsgError(firstHandInformation);
                    break;
                case "cmderr":
                    onCmdError(firstHandInformation);
                    break;
                case "supported":
                    onSupported(firstHandInformation.split(" "));
                    break;
                case "modeok":
                    System.out.println("modeok");
                    //Nothing needs to be done!
                    break;
                case "inbox":
                    //TODO
                    break;
            }
            // TODO Step 3: Implement this method (maybe done)
            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method

            // TODO Step 5: update this method, handle user-list response from the server (maybe done)
            // Hint: In Step 5 reuse onUserList() method

            // TODO Step 7: add support for incoming chat messages from other users (types: msg, privmsg) (maybe done)
            // TODO Step 7: add support for incoming message errors (type: msgerr) (maybe done)
            // TODO Step 7: add support for incoming command errors (type: cmderr) (maybe done)
            // Hint for Step 7: call corresponding onXXX() methods which will notify all the listeners

            // TODO Step 8: add support for incoming supported command list (type: supported) (maybe done)
        }
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(ChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *
     * @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg) {
        for (ChatListener l : listeners) {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect() {
        for (ChatListener listener : listeners) {
            listener.onDisconnect();
        }
        // TODO Step 4: Implement this method (Maybe done)
        // Hint: all the onXXX() methods will be similar to onLoginResult()
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        for (ChatListener listener : listeners) {
          listener.onUserList(users);
        }
        // TODO Step 5: Implement this method (Maybe done)
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text) {
        for (ChatListener listener : listeners) {
            TextMessage msg = new TextMessage(sender, priv, text);
            listener.onMessageReceived(msg);
        }
        // TODO Step 7: Implement this method (Maybe done)
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        for (ChatListener listener : listeners) {
            listener.onMessageError(errMsg);
        }

        // TODO Step 7: Implement this method (Maybe done)
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        for (ChatListener listener : listeners) {
            listener.onCommandError(errMsg);
        }
        // TODO Step 7: Implement this method (Maybe done)
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {
        for(ChatListener listener : listeners) {
           listener.onSupportedCommands(commands);
        }
        // TODO Step 8: Implement this method (Maybe done)
    }
}
