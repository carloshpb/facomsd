package server_client.client.threads.consumer;

import server_client.client.view.TerminalView;
import server_client.model.Message;

import javax.swing.*;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.util.logging.Logger;

public class AnswerPresentationThread implements Runnable {

    protected final static Logger LOGGER = Logger.getLogger(AnswerPresentationThread.class.getName());

    protected volatile boolean exit = false;

    protected Boolean producerIsOver;

    protected ObjectInputStream objectInputStream;

    protected TerminalView view;

    public AnswerPresentationThread(Boolean producerIsOver, ObjectInputStream objectInputStream, TerminalView view ) {

        this.producerIsOver = producerIsOver;
        this.objectInputStream = objectInputStream;
        this.view = view;
    }

    public AnswerPresentationThread() {
    }

    protected synchronized void shutdown() {
        this.exit = true;
    }

    @Override
    public void run() {

        Timer timer = null;
        Message answer = null;

        while (!this.exit && !Thread.interrupted()) {

            if (this.producerIsOver && timer == null) {
                final int FIVE_SECONDS = 5000;

                timer = new Timer(FIVE_SECONDS, event -> {
                    this.shutdown();
                });

                timer.start();
            }

            try {

                answer = (Message) objectInputStream.readObject();

                if (answer != null) {
                    if (timer != null) {
                        timer.stop();
                    }
                    if (answer.getLastOption() == 5) {
                        System.out.println("Shutting down.");
                        this.shutdown();
                    } else {
                        this.view.readMessage(answer);
                        answer = null;
                    }
                }

            } catch (EOFException e) {
                this.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
                this.shutdown();
            }
        }
    }
}
