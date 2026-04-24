package com.s4etech.desktop.path;

import java.io.File;
import java.net.URISyntaxException;

public final class ApplicationPaths {

    private static final String LOCK_FILE_NAME = "remote_desktop_server.lock";

    private ApplicationPaths() {
    }

    public static File getRuntimeDirectory() {
        try {
            File location = new File(
                    ApplicationPaths.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            if (location.isFile()) {
                File parent = location.getParentFile();
                if (parent != null) {
                    return parent;
                }
            }

            return location;

        } catch (URISyntaxException e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    public static File getProjectRootDirectory() {
        try {
            File location = new File(
                    ApplicationPaths.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            String normalizedPath = normalizePath(location);

            if (normalizedPath.endsWith("/target/classes")) {
                File parent = location.getParentFile();
                File projectDir = parent != null ? parent.getParentFile() : null;
                if (projectDir != null) {
                    return projectDir;
                }
            }

            if (normalizedPath.endsWith("/bin")) {
                File projectDir = location.getParentFile();
                if (projectDir != null) {
                    return projectDir;
                }
            }

            if (location.isFile()) {
                File parent = location.getParentFile();
                if (parent != null) {
                    return parent;
                }
            }

            return location;

        } catch (URISyntaxException e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    public static File getApplicationBaseDirectory() {
        return getRuntimeDirectory();
    }

    public static File getLockFile() {
        return new File(getRuntimeDirectory(), LOCK_FILE_NAME);
    }

    public static File getLogsDirectory() {
        return new File(getRuntimeDirectory(), "logs");
    }

    public static File getConfigFile(String fileName) {
        return new File(getRuntimeDirectory(), fileName);
    }

    private static String normalizePath(File file) {
        return file.getAbsolutePath().replace('\\', '/');
    }
}