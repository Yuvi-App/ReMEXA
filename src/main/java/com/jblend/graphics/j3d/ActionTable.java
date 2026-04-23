package com.jblend.graphics.j3d;

import java.io.IOException;
import java.io.InputStream;
import remexa.host.j3d.MascotActionTableData;
import remexa.host.j3d.MascotLoader;
import remexa.host.runtime.MidletRuntime;

public class ActionTable {
    private final MascotActionTableData data;

    protected ActionTable() {
        this.data = null;
    }

    public ActionTable (byte[] data) {
        if (data == null) {
            throw new NullPointerException();
        }
        try {
            this.data = MascotLoader.loadActionTable(data);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load action table", exception);
        }
    }

    public ActionTable (java.lang.String name) throws java.io.IOException {
        if (name == null) {
            throw new NullPointerException();
        }
        try (InputStream stream = MidletRuntime.openResource(name)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + name);
            }
            this.data = MascotLoader.loadActionTable(stream);
        }
    }


    public final int getNumAction () {
        return data == null ? 0 : data.numActions();
    }

    public final int getNumFrame (int action) {
        if (data == null) {
            return 0;
        }
        if (action < 0 || action >= data.numActions()) {
            throw new IllegalArgumentException();
        }
        return data.maxFrame(action);
    }

    public MascotActionTableData data() {
        return data;
    }
}
