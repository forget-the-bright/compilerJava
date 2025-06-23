package org.hao.compiler.util;

public class JdkVersionUtils {

    public static int getMajorJavaVersion() {
        String javaVersion = System.getProperty("java.version");

        // Java 9+
        if (javaVersion.startsWith("1.")) {
            // For versions like "1.8.0_292"
            return Integer.parseInt(javaVersion.split("\\.")[1]);
        } else {
            // For versions like "11", "17", "21", etc.
            try {
                // Try using Runtime.version().version().get(0)
                Class<?> runtimeClass = Class.forName("java.lang.Runtime");
                java.lang.reflect.Method versionMethod = runtimeClass.getMethod("version");
                Object runtimeVersion = versionMethod.invoke(null); // Runtime.version()

                java.util.List<Integer> versionList = (java.util.List<Integer>) runtimeVersion.getClass()
                        .getMethod("version").invoke(runtimeVersion);

                return versionList.get(0); // major version
            } catch (Exception e) {
                // Fallback for older or unknown environments
                String[] parts = javaVersion.split("\\.");
                try {
                    return Integer.parseInt(parts[0]);
                } catch (NumberFormatException ex) {
                    return -1; // unknown
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Java Major Version: " + getMajorJavaVersion());
    }
}
