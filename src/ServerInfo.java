import java.util.regex.Pattern;

public class ServerInfo {

    private String host;
    private int port;

    public ServerInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    // implement any helper method here if you need any
    public static boolean validateConfiguration(String hostname, int portnum) {
//        Pattern hostnamePattern = Pattern.compile("((localhost)|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))");
//        if (hostnamePattern.matcher(hostname).find() && portnum >= 1024 && portnum <= 65535) {
//            return true;
//        }
//        return false;
        return (validateHostname(hostname) && validatePortNum(portnum));
    }

    public static boolean validateHostname(String hostname) {
        Pattern hostnamePattern = Pattern.compile("((localhost)|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))");
        return hostnamePattern.matcher(hostname).find();
    }

    public static boolean validatePortNum(int portnum) {
        return portnum >= 1024 && portnum <= 65535;
    }
}
