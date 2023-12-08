/*
 * Copyright (C) 2023 PunkerGhoul
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package init;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InitServer {

    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static List<PrintWriter> clientWriters = new ArrayList<>();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int port = showMenu(sc);
        if (port != 0) {
            startServer(port);
        }
    }

    private static int showMenu(Scanner sc) {
        int opcion = 0; // Inicializamos opcion a un valor que garantice entrar al bucle
        try {
            String ip = getActiveNetworkInterfaceIP();
            if (ip != null) {
                Logger.getLogger(InitServer.class.getName()).log(Level.INFO, "IP de la interfaz activa: {0}", ip);
            } else {
                Logger.getLogger(InitServer.class.getName()).log(Level.WARNING, "No se encontró una interfaz de red activa con IP.");
            }
        } catch (SocketException ex) {
            Logger.getLogger(InitServer.class.getName()).log(Level.SEVERE, "Error al obtener la IP de la interfaz de red activa.", ex);
        }

        String menuMsg = "Menu:\n\t1. Empezar Servidor\n\t2. Salir\n";

        do {
            System.out.print(menuMsg + "Ingrese su opción: ");
            if (sc.hasNextInt()) {
                opcion = sc.nextInt();
                if (opcion < 1 || opcion > 2) {
                    Logger.getLogger(InitServer.class.getName()).log(Level.SEVERE, null, "Opción no válida. Debe ser 1 o 2.");
                }
            } else {
                sc.next(); // Consumir el valor no válido
                Logger.getLogger(InitServer.class.getName()).log(Level.SEVERE, null, "Opción no válida. Debe ser un número.");
            }
        } while (opcion < 1 || opcion > 2);

        if (opcion == 1) {
            // Coloca la lógica para "Empezar Servidor" aquí
            Logger.getLogger(InitServer.class.getName()).log(Level.INFO, "Iniciar servidor...");
            return showPortsMenu(sc);
        } else {
            Logger.getLogger(InitServer.class.getName()).log(Level.INFO, "Saliendo del programa.");
        }
        return 0;
    }

    public static String getActiveNetworkInterfaceIP() throws SocketException {
        String[] preferredInterfaces = {"wlan0", "eth0", "ensp"};
        String ipAddress = null;

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        int activeInterfaceCount = 0;

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                    ipAddress = address.getHostAddress();
                    activeInterfaceCount++;
                }
            }
        }

        if (activeInterfaceCount == 1) {
            return ipAddress; // Si hay una sola interfaz activa, devolver su IP
        } else if (activeInterfaceCount > 1) {
            // Si hay más de una interfaz activa, seleccionar la preferida
            for (String preferredInterface : preferredInterfaces) {
                networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    if (networkInterface.getName().equals(preferredInterface)) {
                        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress address = addresses.nextElement();
                            if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                                return address.getHostAddress();
                            }
                        }
                    }
                }
            }
        }

        return null; // No se encontró una interfaz activa con IP
    }

    private static int showPortsMenu(Scanner sc) {
        int choice;

        do {
            System.out.println("\nSeleccione una opción:");
            System.out.println("\t1. Ingresar manualmente el número del puerto");
            System.out.println("\t2. Generar aleatoriamente el número del puerto");
            System.out.print("Opción: ");

            if (sc.hasNextInt()) {
                choice = sc.nextInt();
                if (choice < 1 || choice > 2) {
                    Logger.getLogger(InitServer.class.getName()).log(Level.SEVERE, "Opción no válida. Por favor, seleccione 1 o 2.");
                } else {
                    break;
                }
            } else {
                sc.next();
                Logger.getLogger(InitServer.class.getName()).log(Level.SEVERE, "Opción no válida. Debe ser un número.");
            }
        } while (true);
        int port;
        if (choice == 1) {
            port = enterPortManually(sc);
        } else {
            port = generateRandomPort();
        }
        return port;
    }

    private static int enterPortManually(Scanner sc) {
        int port;

        do {
            System.out.print("Ingrese el número del puerto (49152-65535): ");
            if (sc.hasNextInt()) {
                port = sc.nextInt();
                if (port < 49152 || port > 65535) {
                    Logger.getLogger(InitServer.class.getName()).log(Level.SEVERE, "Opción no válida. Debe estar entre 49152 y 65535.");
                } else {
                    break;
                }
            } else {
                sc.next();
                Logger.getLogger(InitServer.class.getName()).log(Level.SEVERE, "Opción no válida. Debe ser un número.");
            }
        } while (true);

        return port;
    }

    private static int generateRandomPort() {
        Random random = new Random();
        return random.nextInt(16384) + 49152; // Genera un puerto aleatorio entre 49152 y 65535
    }

    private static void startServer(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Logger.getLogger(InitServer.class.getName()).log(Level.INFO, "Servidor escuchando en el puerto {0}", port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Logger.getLogger(InitServer.class.getName()).log(Level.INFO, "Nuevo cliente conectado desde {0}", clientSocket.getInetAddress());

                // Iniciar un nuevo hilo para manejar al cliente
                threadPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            Logger.getLogger(InitServer.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private static class ClientHandler implements Runnable {

        private final Socket clientSocket;
        private PrintWriter writer;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.ISO_8859_1));
                OutputStream output = clientSocket.getOutputStream();
                writer = new PrintWriter(output, true);
                clientWriters.add(writer);

                String clientAddress = clientSocket.getInetAddress().toString();
                Logger.getLogger(InitServer.class.getName()).log(Level.INFO, "Cliente {0} conectado.", clientAddress);

                String message;
                while ((message = reader.readLine()) != null) {
                    Logger.getLogger(InitServer.class.getName()).log(Level.INFO, "Mensaje del cliente {0}: {1}", new Object[]{clientAddress, message});
                    broadcastMessage(message);
                }
            } catch (IOException e) {
                Logger.getLogger(InitServer.class.getName()).log(Level.INFO, "Cliente {0} desconectado.", clientSocket.getInetAddress());
            } finally {
                if (writer != null) {
                    clientWriters.remove(writer);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    Logger.getLogger(InitServer.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }

        private static void broadcastMessage(String message) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }
}
