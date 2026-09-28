import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Scanner;
import java.util.zip.*;
public class Folderlocker {

    // Create an AES key from the password
    private static SecretKeySpec createKey(String password)
            throws Exception {

        MessageDigest sha = MessageDigest.getInstance("SHA-256");

        byte[] key = sha.digest(password.getBytes("UTF-8"));

        byte[] key16 = new byte[16];

        System.arraycopy(key, 0, key16, 0, 16);

        return new SecretKeySpec(key16, "AES");
    }


    // Encrypt a file
    private static void encryptFile(
            File inputFile,
            File outputFile,
            SecretKeySpec key) throws Exception {

        Cipher cipher = Cipher.getInstance("AES");

        cipher.init(Cipher.ENCRYPT_MODE, key);

        try (
            FileInputStream fis =
                    new FileInputStream(inputFile);

            FileOutputStream fos =
                    new FileOutputStream(outputFile);

            CipherOutputStream cos =
                    new CipherOutputStream(fos, cipher)
        ) {

            byte[] buffer = new byte[4096];

            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {

                cos.write(buffer, 0, bytesRead);
            }
        }
    }


    // Decrypt a file
    private static void decryptFile(
            File inputFile,
            File outputFile,
            SecretKeySpec key) throws Exception {

        Cipher cipher = Cipher.getInstance("AES");

        cipher.init(Cipher.DECRYPT_MODE, key);

        try (
            FileInputStream fis =
                    new FileInputStream(inputFile);

            CipherInputStream cis =
                    new CipherInputStream(fis, cipher);

            FileOutputStream fos =
                    new FileOutputStream(outputFile)
        ) {

            byte[] buffer = new byte[4096];

            int bytesRead;

            while ((bytesRead = cis.read(buffer)) != -1) {

                fos.write(buffer, 0, bytesRead);
            }
        }
    }


    // Compress the folder into a ZIP file
    private static void zipFolder(
            String folderPath,
            String zipPath) throws IOException {

        Path sourceFolder = Paths.get(folderPath);

        try (
            FileOutputStream fos =
                    new FileOutputStream(zipPath);

            ZipOutputStream zos =
                    new ZipOutputStream(fos)
        ) {

            Files.walk(sourceFolder).forEach(path -> {

                try {

                    if (Files.isDirectory(path)) {
                        return;
                    }

                    String entryName =
                            sourceFolder
                            .relativize(path)
                            .toString();

                    ZipEntry entry =
                            new ZipEntry(entryName);

                    zos.putNextEntry(entry);

                    Files.copy(path, zos);

                    zos.closeEntry();

                } catch (IOException e) {

                    throw new RuntimeException(e);
                }
            });
        }
    }


    // Extract the ZIP file
    private static void unzipFolder(
            String zipPath,
            String outputFolder) throws IOException {

        File folder = new File(outputFolder);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        try (
            ZipInputStream zis =
                    new ZipInputStream(
                            new FileInputStream(zipPath))
        ) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                File outputFile =
                        new File(folder, entry.getName());

                outputFile.getParentFile().mkdirs();

                try (
                    FileOutputStream fos =
                            new FileOutputStream(outputFile)
                ) {

                    byte[] buffer = new byte[4096];

                    int bytesRead;

                    while ((bytesRead =
                            zis.read(buffer)) != -1) {

                        fos.write(buffer, 0, bytesRead);
                    }
                }

                zis.closeEntry();
            }
        }
    }


    // Delete a folder and its contents
    private static void deleteFolder(File folder)
            throws IOException {

        if (folder.isDirectory()) {

            File[] files = folder.listFiles();

            if (files != null) {

                for (File file : files) {

                    deleteFolder(file);
                }
            }
        }

        folder.delete();
    }


    // Main program
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        try {

            System.out.println("===== FOLDER LOCKER =====");

            System.out.println("1. Lock Folder");
            System.out.println("2. Unlock Folder");

            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();

            scanner.nextLine();

            System.out.print("Enter folder/file path: ");

            String path = scanner.nextLine();

            System.out.print("Enter password: ");

            String password = scanner.nextLine();

            SecretKeySpec key =
                    createKey(password);


            // LOCK
            if (choice == 1) {

                File folder = new File(path);

                if (!folder.exists() ||
                    !folder.isDirectory()) {

                    System.out.println(
                            "Invalid folder path.");

                    return;
                }

                String zipFile =
                        folder.getAbsolutePath() + ".zip";

                String encryptedFile =
                        folder.getAbsolutePath() + ".locked";


                System.out.println(
                        "Compressing folder...");

                zipFolder(path, zipFile);


                System.out.println(
                        "Encrypting folder...");

                encryptFile(
                        new File(zipFile),
                        new File(encryptedFile),
                        key
                );


                // Delete temporary ZIP file
                new File(zipFile).delete();


                // Delete original folder
                deleteFolder(folder);


                System.out.println(
                        "Folder successfully locked!");

                System.out.println(
                        "Locked file: " + encryptedFile);
            }


            // UNLOCK
            else if (choice == 2) {

                File lockedFile = new File(path);

                if (!lockedFile.exists()) {

                    System.out.println(
                            "Locked file not found.");

                    return;
                }


                String decryptedZip =
                        lockedFile.getAbsolutePath()
                        + ".zip";


                System.out.println(
                        "Decrypting...");


                decryptFile(
                        lockedFile,
                        new File(decryptedZip),
                        key
                );


                String outputFolder =
                        lockedFile.getAbsolutePath()
                        .replace(".locked", "");


                System.out.println(
                        "Extracting folder...");


                unzipFolder(
                        decryptedZip,
                        outputFolder
                );


                // Delete temporary ZIP
                new File(decryptedZip).delete();


                // Delete locked file
                lockedFile.delete();


                System.out.println(
                        "Folder successfully unlocked!");

                System.out.println(
                        "Restored folder: "
                        + outputFolder);
            }


            else {

                System.out.println(
                        "Invalid choice.");
            }

        }

        catch (Exception e) {

            System.out.println(
                    "Operation failed.");

            System.out.println(
                    "Check the password and path.");
        }

        scanner.close();
    }
}