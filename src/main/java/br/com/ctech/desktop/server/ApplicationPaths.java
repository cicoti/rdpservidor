package br.com.ctech.desktop.server;

import java.io.File;
import java.net.URISyntaxException;

public final class ApplicationPaths {

    private static final String LOCK_FILE_NAME = "remote_desktop_server.lock";

    private ApplicationPaths() {
    }

    public static File getApplicationBaseDirectory() {
        try {
            File location = new File(
                    ApplicationPaths.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            if (location.isFile()) {
                return location.getParentFile();
            }

            String normalizedPath = normalizePath(location);

            if (normalizedPath.endsWith("/target/classes")) {
                File projectDir = location.getParentFile() != null ? location.getParentFile().getParentFile() : null;
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

            return location;

        } catch (URISyntaxException e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    public static File getLockFile() {
        return new File(getApplicationBaseDirectory(), LOCK_FILE_NAME);
    }

    public static File getLogsDirectory() {
        return new File(getApplicationBaseDirectory(), "logs");
    }

    public static File getConfigFile(String fileName) {
        return new File(getApplicationBaseDirectory(), fileName);
    }

    private static String normalizePath(File file) {
        return file.getAbsolutePath().replace('\\', '/');
    }
}