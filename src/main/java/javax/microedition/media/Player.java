package javax.microedition.media;

public interface Player extends Controllable {
    long TIME_UNKNOWN = -1L;

    int CLOSED = 0;
    int UNREALIZED = 100;
    int REALIZED = 200;
    int PREFETCHED = 300;
    int STARTED = 400;

    void realize() throws MediaException;

    void prefetch() throws MediaException;

    void start() throws MediaException;

    void stop() throws MediaException;

    void deallocate();

    void close();

    void setLoopCount(int count);

    void addPlayerListener(PlayerListener listener);

    void removePlayerListener(PlayerListener listener);

    long setMediaTime(long now) throws MediaException;

    long getMediaTime();

    long getDuration();

    int getState();

    String getContentType();
}
