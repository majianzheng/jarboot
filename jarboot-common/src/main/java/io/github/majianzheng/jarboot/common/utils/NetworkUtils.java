package io.github.majianzheng.jarboot.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.common.ExecNativeCmd;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;

import javax.net.ServerSocketFactory;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

/**
 * 以下代码，有一小部分摘自开源项目Arthas
 * @author majianzheng
 */
public class NetworkUtils {
    public static final int PORT_RANGE_MIN = 1024;
    private static final int MAX_TIMEOUT = 3000;
    public static final int PORT_RANGE_MAX = 65535;
    private static final String QOS_HOST = "localhost";
    private static final int QOS_PORT = 12201;
    private static final String QOS_RESPONSE_START_LINE = "pandora>[QOS Response]";
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 3000;
    private static final String ERROR_MSG = "errorMsg";

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private NetworkUtils() {
    }

    /**
     * 检查host是否能ping通
     * @param host host地址
     * @return 是否ping通
     */
    public static boolean hostReachable(String host) {
        if (null == host || host.trim().isEmpty()) {
            return false;
        }
        try {
            return InetAddress.getByName(host).isReachable(MAX_TIMEOUT);
        } catch (Exception e) {
            //ignore
        }
        return false;
    }

    public static List<String> getLocalAddr4() {
        List<String> localAddr = new ArrayList<>();
        Enumeration<NetworkInterface> ifs;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
        }
        while (ifs.hasMoreElements()) {
            Enumeration<InetAddress> address = ifs.nextElement().getInetAddresses();
            while (address.hasMoreElements()) {
                InetAddress addr = address.nextElement();
                if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                    localAddr.add(addr.getHostAddress());
                }
            }
        }
        return localAddr;
    }

    public static List<String> getLocalAddr() {
        List<String> localAddr = new ArrayList<>();
        Enumeration<NetworkInterface> ifs;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
        }
        while (ifs.hasMoreElements()) {
            Enumeration<InetAddress> address = ifs.nextElement().getInetAddresses();
            while (address.hasMoreElements()) {
                InetAddress addr = address.nextElement();
                if (!addr.isLoopbackAddress()) {
                    localAddr.add(addr.getHostAddress());
                }
            }
        }
        return localAddr;
    }

    /**
     * 检查host是否是本地的
     * @param host host地址
     * @return 是否本地
     */
    public static boolean hostLocal(String host) {
        if (null == host || host.trim().isEmpty()) {
            return false;
        }
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (Exception e) {
            //ignore
        }
        if (null == inetAddress) {
            return false;
        }
        if (inetAddress.isLoopbackAddress()) {
            return true;
        }
        Enumeration<NetworkInterface> ifs;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, e);
        }
        while (ifs.hasMoreElements()) {
            Enumeration<InetAddress> address = ifs.nextElement().getInetAddresses();
            while (address.hasMoreElements()) {
                InetAddress addr = address.nextElement();
                if (inetAddress.equals(addr)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isHostConnectable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * This implementation is based on Apache HttpClient.
     * @param urlString the requested url
     * @return the response string of given url
     */
    public static Response request(String urlString) {
        HttpURLConnection urlConnection = null;
        URL url;
        try {
            url = new URL(urlString);
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            // prefer json to text
            urlConnection.setRequestProperty("Accept", "application/json,text/plain;q=0.2");
        } catch (Exception e) {
            return new Response(e.getMessage(), false);
        }

        try (InputStream in = urlConnection.getInputStream();
             InputStreamReader isr = new InputStreamReader(in);
             BufferedReader br = new BufferedReader(isr)){
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            int statusCode = urlConnection.getResponseCode();
            String result = sb.toString().trim();
            if (statusCode == INTERNAL_SERVER_ERROR) {
                JsonNode errorObj = JsonUtils.readAsJsonNode(result);
                if (null != errorObj && errorObj.has(ERROR_MSG)) {
                    return new Response(errorObj.get(ERROR_MSG).asText("error"), false);
                }
                return new Response(result, false);
            }
            return new Response(result);
        } catch (IOException e) {
            return new Response(e.getMessage(), false);
        } finally {
            urlConnection.disconnect();
        }
    }

    /**
     * Only use this method when tomcat monitor version <= 1.0.1
     * This will send http request to pandora qos port 12201,
     * and display the response.
     * Note that pandora qos response is not fully HTTP compatible under version 2.1.0,
     * so we filtered some of the content and only display useful content.
     * @param path the path relative to http://localhost:12201
     *             e.g. /pandora/ls
     *             For commands that requires arguments, use the following format
     *             e.g. /pandora/find?arg0=RPCProtocolService
     *             Note that the parameter name is never used in pandora qos,
     *             so the name(e.g. arg0) is irrelevant.
     * @return the qos response in string format
     */
    public static Response requestViaSocket(String path) {
        BufferedReader br = null;
        try (Socket s = new Socket(QOS_HOST, QOS_PORT);){
            PrintWriter pw = new PrintWriter(s.getOutputStream());
            pw.println("GET " + path + " HTTP/1.1");
            pw.println("Host: " + QOS_HOST + ":" + QOS_PORT);
            pw.println("");
            pw.flush();
            br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            boolean start = false;
            while ((line = br.readLine()) != null) {
                if (start) {
                    sb.append(line).append("\n");
                }
                if (QOS_RESPONSE_START_LINE.equals(line)) {
                    start = true;
                }
            }
            String result = sb.toString().trim();
            return new Response(result);
        } catch (Exception e) {
            return new Response(e.getMessage(), false);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static class Response {

        private boolean success;
        private String content;

        public Response(String content, boolean success) {
            this.success = success;
            this.content = content;
        }

        public Response(String content) {
            this.content = content;
            this.success = true;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getContent() {
            return content;
        }
    }

    public static int findProcessByListenPort(int port) {//NOSONAR
        try {
            if (OSUtils.isWindows()) {
                String[] command = { "netstat", "-ano", "-p", "TCP" };
                List<String> lines = ExecNativeCmd.exec(command);
                for (String line : lines) {
                    if (line.contains("LISTENING")) {
                        // TCP 0.0.0.0:49168 0.0.0.0:0 LISTENING 476
                        String[] strings = line.trim().split("\\s+");
                        if (strings.length == 5 && strings[1].endsWith(":" + port)) {
                            return Integer.parseInt(strings[4]);
                        }
                    }
                }
            }

            if (OSUtils.isLinux() || OSUtils.isMac()) {
                String pid = ExecNativeCmd.getFirstAnswer("lsof -t -s TCP:LISTEN -i TCP:" + port);
                if (!pid.trim().isEmpty()) {
                    return Integer.parseInt(pid);
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return -1;
    }

    public static boolean isTcpPortAvailable(int port) {
        try {
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 1,
                    InetAddress.getByName(QOS_HOST));
            serverSocket.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Find an available TCP port randomly selected from the range
     * [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}].
     *
     * @return an available TCP port number
     * @throws IllegalStateException if no available port could be found
     */
    public static int findAvailableTcpPort() {
        return findAvailableTcpPort(PORT_RANGE_MIN);
    }

    /**
     * Find an available TCP port randomly selected from the range [{@code minPort},
     * {@value #PORT_RANGE_MAX}].
     *
     * @param minPort the minimum port number
     * @return an available TCP port number
     * @throws IllegalStateException if no available port could be found
     */
    public static int findAvailableTcpPort(int minPort) {
        return findAvailableTcpPort(minPort, PORT_RANGE_MAX);
    }

    /**
     * Find an available TCP port randomly selected from the range [{@code minPort},
     * {@code maxPort}].
     *
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return an available TCP port number
     * @throws IllegalStateException if no available port could be found
     */
    public static int findAvailableTcpPort(int minPort, int maxPort) {
        return findAvailablePort(minPort, maxPort);
    }

    /**
     * 获取本机mac地址列表
     * @return mac地址
     */
    public static List<String> getMacAddrList() {
        List<String> macAddrList = new ArrayList<>();
        try {
            // 获取本机所有的网络接口
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                boolean hasInet4 = false;
                Enumeration<InetAddress> address = networkInterface.getInetAddresses();
                while (address.hasMoreElements()) {
                    InetAddress addr = address.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        hasInet4 = true;
                    }
                }

                // 获取当前网络接口的MAC地址
                byte[] mac = networkInterface.getHardwareAddress();
                // 检查网络接口是否已启用且不是回环接口
                if (hasInet4 && null != mac && !networkInterface.isLoopback()) {
                    // 将MAC地址转换为字符串形式
                    StringBuilder macAddress = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        macAddress.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    macAddrList.add(macAddress.toString());
                }
            }

        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
        return macAddrList;
    }
    /**
     * Find an available port for this {@code SocketType}, randomly selected from
     * the range [{@code minPort}, {@code maxPort}].
     *
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return an available port number for this socket type
     * @throws IllegalStateException if no available port could be found
     */
    private static int findAvailablePort(int minPort, int maxPort) {

        int portRange = maxPort - minPort;
        int candidatePort;
        int searchCounter = 0;
        do {
            if (searchCounter > portRange) {
                throw new IllegalStateException(
                        String.format("Could not find an available tcp port in the range [%d, %d] after %d attempts",
                                minPort, maxPort, searchCounter));
            }
            candidatePort = findRandomPort(minPort, maxPort);
            searchCounter++;
        } while (!isTcpPortAvailable(candidatePort));

        return candidatePort;
    }

    /**
     * Find a pseudo-random port number within the range [{@code minPort},
     * {@code maxPort}].
     *
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return a random port number within the specified range
     */
    private static int findRandomPort(int minPort, int maxPort) {
        int portRange = maxPort - minPort;
        return minPort + RANDOM.nextInt(portRange + 1);
    }
}
