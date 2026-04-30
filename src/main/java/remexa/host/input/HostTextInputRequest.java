package remexa.host.input;

public record HostTextInputRequest(
        String title,
        String initialText,
        int constraints,
        int maxSize,
        boolean wrapAllowed
) {
    public HostTextInputRequest {
        title = title == null ? "" : title;
        initialText = initialText == null ? "" : initialText;
        maxSize = Math.max(0, maxSize);
    }

    @FunctionalInterface
    public interface Handler {
        String requestTextInput(HostTextInputRequest request);
    }

    public record Result(
            String text,
            boolean accepted
    ) {
        public Result {
            text = text == null ? "" : text;
        }
    }
}
