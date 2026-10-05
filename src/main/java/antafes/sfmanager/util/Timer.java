package antafes.sfmanager.util;

import lombok.Getter;

@Getter
public class Timer extends Thread
{
    private int counter = 0;
    private final int showFor;

    public Timer(int showFor) {
        this.showFor = showFor;
    }

    public void run() {
        while (counter <= showFor) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            counter++;
        }
    }
}
