package org.factelect.service;

// ==== 3. Service ====
// src/main/java/com/empresa/service/FacturaService.java

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.factelect.builder.XmlBuilder;
import org.factelect.dto.DocumentoRequest;
import org.factelect.proxy.SunatProxy;

import java.io.IOException;

@ApplicationScoped
public class DocumentoService {

    @Inject
    XmlBuilder xmlBuilder;

    @Inject
    SunatProxy sunatProxy;

    public Uni<String> emitirFactura(DocumentoRequest request) {
        return Uni.createFrom().item(() -> {
                    try {
                        return xmlBuilder.buildInvoiceXml(request);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(xml -> sunatProxy.enviarXmlASunat(xml, request.cabecera.serieNumero));
    }
}
