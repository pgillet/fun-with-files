import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException {
        Path start = Paths.get("/etc");
        SimpleFileVisitor fileVisitor = new SimpleFileVisitor();
        Files.walkFileTree(start, fileVisitor);

        Map<String, List<HashFile>> allFiles = fileVisitor.getVisitedPaths()
                .collect(Collectors.groupingBy(HashFile::getMessageDigest));

        allFiles.entrySet().stream()
                .filter(e -> {
                    // Keep only duplicate files
                    return e.getValue().size() > 1;
                })
                .forEach(e -> {
                    List<HashFile> duplicatePaths = e.getValue();
                    // Keep only the first path
                    HashFile first = duplicatePaths.remove(0);
                    System.out.println("Reference file to keep: " + first.getPath());

                    duplicatePaths.forEach(d -> {
                        try {
                            System.out.println("Deleting duplicate file " + d.getPath());
                            // Files.delete(d.getPath());
                            System.out.println(String.format("Creating symbolic link %s -> %s", d.getPath(), first.getPath()));
                            // Files.createSymbolicLink(d.getPath(), first.getPath());
                        } catch (/*IO*/Exception ex) {
                            System.out.println(String.format("%s: %s", ex.getClass().getName(), ex.getMessage()));
                        }
                    });
                });
    }

    public static class SimpleFileVisitor implements FileVisitor<Path> {

        private List<HashFile> visitedPaths = new ArrayList<>();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (Files.isRegularFile(file) && Files.isReadable(file)) {
                try (InputStream in = new FileInputStream(file.toFile())) {
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                    byte[] buffer = new byte[8192];
                    while (in.read(buffer) > -1) {
                        messageDigest.update(buffer);
                    }
                    HashFile hashFile = new HashFile(file, messageDigest.digest());
                    visitedPaths.add(hashFile);
                } catch (NoSuchAlgorithmException | IOException e) {
                    e.printStackTrace();
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException ex) throws IOException {
            System.out.println(String.format("%s: %s", ex.getClass().getName(), ex.getMessage()));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        public Stream<HashFile> getVisitedPaths() {
            return visitedPaths.stream();
        }
    }

    public static class HashFile {

        private Path path;
        private String messageDigest;

        public HashFile(Path path, String messageDigest) {
            this.path = path;
            this.messageDigest = messageDigest;
        }

        public HashFile(Path path, byte[] messageDigest) {
            this(path, new BigInteger(messageDigest).toString(16));
        }

        public Path getPath() {
            return path;
        }

        public String getMessageDigest() {
            return messageDigest;
        }

        @Override
        public String toString() {
            return "HashFile{" +
                    "path=" + path +
                    ", messageDigest=" + messageDigest +
                    '}';
        }
    }


}
