package com.luan.conectar;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;

public class ServidorSocketComInterface extends Application {
    private static boolean compartilhamentoAtivo = true;
    private static Robot robot;
    private static Socket socket;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    private static Stage primaryStage;

    public static void main(String[] args) {
        new Thread(() -> launch(args)).start();
        int porta = 12345;

        try (ServerSocket servidor = new ServerSocket(porta)) {
            System.out.println("Aguardando conexão na porta: " + porta + "...");

            socket = servidor.accept();
            System.out.println("Conexão estabelecida com: " + socket.getInetAddress().getHostAddress());

            createRobot();

            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            while (compartilhamentoAtivo) {
                enviarCapturaTela(); // Enviar capturas de tela continuamente
                receberComando();
            }

        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        ServidorSocketComInterface.primaryStage = primaryStage;
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("Servidor - Compartilhamento de Tela");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static void createRobot() throws AWTException {
        robot = new Robot();
    }

    private static void receberComando() {
        try {
            Comando comando = (Comando) ois.readObject();
            processarComando(comando);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void processarComando(Comando comando) {
        switch (comando.getTipo()) {
            case MOUSE:
                ComandoMouseTeclado comandoMouse = (ComandoMouseTeclado) comando;
                processarComandoMouse(comandoMouse);
                break;
            case TECLADO:
                ComandoTeclado comandoTeclado = (ComandoTeclado) comando;
                processarComandoTeclado(comandoTeclado);
                break;
            case INICIO_COMPARTILHAMENTO:
                compartilhamentoAtivo = true;
                break;
            // Remove o case para CAPTURA_TELA, pois não é necessário
        }
    }

    private static void processarComandoMouse(ComandoMouseTeclado comandoMouse) {
        DadosMouseTeclado dados = (DadosMouseTeclado) comandoMouse.getDados();

        Point point = dados.getPoint();
        boolean leftClick = dados.isLeftClick();
        boolean rightClick = dados.isRightClick();

        robot.mouseMove(point.x, point.y);

        if (leftClick) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }

        if (rightClick) {
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        }
    }

    private static void processarComandoTeclado(ComandoTeclado comandoTeclado) {
        DadosTeclado dados = (DadosTeclado) comandoTeclado.getDados();

        char keyChar = dados.getKeyChar();
        boolean keyPressed = dados.isKeyPressed();

        try {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(keyChar);

            if (keyPressed) {
                robot.keyPress(keyCode);
            } else {
                robot.keyRelease(keyCode);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Tecla não reconhecida: " + keyChar);
        }
    }

    private static void enviarCapturaTela() {
        try {
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenCapture, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            Comando comandoCapturaTela = new Comando(TipoComando.CAPTURA_TELA, imageBytes);
            oos.writeObject(comandoCapturaTela);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}