import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockchainClient {

    enum CommandType {LIST, ADD, REMOVE, UPDATE, CLEAR, TX, PB, SD}

    public static void main(String[] args) {
        HashMap<CommandType, Pattern> commandPatterns = new HashMap<>();

        // Specific matching patterns for multi-component CommandType
        // Singular patterns are also added for completeness
        Pattern patternCommandList = Pattern.compile("^ls");
        Pattern patternCommandAdd = Pattern.compile("^(?<command>ad)\\|(?<hostname>(localhost)|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))\\|(?<portnum>\\d+)");
        Pattern patternCommandRemove = Pattern.compile("^rm\\|(?<index>-*\\d+)");
        Pattern patternCommandUpdate = Pattern.compile("^up\\|(?<index>\\d+)\\|(?<hostname>(((localhost)|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))))\\|(?<portnum>\\d+)");
        Pattern patternCommandClear = Pattern.compile("^cl");
        Pattern patternCommandTX = Pattern.compile("^tx\\|(?<user>[A-z]{4}\\d{4})\\|(?<message>.+)");
        Pattern patternCommandPB = Pattern.compile("^pb");
        Pattern patternCommandSD = Pattern.compile("^sd");

        // Make our collection to selected pattern based on command type
        commandPatterns.put(CommandType.LIST, patternCommandList);
        commandPatterns.put(CommandType.ADD, patternCommandAdd);
        commandPatterns.put(CommandType.REMOVE, patternCommandRemove);
        commandPatterns.put(CommandType.UPDATE, patternCommandUpdate);
        commandPatterns.put(CommandType.CLEAR, patternCommandClear);
        commandPatterns.put(CommandType.TX, patternCommandTX);
        commandPatterns.put(CommandType.PB, patternCommandPB);
        commandPatterns.put(CommandType.SD, patternCommandSD);


        if (args.length != 1) {
            return;
        }
        String configFileName = args[0];

        ServerInfoList pl = new ServerInfoList();
        pl.initialiseFromFile(configFileName);

        Scanner sc = new Scanner(System.in);

        Pattern commandPatternTemplate = Pattern.compile("^(?<command>[a-z]{2})((\\|)+.*)*$");
        String commandMessage = "";
        while (true) {

            String message = sc.nextLine();
            Matcher mp = commandPatternTemplate.matcher(message);
            if (mp.find()) {
                commandMessage = mp.group("command");
            } else {
                commandMessage = "";
            }
            switch (commandMessage) {
                case "ls":
                    System.out.print(pl.toString() + '\n');
                    break;

                case "ad":
                    mp = commandPatterns.get(CommandType.ADD).matcher(message);
                    if (mp.find()) {
                        try {
                            String hostname = mp.group("hostname");
                            int portnum = Integer.parseInt(mp.group("portnum"));

                            if (ServerInfo.validateConfiguration(hostname, portnum)) {
                                if (pl.addServerInfo(new ServerInfo(hostname, portnum))) {
                                    defSuccess();
                                } else defFailure();
                            } else {
                                defError();
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        defError();
                    }
                    break;

                case "rm":
                    mp = commandPatterns.get(CommandType.REMOVE).matcher(message);
                    if (mp.find()) {
                        try {
                            int index = Integer.parseInt(mp.group("index"));

                            if (pl.removeServerInfo(index)) {
                                defSuccess();
                            } else {
                                defFailure();
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        defError();
                    }
                    break;
                case "up":
                    mp = commandPatterns.get(CommandType.UPDATE).matcher(message);
                    if (mp.find()) {
                        try {
                            int index = Integer.parseInt(mp.group("index"));
                            String hostname = mp.group("hostname");
                            int portnum = Integer.parseInt(mp.group("portnum"));
                            if (ServerInfo.validateConfiguration(hostname, portnum)
                                    && pl.updateServerInfo(index, new ServerInfo(hostname, portnum))) {
                                defSuccess();
                            } else {
                                defFailure();
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        defError();
                    }

                    break;
                case "cl":
                    if (pl.clearServerInfo()) {
                        defSuccess();
                    } else defFailure();

                    break;
                case "tx":
                    // TODO : This is a broadcast command
                    // TODO implement unicast
                    //throw new UnsupportedOperationException("Not yet implemented!");
                    mp = commandPatterns.get(CommandType.TX).matcher(message);
                    if (mp.find()) {

                    }

                case "pb":
                    // This is a unicast/broadcast/multicast command
                    // TODO:
                    //  -Implement unicast
                    //  -Implement broadcast
                    //  -Implement multicast

                    break;
                case "sd":
                    return;
                default:
                    defError();

                    break;
            }
        }

    }

    public void unicast(int serverNumber, ServerInfo p, String message) {
        BlockchainClientRunnable ncs = new BlockchainClientRunnable(serverNumber, p.getHost(), p.getPort(), message);
        Future future = BlockchainClientRunnable.executor.submit(ncs);
        while (!future.isDone()) {
        }
        System.out.print(ncs.getReply());
        return;
    }

    public void broadcast(ServerInfoList pl, String message) {
        ArrayList<ServerInfo> list = pl.getServerInfos();
        for (int i = 0; i < list.size(); i++) {
            unicast(i, list.get(i), message);
        }
    }

    public void multicast(ServerInfoList serverInfoList, ArrayList<Integer> serverIndices, String message) {
        ArrayList<ServerInfo> list = serverInfoList.getServerInfos();
        for (Integer i : serverIndices) {
            unicast(i, list.get(i), message);
        }
    }

    // implement any helper method here if you need any

    public void clientHandler(InputStream serverInputStream, OutputStream serverOutputStream) throws IOException {
        BufferedReader inputReader = new BufferedReader(
                new InputStreamReader(serverInputStream));
        PrintWriter outWriter = new PrintWriter(serverOutputStream, true);

        Scanner sc = new Scanner(System.in);
        int newLineCount = 0;
        char incomingChar;
        String stdLine;
        while (sc.hasNextLine()) {

            stdLine = sc.nextLine();
            outWriter.println(stdLine);
            outWriter.flush();

            if (stdLine.equals("cc")) {
                break;
            }

            while ((incomingChar = ((char) inputReader.read())) != -1) {
                if (incomingChar == '\n') {
                    newLineCount++;
                } else {
                    newLineCount = 0;
                }
                System.out.print(incomingChar);

                if (newLineCount == 2) break;
            }

        }

        sc.close();
        outWriter.close();
        inputReader.close();
    }

    public static void defError() {
        System.out.print("Unknown Command\n\n");
    }

    public static void defSuccess() {
        System.out.print("Succeeded\n\n");
    }

    public static void defFailure() {
        System.out.print("Failed\n\n");
    }
}