package android.content.pm;

import java.io.File;
import java.util.ArrayList;

public class PackageParser {
    public Package parsePackage(File packageFile, int flags) throws PackageParserException {
        throw new RuntimeException("Stub!");
    }

    public static final class Package {
        public String packageName;
        public ApplicationInfo applicationInfo;

        public final ArrayList<Activity> activities;
        public final ArrayList<Activity> receivers;
        public final ArrayList<Provider> providers;
        public final ArrayList<Service> services;

        public Package() {
            throw new RuntimeException("Stub!");
        }
    }

    public static final class Activity {
        public final ActivityInfo info;

        public Activity() {
            throw new RuntimeException("Stub!");
        }
    }

    public static final class Provider {
        public final ProviderInfo info;

        public Provider() {
            throw new RuntimeException("Stub!");
        }
    }

    public static final class Service {
        public final ServiceInfo info;

        public Service() {
            throw new RuntimeException("Stub!");
        }
    }

    public static class PackageParserException extends Exception {

    }
}
