package fr.bluechipit.dvdtheque.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Pays {
    private Integer id;
    @NotNull
    private String lib;
    private String i18n;
}
