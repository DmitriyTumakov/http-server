package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.AttributedCharacterIterator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public ExecutorService threadPool = Executors.newFixedThreadPool(64);
    private final List<String> validPaths;
    private Map<String, HashMap<String, Handler>> methodMap = new HashMap<>();


    public Server(List<String > validPaths) {
        this.validPaths = validPaths;
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(9999)) {
            serverAccept(serverSocket);
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        for (var methodKey : methodMap.keySet()) {
            if (method.equals(methodKey)) {
                for (var titleKey : methodMap.get(methodKey).keySet()) {
                    if (path.equals(titleKey)) {
                        methodMap.get(methodKey).replace(titleKey, handler);
                        return;
                    }
                }
                methodMap.get(methodKey).put(path, handler);
                return;
            }
        }
        methodMap.put(method, new HashMap<>());
        methodMap.get(method).put(path, handler);
    }

    private void serverAccept(ServerSocket serverSocket) throws ExecutionException, InterruptedException {
        while (true) {
            try {
                threadPool.submit(() -> {
                    while (true) {
                        try (
                                final var socket = serverSocket.accept();
                                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                final var out = new BufferedOutputStream(socket.getOutputStream())
                        ) {
                            // read only request line for simplicity
                            // must be in form GET /path HTTP/1.1
                            final var requestLine = in.readLine();
                            final var parts = requestLine.split(" ");

                            if (parts.length != 3) {
                                // just close socket
                                continue;
                            }

                            final var path = parts[1];
                            final var method = parts[0];
                            Request request = new Request(method, path);

                            if (!validPaths.contains(path)) {
                                out.write((
                                        "HTTP/1.1 404 Not Found\r\n" +
                                                "Content-Length: 0\r\n" +
                                                "Connection: close\r\n" +
                                                "\r\n"
                                ).getBytes());
                                out.flush();
                                continue;
                            }

                            final var filePath = Path.of(".", "public", path);
                            final var mimeType = Files.probeContentType(filePath);

                            // special case for classic
                            if (path.equals("/classic.html")) {
                                final var template = Files.readString(filePath);
                                final var content = template.replace(
                                        "{time}",
                                        LocalDateTime.now().toString()
                                ).getBytes();
                                out.write((
                                        "HTTP/1.1 200 OK\r\n" +
                                                "Content-Type: " + mimeType + "\r\n" +
                                                "Content-Length: " + content.length + "\r\n" +
                                                "Connection: close\r\n" +
                                                "\r\n"
                                ).getBytes());
                                out.write(content);
                                out.flush();
                                continue;
                            }

                            final var length = Files.size(filePath);
                            if (!methodMap.isEmpty()) {
                                for (var methodKey : methodMap.keySet()) {
                                    if (method.equals(methodKey)) {
                                        for (var titleKey : methodMap.get(methodKey).keySet()) {
                                            if (path.equals(titleKey)) {
                                                methodMap.get(methodKey).get(titleKey).handle(request, out);
                                                out.flush();
                                            }
                                        }
                                    }
                                }
                            }

                            out.write((
                                    "HTTP/1.1 200 OK\r\n" +
                                            "Content-Type: " + mimeType + "\r\n" +
                                            "Content-Length: " + length + "\r\n" +
                                            "Connection: close\r\n" +
                                            "\r\n"
                            ).getBytes());
                            Files.copy(filePath, out);
                            out.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
