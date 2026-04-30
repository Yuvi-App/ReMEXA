package javax.microedition.lcdui;

import remexa.host.input.HostTextInputRequest;
import remexa.host.runtime.MidletRuntime;

public class TextBox extends Screen {
    private String text;
    private final int maxSize;
    private final int constraints;
    private Display ownerDisplay;
    private Displayable previousDisplayable;
    private CommandListener fallbackCommandListener;
    private boolean hostInputPresented;
    private boolean hostInputInFlight;

    protected TextBox() {
        this(null, "", 0, TextField.ANY);
    }

    public TextBox(String title, String text, int maxSize, int constraints) {
        setTitle(title);
        this.text = text;
        this.maxSize = maxSize;
        this.constraints = constraints;
    }

    public String getString() {
        return text;
    }

    public void setString(String text) {
        this.text = text == null ? "" : text;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getConstraints() {
        return constraints;
    }

    @Override
    public void addCommand(Command command) {
        super.addCommand(command);
        maybePresentHostInput();
    }

    @Override
    public void setCommandListener(CommandListener commandListener) {
        super.setCommandListener(commandListener);
        maybePresentHostInput();
    }

    void attachDisplay(Display ownerDisplay, Displayable previousDisplayable) {
        this.ownerDisplay = ownerDisplay;
        this.previousDisplayable = previousDisplayable;
        this.fallbackCommandListener = previousDisplayable == null ? null : previousDisplayable.getCommandListener();
        this.hostInputPresented = false;
        this.hostInputInFlight = false;
        maybePresentHostInput();
    }

    void detachDisplay() {
        ownerDisplay = null;
        previousDisplayable = null;
        fallbackCommandListener = null;
        hostInputInFlight = false;
    }

    void onShown() {
        maybePresentHostInput();
    }

    private void maybePresentHostInput() {
        if (hostInputPresented || hostInputInFlight || ownerDisplay == null || !isShown()) {
            return;
        }
        // Wait until the TextBox itself is fully wired. Many legacy apps call
        // setCurrent(tb), then addCommand(...), then setCommandListener(...).
        // Presenting input before the explicit listener is attached can re-enter
        // app code in the middle of that setup and null out tb prematurely.
        if (commandCountInternal() == 0 || getCommandListener() == null) {
            return;
        }
        hostInputInFlight = true;
        try {
            var result = MidletRuntime.requestTextInputResult(new HostTextInputRequest(
                    getTitle(),
                    text,
                    constraints,
                    maxSize,
                    true
            ));
            setString(result.text());
            hostInputPresented = true;
            dispatchHostResult(result);
        } finally {
            hostInputInFlight = false;
        }
    }

    private void dispatchHostResult(HostTextInputRequest.Result result) {
        if (result.accepted()) {
            var acceptCommand = softKeyCommands()[0];
            if (acceptCommand == null) {
                acceptCommand = resolveCommand(0);
            }
            if (dispatchCommand(acceptCommand)) {
                return;
            }
            returnToPreviousDisplay();
            return;
        }

        var cancelCommand = softKeyCommands()[1];
        if (dispatchCommand(cancelCommand)) {
            return;
        }
        returnToPreviousDisplay();
    }

    private boolean dispatchCommand(Command command) {
        if (command == null) {
            return false;
        }
        var commandListener = getCommandListener();
        if (commandListener != null) {
            commandListener.commandAction(command, this);
            return true;
        }
        if (fallbackCommandListener != null) {
            fallbackCommandListener.commandAction(command, this);
            return true;
        }
        return false;
    }

    private void returnToPreviousDisplay() {
        if (ownerDisplay != null && previousDisplayable != null && ownerDisplay.getCurrent() == this) {
            ownerDisplay.setCurrent(previousDisplayable);
        }
    }
}
