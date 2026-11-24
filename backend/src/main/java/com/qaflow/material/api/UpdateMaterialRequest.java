package com.qaflow.material.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.qaflow.material.domain.UnitOfMeasure;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UpdateMaterialRequest {

    private String version;
    private String code;
    private String name;
    private String description;
    private UnitOfMeasure unitOfMeasure;
    private Boolean active;
    private boolean codePresent;
    private boolean namePresent;
    private boolean descriptionPresent;
    private boolean unitOfMeasurePresent;
    private boolean activePresent;

    @NotBlank(message = "La versión es obligatoria.")
    @Pattern(regexp = "^[0-9]+$", message = "La versión debe ser un entero no negativo.")
    public String getVersion() {
        return version;
    }

    @JsonSetter(value = "version", nulls = Nulls.FAIL)
    public void setVersion(String version) {
        this.version = version == null ? null : version.trim();
    }

    @Size(max = 64, message = "El código no puede exceder 64 caracteres.")
    @Pattern(regexp = ".*\\S.*", message = "El código no puede estar vacío.")
    public String getCode() {
        return code;
    }

    @JsonSetter(value = "code", nulls = Nulls.FAIL)
    public void setCode(String code) {
        this.codePresent = true;
        this.code = code == null ? null : code.trim();
    }

    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres.")
    @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío.")
    public String getName() {
        return name;
    }

    @JsonSetter(value = "name", nulls = Nulls.FAIL)
    public void setName(String name) {
        this.namePresent = true;
        this.name = name == null ? null : name.trim();
    }

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres.")
    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.descriptionPresent = true;
        this.description = description == null ? null : description.trim();
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    @JsonSetter(value = "unitOfMeasure", nulls = Nulls.FAIL)
    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasurePresent = true;
        this.unitOfMeasure = unitOfMeasure;
    }

    public Boolean getActive() {
        return active;
    }

    @JsonSetter(value = "active", nulls = Nulls.FAIL)
    public void setActive(Boolean active) {
        this.activePresent = true;
        this.active = active;
    }

    @JsonIgnore
    public boolean hasCode() {
        return codePresent;
    }

    @JsonIgnore
    public boolean hasName() {
        return namePresent;
    }

    @JsonIgnore
    public boolean hasDescription() {
        return descriptionPresent;
    }

    @JsonIgnore
    public boolean hasUnitOfMeasure() {
        return unitOfMeasurePresent;
    }

    @JsonIgnore
    public boolean hasActive() {
        return activePresent;
    }

    @JsonIgnore
    @AssertTrue(message = "Debe enviarse al menos uno de code, name, description, unitOfMeasure o active.")
    public boolean isChangeRequested() {
        return codePresent || namePresent || descriptionPresent || unitOfMeasurePresent || activePresent;
    }
}
