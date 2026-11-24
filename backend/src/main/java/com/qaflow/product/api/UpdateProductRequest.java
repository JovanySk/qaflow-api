package com.qaflow.product.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UpdateProductRequest {

    private String version;
    private String sku;
    private String name;
    private String description;
    private Boolean active;
    private boolean skuPresent;
    private boolean namePresent;
    private boolean descriptionPresent;
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

    @Size(max = 64, message = "El SKU no puede exceder 64 caracteres.")
    @Pattern(regexp = ".*\\S.*", message = "El SKU no puede estar vacío.")
    public String getSku() {
        return sku;
    }

    @JsonSetter(value = "sku", nulls = Nulls.FAIL)
    public void setSku(String sku) {
        this.skuPresent = true;
        this.sku = sku == null ? null : sku.trim();
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

    public Boolean getActive() {
        return active;
    }

    @JsonSetter(value = "active", nulls = Nulls.FAIL)
    public void setActive(Boolean active) {
        this.activePresent = true;
        this.active = active;
    }

    @JsonIgnore
    public boolean hasSku() {
        return skuPresent;
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
    public boolean hasActive() {
        return activePresent;
    }

    @JsonIgnore
    @AssertTrue(message = "Debe enviarse al menos uno de sku, name, description o active.")
    public boolean isChangeRequested() {
        return skuPresent || namePresent || descriptionPresent || activePresent;
    }
}
