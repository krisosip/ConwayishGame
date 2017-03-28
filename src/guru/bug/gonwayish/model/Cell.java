package guru.bug.gonwayish.model;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Dimitrijs Fedotovs <a href="http://www.bug.guru">www.bug.guru</a>
 * @version 1.0
 * @since 1.0
 */
public class Cell implements Runnable {
    private static final long LIFE_PERIOD = 1000; // milliseconds
    private final ReentrantLock lock = new ReentrantLock();
    private final Field field;
    private final Position position;
    private double size;
    private long birthtime;

    Cell(Field field, Position position, boolean initialAlive) {
        this.field = field;
        this.position = position;
        this.size = 0;

        if (initialAlive) {
            birthtime = System.currentTimeMillis();
            size = 1;
        } else {
            birthtime = -1;
        }
    }

    public Position getPosition() {
        return position;
    }

    public Field getField() {
        return field;
    }

    @Override
    public void run() {
        waitUntilFieldReady();
        while (field.isRunning()) {
            pause();
            lock();
            try {
                long bt = getBirthtime();
                long cur = System.currentTimeMillis();

                List<Cell> around = field.findAroundAndTryLock(position);
                if (around == null){
                    continue;
                }

                try {
                    long liveCount = around.stream()
                            .map(Cell::getCellInfo)
                            .filter(CellInfo::isAlive)
                            .count();

                    if (bt == -1 && liveCount == 3) {
                        System.out.println("Cell " + position + " was born");
                        updateCellInfo(System.currentTimeMillis(), 1);
                    }

                    if (bt != -1 && (liveCount == 2 || liveCount == 3)) {
                        System.out.println("Cell " + position + " has died");
                        updateCellInfo(System.currentTimeMillis(), 1);
                    }

                    if (bt != -1 && (liveCount < 2 || liveCount > 3)) {
                        System.out.println("Cell " + position + " has died");
                        updateCellInfo(-1, 0);
                    }

                    long age = cur - bt;
                    if (bt != -1 && age > LIFE_PERIOD) {
                        System.out.println("Cell " + position + " is too old");
                        updateCellInfo(-1, 0);
                        //break;
                    }
                } finally {
                    field.releaseAround(position);
                }
//
//            double p = (age - LIFE_PERIOD / 2.0) / LIFE_PERIOD * Math.PI;
//            double s = Math.cos(p);
//            setSize(s);
            } finally {
                unlock();
            }
        }
        System.out.println("Cell " + position + " finished");
    }

    private void waitUntilFieldReady (){
        synchronized (field){
            while (!field.isRunning()){
                try {
                    field.wait();
                } catch (InterruptedException e){

                }
            }
        }
    }

    private void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private synchronized void updateCellInfo(long birthtime, double size) {
        setBirthtime(birthtime);
        setSize(size);
    }

    private synchronized void setSize(double size) {
        this.size = size;
    }

    private synchronized void setBirthtime(long birthtime) {
        this.birthtime = birthtime;
    }

    private synchronized long getBirthtime() {
        return birthtime;
    }

    public synchronized CellInfo getCellInfo() {
        return new CellInfo(position, birthtime > -1, size);
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();;
    }

    public boolean tryLock() {
        return lock.tryLock();
    }
}
