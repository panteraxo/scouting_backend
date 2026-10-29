package com.scouting.scouting.config;

import com.scouting.scouting.user.entity.DocumentType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del bootstrap del primer administrador. Se enlazan desde variables de
 * entorno locales ({@code BOOTSTRAP_ADMIN_*}) y no tienen valores por defecto en Git.
 */
@ConfigurationProperties(prefix = "bootstrap.admin")
public class BootstrapAdminProperties {

    private boolean enabled = true;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String username;
    private DocumentType documentType;
    private String documentNumber;
    private String phone;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
