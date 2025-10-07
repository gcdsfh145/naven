package com.heypixel.heypixelmod.obsoverlay.Config;

import java.io.File;

public abstract class Config {
    protected final File file;

    public Config(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public abstract void read() throws Throwable;

    public abstract boolean write() throws Throwable;
}