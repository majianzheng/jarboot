package com.mz.jarboot.common;

import javax.net.ServerSocketFactory;
import java.net.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

/**
 * 以下代码，有一小部分摘自开源项目Arthas
 */
public class NetworkUtils {
    public static final int PORT_RANGE_MIN = 1024;
    private static final int MAX_TIMEOUT = 3000;
    public static final int PORT_RANGE_MAX = 65535;

    private static final Random random = new Random(System.currentTimeMillis());

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
            throw new MzException(ResultCodeConst.INVALID_PARAM, "请填写真实IP地址或域名，而不是环路地址：" + host);
        }
        Enumeration<NetworkInterface> ifs;
        try {
            ifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, e);
        }
        while (ifs.hasMoreElements()) {
            Enumeration<InetAddress> addrs = ifs.nextElement().getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
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
        } catch (Throwable e) {//NOSONAR
            // ignore
        }

        return -1;
    }

    public static boolean isTcpPortAvailable(int port) {
        try {
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 1,
                    InetAddress.getByName("localhost"));
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
        return minPort + random.nextInt(portRange + 1);
    }
}
