package net.q14.commands;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import net.q14.store.ItemData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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

/**
 *
 * Command:
 * > bsa analyze duplicates
 *
 * Loads the CSV file
 *
 * All items are copied to the duplicates folder for manual check.
 *
 * - items with same hash and same size -> moved to "exaxt" folder.
 *      You can probably delete them all, we keep the newest image.
 * - items with same hash and nearly same size -> moved to "nearSize" folder.
 *      If you delete them all, we keep the newer (last update) image af identified duplicates (or randomly select one)
 *      We keep any image left in the folder (but delete anything else besides the identified original)
 * - items with same hash and nearly same hash -> moved to "nearHash" folder
 *
 *
 *
 */
@ShellComponent
public class MigrateDuplicates {
    @Value("${folder.destination}")
    public String destination;

    @Value("${folder.source}")
    public String source;



    // Key bit resolution
    private int keyLength = 64;

    // Pick an algorithm
    private HashingAlgorithm hasher = new AverageHash(keyLength);


    List<Path> filesWithExceptions = new ArrayList<>();
    long counter = 0;

    @ShellMethod("Add two integers together.")
    public int add(int a, int b) {
        return a + b;
    }

    @ShellMethod(value="Lists all identified duplicates", group="show", key="duplicates")
    private void listDuplicates() throws IOException {
        Path source = Paths.get(this.source);
        Path destination = Paths.get(this.destination);

        Writer out = new FileWriter(this.destination, false);

        CSVPrinter printer = CSVFormat.DEFAULT.withHeader(Headers.class).print(out);

        try (Stream<Path> walk = Files.walk(Paths.get(this.source))) {

            walk.limit(Long.MAX_VALUE).forEach(path -> {
                if(ignorePath(path)) return;
                counter++;
                if(counter%10 == 0) System.out.print(".");
                if(counter%1000 == 0) System.out.print("\n");

                try {
                    // name, path, created, modified, taken, size, width, height



                    // extract
                    ItemData currentData = this.extractMetadata(path);
                    currentData = this.generateHash(path, currentData);

                    List<Object> values = new ArrayList();
                    values.add(currentData.filename);
                    values.add(source.relativize(currentData.path.getParent()));
                    values.add(currentData.creationTime);
                    values.add(currentData.modifiedTime);
                    values.add(currentData.dateTaken);
                    values.add(currentData.size);
                    values.add(currentData.imageWidth);
                    values.add(currentData.imageHeight);
                    values.add(currentData.hash);

                    printer.printRecord(values);

                } catch (ImageProcessingException | IOException e) {
                    e.printStackTrace();
                }
            });
            printer.close(true);
            out.flush();
            out.close();
            //...

        }

    }

    private BufferedImage loadImage(Path path) throws IOException {
        return ImageIO.read(path.toFile());
    }

    private boolean ignorePath(Path path) {

        if (!Files.isRegularFile(path)) return true;
        if(path.getParent().getFileName().startsWith(".")) return true;

        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            FileType fileType = FileTypeDetector.detectFileType(bis);
            if(fileType == FileType.Unknown) return true;
        } catch(IOException ex) {
            return true;
        }

        return false;

    }
    
    private ItemData generateHash(Path path, ItemData ida) throws IOException {
        Hash hash = hasher.hash(path.toFile());
        ida.hash = hash.getHashValue().toString();
        return ida;

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

        // generate hash

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


}
