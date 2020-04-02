package net.q14.commands;

import net.q14.store.CsvHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;

@ShellComponent
public class Admin {

    @Autowired
    CsvHandler itemStore;

    @ShellMethod(key="reload", value = "Reloads all items from the sheet")
    public String run() {
        try {
            this.itemStore.loadFile();
            return "Done!";
        } catch (IOException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }

    }

}
