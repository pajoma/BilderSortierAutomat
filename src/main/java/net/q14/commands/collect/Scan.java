package net.q14.commands.collect;


import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import lombok.extern.java.Log;
import net.q14.store.ItemData;
import net.q14.store.ItemsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;

/*
    Collects all files and stores them into DB
 */
@ShellComponent
@Log
public class Scan {
    @Value("${folder.source}")
    public String source;

    @Autowired
    ItemsRepository itemStore;

    private long counter = 0;

    @ShellMethod(key = "scan", value="Scans the directory", group = "collect")
    public void readDirectory() {

        // check if this file already exists, otherwise ignore




        try (Stream<Path> walk = Files.walk(Paths.get(this.source))) {

            walk.limit(Long.MAX_VALUE).forEach(path -> {
                if(ignorePath(path)) return;







                try {
                    ItemData currentData = this.extractMetadata(path);
                    if(itemStore.findByHashCode(Objects.hash(currentData.creationTime, currentData.size)) != null) {
                        // we ignore this one
                    } else {

                        this.itemStore.save(currentData);

                    }

                    counter++;
                    if(counter%10 == 0) System.out.print(".");
                    if(counter%1000 == 0) System.out.print("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
            log.info(String.format("Found %d items and stored them into the database. Don't forget to persist before exit.", counter));
        } catch (IOException e) {
            e.printStackTrace();
        }


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


    private ItemData extractMetadata(Path path) throws IOException {
    // file attributes
        BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
        String filename = path.getFileName().toString();

        // custom metadata
        UserDefinedFileAttributeView customAttributes = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);

        ItemData build = ItemData.builder()
                .relativeSourcePath(Paths.get(this.source).relativize(path).toString())
                .creationTime(LocalDateTime.ofInstant(basicFileAttributes.creationTime().toInstant(), ZoneId.systemDefault()))
                .modifiedTime(LocalDateTime.ofInstant(basicFileAttributes.lastModifiedTime().toInstant(), ZoneId.systemDefault()))
                .filename(filename)
                .name(filename.substring(0, filename.lastIndexOf('.')))
                .extension(filename.substring(filename.lastIndexOf('.') + 1))
                .size(basicFileAttributes.size())
                .oldFileName(readCustomAttribute(customAttributes, "bildersortierautomat.old.FileName"))
                .oldPath(readCustomAttribute(customAttributes, "bildersortierautomat.old.Path"))
                .counter(readCustomAttribute(customAttributes, "bildersortierautomat.counter"))
                .build();

        build.setHashCode(build.hashCode());
        return build;

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
