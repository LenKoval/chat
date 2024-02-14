package ru.home.kovaleva;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Server {
    private ServerSocket serverSocket;
    private int port;
    private List<ClientHandler> clients;
    private final AuthenticationProvider authenticationProvider;
    private static final Logger logger = LogManager.getLogger(Server.class.getName());

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.authenticationProvider = authenticationProvider;
    }

    public synchronized void subscribe(ClientHandler client) {
        clients.add(client);
        broadcastMessage(client.getUsername() + " вошел в чат");
        logger.info("Клиент " + client.getUsername() + " вошел в чат.");
    }

    public synchronized void unsubscribe(ClientHandler client) {
        clients.remove(client);
        logger.info("Клиент " + client.getUsername() + " вышел из чата.");
        broadcastMessage(client.getUsername() + " вышел из чата.");
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void pointToPoint(ClientHandler from, String message) {
        String[] data = message.split(" ", 3);
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(data[1])) {
                client.sendMessage("сообщение от " + from.getUsername() + " : " + data[2]);
                // обратная связь для отправителя
                logger.info(from.getUsername() + " отправил личное сообщение " + client.getUsername());
                // сделать return
            }
        }
        // написать отправителю если клиента не существует
    }

    public synchronized List<String> getUsernameList() {
        return clients.stream().map(ClientHandler::getUsername).collect(Collectors.toList());
    }

    public synchronized boolean changeNick(String[] data) { // сделать аргументы, пароль убрать
        ClientHandler changeNick = null;
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(data[3])) { // сделать проверку на уровне базы только
                return false;
            } else if (client.getUsername().equals(data[1])) {
                changeNick = client;
            }
        }
        logger.info("Клиент " + changeNick.getUsername() + " изменил ник на " + data[3]);
        changeNick.setUsername(data[3]);
        changeNick.sendMessage("Вы успешно изменили ник на " + changeNick.getUsername());
        return true;
    }

    public synchronized void kickUser(String message) {
        String[] data = message.split(" ", 2);
        ClientHandler kickedClient = null;
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(data[1])) {
                kickedClient = client;
            }
        }
        if (kickedClient != null) {
            logger.info("Клиент " + kickedClient.getUsername() + " удален из чата Администратором.");
            kickedClient.sendMessage("Вы удалены из чата.");
            kickedClient.disconnect();
        }
    }

    public synchronized void banClient(String message) {
        String[] data = message.split(" ");
        getAuthenticationProvider().changeBanUser(message);
        if (data[2].equals("true")) {
            ClientHandler bannedUser = null;
            for (ClientHandler client : clients) {
                if (client.getUsername().equals(data[1]));
                bannedUser = client;
            }
            if (bannedUser != null) {
                logger.info("Клиент " + bannedUser.getUsername() + " заблокирован Администратором.");
                bannedUser.sendMessage("Вы заблокированы Администратором.");
                bannedUser.disconnect();
            }
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("Сервер запущен на порту " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            stop();
        }
    }

    public void stop() {
        broadcastMessage("Соединение с сервером прервано. Попоробуте подлючиться позже.");
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).disconnect();
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
