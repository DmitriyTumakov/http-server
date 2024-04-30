package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    Server server = new Server(validPaths);
    server.addHandler("GET", "/resources.html", new Handler() {
      @Override
      public void handle(Request request, BufferedOutputStream bos) {
        System.out.println("Test handler");
        try {
          final var filePath = Path.of(".", "public", request.getTitle());
          final var mimeType = Files.probeContentType(filePath);
          final var length = Files.size(filePath);
          bos.write((
                  "HTTP/1.1 200 OK\r\n" +
                          "Content-Type: " + mimeType + "\r\n" +
                          "Content-Length: " + length + "\r\n" +
                          "Connection: close\r\n" +
                          "\r\n"
          ).getBytes());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    server.start();
  }
}


