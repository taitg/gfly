import java.net.Socket;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;

public class WebServer extends Thread {

  private volatile boolean shutdown = false;
  private DeviceController controller;
  private int port;

  public WebServer(DeviceController controller, int port) {
    this.controller = controller;
    this.port = port;
  }

  public void runServer() {
    start();
    if (Config.verbose)
      System.out.println("WebServer: started");
  }

  public void run() {
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(port);
      serverSocket.setSoTimeout(1000);
      while (!shutdown) {
        try {
          Socket clientSocket = serverSocket.accept();
          WebServerWorker worker = new WebServerWorker(clientSocket);
          (new Thread(worker)).start();
        } catch (SocketTimeoutException e) {
          // do nothing, this is OK
          // allows the process to check the shutdown flag
        }
      }
      serverSocket.close();
    } catch (IOException e) {

    }
  }

  public void shutdown() {
    shutdown = true;
  }

  public class WebServerWorker implements Runnable {

    private Socket socket = null;

    public WebServerWorker(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      boolean isBadRequest = false;
      boolean isNotFound = false;
      byte[] bytes = new byte[16384];
      String fileName = "";
      String output = "";

      try {
        // read and parse HTTP request
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        OutputStream out = socket.getOutputStream();
        String request = in.readLine();
        if (request == null)
          return;

        // ensure request is well formed
        String[] reqSplit = request.split("/");
        if (reqSplit.length != 3) {
          isBadRequest = true;
        } else {
          // check the HTTP method
          if (!reqSplit[0].trim().equals("GET") && !reqSplit[0].trim().equals("POST"))
            isBadRequest = true;

          // check if the request is well-formed
          String[] fileNameSplit = reqSplit[1].split(" ");
          if (fileNameSplit.length != 2) {
            isBadRequest = true;
          } else {
            if (!fileNameSplit[1].trim().equals("HTTP"))
              isBadRequest = true;

            fileName = fileNameSplit[0];

            // if request is valid and not a file, generate HTML to output
            if (fileName.equals("") || fileName.equals("index.html") || fileName.startsWith("make?"))
              output = generateOutput(fileName);
          }
        }

        // determine if requested object exists, unless we are transmitting HTML
        File f = new File(fileName);
        if (!f.exists() && output.equals(""))
          isNotFound = true;

        // transmit content over existing connection
        // first send HTTP header
        String header = generateHTTPHeader(isBadRequest, isNotFound, f);
        out.write(header.getBytes("US-ASCII"));
        out.flush();

        // send file if OK and applicable
        if (!isBadRequest && !isNotFound && output.equals("")) {
          FileInputStream fStream = new FileInputStream(f);
          BufferedInputStream fBuffer = new BufferedInputStream(fStream);
          int n;

          while ((n = fBuffer.read(bytes)) > 0) {
            out.write(bytes, 0, n);
            out.flush();
          }

          fBuffer.close();
          fStream.close();
        }

        // otherwise send output string if OK
        else if (!isBadRequest && !output.equals("")) {
          out.write(output.getBytes("US-ASCII"));
          out.flush();
        }

        // close connection
        in.close();
        out.close();
        socket.close();
      } catch (IOException e) {
        System.err.println("Worker thread exception: " + e.toString());
      }
    }

    private String generateHTTPHeader(boolean isBadRequest, boolean isNotFound, File f) {
      String header = "HTTP/1.1 ";

      // response code
      if (isBadRequest)
        header += "400 Bad Request\r\n";
      else if (isNotFound)
        header += "404 Not Found\r\n";
      else
        header += "200 OK\r\n";

      // server name and version
      header += "Server: GFLY/1.0\r\n";

      // file info if applicable
      if (!isBadRequest && !isNotFound && f.exists()) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
        header += "Last-Modified: " + sdf.format(f.lastModified()) + "\r\n";
        header += "Content-Length: " + f.length() + "\r\n";
      }

      header += "Connection: close\r\n\r\n";
      return header;
    }

    private String generateOutput(String request) {
      return "<html><div>vario audio: " + (Config.varioAudioOn == true ? "on" : "off") + "</div></html>";
    }
  }
}