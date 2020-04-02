package net.q14.commands;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import net.q14.store.ItemData;
import net.q14.store.ItemsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@ShellComponent
public class Exif {

    @Value("${folder.source}")
    public String source;

    @Autowired
    ItemsRepository itemStore;

    private List<Path> filesWithExceptions;

    private long counter = 0;

    @ShellMethod(key = "exif", group = "scan", value = "Extracting EXIF information from images")
    public String acquireExifProperties() {
        for (ItemData itemData : itemStore.findAll()) {

            Path path = Paths.get(this.source, itemData.relativeSourcePath, itemData.name);



            if(itemData.imageWidth.length() == 0) {
                this.analyze(path, itemData);
                itemStore.save(itemData);
            }

            counter++;
            if(counter%100 == 0) System.out.print(".");
            if(counter%10000 == 0) System.out.print("\n");
        }
        return "Done!";

    }

    private void analyze(Path path, ItemData id) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());

            ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class));
            id.imageHeight = descriptor.getExifImageHeightDescription();
            id.imageWidth = descriptor.getExifImageWidthDescription();


            Date dateTaken = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
            id.dateTaken = LocalDateTime.ofInstant(dateTaken.toInstant(), ZoneId.systemDefault());

        } catch (NullPointerException np) {
            this.filesWithExceptions.add(path);
        } catch (IOException | ImageProcessingException e) {
            e.printStackTrace();
        }

    }
}
