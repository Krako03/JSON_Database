package client;

import com.beust.jcommander.JCommander;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientTest {

    private static final String[] TEST_ARGS = {"--type", "get", "--key", "testKey"};
    private Client client;
    private Socket mockSocket;

    @Mock
    private DataInputStream mockInput;
    @Mock
    private DataOutputStream mockOutput;

    @BeforeEach
    void setUp() {
        mockSocket = mock(Socket.class);
        client = new Client(TEST_ARGS);
    }

    @Test
    void testGetConnectionSuccess() throws Exception {
        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class)) {
            mockedInetAddress.when(() -> InetAddress.getByName("localhost"))
                    .thenReturn(mock(InetAddress.class));

            // Mock Socket behavior
            when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
            when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

            // Test method
            client.getConnection();

            // No exceptions should occur
            assertTrue(true);
        }
    }

    @Test
    void testSuccesfulConnection() throws IOException {
        InetAddress mockInetAddress = mock(InetAddress.class);
        ParametersClient mockParameters = mock(ParametersClient.class);
        JCommander mockJCommander = mock(JCommander.class);
        Message mockMessage = mock(Message.class);
        Gson gson = mock(Gson.class);

        client = new Client(new String[]{"set", "person", "elon"});

//        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class);
//        MockedStatic<JCommander> mockedJCommander = mockStatic(JCommander.class)) {
//            mockedInetAddress.when(() -> InetAddress.getByName("localhost"))
//                    .thenReturn(mockInetAddress);
//            mockedJCommander.when(() -> JCommander.newBuilder().addObject(mockParameters).build())
//                    .thenReturn(mockJCommander);
//        }

        when(mockSocket.getInputStream()).thenReturn(mockInput);
        when(mockSocket.getOutputStream()).thenReturn(mockOutput);
        doNothing().when(mockJCommander).parse(client.getArgs());
        doNothing().when(mockJCommander).usage();
        doNothing().when(mockOutput).writeUTF(anyString());
        when(mockParameters.getTypeRequest()).thenReturn("set");
        when(mockParameters.getKey()).thenReturn("person");
        when(mockParameters.getTextRequest()).thenReturn("elon");
        when(mockParameters.getTextRequest()).thenReturn("");
        when(gson.toJson(mockMessage)).thenReturn(
                "{}");
        when(mockInput.readUTF()).thenReturn(
                "{}");

        client.getConnection();
    }
}