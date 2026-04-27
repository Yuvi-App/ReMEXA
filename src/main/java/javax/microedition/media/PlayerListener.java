package javax.microedition.media;

public interface PlayerListener {
    String STARTED = "started";
    String STOPPED = "stopped";
    String STOPPED_AT_TIME = "stoppedAtTime";
    String END_OF_MEDIA = "endOfMedia";
    String DURATION_UPDATED = "durationUpdated";
    String DEVICE_AVAILABLE = "deviceAvailable";
    String DEVICE_UNAVAILABLE = "deviceUnavailable";
    String VOLUME_CHANGED = "volumeChanged";
    String CLOSED = "closed";
    String ERROR = "error";

    void playerUpdate(Player player, String event, Object eventData);
}
