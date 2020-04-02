package net.q14;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

public class Main {


    /*

        Offene Features:
            - Doppelte Migration:
                - im Quellordner neue Unterordner anlegen:
                    - Migrated
                    - Deleted in Target
                    - Possible Duplicates

            - Counter scheinen noch nicht zu funktionieren
            -
            - Aufräumaktionen im Ordner erlauben
                - Erfolgreiche Kopieraktionen sollten geloggt werden, um einmal gelöschte Bilder nicht erneut da rein zu kopieren
                    - Bilder aus Quellordnern, die bereits einmal gelöscht wurden, in neuen Unterordner (deleted) verschieben
                - Das gleiche gilt für das Zusammenfassen von Bildern in Ordnern/Alben
                    Duplicate Detection nicht über Dateinamen, sondern über CSV
            -

     */

    //@ConfigProperty(name = "folder.destination")
    public String destination = "C:/Users/pajoma/Documents/Synced/Fotos/Inbox";

    //@ConfigProperty(name = "folder.source")
    public String source = "C:/Users/pajoma/Documents/Synced/Fotos/Inbox";

    List<Path> filesWithExceptions = new ArrayList<>();
    long counter = 0;

    public static void main(String[] args) throws IOException {
        new Main().run();
    }

    private void run() throws IOException {
        Path source = Paths.get(this.source);
        Path destination = Paths.get(this.destination);




        try (Stream<Path> walk = Files.walk(Paths.get(this.source))) {

            walk.forEach(path -> {
                if(ignorePath(path)) return;





                try {
                    // extract
                    ItemData currentData = this.extractMetadata(path);

                    System.out.println(String.format("\nFile \"%s\"", currentData.filename));

                    // get destination file
                    try {
                        currentData.targetPath = this.getCopyDestination(currentData, destination);
                    } catch (IOException e) {
                        // failed to build target path, store in invalid_date folder
                        Path invalid = destination.resolve("invalid_date");
                        if (!Files.exists(invalid)) Files.createDirectory(invalid);
                        currentData.targetPath = invalid;
                    }




                    // check if new file exists already
                    if(Files.exists(currentData.targetPath)) {
                        // check if duplicate
                        ItemData duplicateData = this.extractMetadata(currentData.targetPath);

                        if(duplicateData.equals(currentData)) {
                            System.err.print("  Ignoring same file: "+currentData.name);
                        } else {
                            System.out.print("  Duplicate (same date) detected ");
                            /*
                            // rename duplicate, add counter



                            // check if target is already part of series

                                Path newFileOfDuplicate = this.applyCounter(duplicateData, 0);
                                Files.move(duplicateData.path, newFileOfDuplicate);

                                targetPath = this.applyCounter(currentData, 1);
                                this.copyFile(currentData, targetPath);

                            } else {
                                this.applyCounter(currentData, Integer.parseInt(duplicateData.counter)+1);
                            }*/

                        }
                    } else {
                        this.copyFile(currentData);
                        
                    }


                } catch (IOException e) {
                    e.printStackTrace();

                } catch (ImageProcessingException e) {
                    System.out.print("Not an image:"+path);
                    // e.printStackTrace();
                }


            });
            //...

        }

    }

    // creates a copy of the current file in the destination. Writes custom attributes with old image data into new file
    private void copyFile(ItemData current) throws IOException {

        Path copiedFile = Files.copy(current.path, current.targetPath);

        UserDefinedFileAttributeView customAttributes = Files.getFileAttributeView(copiedFile, UserDefinedFileAttributeView.class);
        this.writeCustomAttribute(customAttributes, "bildersortierautomat.counter", current.counter);
        this.writeCustomAttribute(customAttributes, "bildersortierautomat.old.FileName", current.filename);
        this.writeCustomAttribute(customAttributes, "bildersortierautomat.old.Path", current.path.getParent().toString());


    }

    private boolean ignorePath(Path path) {

        if (!Files.isRegularFile(path)) return true;

        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            FileType fileType = FileTypeDetector.detectFileType(bis);
            if(fileType == FileType.Unknown) return true;
        } catch(IOException ex) {
            return true;
        }

        return false;

    }


    /**
     * Appends the counter to filename
     * @param id
     * @param i
     *
    private Path applyCounter(Path p, int i) throws IOException {



        String newName = String.format("%s %s.%s", id.name, this.addPrefix(i), id.extension);
        Path target = p.getParent().resolve(newName);


        UserDefinedFileAttributeView customAttributes = Files.getFileAttributeView(id.path, UserDefinedFileAttributeView.class);
        this.writeCustomAttribute(customAttributes, "com.q14.bildersortierautomat.counter", i+"");
        return target;
    }
    */
    private Path getCopyDestination(ItemData id, Path destinationParent) throws IOException {
        try {
            Path year = destinationParent.resolve(id.dateTaken.getYear() + "");
            if (!Files.exists(year)) Files.createDirectory(year);


            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%d-%s-%s %s-%s-%s", id.dateTaken.getYear(), this.addPrefix(id.dateTaken.getMonthValue()),
                    this.addPrefix(id.dateTaken.getDayOfMonth()),
                    this.addPrefix(id.dateTaken.getHour()),
                    this.addPrefix(id.dateTaken.getMinute()),
                    this.addPrefix(id.dateTaken.getSecond())
            ));

            if(!id.counter.isBlank()) sb.append(" ").append(id.counter);

            sb.append(".").append(id.extension);


            return year.resolve(sb.toString());
        } catch (Exception e) {
            throw new IOException(e);
        }

    }

    private String addPrefix(Integer intValue) {
        if (intValue >= 10) return intValue.toString();
        else return "0" + intValue.toString();
    }

    private ItemData extractMetadata(Path path) throws IOException, ImageProcessingException {

        Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());


        // ImagePlus img = IJ.openImage(path.toString());
        ItemData id = new ItemData();
        id.path = path;

        // file attributes
        BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
        id.creationTime = LocalDateTime.ofInstant(basicFileAttributes.creationTime().toInstant(), ZoneId.systemDefault());
        id.modifiedTime = LocalDateTime.ofInstant(basicFileAttributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());




        id.filename = path.getFileName().toString();
        id.name = id.filename.substring(0, id.filename.lastIndexOf('.'));
        id.extension = id.filename.substring(id.filename.lastIndexOf('.')+1);
        id.size = basicFileAttributes.size();


        // custom metadata
        UserDefinedFileAttributeView customAttributes = Files.getFileAttributeView(id.path, UserDefinedFileAttributeView.class);

        id.oldFileName = readCustomAttribute(customAttributes, "bildersortierautomat.old.FileName");
        id.oldPath = readCustomAttribute(customAttributes, "bildersortierautomat.old.Path");
        id.counter = readCustomAttribute(customAttributes, "bildersortierautomat.counter");



        // exif
        try {
            ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class));
            id.imageHeight = descriptor.getExifImageHeightDescription();
            id.imageWidth = descriptor.getExifImageWidthDescription();


            Date dateTaken = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
            id.dateTaken = LocalDateTime.ofInstant(dateTaken.toInstant(), ZoneId.systemDefault());


        } catch (NullPointerException np) {
            this.filesWithExceptions.add(path);
        }

        return id;


    }

    private String readCustomAttribute(UserDefinedFileAttributeView customAttributesView, String key) throws IOException {
        if(customAttributesView.list().contains(key)) {
            ByteBuffer readBuffer = ByteBuffer.allocate(customAttributesView.size(key));
            customAttributesView.read(key, readBuffer);
            readBuffer.flip();
            return new String(readBuffer.array(), StandardCharsets.UTF_8);
        }
        else return "";


    }

    private void writeCustomAttribute(UserDefinedFileAttributeView customAttributesView, String key, String value) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
        writeBuffer.put(bytes);
        writeBuffer.flip();
        customAttributesView.write(key, writeBuffer);
    }
}
