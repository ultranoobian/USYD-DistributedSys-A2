import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerInfoList {

    ArrayList<ServerInfo> serverInfos;
    private String regex;

    enum configLineType {NUM, HOST, PORT}

    Dictionary<Pattern, configLineType> configMatchingLibrary = new Hashtable();


    public ServerInfoList() {
        serverInfos = new ArrayList<>();

        Pattern numConfigPattern = Pattern.compile("[sS]ervers\\.num\\s*=\\s*\\d+");
        Pattern hostConfigPattern = Pattern.compile("[sS]erver\\d+\\.(host\\s*=\\s*((localhost)|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)))");
        Pattern hostPortPattern = Pattern.compile("[sS]erver\\d+\\.port\\s*=\\s*\\d+");

        configMatchingLibrary.put(numConfigPattern, configLineType.NUM);
        configMatchingLibrary.put(hostConfigPattern, configLineType.HOST);
        configMatchingLibrary.put(hostPortPattern, configLineType.PORT);

    }

    public void initialiseFromFile(String filename) {
        List<String> lines = getLinesFromFile(filename);
        if (lines == null) return;
        regex = "([sS]erver[s|\\d]+)\\.((host\\s*=\\s*((localhost)|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)))|(port\\s*=\\s*\\d+)|num\\s*=\\s*\\d+)";
        FilterLines(Pattern.compile(
                regex
        ), lines);

        // Process each line
        Iterator<String> lineIterator = lines.iterator();
        ArrayList<ServerInfo> servers = processConfig(lineIterator, configMatchingLibrary);


    }

    private List<String> getLinesFromFile(String filename) {
        List<String> lines = new ArrayList<>();

        try {
            Path path = Paths.get(filename);
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            // Continue with empty list
            e.printStackTrace();
            return null;
        }
        return lines;
    }

    private static void FilterLines(Pattern filterPattern, List<String> lines) {
        // Remove all empty lines & non-matching patterns
        lines.removeAll(Collections.singleton(""));
        lines.removeIf(s -> !filterPattern.matcher(s).lookingAt());
    }

    private ArrayList<ServerInfo> processConfig(Iterator<String> lineIterator, Dictionary<Pattern, configLineType> configMatchingLibrary) {
        int configLength = 0;
        ArrayList<Pattern> comparativeTestPatterns = Collections.list(configMatchingLibrary.keys());
        HashMap<Integer, ServerInfo> processedList = new HashMap<Integer, ServerInfo>();
        configLineType nextExpectedType = null;
        int serverIndexNumber = -1;
        String hostname = null;
        int portNumber = -1;

        Pattern hostPattern = Pattern.compile("((localhost)|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))");
        Pattern portNumberPattern = Pattern.compile("=(?<port>\\d+)\\b");
        Pattern serverIdPattern = Pattern.compile("[sS]erver(?<digit>[\\d]+)");

        while (lineIterator.hasNext()) {
            String line = lineIterator.next();

            // Determine the configLineType of the current line
            configLineType type = getConfigLineType(configMatchingLibrary, comparativeTestPatterns, line);

            // If we expected a PORT line next, but didn't get one
            // Null the serverInfo object.
            if (nextExpectedType == configLineType.PORT && type != configLineType.PORT) {
                // TODO: Handle expected port but go otherwise
            }

            // Process individually based on identified configLineType
            Matcher mp;
            switch (type) {
                case NUM:
                    Pattern quantityPattern = Pattern.compile("(\\d)+\\b");
                    mp = quantityPattern.matcher(line);
                    mp.reset();
                    if (mp.find()) {
                        String temp = mp.group();
                        configLength = Integer.parseInt(temp);
                    }
                    nextExpectedType = null;
                    break;

                case HOST:
                    // Determine if there is an incomplete server entry (-1)
                    if (serverIndexNumber != -1) {
                        // Then fill with null instead
                        processedList.put(serverIndexNumber, null);

                        // Reset for new entry
                        serverIndexNumber = -1;
                        hostname = null;
                        portNumber = -1;
                    }

                    // First find the server index value
                    mp = serverIdPattern.matcher(line);
                    mp.reset();
                    if (mp.find()) {
                        try {
                            serverIndexNumber = Integer.parseInt(mp.group("digit"));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    // Second: Find the value
                    mp = hostPattern.matcher(line);
                    mp.reset();
                    if (mp.find()) {
                        hostname = mp.group();
                        if (processedList.containsKey(serverIndexNumber) && processedList.get(serverIndexNumber) != null) {
                            processedList.get(serverIndexNumber).setHost(hostname);
                            serverIndexNumber = -1;
                            hostname = null;
                        } else {

                            // Setup to expect a PORT line.
                            nextExpectedType = configLineType.PORT;
                        }
                    }

                    break;
                case PORT:
                    mp = serverIdPattern.matcher(line);
                    mp.reset();
                    if (mp.find()) {
                        try {
                            serverIndexNumber = Integer.parseInt(mp.group("digit"));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }

                    mp = portNumberPattern.matcher(line);
                    mp.reset();
                    if (mp.find()) {
                        try {
                            portNumber = Integer.parseInt(mp.group("port"));

                            // Check if update or new?
                            if (processedList.containsKey(serverIndexNumber) && processedList.get(serverIndexNumber) != null) {
                                if (validatePortNumber(portNumber)) {
                                    // Update the port number
                                    processedList.get(serverIndexNumber).setPort(portNumber);
                                } else {
                                    //Nullify the entry
                                    processedList.put(serverIndexNumber, null);
                                }

                            } else { // New
                                if (validatePortNumber(portNumber)) {
                                    // Valid, new entry
                                    processedList.put(serverIndexNumber, new ServerInfo(hostname, portNumber));
                                } else {
                                    // new entry, bad value, nullify
                                    processedList.put(serverIndexNumber, null);
                                }
                            }
                            serverIndexNumber = -1;
                            portNumber = -1;

                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }


                    nextExpectedType = null;
                    serverIndexNumber = -1;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + type);
            }
        }

        ArrayList<ServerInfo> finalList = new ArrayList<>();
        for (int i = 0; i < configLength; i++) {
            finalList.add(processedList.get(i));
            this.serverInfos.add(processedList.get(i));
        }

        return finalList;
    }

    private configLineType getConfigLineType(Dictionary<Pattern, configLineType> configMatchingLibrary, ArrayList<Pattern> comparativeTestPatterns, String line) {
        // Check against all known test patterns
        for (Pattern pattern : comparativeTestPatterns) {
            if (pattern.matcher(line).lookingAt()) {
                return configMatchingLibrary.get(pattern);
            }
        }
        return null;
    }

    private boolean validatePortNumber(int portNumber) {
        if (portNumber >= 1024 && portNumber <= 65535) {
            return true;
        }
        return false;
    }

    public ArrayList<ServerInfo> getServerInfos() {
        return serverInfos;
    }

    public void setServerInfos(ArrayList<ServerInfo> serverInfos) {
        this.serverInfos = serverInfos;
    }

    public boolean addServerInfo(ServerInfo newServerInfo) {
        return this.serverInfos.add(newServerInfo);

    }

    public boolean updateServerInfo(int index, ServerInfo newServerInfo) {
        if (index >= 0 && index < this.serverInfos.size()) {
            this.serverInfos.get(index).setHost(newServerInfo.getHost());
            this.serverInfos.get(index).setPort(newServerInfo.getPort());
            return true;
        } else {
            return false;
        }
    }

    public boolean removeServerInfo(int index) {
        if (index >= 0 && index < this.serverInfos.size()) {
            this.serverInfos.set(index, null);
            return true;
        } else {
            return false;
        }
    }

    public boolean clearServerInfo() {
        ArrayList<ServerInfo> newList = new ArrayList<>();
        for (int i = 0; i < serverInfos.size(); i++) {
            if (serverInfos.get(i) != null) {
                newList.add(serverInfos.get(i));
            }
        }
        setServerInfos(newList);
        return true;
    }

    public String toString() {
        String s = "";
        for (int i = 0; i < serverInfos.size(); i++) {
            if (serverInfos.get(i) != null) {
                s += "Server" + i + ": " + serverInfos.get(i).getHost() + " " + serverInfos.get(i).getPort() + "\n";
            }
        }
        return s;
    }

    // implement any helper method here if you need any

}