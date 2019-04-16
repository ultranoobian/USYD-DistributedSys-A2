import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;


class ServerInfoListTest {

    @Test
    void initialiseFromFile() {
        ServerInfoList serverInfoList = new ServerInfoList();

        serverInfoList.initialiseFromFile("src/test_files/test_server_list_3.ini");
        ArrayList<ServerInfo> serverInfo = serverInfoList.getServerInfos();
        assertEquals(3, serverInfo.size());
        assertNull(serverInfo.get(0));
        assertNull(serverInfo.get(1));
        assertNotNull(serverInfo.get(2));

        assertEquals("localhost", serverInfo.get(2).getHost());
        assertEquals(8333, serverInfo.get(2).getPort());

    }

    @Test
    void initialiseFromFile_portUpdate() {
        ServerInfoList serverInfoList = new ServerInfoList();

        serverInfoList.initialiseFromFile("src/test_files/test_server_list_3_updatePort.ini");
        ArrayList<ServerInfo> serverInfo = serverInfoList.getServerInfos();
        assertEquals(3, serverInfo.size());
        assertNull(serverInfo.get(0));
        assertNull(serverInfo.get(1));
        assertNotNull(serverInfo.get(2));

        assertEquals("localhost", serverInfo.get(2).getHost());
        assertEquals(8335, serverInfo.get(2).getPort());

    }

    @Test
    void initialiseFromFile_serverUpdate() {
        ServerInfoList serverInfoList = new ServerInfoList();

        serverInfoList.initialiseFromFile("src/test_files/test_server_list_3_updateServer.ini");
        ArrayList<ServerInfo> serverInfo = serverInfoList.getServerInfos();
        assertEquals(3, serverInfo.size());
        assertNull(serverInfo.get(0));
        assertNull(serverInfo.get(1));
        assertNotNull(serverInfo.get(2));

        assertEquals("192.168.1.1", serverInfo.get(2).getHost());
        assertEquals(8333, serverInfo.get(2).getPort());

    }

    @Test
    void initialiseFromFile_0() {
        ServerInfoList serverInfoList = new ServerInfoList();

        serverInfoList.initialiseFromFile("src/test_files/test_server_list_0.ini");
        ArrayList<ServerInfo> serverInfo = serverInfoList.getServerInfos();
        assertEquals(0, serverInfo.size());

    }
}