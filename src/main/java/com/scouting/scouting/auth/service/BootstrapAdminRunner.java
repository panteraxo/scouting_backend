package com.scouting.scouting.auth.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Ejecuta el bootstrap del administrador al arrancar la aplicacion. */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private final BootstrapAdminService bootstrapAdminService;

    public BootstrapAdminRunner(BootstrapAdminService bootstrapAdminService) {
        this.bootstrapAdminService = bootstrapAdminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapAdminService.ensureAdmin();
    }
}
