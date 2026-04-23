package javax.microedition.lcdui;

public class Displayable {
    private String title;
    private Ticker ticker;
    private CommandListener commandListener;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    public void addCommand(Command command) {
    }

    public void removeCommand(Command command) {
    }

    public void setCommandListener(CommandListener commandListener) {
        this.commandListener = commandListener;
    }

    public CommandListener getCommandListener() {
        return commandListener;
    }
}
