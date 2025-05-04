package org.factelect.controller;

// ==== 2. Controller (Reactive) ====
// src/main/java/com/empresa/controller/FacturaController.java

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.factelect.dto.DocumentoRequest;
import org.factelect.service.DocumentoService;

@Path("/api/documentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DcoumentController {

    @Inject
    DocumentoService documentoService;

    @POST
    @Path("/emitir")
    public Uni<Response> emitirFactura(DocumentoRequest request) {
        return documentoService.emitirFactura(request)
                .onItem().transform(resp -> Response.ok(resp).build())
                .onFailure().recoverWithItem(err -> Response.serverError().entity(err.getMessage()).build());
    }
}