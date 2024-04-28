package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public ExecutorService threadPool = Executors.newFixedThreadPool(64);
    private final List<String> validPaths;
    private final Map<String, Map<String, Handler>> methodMap = new HashMap<>();

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
        methodMap.put(method, (Map<String, Handler>) new HashMap<String, Handler>().put(path, handler));
    }

    private void serverAccept(ServerSocket serverSocket) throws ExecutionException, InterruptedException {
        while (true) {
            try {
                threadPool.submit(() -> {
                    while (true) {
                        try (
                                final var socket = serverSocket.accept();
                                final var in = new BufferedInputStream(socket.getInputStream());
                                final var out = new BufferedOutputStream(socket.getOutputStream())
                        ) {
                            // read only request line for simplicity
                            // must be in form GET /path HTTP/1.1

                            final var limit = 4096;
                            in.mark(limit);
                            final var buffer = new byte[limit];
                            final var read = in.read(buffer);

                            final var requestLineDelimiter = new byte[]{'\r', '\n'};
                            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

                            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                            final var headersStart = requestLineEnd + requestLineDelimiter.length;
                            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                            in.reset();
                            in.skip(headersStart);

                            final var headersBytes = in.readNBytes(headersEnd - headersStart);
                            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

                            final var pathWithQuery = requestLine[1];
                            final var method = requestLine[0];
                            final var pathArray = pathWithQuery.split("\\?");
                            final var path = pathArray[0];

                            String body = null;
                            if (!method.equals("GET")) {
                                in.skip(headersDelimiter.length);
                                final var contentLength = extractHeader(headers, "Content-Length");
                                if (contentLength.isPresent()) {
                                    final var length = Integer.parseInt(contentLength.get());
                                    final var bodyBytes = in.readNBytes(length);
                                    body = new String(bodyBytes);
                                }
                            }

                            Request request = null;
                            if (!method.equals("GET")) {
                                request = new Request(method, pathWithQuery, body);
                            } else {
                                request = new Request(method, pathWithQuery);
                            }

                            System.out.println(request.getQueryParams());

                            if (!validPaths.contains(pathWithQuery)) {
                                out.write((
                                        "HTTP/1.1 404 Not Found\r\n" +
                                                "Content-Length: 0\r\n" +
                                                "Connection: close\r\n" +
                                                "\r\n"
                                ).getBytes());
                                out.flush();
                                continue;
                            }

                            final var filePath = Path.of(".", "public", pathWithQuery);
                            final var mimeType = Files.probeContentType(filePath);
                            request.addMimeType(mimeType);

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

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
