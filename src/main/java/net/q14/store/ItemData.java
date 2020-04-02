package net.q14.store;

import com.sun.scenario.effect.ImageData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.awt.*;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemData {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    public LocalDateTime lastUpdate;
    public String name;
    public LocalDateTime creationTime;
    public LocalDateTime modifiedTime;
    public LocalDateTime dateTaken;
    public long size;
    public String imageHeight;
    public String imageWidth;
    public String extension;
    public String relativeSourcePath;
    public String relativeTargetPath;

    public String counter = "0";
    public String filename;
    public String oldFileName;
    public String oldPath;

    public int hashCode;


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.append(sb, "Name", this.name);
        this.append(sb, "Date (Update)", String.valueOf(this.modifiedTime));
        this.append(sb, "Date (Create)", String.valueOf(this.creationTime));
        this.append(sb, "Date (Taken )", String.valueOf(this.dateTaken));
        this.append(sb, "File Size", String.valueOf(this.size));
        this.append(sb, "Image Width", this.imageWidth);
        this.append(sb, "Image Height", this.imageHeight);
        return sb.toString();
    }

    private void append(StringBuilder sb, String key, String value) {
        if((value != null) && (!value.isBlank())) {
            sb.append(key).append(": ").append(value).append('\n');
        }
    }

    public void print2Console() {
        System.out.println(this.toString());

    }

    public boolean equalDimensions(Object obj) {
        if(obj instanceof ItemData) {
            ItemData oid = (ItemData) obj;

            boolean result = false;

            /*
            result = oid.dateTaken.equals((this.dateTaken));
            result = result && oid.size == this.size;
            result = oid.im
            */

            return oid.dateTaken.equals(this.dateTaken) &&
                    oid.size == this.size &&
                    (oid.imageHeight != null && this.imageHeight != null) &&
                    oid.imageHeight.equalsIgnoreCase(this.imageHeight) &&
                    (oid.imageWidth != null && this.imageWidth != null) &&
                    oid.imageWidth.equalsIgnoreCase(this.imageWidth);
        }

        return false;
    }

    /*
        Hashcode consists of filesize and creation date
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.creationTime, this.size);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ImageData && this.hashCode() == obj.hashCode();
    }
}

