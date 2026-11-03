package javax.microedition.lcdui;

public class Item {
    private String label;
    private Form owner;

    protected Item(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        repaintOwner();
    }

    protected void notifyStateChanged() {
        if (owner == null) {
            throw new IllegalStateException("Item is not contained in a Form.");
        }
        owner.itemStateChanged(this);
    }

    final Form owner() {
        return owner;
    }

    final void setOwner(Form owner) {
        this.owner = owner;
    }

    final void repaintOwner() {
        if (owner != null && owner.isShown()) {
            owner.repaintHost();
        }
    }
}
